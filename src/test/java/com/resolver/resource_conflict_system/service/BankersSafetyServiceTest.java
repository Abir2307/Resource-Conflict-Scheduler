package com.resolver.resource_conflict_system.service;

import com.resolver.resource_conflict_system.domain.AssetItem;
import com.resolver.resource_conflict_system.domain.AvailabilitySlot;
import com.resolver.resource_conflict_system.domain.ProjectTask;
import com.resolver.resource_conflict_system.domain.ResourceProfile;
import com.resolver.resource_conflict_system.domain.SafetyAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class BankersSafetyServiceTest {

    @Autowired
    BankersSafetyService bankersSafetyService;

    @Test
    public void analyzeSafetyBuildsExpectedSafeRoute() {
    LocalDateTime start = LocalDateTime.of(2026, 5, 25, 9, 0);

    List<ProjectTask> tasks = List.of(
        new ProjectTask("T-1", "P-1", "Setup", Duration.ofHours(2), Set.of("JAVA"), 10, start,
            start.plusDays(1), List.of(), List.of("user"), List.of(), "Site-1", false),
        new ProjectTask("T-2", "P-1", "Build", Duration.ofHours(2), Set.of("JAVA"), 9, start.plusHours(2),
            start.plusDays(1), List.of("T-1"), List.of("user"), List.of(), "Site-1", false)
    );

    List<ResourceProfile> resources = List.of(
        new ResourceProfile("R-1", "Alice", Set.of("JAVA"),
            List.of(new AvailabilitySlot(start, start.plusHours(8))), 8, List.of(), "Office-A",
            new BigDecimal("50.00"), 40)
    );

    List<AssetItem> assets = List.of();

    SafetyAdvice advice = bankersSafetyService.analyzeSafety(tasks, resources, assets, 8);

    assertThat(advice.currentSafe()).isTrue();
    assertThat(advice.safeAfterSuggestedChanges()).isTrue();
    assertThat(advice.safeRoute()).containsExactly("Setup", "Build");
    assertThat(advice.extraResourcesBySkill()).isEmpty();
    assertThat(advice.extraAssetsById()).isEmpty();
    }
}
