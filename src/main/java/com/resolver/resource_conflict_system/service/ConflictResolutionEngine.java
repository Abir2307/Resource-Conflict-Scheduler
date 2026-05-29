package com.resolver.resource_conflict_system.service;

import com.resolver.resource_conflict_system.domain.ConflictEvent;
import com.resolver.resource_conflict_system.domain.ConflictType;
import com.resolver.resource_conflict_system.domain.ProjectTask;

import java.time.LocalDateTime;

public final class ConflictResolutionEngine {

	public ConflictEvent buildConflict(ProjectTask task, String resourceId, ConflictType type, String reason,
			String resolution) {
		return new ConflictEvent(task.id(), resourceId, type, reason, resolution, LocalDateTime.now());
	}

	public String summarizeConflict(ProjectTask task, String reason) {
		return "Task " + task.id() + " (" + task.title() + ") " + reason;
	}
}