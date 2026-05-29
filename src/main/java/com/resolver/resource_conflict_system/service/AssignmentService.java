package com.resolver.resource_conflict_system.service;
import com.resolver.resource_conflict_system.domain.AssignmentResult;
import com.resolver.resource_conflict_system.entity.AssignmentEntity;
import com.resolver.resource_conflict_system.entity.AssignmentStatus;
import com.resolver.resource_conflict_system.repository.AssignmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;

    public AssignmentService(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    public void persistAssignments(List<AssignmentResult> results) {
        List<AssignmentEntity> entities = results.stream().map(r -> new AssignmentEntity(
                r.taskId(), r.resourceId(), r.resourceName(), null, r.start(), r.end(), AssignmentStatus.SCHEDULED, r.score(), r.rationale()
        )).collect(Collectors.toList());
        assignmentRepository.saveAll(Objects.requireNonNull(entities));
    }

    public List<AssignmentResult> activeAssignments() {
        return assignmentRepository.findByStatusIn(List.of(AssignmentStatus.SCHEDULED, AssignmentStatus.IN_PROGRESS)).stream()
                .map(this::toResult)
                .collect(Collectors.toList());
    }

    public List<AssignmentResult> assignmentsForResource(String resourceId) {
        return assignmentRepository.findByResourceIdAndStatusIn(resourceId, List.of(AssignmentStatus.SCHEDULED, AssignmentStatus.IN_PROGRESS)).stream()
                .map(this::toResult)
                .collect(Collectors.toList());
    }

    public void markAssignmentsDoneForTaskAndUser(String taskId, String username) {
        var entities = assignmentRepository.findByTaskIdAndAssigneeUsernameAndStatusIn(taskId, username, List.of(AssignmentStatus.SCHEDULED, AssignmentStatus.IN_PROGRESS));
        for (var e : entities) {
            e.setStatus(AssignmentStatus.DONE);
        }
        assignmentRepository.saveAll(Objects.requireNonNull(entities));
    }

    private AssignmentResult toResult(AssignmentEntity e) {
        return new AssignmentResult(e.getTaskId(), e.getResourceId(), e.getResourceName(), e.getResourceName(), e.getStart(), e.getEnd(), e.getScore(), e.getRationale());
    }
}
