package com.resolver.resource_conflict_system.repository;

import com.resolver.resource_conflict_system.entity.AssignmentEntity;
import com.resolver.resource_conflict_system.entity.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<AssignmentEntity, Long> {
    List<AssignmentEntity> findByStatusIn(List<AssignmentStatus> statuses);
    List<AssignmentEntity> findByResourceIdAndStatusIn(String resourceId, List<AssignmentStatus> statuses);
    List<AssignmentEntity> findByTaskId(String taskId);
    List<AssignmentEntity> findByTaskIdAndAssigneeUsernameAndStatusIn(String taskId, String username, List<AssignmentStatus> statuses);
}
