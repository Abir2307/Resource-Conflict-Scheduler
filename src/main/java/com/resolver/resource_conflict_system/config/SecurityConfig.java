package com.resolver.resource_conflict_system.config;

import com.resolver.resource_conflict_system.service.PersistentUserDetailsManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/css/**", "/js/**", "/error", "/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/").permitAll()
                .requestMatchers(HttpMethod.GET, "/resources", "/assets", "/tasks", "/audits", "/account", "/admin/users", "/admin/users/search", "/api/scheduler/resources", "/api/scheduler/tasks", "/api/scheduler/audits", "/api/scheduler/audits/*").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/account/profile").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/tasks/status").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/admin/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/resources/new", "/resources/edit/**", "/tasks/new", "/tasks/edit/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/assets/new", "/assets/edit/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/resources/save", "/resources/delete", "/tasks/save", "/tasks/delete", "/assets/save", "/assets/delete", "/api/scheduler/resources", "/api/scheduler/tasks", "/api/scheduler/simulate", "/api/scheduler/reset", "/api/scheduler/resources/*", "/api/scheduler/tasks/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/resources/request").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/admin/availability-requests").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/admin/availability-requests/*/approve", "/admin/availability-requests/*/reject").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/scheduler/resources/*", "/api/scheduler/tasks/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/scheduler/resources/*", "/api/scheduler/tasks/*").hasRole("ADMIN")
                .requestMatchers("/simulate/ui").hasRole("ADMIN")
                .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
                .logout(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner seedDefaultUsers(PersistentUserDetailsManager userDetailsManager, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userDetailsManager.userExists("admin")) {
                userDetailsManager.createUser(User.withUsername("admin")
                        .password(passwordEncoder.encode("Abir@1991"))
                        .roles("ADMIN")
                        .build());
            }
        };
    }
}
