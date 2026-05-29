package com.resolver.resource_conflict_system.service;

import com.resolver.resource_conflict_system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PersistentUserDetailsManagerTest {

    @Autowired
    PersistentUserDetailsManager userDetailsManager;

    @Autowired
    UserRepository userRepository;

    @Test
    void seededAdminAndUserArePersisted() {
        assertThat(userDetailsManager.userExists("admin")).isTrue();
        assertThat(userDetailsManager.userExists("user")).isTrue();
        assertThat(userRepository.findById("admin")).isPresent();
        assertThat(userRepository.findById("user")).isPresent();
    }
}