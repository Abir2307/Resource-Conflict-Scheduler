package com.resolver.resource_conflict_system.service;

import com.resolver.resource_conflict_system.entity.ResourceEntity;
import com.resolver.resource_conflict_system.repository.ResourceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResourceAvailabilityResetScheduler {

    private final ResourceRepository resourceRepository;

    public ResourceAvailabilityResetScheduler(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    // Run every Monday at 00:10
    @Scheduled(cron = "0 10 0 * * MON")
    public void resetWeeklyAvailability() {
        List<ResourceEntity> all = resourceRepository.findAll();
        for (ResourceEntity r : all) {
            r.setAvailableHoursPerWeek(r.getMaxWorkloadHours());
        }
        resourceRepository.saveAll(all);
    }
}
