package com.resolver.resource_conflict_system.service;

import com.resolver.resource_conflict_system.domain.AssignmentResult;
import com.resolver.resource_conflict_system.domain.ConflictEvent;
import com.resolver.resource_conflict_system.domain.ConflictType;
import com.resolver.resource_conflict_system.domain.ProjectTask;
import com.resolver.resource_conflict_system.domain.ResourceProfile;
import com.resolver.resource_conflict_system.domain.ScheduleReport;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class SchedulerEngine {

	private final ConflictResolutionEngine conflictResolutionEngine;

	public SchedulerEngine() {
		this.conflictResolutionEngine = new ConflictResolutionEngine();
	}

	public ScheduleReport simulate(List<ProjectTask> tasks, List<ResourceProfile> resources) {
		List<ProjectTask> orderedTasks = tasks.stream()
				.sorted(Comparator.comparingInt(ProjectTask::priority).reversed()
						.thenComparing(ProjectTask::preferredStart, Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparing(ProjectTask::duration))
				.toList();

		List<MutableResourceState> resourceStates = resources.stream().map(MutableResourceState::new).toList();
		List<AssignmentResult> assignments = new ArrayList<>();
		List<ConflictEvent> conflicts = new ArrayList<>();
		List<ProjectTask> deferred = new ArrayList<>();
		Set<String> completedTaskIds = new HashSet<>();

		for (ProjectTask task : orderedTasks) {
			if (!dependenciesReady(task, completedTaskIds)) {
				deferred.add(task);
				conflicts.add(conflictResolutionEngine.buildConflict(task, null, ConflictType.DEPENDENCY_BLOCKED,
						conflictResolutionEngine.summarizeConflict(task, "is waiting on dependencies"),
						"Deferred until prerequisite tasks are completed"));
				continue;
			}

			Optional<AssignmentCandidate> assigned = allocateTask(task, resourceStates, false);
			if (assigned.isEmpty()) {
				assigned = allocateTask(task, resourceStates, true);
			}

			if (assigned.isPresent()) {
				AssignmentCandidate candidate = assigned.get();
				assignments.add(candidate.assignment());
				completedTaskIds.add(task.id());
				continue;
			}

			ConflictEvent conflict = buildFinalConflict(task, resourceStates);
			conflicts.add(conflict);
			deferred.add(task);
		}

		for (ProjectTask task : new ArrayList<>(deferred)) {
			if (!dependenciesReady(task, completedTaskIds)) {
				continue;
			}
			Optional<AssignmentCandidate> assigned = allocateTask(task, resourceStates, true);
			if (assigned.isPresent()) {
				AssignmentCandidate candidate = assigned.get();
				assignments.add(candidate.assignment());
				completedTaskIds.add(task.id());
				deferred.remove(task);
				conflicts.removeIf(event -> event.taskId().equals(task.id())
						&& event.type() == ConflictType.DEPENDENCY_BLOCKED);
			}
		}

		String summary = "Scheduled " + assignments.size() + " tasks with " + conflicts.size()
				+ " conflicts and " + deferred.size() + " deferred tasks.";
		return new ScheduleReport(assignments, conflicts, deferred, LocalDateTime.now(), summary);
	}

	private Optional<AssignmentCandidate> allocateTask(ProjectTask task, List<MutableResourceState> resourceStates,
			boolean allowDelay) {
		List<AssignmentCandidate> candidates = new ArrayList<>();
		for (MutableResourceState resourceState : resourceStates) {
			resourceState.findBestCandidate(task, allowDelay).ifPresent(candidates::add);
		}

		return candidates.stream().min(Comparator.comparingInt(AssignmentCandidate::score)
				.thenComparing(candidate -> candidate.assignment().end())
				.thenComparing(candidate -> candidate.assignment().resourceName()));
	}

	private ConflictEvent buildFinalConflict(ProjectTask task, List<MutableResourceState> resourceStates) {
		boolean skillMatchExists = resourceStates.stream().anyMatch(resource -> resource.supports(task.requiredSkills()));
		ConflictType type = skillMatchExists ? ConflictType.NO_FEASIBLE_RESOURCE : ConflictType.SKILL_MISMATCH;
		String reason = skillMatchExists
				? "all matching resources are either overloaded or blocked by availability windows"
				: "no resource in the current pool satisfies the required skills";
			String resolution = skillMatchExists ? "Escalate to admin or rebalance existing workload"
				: "Trigger skill-gap escalation or create a staffing request";
		return conflictResolutionEngine.buildConflict(task, null, type,
				conflictResolutionEngine.summarizeConflict(task, reason), resolution);
	}

	private boolean dependenciesReady(ProjectTask task, Set<String> completedTaskIds) {
		return completedTaskIds.containsAll(task.dependencyTaskIds());
	}

	private static final class MutableResourceState {

		private final ResourceProfile baseProfile;
		private final List<AssignmentResult> assignments = new ArrayList<>();

		private MutableResourceState(ResourceProfile resource) {
			this.baseProfile = resource;
			this.assignments.addAll(resource.assignments());
		}

		private boolean supports(Set<String> requiredSkills) {
			return baseProfile.supports(requiredSkills);
		}

		private Optional<AssignmentCandidate> findBestCandidate(ProjectTask task, boolean allowDelay) {
			if (!supports(task.requiredSkills())) {
				return Optional.empty();
			}

			LocalDateTime preferredStart = task.preferredStart() != null ? task.preferredStart() : LocalDateTime.now();
			int currentLoad = assignedHours();
			if (currentLoad + task.duration().toHours() > baseProfile.maxWorkloadHours()) {
				return Optional.empty();
			}

			AssignmentCandidate bestCandidate = null;
			for (var slot : baseProfile.availabilitySlots()) {
				LocalDateTime candidateStart = preferredStart.isAfter(slot.start()) ? preferredStart : slot.start();
				while (!candidateStart.plus(task.duration()).isAfter(slot.end())) {
					LocalDateTime candidateEnd = candidateStart.plus(task.duration());
					if (!hasOverlap(candidateStart, candidateEnd) && slot.contains(candidateStart, candidateEnd)) {
						int score = score(task, candidateStart, currentLoad);
						AssignmentResult assignment = new AssignmentResult(task.id(), baseProfile.id(), task.title(),
								baseProfile.name(), candidateStart, candidateEnd, score,
								buildRationale(task, candidateStart, score));
						if (bestCandidate == null || score < bestCandidate.score()
								|| (score == bestCandidate.score() && candidateEnd.isBefore(bestCandidate.assignment().end()))) {
							bestCandidate = new AssignmentCandidate(baseProfile.id(), assignment, score);
						}
						if (!allowDelay) {
							assignments.add(bestCandidate.assignment());
							return Optional.of(bestCandidate);
						}
					}
					candidateStart = nextStart(candidateStart);
				}
			}

			if (bestCandidate != null) {
				assignments.add(bestCandidate.assignment());
				return Optional.of(bestCandidate);
			}
			return Optional.empty();
		}

		private int assignedHours() {
			return assignments.stream().mapToInt(result -> (int) result.duration().toHours()).sum();
		}

		private boolean hasOverlap(LocalDateTime candidateStart, LocalDateTime candidateEnd) {
			return assignments.stream().anyMatch(existing -> candidateStart.isBefore(existing.end())
					&& candidateEnd.isAfter(existing.start()));
		}

		private int score(ProjectTask task, LocalDateTime candidateStart, int currentLoad) {
			Duration delay = Duration.between(task.preferredStart() != null ? task.preferredStart() : candidateStart,
					candidateStart);
			int delayPenalty = (int) Math.max(0, delay.toHours()) * 12;
			int workloadPenalty = (currentLoad * 10) / baseProfile.maxWorkloadHours();
			int skillCoverage = baseProfile.skills().size() - task.requiredSkills().size();
			return delayPenalty + workloadPenalty + Math.max(0, skillCoverage);
		}

		private String buildRationale(ProjectTask task, LocalDateTime candidateStart, int score) {
			return "Matched skills " + task.requiredSkills() + ", scheduled at " + candidateStart + " with cost " + score;
		}

		private LocalDateTime nextStart(LocalDateTime candidateStart) {
			return candidateStart.plusHours(1);
		}
	}

	private record AssignmentCandidate(String resourceId, AssignmentResult assignment, int score) {
	}
}