package com.resolver.resource_conflict_system.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record ProjectTask(String id, String projectId, String title, Duration duration, Set<String> requiredSkills,
	int priority, LocalDateTime preferredStart, LocalDateTime deadline, List<String> dependencyTaskIds, List<String> assigneeUsernames,
	List<String> requiredAssetIds, String location, boolean approved) {

	public ProjectTask {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(projectId, "projectId must not be null");
		Objects.requireNonNull(title, "title must not be null");
		Objects.requireNonNull(duration, "duration must not be null");
		Objects.requireNonNull(requiredSkills, "requiredSkills must not be null");
		Objects.requireNonNull(deadline, "deadline must not be null");
		Objects.requireNonNull(dependencyTaskIds, "dependencyTaskIds must not be null");
		Objects.requireNonNull(assigneeUsernames, "assigneeUsernames must not be null");
		Objects.requireNonNull(requiredAssetIds, "requiredAssetIds must not be null");
		if (preferredStart != null && deadline.isBefore(preferredStart)) {
			throw new IllegalArgumentException("Deadline must not be before preferred start");
		}
		if (duration.isZero() || duration.isNegative()) {
			throw new IllegalArgumentException("Task duration must be positive");
		}
		if (priority < 1 || priority > 10) {
			throw new IllegalArgumentException("Priority must be between 1 and 10");
		}
		requiredSkills = normalizeSkills(requiredSkills);
		dependencyTaskIds = List.copyOf(dependencyTaskIds);
		assigneeUsernames = List.copyOf(assigneeUsernames);
		requiredAssetIds = List.copyOf(requiredAssetIds);
	}

	private static Set<String> normalizeSkills(Set<String> values) {
		return values.stream()
				.filter(value -> value != null && !value.isBlank())
				.map(String::trim)
				.map(value -> value.toUpperCase(Locale.ROOT))
				.collect(Collectors.toUnmodifiableSet());
	}
}