package com.resolver.resource_conflict_system.service;

import com.resolver.resource_conflict_system.domain.AvailabilitySlot;
import com.resolver.resource_conflict_system.domain.ProjectTask;
import com.resolver.resource_conflict_system.domain.ResourceProfile;
import com.resolver.resource_conflict_system.entity.AvailabilitySlotEmbeddable;
import com.resolver.resource_conflict_system.entity.ProjectTaskEntity;
import com.resolver.resource_conflict_system.entity.TaskStatusEntity;
import com.resolver.resource_conflict_system.entity.ResourceEntity;
import com.resolver.resource_conflict_system.entity.AssignmentStatus;
import com.resolver.resource_conflict_system.repository.ProjectTaskRepository;
import com.resolver.resource_conflict_system.repository.TaskStatusRepository;
import com.resolver.resource_conflict_system.repository.ResourceRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.Map;

@Service
public class ResourceCatalogService {

	private final ResourceRepository resourceRepository;
	private final ProjectTaskRepository taskRepository;
	private final TaskStatusRepository taskStatusRepository;
	private final com.resolver.resource_conflict_system.repository.AssignmentRepository assignmentRepository;
	private final AtomicInteger resourceSequence = new AtomicInteger(4);
	private final AtomicInteger taskSequence = new AtomicInteger(100);

	public ResourceCatalogService(ResourceRepository resourceRepository, ProjectTaskRepository taskRepository,
			TaskStatusRepository taskStatusRepository, com.resolver.resource_conflict_system.repository.AssignmentRepository assignmentRepository) {
		this.resourceRepository = resourceRepository;
		this.taskRepository = taskRepository;
		this.taskStatusRepository = taskStatusRepository;
		this.assignmentRepository = assignmentRepository;
	}

	@PostConstruct
	public void initialize() {
		resourceSequence.set(nextResourceSequenceStart());
		taskSequence.set(nextTaskSequenceStart());
	}

	public synchronized void reset() {
		resourceRepository.deleteAll();
		taskRepository.deleteAll();
		taskStatusRepository.deleteAll();
		resourceSequence.set(4);
		taskSequence.set(100);
	}

	public synchronized List<ResourceProfile> snapshotResources() {
		return resourceRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
	}

	public synchronized List<ProjectTask> snapshotTasks() {
		return taskRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
	}

	public synchronized Optional<ResourceProfile> findResource(@NonNull String id) {
		return resourceRepository.findById(Objects.requireNonNull(id, "id must not be null")).map(this::toDomain);
	}

	public synchronized Optional<ProjectTask> findTask(String id) {
		return taskRepository.findById(Objects.requireNonNull(id, "id must not be null")).map(this::toDomain);
	}

	public synchronized ProjectTask addTask(ProjectTask task) {
		String id = task.id() == null || task.id().isBlank() || "pending".equalsIgnoreCase(task.id())
				? nextTaskId()
				: task.id();
		ProjectTask saved = saveTask(new ProjectTask(id, task.projectId(), task.title(), task.duration(),
			task.requiredSkills(), task.priority(), task.preferredStart(), task.deadline(), task.dependencyTaskIds(), task.assigneeUsernames(), task.requiredAssetIds(), task.location(), task.approved()));
		return saved;
	}

	public synchronized ProjectTask updateTask(ProjectTask task) {
		if (task.id() == null || task.id().isBlank()) {
			throw new IllegalArgumentException("Task id is required for updates");
		}
		return saveTask(task);
	}

	public synchronized ResourceProfile addResource(ResourceProfile resource) {
		String id = resource.id() == null || resource.id().isBlank() ? nextResourceId() : resource.id();
		return saveResource(new ResourceProfile(id, resource.name(), resource.skills(), resource.availabilitySlots(),
				resource.maxWorkloadHours(), resource.assignments(), resource.location(), resource.salaryPerHour(), resource.availableHoursPerWeek()));
	}

	public synchronized ResourceProfile updateResource(ResourceProfile resource) {
		if (resource.id() == null || resource.id().isBlank()) {
			throw new IllegalArgumentException("Resource id is required for updates");
		}
		return saveResource(resource);
	}

	public synchronized void deleteTask(String id) {
		taskRepository.deleteById(Objects.requireNonNull(id, "id must not be null"));
		taskStatusRepository.deleteByTaskId(id);
	}

	public synchronized void updateTaskStatus(String taskId, String username, boolean completed) {
		TaskStatusEntity entity = taskStatusRepository.findByTaskIdAndUsername(taskId, username)
				.orElseGet(() -> new TaskStatusEntity(taskId, username, completed, LocalDateTime.now()));
		entity.setCompleted(completed);
		entity.setUpdatedAt(LocalDateTime.now());
		taskStatusRepository.save(entity);
		// mark corresponding assignments done for this task and user
		// assignmentRepository will be used to update statuses
		var assignments = assignmentRepository.findByTaskIdAndAssigneeUsernameAndStatusIn(taskId, username, List.of(AssignmentStatus.SCHEDULED, AssignmentStatus.IN_PROGRESS));
		for (var a : assignments) {
			a.setStatus(AssignmentStatus.DONE);
		}
		assignmentRepository.saveAll(Objects.requireNonNull(assignments));
	}

