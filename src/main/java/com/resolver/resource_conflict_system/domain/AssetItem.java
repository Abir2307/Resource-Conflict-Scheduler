package com.resolver.resource_conflict_system.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record AssetItem(String id, String name, AssetCategory category, int quantity, String unit, String location,
                        String notes, List<String> usedByTaskIds, List<String> dependencyAssetIds,
                        BigDecimal costPerPiece, long estimatedArrivalHours, String supplierNodeId) {

    public AssetItem {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(unit, "unit must not be null");
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(notes, "notes must not be null");
        Objects.requireNonNull(usedByTaskIds, "usedByTaskIds must not be null");
        Objects.requireNonNull(dependencyAssetIds, "dependencyAssetIds must not be null");
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be zero or positive");
        }
        usedByTaskIds = List.copyOf(usedByTaskIds);
        dependencyAssetIds = List.copyOf(dependencyAssetIds);
        if (costPerPiece == null) {
            costPerPiece = BigDecimal.ZERO;
        }
    }
}