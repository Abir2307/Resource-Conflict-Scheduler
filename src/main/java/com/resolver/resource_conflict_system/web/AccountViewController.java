package com.resolver.resource_conflict_system.web;

import com.resolver.resource_conflict_system.domain.ProjectTask;
import com.resolver.resource_conflict_system.entity.UserAccountEntity;
import com.resolver.resource_conflict_system.repository.UserRepository;
import com.resolver.resource_conflict_system.service.ResourceCatalogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Map;

@Controller
public class AccountViewController {

    private final ResourceCatalogService resourceCatalogService;
    private final UserRepository userRepository;

    public AccountViewController(ResourceCatalogService resourceCatalogService, UserRepository userRepository) {
        this.resourceCatalogService = resourceCatalogService;
        this.userRepository = userRepository;
    }

    @GetMapping("/account")
    public String account(Authentication authentication, Model model) {
        String username = authentication == null ? "" : authentication.getName();
        boolean isAdmin = authentication != null && AuthorityUtils.authorityListToSet(authentication.getAuthorities()).contains("ROLE_ADMIN");
        UserAccountEntity account = userRepository.findById(Objects.requireNonNull(username, "Username must not be null")).orElse(null);
        if (account == null) {
            return "redirect:/logout";
        }

        List<ProjectTask> assignedTasks = isAdmin
                ? resourceCatalogService.snapshotTasks()
                : resourceCatalogService.snapshotTasks().stream()
                .filter(task -> task.assigneeUsernames().contains(username))
                .toList();

        model.addAttribute("profile", ProfileViewData.from(account));
        model.addAttribute("profileForm", ProfileFormData.from(account));
        model.addAttribute("username", username);
        model.addAttribute("displayName", displayName(account));
        model.addAttribute("assignedTasks", assignedTasks);
        model.addAttribute("taskStatuses", username.isBlank() ? Map.of() : resourceCatalogService.findTaskStatusesForUser(username));
        model.addAttribute("currentRole", isAdmin ? "ADMIN" : "USER");
        model.addAttribute("assignedTaskCount", assignedTasks.size());
        model.addAttribute("roleLabel", isAdmin ? "ADMIN" : "USER");
        return "account";
    }

    @PostMapping("/account/profile")
    public String updateProfile(Authentication authentication,
                               @RequestParam String displayName,
                               @RequestParam String skillsCsv,
                               @RequestParam String location,
                               @RequestParam BigDecimal salaryPerHour) {
        String username = authentication == null ? "" : authentication.getName();
        UserAccountEntity account = userRepository.findById(Objects.requireNonNull(username, "Username must not be null"))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        boolean skillsChanged = !Objects.equals(normalize(account.getSkillsCsv()), normalize(skillsCsv));
        boolean locationChanged = !Objects.equals(normalize(account.getLocation()), normalize(location));
        boolean salaryChanged = account.getSalaryPerHour().compareTo(salaryPerHour) != 0;

        account.setDisplayName(displayName);
        account.setSkillsCsv(skillsCsv);
        account.setLocation(location);
        account.setSalaryPerHour(salaryPerHour);
        if (skillsChanged) {
            account.setSkillsVerified(false);
        }
        if (locationChanged) {
            account.setLocationVerified(false);
        }
        if (salaryChanged) {
            account.setSalaryVerified(false);
        }
        userRepository.save(account);
        return "redirect:/account?updated";
    }

    @GetMapping("/admin/users")
    public String adminUsers(Model model, Authentication authentication) {
        return adminUsers(model, authentication, null);
    }

    @GetMapping("/admin/users/search")
    public String adminUsers(Model model, Authentication authentication, @RequestParam(required = false) String query) {
        boolean isAdmin = authentication != null && AuthorityUtils.authorityListToSet(authentication.getAuthorities()).contains("ROLE_ADMIN");
        if (!isAdmin) {
            return "redirect:/account";
        }
        String normalized = query == null ? "" : query.trim();
        List<UserAccountEntity> allProfiles = userRepository.findAll();
        List<UserAccountEntity> profiles = normalized.isBlank()
            ? allProfiles
            : allProfiles.stream().filter(profile -> displayName(profile).toLowerCase().contains(normalized.toLowerCase())
                || profile.getUsername().toLowerCase().contains(normalized.toLowerCase())).toList();
        model.addAttribute("profiles", profiles.stream().map(ProfileViewData::from).toList());
        model.addAttribute("searchQuery", normalized);
        model.addAttribute("selectedUsername", profiles.size() == 1 ? profiles.get(0).getUsername() : "");
        model.addAttribute("selectedTasks", profiles.size() == 1
                ? resourceCatalogService.snapshotTasks().stream().filter(task -> task.assigneeUsernames().contains(profiles.get(0).getUsername())).toList()
                : List.of());
        model.addAttribute("selectedTaskStatuses", profiles.size() == 1
                ? resourceCatalogService.findTaskStatusesForUser(profiles.get(0).getUsername())
                : Map.of());
        return "admin-users";
    }

    @PostMapping("/admin/users/{username}/verify")
    public String verifyUserProfile(@PathVariable String username,
                                    @RequestParam(required = false) boolean skills,
                                    @RequestParam(required = false) boolean location,
                                    @RequestParam(required = false) boolean salary,
                                    Authentication authentication) {
        boolean isAdmin = authentication != null && AuthorityUtils.authorityListToSet(authentication.getAuthorities()).contains("ROLE_ADMIN");
        if (!isAdmin) {
            return "redirect:/account";
        }
        UserAccountEntity account = userRepository.findById(Objects.requireNonNull(username, "Username must not be null"))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        if (skills) {
            account.setSkillsVerified(true);
        }
        if (location) {
            account.setLocationVerified(true);
        }
        if (salary) {
            account.setSalaryVerified(true);
        }
        userRepository.save(Objects.requireNonNull(account, "Account must not be null"));
        return "redirect:/admin/users";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String displayName(UserAccountEntity account) {
        String displayName = account.getDisplayName();
        return displayName == null || displayName.isBlank() ? account.getUsername() : displayName;
    }

    public record ProfileViewData(String username, String skillsCsv, String location, BigDecimal salaryPerHour,
                                  boolean skillsVerified, boolean locationVerified, boolean salaryVerified, String displayName) {
        static ProfileViewData from(UserAccountEntity account) {
            return new ProfileViewData(account.getUsername(), account.getSkillsCsv(), account.getLocation(),
                    account.getSalaryPerHour(), account.isSkillsVerified(), account.isLocationVerified(), account.isSalaryVerified(), AccountViewController.displayName(account));
        }
    }

    public record ProfileFormData(String displayName, String skillsCsv, String location, BigDecimal salaryPerHour) {
        static ProfileFormData from(UserAccountEntity account) {
            return new ProfileFormData(AccountViewController.displayName(account), account.getSkillsCsv(), account.getLocation(), account.getSalaryPerHour());
        }
    }
}
