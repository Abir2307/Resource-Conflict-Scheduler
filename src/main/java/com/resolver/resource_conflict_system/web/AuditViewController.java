package com.resolver.resource_conflict_system.web;

import com.resolver.resource_conflict_system.repository.AuditRecordRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AuditViewController {

    private final AuditRecordRepository auditRecordRepository;

    public AuditViewController(AuditRecordRepository auditRecordRepository) {
        this.auditRecordRepository = auditRecordRepository;
    }

    @GetMapping("/audits")
    public String auditsPage(Model model) {
        model.addAttribute("audits", auditRecordRepository.findAll());
        return "audits";
    }

    @GetMapping("/audits/{id}")
    public String viewAudit(@PathVariable long id, Model model) {
        return auditRecordRepository.findById(id)
                .map(audit -> {
                    model.addAttribute("audit", audit);
                    return "audit_view";
                })
                .orElse("redirect:/audits");
    }
}