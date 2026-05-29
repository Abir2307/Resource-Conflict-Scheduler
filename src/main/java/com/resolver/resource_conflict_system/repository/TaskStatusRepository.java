package com.resolver.resource_conflict_system.repository;

import com.resolver.resource_conflict_system.entity.TaskStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskStatusRepository extends JpaRepository<TaskStatusEntity, Long> {

    Optional<TaskStatusEntity> findByTaskIdAndUsername(String taskId, String username);

    List<TaskStatusEntity> findByUsername(String username);

    void deleteByTaskId(String taskId);
}