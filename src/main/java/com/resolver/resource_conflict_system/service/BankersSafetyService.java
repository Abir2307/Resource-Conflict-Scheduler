package com.resolver.resource_conflict_system.service;

import com.resolver.resource_conflict_system.domain.AssetItem;
import com.resolver.resource_conflict_system.domain.AssignmentResult;
import com.resolver.resource_conflict_system.domain.AvailabilitySlot;
import com.resolver.resource_conflict_system.domain.ProjectTask;
import com.resolver.resource_conflict_system.domain.ResourceProfile;
import com.resolver.resource_conflict_system.domain.SafetyAdvice;
import com.resolver.resource_conflict_system.domain.ScheduleReport;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

@Service
public class BankersSafetyService {

    private final SchedulerEngine schedulerEngine;

    public BankersSafetyService(SchedulerEngine schedulerEngine) {
        this.schedulerEngine = schedulerEngine;
    }

    /**
     * Matrix-based shortage estimator: build task request rows and resource capacity
     * columns, then compute the minimal extra capacity needed per skill column.
     */
    public Map<String, Integer> estimateExtraResources(List<ProjectTask> tasks, List<ResourceProfile> resources,
            int workHoursPerResource) {
        MatrixSnapshot snapshot = buildMatrixSnapshot(tasks, resources, List.of(), Set.of());
        return estimateExtraResources(snapshot, workHoursPerResource);
    }

    public SafetyAdvice analyzeSafety(List<ProjectTask> tasks, List<ResourceProfile> resources, List<AssetItem> assets,
            int workHoursPerResource) {
        return analyzeSafety(tasks, resources, assets, workHoursPerResource, null, 0L, List.of());
    }

    public SafetyAdvice analyzeSafety(List<ProjectTask> tasks, List<ResourceProfile> resources, List<AssetItem> assets,
            int workHoursPerResource, java.math.BigDecimal budget) {
        return analyzeSafety(tasks, resources, assets, workHoursPerResource, budget, 0L, List.of());
    }

    public SafetyAdvice analyzeSafety(List<ProjectTask> tasks, List<ResourceProfile> resources, List<AssetItem> assets,
            int workHoursPerResource, java.math.BigDecimal budget, long deliveryEtaHours) {
        return analyzeSafety(tasks, resources, assets, workHoursPerResource, budget, deliveryEtaHours, List.of());
    }

