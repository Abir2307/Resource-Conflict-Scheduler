package com.resolver.resource_conflict_system.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_availability_requests")
public class ResourceAvailabilityRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private int requestedAvailableHours;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private boolean approved;

    private String approvedBy;

    private LocalDateTime approvedAt;

    public ResourceAvailabilityRequestEntity() {}

    public ResourceAvailabilityRequestEntity(String username, int requestedAvailableHours, LocalDate startDate, LocalDate endDate, LocalDateTime createdAt) {
        this.username = username;
        this.requestedAvailableHours = requestedAvailableHours;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
        this.approved = false;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public int getRequestedAvailableHours() { return requestedAvailableHours; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isApproved() { return approved; }
    public String getApprovedBy() { return approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }

    public void setApproved(boolean approved) { this.approved = approved; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
}
