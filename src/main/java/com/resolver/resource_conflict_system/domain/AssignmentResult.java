package com.resolver.resource_conflict_system.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public record AssignmentResult(String taskId, String resourceId, String taskTitle, String resourceName,
		LocalDateTime start, LocalDateTime end, int score, String rationale) {

	public AssignmentResult {
		Objects.requireNonNull(taskId, "taskId must not be null");
		Objects.requireNonNull(resourceId, "resourceId must not be null");
		Objects.requireNonNull(taskTitle, "taskTitle must not be null");
		Objects.requireNonNull(resourceName, "resourceName must not be null");
		Objects.requireNonNull(start, "start must not be null");
		Objects.requireNonNull(end, "end must not be null");
		Objects.requireNonNull(rationale, "rationale must not be null");
		if (!start.isBefore(end)) {
			throw new IllegalArgumentException("Assignment end must be after start");
		}
	}

	public Duration duration() {
		return Duration.between(start, end);
	}
}