package com.resolver.resource_conflict_system.web;

import com.resolver.resource_conflict_system.domain.ProjectTask;
import com.resolver.resource_conflict_system.domain.ScheduleReport;
import com.resolver.resource_conflict_system.domain.SafetyAdvice;
import com.resolver.resource_conflict_system.service.AssetCatalogService;
import com.resolver.resource_conflict_system.repository.UserRepository;
import com.resolver.resource_conflict_system.service.BankersSafetyService;
import com.resolver.resource_conflict_system.service.ResourceCatalogService;
import com.resolver.resource_conflict_system.service.SchedulerEngine;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class TaskViewController {

    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final ResourceCatalogService resourceCatalogService;
    private final AssetCatalogService assetCatalogService;
    private final SchedulerEngine schedulerEngine;
    private final BankersSafetyService bankersSafetyService;
    private final UserRepository userRepository;

    public TaskViewController(ResourceCatalogService resourceCatalogService, AssetCatalogService assetCatalogService,
                              SchedulerEngine schedulerEngine, BankersSafetyService bankersSafetyService, UserRepository userRepository) {
        this.resourceCatalogService = resourceCatalogService;
        this.assetCatalogService = assetCatalogService;
        this.schedulerEngine = schedulerEngine;
        this.bankersSafetyService = bankersSafetyService;
        this.userRepository = userRepository;
    }

    @GetMapping("/tasks")
    public String tasksPage(Model model, Authentication authentication, @org.springframework.web.bind.annotation.RequestParam(required = false) String saved) {
        String currentUsername = authentication == null ? "" : authentication.getName();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        List<ProjectTask> allTasks = resourceCatalogService.snapshotTasks();
        List<ProjectTask> visibleTasks = isAdmin
            ? allTasks
            : allTasks.stream()
                .filter(task -> task.assigneeUsernames().contains(currentUsername) && task.approved())
                .toList();
        model.addAttribute("tasks", visibleTasks);
        model.addAttribute("taskStatuses", currentUsername.isBlank() ? Map.of() : resourceCatalogService.findTaskStatusesForUser(currentUsername));
        model.addAttribute("currentUsername", currentUsername);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("visibleTaskCount", visibleTasks.size());
        model.addAttribute("allResources", resourceCatalogService.snapshotResources());
        if (saved != null && !saved.isBlank()) {
            model.addAttribute("savedMessage", "Task saved as unapproved. Use 'Validate' to run safety check and approve before assignees see it.");
        }
        return "tasks";
    }

    @GetMapping("/tasks/new")
    public String newTask(Model model) {
        model.addAttribute("form", TaskFormData.blank());
        model.addAttribute("action", "/tasks/save");
        model.addAttribute("title", "Add Task");
        // Provide lists for assignee selection and asset selection
        model.addAttribute("allUsers", userRepository.findAll());
        model.addAttribute("allAssets", assetCatalogService.snapshotAssets());
        return "task-form";
    }

    @GetMapping("/tasks/edit/{id}")
    public String editTask(@PathVariable String id, Model model) {
        return resourceCatalogService.findTask(id)
                .map(task -> {
                    model.addAttribute("form", TaskFormData.from(task));
                    model.addAttribute("action", "/tasks/save");
                    model.addAttribute("title", "Edit Task");
                    model.addAttribute("allUsers", userRepository.findAll());
                    model.addAttribute("allAssets", assetCatalogService.snapshotAssets());
                    return "task-form";
                })
                .orElse("redirect:/tasks");
    }

    @PostMapping("/tasks/save")
    public String saveTask(@RequestParam(required = false) String id,
                           @RequestParam String projectId,
                           @RequestParam String title,
                           @RequestParam long durationHours,
                           @RequestParam String requiredSkills,
                           @RequestParam String deadline,
                           @RequestParam(required = false) String assigneeUsernames,
                           @RequestParam(required = false) String requiredAssetIds,
                           @RequestParam int priority,
                           @RequestParam String preferredStart,
                           @RequestParam(required = false) String dependencyTaskIds,
                           @RequestParam String location,
                           Model model,
                           Authentication authentication) {
        Set<String> skills = splitCsv(requiredSkills);
        List<String> dependencies = splitCsvList(dependencyTaskIds);
        List<String> assignees = splitCsvList(assigneeUsernames);
        List<String> assets = splitCsvList(requiredAssetIds);
        LocalDateTime preferredStartAt = LocalDateTime.parse(preferredStart, INPUT_FORMAT);
        LocalDateTime deadlineAt = deadline == null || deadline.isBlank()
                ? preferredStartAt.plusDays(7)
                : LocalDateTime.parse(deadline, INPUT_FORMAT);
        // Save task as unapproved; admin will validate using Banker's algorithm separately
        ProjectTask task = new ProjectTask(id == null ? "pending" : id, projectId, title, Duration.ofHours(durationHours),
            skills, priority, preferredStartAt, deadlineAt, dependencies, assignees, assets, location, false);

        if (task.id().isBlank() || "pending".equalsIgnoreCase(task.id())) {
            resourceCatalogService.addTask(task);
        } else {
            resourceCatalogService.updateTask(task);
        }
        return "redirect:/tasks";
    }

    @PostMapping("/tasks/validate")
    public String validateTask(@RequestParam String id, Model model) {
        // Run safety analysis for the catalog including the task; if safe, mark approved
        List<ProjectTask> current = resourceCatalogService.snapshotTasks();
        List<com.resolver.resource_conflict_system.domain.ResourceProfile> resources = resourceCatalogService.snapshotResources();
        ScheduleReport report = schedulerEngine.simulate(current, resources);
        SafetyAdvice advice = bankersSafetyService.analyzeSafety(current, resources, assetCatalogService.snapshotAssets(), 8);
        if (advice.currentSafe()) {
            resourceCatalogService.findTask(id).ifPresent(t -> {
                ProjectTask approved = new ProjectTask(t.id(), t.projectId(), t.title(), t.duration(), t.requiredSkills(), t.priority(), t.preferredStart(), t.deadline(), t.dependencyTaskIds(), t.assigneeUsernames(), t.requiredAssetIds(), t.location(), true);
                resourceCatalogService.updateTask(approved);
            });
            return "redirect:/tasks";
        } else {
            model.addAttribute("report", report);
            model.addAttribute("safety", advice);
            model.addAttribute("budget", null);
            model.addAttribute("generatedAt", report.generatedAt());
            return "simulate";
        }
    }

    @PostMapping("/tasks/status")
    public String updateTaskStatus(@RequestParam String taskId,
                                   @RequestParam boolean completed,
                                   Authentication authentication) {
        String currentUsername = authentication == null ? "" : authentication.getName();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!isAdmin) {
            boolean assigned = resourceCatalogService.findTask(taskId)
                    .map(task -> task.assigneeUsernames().contains(currentUsername))
                    .orElse(false);
            if (!assigned) {
                return "redirect:/tasks";
            }
        }
        resourceCatalogService.updateTaskStatus(taskId, currentUsername, completed);
        return "redirect:/tasks";
    }

    @PostMapping("/tasks/delete")
    public String deleteTask(@RequestParam String id) {
        resourceCatalogService.deleteTask(id);
        return "redirect:/tasks";
    }

    private Set<String> splitCsv(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }

    private List<String> splitCsvList(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    public record TaskFormData(String id, String projectId, String title, long durationHours, String requiredSkills,
                               int priority, String preferredStart, String deadline, String dependencyTaskIds, String assigneeUsernames,
                               String requiredAssetIds, String location) {

        static TaskFormData blank() {
            LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime deadline = start.plusDays(7);
            return new TaskFormData("", "PRJ-NEW", "", 2, "JAVA", 5, start.format(INPUT_FORMAT), deadline.format(INPUT_FORMAT), "", "user", "asset-1", "Site-1");
        }

        static TaskFormData from(ProjectTask task) {
            return new TaskFormData(task.id(), task.projectId(), task.title(), task.duration().toHours(),
                    String.join(", ", task.requiredSkills()), task.priority(),
                    task.preferredStart() == null ? "" : task.preferredStart().format(INPUT_FORMAT),
                    task.deadline() == null ? "" : task.deadline().format(INPUT_FORMAT),
                    String.join(", ", task.dependencyTaskIds()), String.join(", ", task.assigneeUsernames()),
                    String.join(", ", task.requiredAssetIds()), task.location());
        }
    }
}