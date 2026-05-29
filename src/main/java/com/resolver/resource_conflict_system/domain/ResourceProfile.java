package com.resolver.resource_conflict_system.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record ResourceProfile(String id, String name, Set<String> skills, List<AvailabilitySlot> availabilitySlots,
	int maxWorkloadHours, List<AssignmentResult> assignments, String location, java.math.BigDecimal salaryPerHour, int availableHoursPerWeek) {

	public ResourceProfile {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(skills, "skills must not be null");
		Objects.requireNonNull(availabilitySlots, "availabilitySlots must not be null");
		Objects.requireNonNull(assignments, "assignments must not be null");
		if (maxWorkloadHours <= 0) {
			throw new IllegalArgumentException("maxWorkloadHours must be positive");
		}
		skills = normalizeSkills(skills);
		availabilitySlots = List.copyOf(availabilitySlots);
		assignments = List.copyOf(assignments);
	}

	public int assignedHours() {
		return assignments.stream().mapToInt(result -> (int) result.duration().toHours()).sum();
	}

	public boolean supports(Set<String> requiredSkills) {
		return skills.containsAll(requiredSkills);
	}

	public boolean canFit(ProjectTask task, LocalDateTime candidateStart) {
		LocalDateTime candidateEnd = candidateStart.plus(task.duration());
		if (assignedHours() + task.duration().toHours() > maxWorkloadHours) {
			return false;
		}
		for (AvailabilitySlot slot : availabilitySlots) {
			if (!slot.contains(candidateStart, candidateEnd)) {
				continue;
			}
			boolean overlaps = assignments.stream()
					.anyMatch(assignment -> overlaps(candidateStart, candidateEnd, assignment.start(), assignment.end()));
			if (!overlaps) {
				return true;
			}
		}
		return false;
	}

	public ResourceProfile withAssignment(AssignmentResult assignment) {
		List<AssignmentResult> updatedAssignments = new ArrayList<>(assignments);
		updatedAssignments.add(assignment);
		return new ResourceProfile(id, name, skills, availabilitySlots, maxWorkloadHours, updatedAssignments, location, salaryPerHour, availableHoursPerWeek);
	}

	private static Set<String> normalizeSkills(Set<String> values) {
		return values.stream()
				.filter(value -> value != null && !value.isBlank())
				.map(String::trim)
				.map(value -> value.toUpperCase(Locale.ROOT))
				.collect(Collectors.toUnmodifiableSet());
	}

	private static boolean overlaps(LocalDateTime start, LocalDateTime end, LocalDateTime otherStart,
			LocalDateTime otherEnd) {
		return start.isBefore(otherEnd) && end.isAfter(otherStart);
	}
}