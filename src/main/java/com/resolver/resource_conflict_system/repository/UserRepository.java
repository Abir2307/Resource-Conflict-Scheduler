package com.resolver.resource_conflict_system.repository;

import com.resolver.resource_conflict_system.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserAccountEntity, String> {
}