package com.resolver.resource_conflict_system.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class PageSecurityAdvice {

    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    @ModelAttribute("currentRole")
    public String currentRole() {
        if (hasRole("ROLE_ADMIN")) {
            return "ADMIN";
        }
        if (hasRole("ROLE_USER")) {
            return "USER";
        }
        return "GUEST";
    }

    @ModelAttribute("currentUsername")
    public String currentUsername() {
        if (isGuest()) {
            return "";
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "";
        }
        return authentication.getName();
    }

    private boolean isGuest() {
        return !hasRole("ROLE_ADMIN") && !hasRole("ROLE_USER");
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