    public SafetyAdvice analyzeSafety(List<ProjectTask> tasks, List<ResourceProfile> resources, List<AssetItem> assets,
            int workHoursPerResource, java.math.BigDecimal budget, long deliveryEtaHours, List<String> requestedSequence) {
        ScheduleReport currentReport = schedulerEngine.simulate(tasks, resources);
        List<AssetItem> deliveryAwareAssets = normalizeAssetsForDelivery(assets, deliveryEtaHours);
        Set<String> allocatedTaskIds = currentReport.assignments().stream()
                .map(AssignmentResult::taskId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        MatrixSnapshot currentSnapshot = buildMatrixSnapshot(tasks, resources, deliveryAwareAssets, allocatedTaskIds);
        boolean currentSafe = currentSnapshot.isSafe();

        Map<String, Integer> extraResourcesBySkill = estimateExtraResources(currentSnapshot, workHoursPerResource);
        Map<String, Integer> extraAssetsById = estimateExtraAssets(currentSnapshot);

        // If a budget is provided, try to pick a subset of suggested augmentations that fits the budget (greedy cheapest-first)
        if (budget != null) {
            java.math.BigDecimal remaining = budget;

            // Build per-unit cost suggestions
            class Suggestion { String kind; String key; java.math.BigDecimal unitCost; }

            List<Suggestion> pool = new ArrayList<>();

            // resource headcount: estimate cost per head = avg salary for skill * workHoursPerResource
            Map<String, java.math.BigDecimal> avgSalaryBySkill = new HashMap<>();
            for (ResourceProfile r : resources) {
                for (String s : r.skills()) {
                    java.math.BigDecimal salary = r.salaryPerHour() == null ? java.math.BigDecimal.ZERO : r.salaryPerHour();
                    avgSalaryBySkill.merge(s, salary, java.math.BigDecimal::add);
                }
            }
            Map<String, Integer> countBySkill = new HashMap<>();
            for (ResourceProfile r : resources) {
                for (String s : r.skills()) {
                    countBySkill.put(s, countBySkill.getOrDefault(s, 0) + 1);
                }
            }
            for (String skill : extraResourcesBySkill.keySet()) {
                int qty = extraResourcesBySkill.get(skill);
                java.math.BigDecimal avg = avgSalaryBySkill.getOrDefault(skill, java.math.BigDecimal.valueOf(50));
                int denom = countBySkill.getOrDefault(skill, 1);
                java.math.BigDecimal unit = avg.divide(java.math.BigDecimal.valueOf(Math.max(1, denom)), java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(workHoursPerResource));
                for (int i = 0; i < qty; i++) {
                    Suggestion s = new Suggestion(); s.kind = "RESOURCE"; s.key = skill; s.unitCost = unit; pool.add(s);
                }
            }

            // assets: split each unit as individual suggestion
            Map<String, AssetItem> byId = deliveryAwareAssets.stream().collect(Collectors.toMap(AssetItem::id, a -> a, (a,b)->a));
            for (Map.Entry<String, Integer> e : extraAssetsById.entrySet()) {
                String assetId = e.getKey();
                int qty = e.getValue();
                java.math.BigDecimal cp = java.math.BigDecimal.valueOf(100);
                AssetItem ai = byId.get(assetId);
                if (ai != null && ai.costPerPiece() != null) cp = ai.costPerPiece();
                for (int i = 0; i < qty; i++) {
                    Suggestion s = new Suggestion(); s.kind = "ASSET"; s.key = assetId; s.unitCost = cp; pool.add(s);
                }
            }

            // sort cheapest first and pick until budget exhausted
            pool.sort(Comparator.comparing(s -> s.unitCost));
            Map<String, Integer> chosenAssets = new LinkedHashMap<>();
            Map<String, Integer> chosenResources = new LinkedHashMap<>();
            for (Suggestion s : pool) {
                if (remaining.compareTo(s.unitCost) >= 0) {
                    remaining = remaining.subtract(s.unitCost);
                    if (s.kind.equals("ASSET")) {
                        chosenAssets.put(s.key, chosenAssets.getOrDefault(s.key, 0) + 1);
                    } else {
                        chosenResources.put(s.key, chosenResources.getOrDefault(s.key, 0) + 1);
                    }
                }
            }

            extraAssetsById = chosenAssets;
            extraResourcesBySkill = chosenResources;
        }

        List<ResourceProfile> augmentedResources = augmentResources(resources, extraResourcesBySkill, tasks,
                workHoursPerResource);
        List<AssetItem> augmentedAssets = augmentAssets(deliveryAwareAssets, extraAssetsById);
        Set<String> augmentedAllocatedTaskIds = schedulerEngine.simulate(tasks, augmentedResources).assignments().stream()
                .map(AssignmentResult::taskId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        MatrixSnapshot augmentedSnapshot = buildMatrixSnapshot(tasks, augmentedResources, augmentedAssets,
                augmentedAllocatedTaskIds);
        boolean safeAfterSuggestedChanges = augmentedSnapshot.isSafe();

        List<String> proposedSequence = normalizeSequence(requestedSequence);
        boolean requestedSequenceSafe = !proposedSequence.isEmpty()
            && currentSnapshot.isSafeRoute(proposedSequence);
        List<String> safeRoute = safeAfterSuggestedChanges
            ? (requestedSequenceSafe ? proposedSequence : buildSafeRoute(augmentedSnapshot))
            : List.of();
        String summary;
        if (!proposedSequence.isEmpty()) {
            if (requestedSequenceSafe) {
                summary = currentSafe
                        ? "The entered sequence is safe for the current catalog."
                        : "The entered sequence becomes safe after the suggested resource and asset adjustments.";
            } else if (safeAfterSuggestedChanges) {
                summary = "The entered sequence is not safe; the suggested safe route is shown instead.";
            } else {
                summary = "The entered sequence is not safe and the current heuristic still cannot make it safe.";
            }
        } else if (currentSafe) {
            summary = "The current catalog already has a safe route.";
        } else if (safeAfterSuggestedChanges) {
            summary = "Add the suggested resources and asset top-ups to obtain a safe route.";
        } else {
            summary = "No safe route was found with the current heuristic; add more capacity and re-run the simulation.";
        }

        return new SafetyAdvice(currentSafe, safeAfterSuggestedChanges, safeRoute, proposedSequence, requestedSequenceSafe,
            extraResourcesBySkill, extraAssetsById, summary);
    }

    private List<AssetItem> normalizeAssetsForDelivery(List<AssetItem> assets, long deliveryEtaHours) {
        if (deliveryEtaHours <= 0) {
            return assets;
        }
        List<AssetItem> normalized = new ArrayList<>(assets.size());
        for (AssetItem asset : assets) {
            long eta = asset.estimatedArrivalHours() <= 0 ? deliveryEtaHours : asset.estimatedArrivalHours();
            normalized.add(new AssetItem(asset.id(), asset.name(), asset.category(), asset.quantity(), asset.unit(), asset.location(), asset.notes(),
                    asset.usedByTaskIds(), asset.dependencyAssetIds(), asset.costPerPiece(), eta, asset.supplierNodeId()));
        }
        return normalized;
    }

    /**
     * Allocation-based safety checker: simulate adding virtual hires (one by one)
     * for each skill until the scheduler returns no conflicts/deferred tasks or
     * the per-skill maxExtra limit is reached. Returns a map skill->extra hires required.
     */
    public Map<String, Integer> estimateExtraResourcesBySimulation(List<ProjectTask> tasks,
            List<ResourceProfile> resources, int workHoursPerResource, int maxExtraPerSkill) {
        MatrixSnapshot snapshot = buildMatrixSnapshot(tasks, resources, List.of(), Set.of());
        Map<String, Integer> result = new HashMap<>();
        for (String skill : snapshot.skillColumns()) {
            int extra = 0;
            while (extra <= maxExtraPerSkill) {
                List<ResourceProfile> augmented = augmentResources(resources, Map.of(skill, extra), tasks,
                        workHoursPerResource);
                ScheduleReport report = schedulerEngine.simulate(tasks, augmented);
                MatrixSnapshot augmentedSnapshot = buildMatrixSnapshot(tasks, augmented, List.of(),
                        report.assignments().stream().map(AssignmentResult::taskId)
                                .collect(Collectors.toCollection(LinkedHashSet::new)));
                if (augmentedSnapshot.isSafe()) {
                    result.put(skill, extra);
                    break;
                }
                extra++;
            }
            if (extra > maxExtraPerSkill) {
                result.put(skill, -1); // indicates not solvable within limit
            }
        }
        return result;
    }

    private Map<String, Integer> estimateExtraResources(MatrixSnapshot snapshot, int workHoursPerResource) {
        Map<String, Integer> needed = new LinkedHashMap<>();
        for (String skill : snapshot.skillColumns()) {
            int shortageHours = Math.max(0, snapshot.maxNeed(skill) - snapshot.availableSkillHours().getOrDefault(skill, 0));
            int headcount = (int) Math.ceil((double) shortageHours / (double) workHoursPerResource);
            if (headcount > 0) {
                needed.put(skill, headcount);
            }
        }
        return needed;
    }

    private Map<String, Integer> estimateExtraAssets(MatrixSnapshot snapshot) {
        Map<String, Integer> shortages = new LinkedHashMap<>();
        for (String assetId : snapshot.assetColumns()) {
            int shortage = Math.max(0, snapshot.maxNeedAsset(assetId) - snapshot.availableAssets().getOrDefault(assetId, 0));
            if (shortage > 0) {
                shortages.put(assetId, shortage);
            }
        }
        return shortages;
    }

    private List<ResourceProfile> augmentResources(List<ResourceProfile> resources, Map<String, Integer> extraResourcesBySkill,
            List<ProjectTask> tasks, int workHoursPerResource) {
        List<ResourceProfile> augmented = new ArrayList<>(resources);
        LocalDateTime slotStart = tasks.stream()
                .map(ProjectTask::preferredStart)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(LocalDateTime.now().plusDays(1))
                .withHour(8)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        LocalDateTime slotEnd = slotStart.plusDays(7);

        int sequence = 1;
        Map<String, Integer> orderedSkills = new LinkedHashMap<>(extraResourcesBySkill);
        for (Map.Entry<String, Integer> entry : orderedSkills.entrySet()) {
            String skill = entry.getKey();
            for (int i = 0; i < entry.getValue(); i++) {
                augmented.add(new ResourceProfile("virt-" + skill.toLowerCase() + "-" + sequence,
                    "Suggested " + skill + " " + sequence, Set.of(skill),
                    List.of(new AvailabilitySlot(slotStart, slotEnd)), workHoursPerResource, List.of(), "suggested", new java.math.BigDecimal("50.00"), workHoursPerResource));
                sequence++;
            }
        }
        return augmented;
    }

    private List<AssetItem> augmentAssets(List<AssetItem> assets, Map<String, Integer> extraAssetsById) {
        Map<String, AssetItem> byId = assets.stream().collect(Collectors.toMap(AssetItem::id, asset -> asset, (left, right) -> left));
        List<AssetItem> augmented = new ArrayList<>(assets);
        for (Map.Entry<String, Integer> entry : extraAssetsById.entrySet()) {
            AssetItem current = byId.get(entry.getKey());
            if (current == null) {
                augmented.add(new AssetItem(entry.getKey(), entry.getKey(), com.resolver.resource_conflict_system.domain.AssetCategory.INVENTORY,
                        entry.getValue(), "units", "suggested", "Auto-added to satisfy the matrix", List.of(), List.of(), java.math.BigDecimal.ZERO, 0L, null));
            } else {
                augmented.add(new AssetItem(current.id(), current.name(), current.category(), entry.getValue(), current.unit(),
                        current.location(), current.notes(), List.of(), current.dependencyAssetIds(), current.costPerPiece(), current.estimatedArrivalHours(), current.supplierNodeId()));
            }
        }
        return augmented;
    }

    private List<String> buildSafeRoute(MatrixSnapshot snapshot) {
        return snapshot.safeRoute();
    }

    private List<String> normalizeSequence(List<String> sequence) {
        if (sequence == null) {
            return List.of();
        }
        return sequence.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private MatrixSnapshot buildMatrixSnapshot(List<ProjectTask> tasks, List<ResourceProfile> resources,
            List<AssetItem> assets, Set<String> allocatedTaskIds) {
        List<String> skillColumns = tasks.stream()
                .flatMap(task -> task.requiredSkills().stream())
                .distinct()
                .sorted()
                .toList();
        List<String> assetColumns = tasks.stream()
                .flatMap(task -> task.requiredAssetIds().stream())
                .distinct()
                .sorted()
                .toList();

        Map<String, Integer> availableSkillHours = new LinkedHashMap<>();
        for (String skill : skillColumns) {
            availableSkillHours.put(skill, 0);
        }
        for (ResourceProfile resource : resources) {
            for (String skill : resource.skills()) {
                availableSkillHours.put(skill, availableSkillHours.getOrDefault(skill, 0) + resource.maxWorkloadHours());
            }
        }

        Map<String, Integer> availableAssets = new LinkedHashMap<>();
        for (String assetId : assetColumns) {
            availableAssets.put(assetId, 0);
        }
        for (AssetItem asset : assets) {
            availableAssets.put(asset.id(), availableAssets.getOrDefault(asset.id(), 0) + asset.quantity());
        }

        List<List<Integer>> requestMatrix = new ArrayList<>();
        List<List<Integer>> allocationMatrix = new ArrayList<>();
        List<List<Integer>> needMatrix = new ArrayList<>();
        for (ProjectTask task : tasks) {
            List<Integer> requestRow = new ArrayList<>();
            List<Integer> allocationRow = new ArrayList<>();
            List<Integer> needRow = new ArrayList<>();
            for (String skill : skillColumns) {
                int request = task.requiredSkills().contains(skill) ? (int) task.duration().toHours() : 0;
                int allocation = allocatedTaskIds.contains(task.id()) ? request : 0;
                requestRow.add(request);
                allocationRow.add(allocation);
                needRow.add(Math.max(0, request - allocation));
            }
            for (String assetId : assetColumns) {
                int request = task.requiredAssetIds().contains(assetId) ? 1 : 0;
                int allocation = allocatedTaskIds.contains(task.id()) ? request : 0;
                requestRow.add(request);
                allocationRow.add(allocation);
                needRow.add(Math.max(0, request - allocation));
            }
            requestMatrix.add(requestRow);
            allocationMatrix.add(allocationRow);
            needMatrix.add(needRow);
        }

        return new MatrixSnapshot(tasks, skillColumns, assetColumns, availableSkillHours, availableAssets,
                requestMatrix, allocationMatrix, needMatrix);
    }

    private record MatrixSnapshot(List<ProjectTask> tasks, List<String> skillColumns, List<String> assetColumns,
                                  Map<String, Integer> availableSkillHours, Map<String, Integer> availableAssets,
                                  List<List<Integer>> requestMatrix, List<List<Integer>> allocationMatrix,
                                  List<List<Integer>> needMatrix) {

        private boolean isSafe() {
            return safeRoute().size() == tasks.size();
        }

        private int maxNeed(String skill) {
            int column = skillColumns.indexOf(skill);
            if (column < 0) {
                return 0;
            }
            int max = 0;
            for (List<Integer> row : needMatrix) {
                max = Math.max(max, row.get(column));
            }
            return max;
        }

        private int maxNeedAsset(String assetId) {
            int column = skillColumns.size() + assetColumns.indexOf(assetId);
            if (column < skillColumns.size()) {
                return 0;
            }
            int max = 0;
            for (List<Integer> row : needMatrix) {
                max = Math.max(max, row.get(column));
            }
            return max;
        }

        private List<String> safeRoute() {
            Map<String, ProjectTask> tasksById = tasks.stream().collect(Collectors.toMap(ProjectTask::id, task -> task));
            Map<String, Integer> indegree = new HashMap<>();
            Map<String, List<String>> dependents = new HashMap<>();
            Map<String, Integer> workSkillHours = new LinkedHashMap<>(availableSkillHours);
            Map<String, Integer> workAssets = new LinkedHashMap<>(availableAssets);

            for (int row = 0; row < tasks.size(); row++) {
                List<Integer> allocationRow = allocationMatrix.get(row);
                int index = 0;
                for (String skill : skillColumns) {
                    int updatedSkillHours = workSkillHours.getOrDefault(skill, 0) - allocationRow.get(index++);
                    workSkillHours.put(skill, updatedSkillHours);
                }
                for (String assetId : assetColumns) {
                    int updatedAssetCount = workAssets.getOrDefault(assetId, 0) - allocationRow.get(index++);
                    workAssets.put(assetId, updatedAssetCount);
                }
            }

            for (ProjectTask task : tasks) {
                indegree.put(task.id(), task.dependencyTaskIds().size());
                for (String dependency : task.dependencyTaskIds()) {
                    dependents.computeIfAbsent(dependency, ignored -> new ArrayList<>()).add(task.id());
                }
            }

            PriorityQueue<ProjectTask> ready = new PriorityQueue<>(Comparator
                    .comparingInt(ProjectTask::priority).reversed()
                    .thenComparing(ProjectTask::preferredStart, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(ProjectTask::duration)
                    .thenComparing(ProjectTask::id));

            Set<String> finished = new LinkedHashSet<>();
            List<String> route = new ArrayList<>();

            for (ProjectTask task : tasks) {
                if (indegree.getOrDefault(task.id(), 0) == 0 && canFinish(task, workSkillHours, workAssets)) {
                    ready.add(task);
                }
            }

            while (!ready.isEmpty()) {
                ProjectTask task = ready.poll();
                if (!finished.add(task.id())) {
                    continue;
                }
                route.add(task.title());
                release(task, workSkillHours, workAssets);

                for (String dependent : dependents.getOrDefault(task.id(), List.of())) {
                    int next = indegree.getOrDefault(dependent, 0) - 1;
                    indegree.put(dependent, next);
                    ProjectTask nextTask = tasksById.get(dependent);
                    if (next == 0 && nextTask != null && canFinish(nextTask, workSkillHours, workAssets)) {
                        ready.add(nextTask);
                    }
                }

                for (ProjectTask candidate : tasks) {
                    if (!finished.contains(candidate.id())
                            && indegree.getOrDefault(candidate.id(), 0) == 0
                            && canFinish(candidate, workSkillHours, workAssets)) {
                        ready.add(candidate);
                    }
                }
            }

            return route;
        }

        private boolean isSafeRoute(List<String> proposedRoute) {
            if (proposedRoute.size() != tasks.size()) {
                return false;
            }
            Map<String, ProjectTask> tasksById = tasks.stream().collect(Collectors.toMap(ProjectTask::id, task -> task));
            Map<String, List<ProjectTask>> tasksByTitle = tasks.stream().collect(Collectors.groupingBy(task -> task.title().toLowerCase()));
            Map<String, Integer> indegree = new HashMap<>();
            Map<String, List<String>> dependents = new HashMap<>();
            Map<String, Integer> workSkillHours = new LinkedHashMap<>(availableSkillHours);
            Map<String, Integer> workAssets = new LinkedHashMap<>(availableAssets);

            for (int row = 0; row < tasks.size(); row++) {
                List<Integer> allocationRow = allocationMatrix.get(row);
                int index = 0;
                for (String skill : skillColumns) {
                    workSkillHours.put(skill, workSkillHours.getOrDefault(skill, 0) - allocationRow.get(index++));
                }
                for (String assetId : assetColumns) {
                    workAssets.put(assetId, workAssets.getOrDefault(assetId, 0) - allocationRow.get(index++));
                }
            }

            for (ProjectTask task : tasks) {
                indegree.put(task.id(), task.dependencyTaskIds().size());
                for (String dependency : task.dependencyTaskIds()) {
                    dependents.computeIfAbsent(dependency, ignored -> new ArrayList<>()).add(task.id());
                }
            }

            Set<String> finished = new LinkedHashSet<>();
            for (String token : proposedRoute) {
                ProjectTask task = resolveTaskToken(token, tasksById, tasksByTitle);
                if (task == null || !finished.add(task.id())) {
                    return false;
                }
                if (indegree.getOrDefault(task.id(), 0) > 0 || !canFinish(task, workSkillHours, workAssets)) {
                    return false;
                }
                release(task, workSkillHours, workAssets);
                for (String dependent : dependents.getOrDefault(task.id(), List.of())) {
                    indegree.put(dependent, indegree.getOrDefault(dependent, 0) - 1);
                }
            }
            return finished.size() == tasks.size();
        }

        private ProjectTask resolveTaskToken(String token, Map<String, ProjectTask> tasksById,
                Map<String, List<ProjectTask>> tasksByTitle) {
            ProjectTask byId = tasksById.get(token);
            if (byId != null) {
                return byId;
            }
            List<ProjectTask> byTitle = tasksByTitle.get(token.toLowerCase());
            if (byTitle == null || byTitle.size() != 1) {
                return null;
            }
            return byTitle.get(0);
        }

        private boolean canFinish(ProjectTask task, Map<String, Integer> workSkillHours, Map<String, Integer> workAssets) {
            int row = tasks.indexOf(task);
            if (row < 0) {
                return false;
            }
            List<Integer> needRow = needMatrix.get(row);
            int index = 0;
            for (String skill : skillColumns) {
                if (needRow.get(index++) > workSkillHours.getOrDefault(skill, 0)) {
                    return false;
                }
            }
            for (String assetId : assetColumns) {
                if (needRow.get(index++) > workAssets.getOrDefault(assetId, 0)) {
                    return false;
                }
            }
            return true;
        }

        private void release(ProjectTask task, Map<String, Integer> workSkillHours, Map<String, Integer> workAssets) {
            int row = tasks.indexOf(task);
            if (row < 0) {
                return;
            }
            List<Integer> allocationRow = allocationMatrix.get(row);
            int index = 0;
            for (String skill : skillColumns) {
                int updatedSkillHours = workSkillHours.getOrDefault(skill, 0) + allocationRow.get(index++);
                workSkillHours.put(skill, updatedSkillHours);
            }
            for (String assetId : assetColumns) {
                int updatedAssetCount = workAssets.getOrDefault(assetId, 0) + allocationRow.get(index++);
                workAssets.put(assetId, updatedAssetCount);
            }
        }
    }
}
