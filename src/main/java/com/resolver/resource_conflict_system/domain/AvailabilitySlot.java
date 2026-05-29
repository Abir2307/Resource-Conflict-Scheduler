package com.resolver.resource_conflict_system.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public record AvailabilitySlot(LocalDateTime start, LocalDateTime end) {

	public AvailabilitySlot {
		Objects.requireNonNull(start, "start must not be null");
		Objects.requireNonNull(end, "end must not be null");
		if (!start.isBefore(end)) {
			throw new IllegalArgumentException("Availability slot end must be after start");
		}
	}

	public boolean contains(LocalDateTime candidateStart, LocalDateTime candidateEnd) {
		return !candidateStart.isBefore(start) && !candidateEnd.isAfter(end);
	}

	public Duration duration() {
		return Duration.between(start, end);
	}
}