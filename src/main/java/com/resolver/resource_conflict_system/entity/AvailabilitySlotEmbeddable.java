package com.resolver.resource_conflict_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

@Embeddable
public class AvailabilitySlotEmbeddable {

    @Column(name = "slot_start")
    private LocalDateTime start;

    @Column(name = "slot_end")
    private LocalDateTime end;

    public AvailabilitySlotEmbeddable() {}

    public AvailabilitySlotEmbeddable(LocalDateTime start, LocalDateTime end) {
        this.start = start;
        this.end = end;
    }

    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
    public void setStart(LocalDateTime s) { this.start = s; }
    public void setEnd(LocalDateTime e) { this.end = e; }
}
