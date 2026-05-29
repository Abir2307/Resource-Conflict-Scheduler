package com.resolver.resource_conflict_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignments")
public class AssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String taskId;

    @Column(nullable = false)
    private String resourceId;

    @Column(nullable = false)
    private String resourceName;

    @Column
    private String assigneeUsername;

    @Column(nullable = false)
    private LocalDateTime start;

    @Column(nullable = false)
    private LocalDateTime end;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status;

    @Column
    private int score;

    @Column(length = 2000)
    private String rationale;

    protected AssignmentEntity() {}

    public AssignmentEntity(String taskId, String resourceId, String resourceName, String assigneeUsername, LocalDateTime start, LocalDateTime end, AssignmentStatus status, int score, String rationale) {
        this.taskId = taskId;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.assigneeUsername = assigneeUsername;
        this.start = start;
        this.end = end;
        this.status = status;
        this.score = score;
        this.rationale = rationale;
    }

    public Long getId() { return id; }
    public String getTaskId() { return taskId; }
    public String getResourceId() { return resourceId; }
    public String getResourceName() { return resourceName; }
    public String getAssigneeUsername() { return assigneeUsername; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
    public AssignmentStatus getStatus() { return status; }
    public int getScore() { return score; }
    public String getRationale() { return rationale; }

    public void setStatus(AssignmentStatus status) { this.status = status; }
    public void setAssigneeUsername(String assigneeUsername) { this.assigneeUsername = assigneeUsername; }
}
