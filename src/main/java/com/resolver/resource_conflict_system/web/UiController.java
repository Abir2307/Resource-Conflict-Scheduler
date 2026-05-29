package com.resolver.resource_conflict_system.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolver.resource_conflict_system.domain.SafetyAdvice;
import com.resolver.resource_conflict_system.service.AssetCatalogService;
import com.resolver.resource_conflict_system.service.BankersSafetyService;
import com.resolver.resource_conflict_system.domain.ScheduleReport;
import com.resolver.resource_conflict_system.service.ResourceCatalogService;
import com.resolver.resource_conflict_system.service.SchedulerEngine;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class UiController {

    private final ResourceCatalogService resourceCatalogService;
    private final AssetCatalogService assetCatalogService;
    private final BankersSafetyService bankersSafetyService;
    private final SchedulerEngine schedulerEngine;
    private final com.resolver.resource_conflict_system.repository.AuditRecordRepository auditRecordRepository;
    private final ObjectMapper objectMapper;

    public UiController(ResourceCatalogService resourceCatalogService, SchedulerEngine schedulerEngine,
            AssetCatalogService assetCatalogService, BankersSafetyService bankersSafetyService,
            com.resolver.resource_conflict_system.repository.AuditRecordRepository auditRecordRepository,
            ObjectMapper objectMapper) {
        this.resourceCatalogService = resourceCatalogService;
        this.assetCatalogService = assetCatalogService;
        this.bankersSafetyService = bankersSafetyService;
        this.schedulerEngine = schedulerEngine;
        this.auditRecordRepository = auditRecordRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("form", new RegistrationController.RegistrationForm());
        model.addAttribute("resourceCount", resourceCatalogService.snapshotResources().size());
        model.addAttribute("taskCount", resourceCatalogService.snapshotTasks().size());
        model.addAttribute("assetCount", assetCatalogService.snapshotAssets().size());
        model.addAttribute("auditCount", auditRecordRepository.count());
        return "index";
    }

    @GetMapping("/simulate/ui")
    public String simulateUi(@RequestParam(required = false) java.math.BigDecimal budget,
            @RequestParam(required = false) Long deliveryEtaHours,
            @RequestParam(required = false) String sequence, Model model) {
        List<com.resolver.resource_conflict_system.domain.ProjectTask> tasks = resourceCatalogService.snapshotTasks();
        java.math.BigDecimal safeBudget = budget == null ? java.math.BigDecimal.ZERO : budget;
        long safeDeliveryEtaHours = deliveryEtaHours == null ? 0L : deliveryEtaHours;

        model.addAttribute("budget", safeBudget);
        model.addAttribute("deliveryEtaHours", safeDeliveryEtaHours);
        model.addAttribute("sequence", sequence);

        if (tasks.isEmpty()) {
            model.addAttribute("errorMessage", "No tasks exist yet. Create at least one task before running simulation.");
            return "simulate";
        }

        ScheduleReport report = schedulerEngine.simulate(tasks, resourceCatalogService.snapshotResources());
        SafetyAdvice safety = bankersSafetyService.analyzeSafety(tasks,
                resourceCatalogService.snapshotResources(), assetCatalogService.snapshotAssets(), 8, safeBudget, safeDeliveryEtaHours, splitSequence(sequence));
        try {
            String payload = objectMapper.writeValueAsString(report);
            auditRecordRepository.save(new com.resolver.resource_conflict_system.domain.AuditRecord(report.generatedAt(), report.summary(), payload));
        } catch (JsonProcessingException ignored) {
        }
        model.addAttribute("report", report);
        model.addAttribute("safety", safety);
        return "simulate";
    }

    private List<String> splitSequence(String sequence) {
        if (sequence == null || sequence.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(sequence.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }
}
