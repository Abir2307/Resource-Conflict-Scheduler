package com.resolver.resource_conflict_system.service;

import com.resolver.resource_conflict_system.domain.AssetItem;
import com.resolver.resource_conflict_system.entity.AssetEntity;
import com.resolver.resource_conflict_system.repository.AssetRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;

import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import com.resolver.resource_conflict_system.service.DijkstraRoutingService.Edge;
import com.resolver.resource_conflict_system.service.DijkstraRoutingService.PathResult;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

@Service
public class AssetCatalogService {

    private final AssetRepository assetRepository;
    private final DijkstraRoutingService routingService;
    private final AtomicInteger sequence = new AtomicInteger(5);
    private final Map<String, List<Edge>> supplyGraph = new HashMap<>();

    public AssetCatalogService(AssetRepository assetRepository, DijkstraRoutingService routingService) {
        this.assetRepository = assetRepository;
        this.routingService = routingService;
    }

    @PostConstruct
    public void initialize() {
        supplyGraph.clear();
        supplyGraph.put("Store-A", List.of(new Edge("Hub", 4.0)));
        supplyGraph.put("Store-B", List.of(new Edge("Hub", 6.0)));
        supplyGraph.put("Vault-1", List.of(new Edge("Hub", 2.0)));
        supplyGraph.put("Store-C", List.of(new Edge("Hub", 8.0)));
        supplyGraph.put("Hub", List.of(new Edge("HQ", 2.0)));
        sequence.set(nextSequenceValue(assetRepository.findAll().stream()
                .map(AssetEntity::getId)
                .mapToInt(this::extractAssetNumber)
                .max()
                .orElse(4) + 1));
    }

    public synchronized void reset() {
        assetRepository.deleteAll();
        sequence.set(5);
    }

    public synchronized List<AssetItem> snapshotAssets() {
        List<AssetItem> rawAssets = assetRepository.findAll().stream().map(this::toRawDomain).collect(Collectors.toList());
        return resolveDerivedArrivalTimes(rawAssets);
    }

    public synchronized Optional<AssetItem> findAsset(@NonNull String id) {
        return snapshotAssets().stream().filter(asset -> asset.id().equals(Objects.requireNonNull(id, "id must not be null"))).findFirst();
    }

    public synchronized AssetItem addAsset(AssetItem asset) {
        String id = asset.id() == null || asset.id().isBlank() ? nextAssetId() : asset.id();
        return saveAsset(new AssetItem(id, asset.name(), asset.category(), asset.quantity(), asset.unit(), asset.location(), asset.notes(), asset.usedByTaskIds(), asset.dependencyAssetIds(), asset.costPerPiece(), asset.estimatedArrivalHours(), asset.supplierNodeId()));
    }

    public synchronized AssetItem updateAsset(AssetItem asset) {
        if (asset.id() == null || asset.id().isBlank()) {
            throw new IllegalArgumentException("Asset id is required for updates");
        }
        return saveAsset(asset);
    }

    public synchronized void deleteAsset(String id) {
        assetRepository.deleteById(Objects.requireNonNull(id, "id must not be null"));
    }

    private AssetItem saveAsset(AssetItem asset) {
        AssetEntity entity = new AssetEntity(asset.id(), asset.name(), asset.category(), asset.quantity(), asset.unit(), asset.location(), asset.notes(),
                asset.dependencyAssetIds(), asset.costPerPiece(), asset.estimatedArrivalHours(), asset.supplierNodeId());
        AssetEntity saved = assetRepository.save(entity);
        String savedId = Objects.requireNonNull(saved.getId(), "saved asset id must not be null");
        return findAsset(savedId).orElseGet(() -> resolveDerivedArrivalTimes(List.of(toRawDomain(saved))).get(0));
    }

    private AssetItem toRawDomain(AssetEntity entity) {
        java.math.BigDecimal cost = entity.getCostPerPiece() == null ? BigDecimal.ZERO : entity.getCostPerPiece();
        String id = Objects.requireNonNull(entity.getId(), "asset id must not be null");
        return new AssetItem(id, entity.getName(), entity.getCategory(), entity.getQuantity(), entity.getUnit(), entity.getLocation(), entity.getNotes(), List.of(),
                entity.getDependencyAssetIds() == null ? List.of() : new ArrayList<>(entity.getDependencyAssetIds()), cost, entity.getEstimatedArrivalHours(), entity.getSupplierNodeId());
    }

    private List<AssetItem> resolveDerivedArrivalTimes(List<AssetItem> rawAssets) {
        Map<String, AssetItem> byId = rawAssets.stream().collect(Collectors.toMap(AssetItem::id, asset -> asset, (left, right) -> left));
        Map<String, Long> memo = new HashMap<>();
        List<AssetItem> resolved = new ArrayList<>();
        for (AssetItem asset : rawAssets) {
            long eta = resolveEta(asset, byId, memo, new java.util.LinkedHashSet<>());
            resolved.add(new AssetItem(asset.id(), asset.name(), asset.category(), asset.quantity(), asset.unit(), asset.location(), asset.notes(),
                    asset.usedByTaskIds(), asset.dependencyAssetIds(), asset.costPerPiece(), eta, asset.supplierNodeId()));
        }
        return resolved;
    }

    private long resolveEta(AssetItem asset, Map<String, AssetItem> byId, Map<String, Long> memo, java.util.Set<String> visiting) {
        if (memo.containsKey(asset.id())) {
            return memo.get(asset.id());
        }
        if (!visiting.add(asset.id())) {
            return Math.max(0L, asset.estimatedArrivalHours());
        }
        long eta = Math.max(0L, asset.estimatedArrivalHours());
        if (eta <= 0L && asset.supplierNodeId() != null && !asset.supplierNodeId().isBlank() && routingService != null) {
            try {
                PathResult pr = routingService.shortestPath(supplyGraph, asset.supplierNodeId(), "HQ");
                if (pr != null && pr.cost != Double.POSITIVE_INFINITY) {
                    eta = Math.max(eta, (long) Math.ceil(pr.cost));
                }
            } catch (Exception ignored) {
            }
        }
        for (String dependencyId : asset.dependencyAssetIds()) {
            AssetItem dependency = byId.get(dependencyId);
            if (dependency != null) {
                eta = Math.max(eta, resolveEta(dependency, byId, memo, visiting));
            }
        }
        visiting.remove(asset.id());
        memo.put(asset.id(), eta);
        return eta;
    }

    private String nextAssetId() {
        return "asset-" + sequence.getAndIncrement();
    }

    private int extractAssetNumber(String id) {
        if (id == null || id.isBlank() || !id.startsWith("asset-")) {
            return 4;
        }
        try {
            return Integer.parseInt(id.substring("asset-".length()));
        } catch (NumberFormatException ignored) {
            return 4;
        }
    }

    private int nextSequenceValue(int candidate) {
        return Math.max(candidate, 5);
    }
}