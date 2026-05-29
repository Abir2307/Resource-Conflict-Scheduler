package com.resolver.resource_conflict_system.entity;

import com.resolver.resource_conflict_system.domain.AssetCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assets")
public class AssetEntity {

    @Id
    private String id;

    private String name;

    @Enumerated(EnumType.STRING)
    private AssetCategory category;

    private int quantity;

    private String unit;

    private String location;

    @Column(length = 2000)
    private String notes;

    private BigDecimal costPerPiece;

    /** Estimated arrival time in hours from supplier node to local node; may be zero when unknown */
    private long estimatedArrivalHours;

    /** Optional supplier node id used with routing graph */
    private String supplierNodeId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "asset_dependencies", joinColumns = @JoinColumn(name = "asset_id"))
    @Column(name = "dependency_asset_id")
    private List<String> dependencyAssetIds = new ArrayList<>();

    public AssetEntity() {
    }

    public AssetEntity(String id, String name, AssetCategory category, int quantity, String unit, String location, String notes) {
        this(id, name, category, quantity, unit, location, notes, List.of(), BigDecimal.ZERO, 0L, null);
    }

    public AssetEntity(String id, String name, AssetCategory category, int quantity, String unit, String location, String notes,
                       List<String> dependencyAssetIds, BigDecimal costPerPiece, long estimatedArrivalHours, String supplierNodeId) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.location = location;
        this.notes = notes;
        this.costPerPiece = costPerPiece == null ? BigDecimal.ZERO : costPerPiece;
        this.estimatedArrivalHours = estimatedArrivalHours;
        this.supplierNodeId = supplierNodeId;
        this.dependencyAssetIds = dependencyAssetIds == null ? new ArrayList<>() : new ArrayList<>(dependencyAssetIds);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AssetCategory getCategory() { return category; }
    public void setCategory(AssetCategory category) { this.category = category; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public java.math.BigDecimal getCostPerPiece() { return costPerPiece; }
    public void setCostPerPiece(java.math.BigDecimal costPerPiece) { this.costPerPiece = costPerPiece; }
    public long getEstimatedArrivalHours() { return estimatedArrivalHours; }
    public void setEstimatedArrivalHours(long estimatedArrivalHours) { this.estimatedArrivalHours = estimatedArrivalHours; }
    public String getSupplierNodeId() { return supplierNodeId; }
    public void setSupplierNodeId(String supplierNodeId) { this.supplierNodeId = supplierNodeId; }
    public List<String> getDependencyAssetIds() { return dependencyAssetIds; }
    public void setDependencyAssetIds(List<String> dependencyAssetIds) { this.dependencyAssetIds = dependencyAssetIds == null ? new ArrayList<>() : new ArrayList<>(dependencyAssetIds); }
}