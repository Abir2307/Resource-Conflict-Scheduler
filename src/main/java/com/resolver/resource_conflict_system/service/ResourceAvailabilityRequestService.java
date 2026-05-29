package com.resolver.resource_conflict_system.service;

import com.resolver.resource_conflict_system.entity.ResourceAvailabilityRequestEntity;
import com.resolver.resource_conflict_system.entity.ResourceEntity;
import com.resolver.resource_conflict_system.repository.ResourceAvailabilityRequestRepository;
import com.resolver.resource_conflict_system.repository.ResourceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ResourceAvailabilityRequestService {

    private final ResourceAvailabilityRequestRepository requestRepository;
    private final ResourceRepository resourceRepository;

    public ResourceAvailabilityRequestService(ResourceAvailabilityRequestRepository requestRepository, ResourceRepository resourceRepository) {
        this.requestRepository = requestRepository;
        this.resourceRepository = resourceRepository;
    }

    public ResourceAvailabilityRequestEntity createRequest(String username, int requestedHours, LocalDate startDate, LocalDate endDate) {
        ResourceAvailabilityRequestEntity e = new ResourceAvailabilityRequestEntity(username, requestedHours, startDate, endDate, LocalDateTime.now());
        return requestRepository.save(e);
    }

    public List<ResourceAvailabilityRequestEntity> listPending() {
        return requestRepository.findByApprovedFalseOrderByCreatedAtDesc();
    }

    public ResourceAvailabilityRequestEntity approve(Long id, String admin) {
        ResourceAvailabilityRequestEntity e = requestRepository.findById(Objects.requireNonNull(id)).orElseThrow();
        // apply to resource if found by username (match id or name)
        resourceRepository.findAll().stream()
                .filter(r -> usernameMatchesResource(e.getUsername(), r))
                .findFirst()
                .ifPresent(r -> {
                    r.setAvailableHoursPerWeek(e.getRequestedAvailableHours());
                    resourceRepository.save(r);
                });
        e.setApproved(true);
        e.setApprovedBy(admin);
        e.setApprovedAt(LocalDateTime.now());
        return requestRepository.save(e);
    }

    public void reject(Long id, String admin) {
        ResourceAvailabilityRequestEntity e = requestRepository.findById(Objects.requireNonNull(id)).orElseThrow();
        e.setApproved(false);
        e.setApprovedBy(admin);
        e.setApprovedAt(LocalDateTime.now());
        requestRepository.save(e);
    }

    private boolean usernameMatchesResource(String username, ResourceEntity r) {
        if (r.getId() != null && r.getId().equalsIgnoreCase(username)) return true;
        if (r.getName() != null && r.getName().equalsIgnoreCase(username)) return true;
        if (r.getName() != null && r.getName().toLowerCase().contains(username.toLowerCase())) return true;
        return false;
    }
}
