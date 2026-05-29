package com.resolver.resource_conflict_system.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record ScheduleReport(List<AssignmentResult> assignments, List<ConflictEvent> conflicts,
		List<ProjectTask> deferredTasks, LocalDateTime generatedAt, String summary) {

	public ScheduleReport {
		Objects.requireNonNull(assignments, "assignments must not be null");
		Objects.requireNonNull(conflicts, "conflicts must not be null");
		Objects.requireNonNull(deferredTasks, "deferredTasks must not be null");
		Objects.requireNonNull(generatedAt, "generatedAt must not be null");
		Objects.requireNonNull(summary, "summary must not be null");
		assignments = List.copyOf(assignments);
		conflicts = List.copyOf(conflicts);
		deferredTasks = List.copyOf(deferredTasks);
	}
}