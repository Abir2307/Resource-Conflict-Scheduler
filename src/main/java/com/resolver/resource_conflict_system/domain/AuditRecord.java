package com.resolver.resource_conflict_system.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime createdAt;

    private String summary;

    @Column(columnDefinition = "TEXT")
    private String payload;

    public AuditRecord() {
    }

    public AuditRecord(LocalDateTime createdAt, String summary, String payload) {
        this.createdAt = createdAt;
        this.summary = summary;
        this.payload = payload;
    }

    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getSummary() { return summary; }
    public String getPayload() { return payload; }
}
