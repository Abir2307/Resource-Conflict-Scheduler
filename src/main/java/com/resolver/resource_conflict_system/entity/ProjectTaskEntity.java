package com.resolver.resource_conflict_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tasks")
public class ProjectTaskEntity {

    @Id
    private String id;

    private String projectId;

    private String title;

    private long durationHours;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_skills", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "skill")
    private Set<String> requiredSkills = new HashSet<>();

    private int priority;

    private LocalDateTime preferredStart;

    private LocalDateTime deadline;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_dependencies", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "dependency_id")
    private Set<String> dependencyTaskIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_assignees", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "assignee_username")
    private Set<String> assigneeUsernames = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_assets", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "asset_id")
    private Set<String> requiredAssetIds = new HashSet<>();

    private String location;
    private boolean approved;

    public ProjectTaskEntity() {}

    public ProjectTaskEntity(String id, String projectId, String title, long durationHours, Set<String> requiredSkills,
                             int priority, LocalDateTime preferredStart, LocalDateTime deadline, Set<String> dependencyTaskIds,
                             Set<String> assigneeUsernames, Set<String> requiredAssetIds, String location, boolean approved) {
        this.id = id; this.projectId = projectId; this.title = title; this.durationHours = durationHours;
        this.requiredSkills = requiredSkills; this.priority = priority; this.preferredStart = preferredStart;
        this.deadline = deadline;
        this.dependencyTaskIds = dependencyTaskIds; this.assigneeUsernames = assigneeUsernames; this.requiredAssetIds = requiredAssetIds; this.location = location;
        this.approved = approved;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String p) { this.projectId = p; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public long getDurationHours() { return durationHours; }
    public void setDurationHours(long d) { this.durationHours = d; }
    public Set<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(Set<String> s) { this.requiredSkills = s; }
    public int getPriority() { return priority; }
    public void setPriority(int p) { this.priority = p; }
    public LocalDateTime getPreferredStart() { return preferredStart; }
    public void setPreferredStart(LocalDateTime p) { this.preferredStart = p; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public Set<String> getDependencyTaskIds() { return dependencyTaskIds; }
    public void setDependencyTaskIds(Set<String> d) { this.dependencyTaskIds = d; }
    public Set<String> getAssigneeUsernames() { return assigneeUsernames; }
    public void setAssigneeUsernames(Set<String> a) { this.assigneeUsernames = a; }
    public Set<String> getRequiredAssetIds() { return requiredAssetIds; }
    public void setRequiredAssetIds(Set<String> requiredAssetIds) { this.requiredAssetIds = requiredAssetIds; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
}
