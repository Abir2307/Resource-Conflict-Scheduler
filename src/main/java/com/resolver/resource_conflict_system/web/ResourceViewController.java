package com.resolver.resource_conflict_system.web;

import com.resolver.resource_conflict_system.domain.AvailabilitySlot;
import com.resolver.resource_conflict_system.domain.ResourceProfile;
import com.resolver.resource_conflict_system.service.ResourceCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Controller
public class ResourceViewController {

    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final ResourceCatalogService resourceCatalogService;

    public ResourceViewController(ResourceCatalogService resourceCatalogService) {
        this.resourceCatalogService = resourceCatalogService;
    }

    @GetMapping("/resources")
    public String resourcesPage(Model model) {
        model.addAttribute("resources", resourceCatalogService.snapshotResources());
        return "resources";
    }

    @GetMapping("/resources/new")
    public String newResource(Model model) {
        model.addAttribute("form", ResourceFormData.blank());
        model.addAttribute("action", "/resources/save");
        model.addAttribute("title", "Add Resource");
        return "resource-form";
    }

    @GetMapping("/resources/edit/{id}")
    public String editResource(@PathVariable String id, Model model) {
        return resourceCatalogService.findResource(Objects.requireNonNull(id, "id must not be null"))
                .map(resource -> {
                    model.addAttribute("form", ResourceFormData.from(resource));
                    model.addAttribute("action", "/resources/save");
                    model.addAttribute("title", "Edit Resource");
                    return "resource-form";
                })
                .orElse("redirect:/resources");
    }

    @PostMapping("/resources/save")
    public String saveResource(@RequestParam(required = false) String id,
                               @RequestParam(required = false) String name,
                               @RequestParam int maxWorkloadHours,
                       @RequestParam(required = false) Integer availableHoursPerWeek) {
        // Load existing resource when editing to preserve immutable fields
        if (id != null && !id.isBlank()) {
            var existing = resourceCatalogService.findResource(id).orElseThrow();
            int availableHours = availableHoursPerWeek == null ? existing.availableHoursPerWeek() : availableHoursPerWeek;
            // build ResourceProfile preserving existing details but updating max and available
            var resource = new com.resolver.resource_conflict_system.domain.ResourceProfile(
                    existing.id(), existing.name(), existing.skills(), existing.availabilitySlots(),
                    maxWorkloadHours, existing.assignments(), existing.location(), existing.salaryPerHour(), availableHours);
            resourceCatalogService.updateResource(resource);
        } else {
            // Creating minimal resource record: name required for new resource
            if (name == null || name.isBlank()) {
                return "redirect:/resources?error=missing_name";
            }
            int availableHours = availableHoursPerWeek == null ? maxWorkloadHours : availableHoursPerWeek;
            var resource = new com.resolver.resource_conflict_system.domain.ResourceProfile("", name, java.util.Set.of(), java.util.List.of(), maxWorkloadHours, java.util.List.of(), "", java.math.BigDecimal.ZERO, availableHours);
            resourceCatalogService.addResource(resource);
        }
        return "redirect:/resources";
    }

    @PostMapping("/resources/delete")
    public String deleteResource(@RequestParam String id) {
        resourceCatalogService.deleteResource(id);
        return "redirect:/resources";
    }

    public record ResourceFormData(String id, String name, String skillsCsv, String availabilityStart,
                   String availabilityEnd, int maxWorkloadHours, String location, java.math.BigDecimal salaryPerHour, int availableHoursPerWeek) {

        static ResourceFormData blank() {
            LocalDateTime now = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
            return new ResourceFormData("", "", "JAVA", now.format(INPUT_FORMAT), now.plusHours(8).format(INPUT_FORMAT), 8, "Office-A", new java.math.BigDecimal("50.00"), 40);
        }

        static ResourceFormData from(ResourceProfile resource) {
            AvailabilitySlot slot = resource.availabilitySlots().isEmpty() ? null : resource.availabilitySlots().get(0);
            LocalDateTime start = slot == null ? LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0) : slot.start();
            LocalDateTime end = slot == null ? start.plusHours(8) : slot.end();
            return new ResourceFormData(resource.id(), resource.name(), String.join(", ", resource.skills()),
                    start.format(INPUT_FORMAT), end.format(INPUT_FORMAT), resource.maxWorkloadHours(), resource.location(), resource.salaryPerHour(), resource.availableHoursPerWeek());
        }
    }
}