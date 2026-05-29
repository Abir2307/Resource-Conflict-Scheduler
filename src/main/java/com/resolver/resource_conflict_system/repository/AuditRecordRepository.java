package com.resolver.resource_conflict_system.repository;

import com.resolver.resource_conflict_system.domain.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

}
