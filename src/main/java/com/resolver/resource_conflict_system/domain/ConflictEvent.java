package com.resolver.resource_conflict_system.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record ConflictEvent(String taskId, String resourceId, ConflictType type, String message, String resolution,
		LocalDateTime createdAt) {

	public ConflictEvent {
		Objects.requireNonNull(taskId, "taskId must not be null");
		Objects.requireNonNull(type, "type must not be null");
		Objects.requireNonNull(message, "message must not be null");
		Objects.requireNonNull(resolution, "resolution must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
	}
}