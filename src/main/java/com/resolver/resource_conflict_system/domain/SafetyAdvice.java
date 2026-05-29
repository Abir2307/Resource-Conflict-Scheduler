package com.resolver.resource_conflict_system.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SafetyAdvice(boolean currentSafe, boolean safeAfterSuggestedChanges, List<String> safeRoute,
                           List<String> requestedSequence, boolean requestedSequenceSafe,
                           Map<String, Integer> extraResourcesBySkill, Map<String, Integer> extraAssetsById,
                           String summary) {

    public SafetyAdvice {
        Objects.requireNonNull(safeRoute, "safeRoute must not be null");
        Objects.requireNonNull(requestedSequence, "requestedSequence must not be null");
        Objects.requireNonNull(extraResourcesBySkill, "extraResourcesBySkill must not be null");
        Objects.requireNonNull(extraAssetsById, "extraAssetsById must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        safeRoute = List.copyOf(safeRoute);
        requestedSequence = List.copyOf(requestedSequence);
        extraResourcesBySkill = Map.copyOf(extraResourcesBySkill);
        extraAssetsById = Map.copyOf(extraAssetsById);
    }
}