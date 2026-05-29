package com.resolver.resource_conflict_system.repository;

import com.resolver.resource_conflict_system.entity.ResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends JpaRepository<ResourceEntity, String> {

}