	public synchronized Map<String, Integer> approvedAssetUsageCounts() {
		Map<String, Integer> counts = new java.util.HashMap<>();
		for (ProjectTask task : snapshotTasks()) {
			if (!task.approved()) {
				continue;
			}
			for (String assetId : task.requiredAssetIds()) {
				int current = counts.getOrDefault(assetId, 0);
				counts.put(assetId, current + 1);
			}
		}
		return counts;
	}

	public synchronized Map<String, Boolean> findTaskStatusesForUser(String username) {
		return taskStatusRepository.findByUsername(username).stream()
				.collect(Collectors.toMap(TaskStatusEntity::getTaskId, TaskStatusEntity::isCompleted, (left, right) -> right));
	}

	public synchronized List<TaskStatusEntity> findTaskStatusEntitiesForUser(String username) {
		return taskStatusRepository.findByUsername(username);
	}

	public synchronized void deleteResource(String id) {
		resourceRepository.deleteById(Objects.requireNonNull(id, "id must not be null"));
	}

	private ResourceProfile saveResource(ResourceProfile resource) {
		ResourceEntity entity = new ResourceEntity(resource.id(), resource.name(), resource.skills(), resource.availabilitySlots().stream()
				.map(slot -> new AvailabilitySlotEmbeddable(slot.start(), slot.end())).toList(), resource.maxWorkloadHours(),
				resource.salaryPerHour(), resource.availableHoursPerWeek(), resource.location());
		ResourceEntity saved = resourceRepository.save(entity);
		return toDomain(saved);
	}

	private ProjectTask saveTask(ProjectTask t) {
		ProjectTaskEntity e = new ProjectTaskEntity(t.id(), t.projectId(), t.title(), t.duration().toHours(), t.requiredSkills(),
				t.priority(), t.preferredStart(), t.deadline(), Set.copyOf(t.dependencyTaskIds()), Set.copyOf(t.assigneeUsernames()), Set.copyOf(t.requiredAssetIds()), t.location(), t.approved());
		ProjectTaskEntity saved = taskRepository.save(e);
		return toDomain(saved);
	}

	private ResourceProfile toDomain(ResourceEntity e) {
		List<AvailabilitySlot> slots = e.getAvailabilitySlots().stream()
			.map(s -> new AvailabilitySlot(s.getStart(), s.getEnd())).collect(Collectors.toList());
		java.math.BigDecimal salary = e.getSalaryPerHour() == null ? java.math.BigDecimal.ZERO : e.getSalaryPerHour();
		// include active assignments for this resource
		List<com.resolver.resource_conflict_system.entity.AssignmentEntity> active = assignmentRepository.findByResourceIdAndStatusIn(e.getId(), List.of(AssignmentStatus.SCHEDULED, AssignmentStatus.IN_PROGRESS));
		List<com.resolver.resource_conflict_system.domain.AssignmentResult> assignments = active.stream().map(a -> new com.resolver.resource_conflict_system.domain.AssignmentResult(
			a.getTaskId(), a.getResourceId(), a.getTaskId(), a.getResourceName(), a.getStart(), a.getEnd(), a.getScore(), a.getRationale()
		)).collect(Collectors.toList());
		return new ResourceProfile(e.getId(), e.getName(), e.getSkills(), slots, e.getMaxWorkloadHours(), assignments, e.getLocation(), salary, e.getAvailableHoursPerWeek());
	}

	private ProjectTask toDomain(ProjectTaskEntity e) {
		return new ProjectTask(e.getId(), e.getProjectId(), e.getTitle(), Duration.ofHours(e.getDurationHours()), e.getRequiredSkills(),
				e.getPriority(), e.getPreferredStart(), e.getDeadline(), new ArrayList<>(e.getDependencyTaskIds()), new ArrayList<>(e.getAssigneeUsernames()), new ArrayList<>(e.getRequiredAssetIds()), e.getLocation(), e.isApproved());
	}

	private String nextTaskId() {
		return "T-" + taskSequence.getAndIncrement();
	}

	private String nextResourceId() {
		return "res-" + resourceSequence.getAndIncrement();
	}

	private int nextResourceSequenceStart() {
		return resourceRepository.findAll().stream()
				.map(ResourceEntity::getId)
				.mapToInt(this::extractNumericSuffix)
				.max()
				.orElse(3) + 1;
	}

	private int nextTaskSequenceStart() {
		return taskRepository.findAll().stream()
				.map(ProjectTaskEntity::getId)
				.mapToInt(this::extractNumericSuffix)
				.max()
				.orElse(99) + 1;
	}

	private int extractNumericSuffix(String value) {
		if (value == null || value.isBlank()) {
			return 0;
		}
		int dash = value.lastIndexOf('-');
		String suffix = dash >= 0 ? value.substring(dash + 1) : value;
		try {
			return Integer.parseInt(suffix);
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}

}