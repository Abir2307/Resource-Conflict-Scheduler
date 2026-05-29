package com.resolver.resource_conflict_system.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;

@Entity
@Table(name = "resources")
public class ResourceEntity {

    @Id
    private String id;

    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "resource_skills", joinColumns = @JoinColumn(name = "resource_id"))
    @Column(name = "skill")
    private Set<String> skills = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "resource_availability", joinColumns = @JoinColumn(name = "resource_id"))
    private List<AvailabilitySlotEmbeddable> availabilitySlots;

    private int maxWorkloadHours;
    private BigDecimal salaryPerHour;
    private int availableHoursPerWeek;

    private String location;

    public ResourceEntity() {}

    public ResourceEntity(String id, String name, Set<String> skills, List<AvailabilitySlotEmbeddable> availabilitySlots,
                          int maxWorkloadHours, String location) {
        this(id, name, skills, availabilitySlots, maxWorkloadHours, BigDecimal.ZERO, maxWorkloadHours, location);
    }

    public ResourceEntity(String id, String name, Set<String> skills, List<AvailabilitySlotEmbeddable> availabilitySlots,
                          int maxWorkloadHours, BigDecimal salaryPerHour, int availableHoursPerWeek, String location) {
        this.id = id; this.name = name; this.skills = skills; this.availabilitySlots = availabilitySlots;
        this.maxWorkloadHours = maxWorkloadHours; this.salaryPerHour = salaryPerHour == null ? BigDecimal.ZERO : salaryPerHour; this.availableHoursPerWeek = availableHoursPerWeek; this.location = location;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<String> getSkills() { return skills; }
    public void setSkills(Set<String> skills) { this.skills = skills; }
    public List<AvailabilitySlotEmbeddable> getAvailabilitySlots() { return availabilitySlots; }
    public void setAvailabilitySlots(List<AvailabilitySlotEmbeddable> s) { this.availabilitySlots = s; }
    public int getMaxWorkloadHours() { return maxWorkloadHours; }
    public void setMaxWorkloadHours(int h) { this.maxWorkloadHours = h; }
    public java.math.BigDecimal getSalaryPerHour() { return salaryPerHour; }
    public void setSalaryPerHour(java.math.BigDecimal salaryPerHour) { this.salaryPerHour = salaryPerHour; }
    public int getAvailableHoursPerWeek() { return availableHoursPerWeek; }
    public void setAvailableHoursPerWeek(int availableHoursPerWeek) { this.availableHoursPerWeek = availableHoursPerWeek; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
