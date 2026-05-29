package com.resolver.resource_conflict_system.web;

import com.resolver.resource_conflict_system.service.ResourceAvailabilityRequestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;

@Controller
public class AvailabilityRequestController {

    private final ResourceAvailabilityRequestService requestService;

    public AvailabilityRequestController(ResourceAvailabilityRequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping("/resources/request")
    public String requestAvailability(@RequestParam int requestedHours, Authentication authentication, Model model) {
        String username = authentication == null ? "" : authentication.getName();
        LocalDate today = LocalDate.now();
        LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        requestService.createRequest(username, requestedHours, today, sunday);
        return "redirect:/account?request_submitted";
    }

    @GetMapping("/admin/availability-requests")
    public String adminList(Model model) {
        model.addAttribute("requests", requestService.listPending());
        return "admin-availability-requests";
    }

    @PostMapping("/admin/availability-requests/{id}/approve")
    public String approve(@PathVariable Long id, Authentication authentication) {
        String admin = authentication == null ? "admin" : authentication.getName();
        requestService.approve(id, admin);
        return "redirect:/admin/availability-requests";
    }

    @PostMapping("/admin/availability-requests/{id}/reject")
    public String reject(@PathVariable Long id, Authentication authentication) {
        String admin = authentication == null ? "admin" : authentication.getName();
        requestService.reject(id, admin);
        return "redirect:/admin/availability-requests";
    }
}
