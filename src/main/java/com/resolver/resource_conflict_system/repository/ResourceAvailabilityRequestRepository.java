package com.resolver.resource_conflict_system.repository;

import com.resolver.resource_conflict_system.entity.ResourceAvailabilityRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceAvailabilityRequestRepository extends JpaRepository<ResourceAvailabilityRequestEntity, Long> {
    List<ResourceAvailabilityRequestEntity> findByApprovedFalseOrderByCreatedAtDesc();
}
