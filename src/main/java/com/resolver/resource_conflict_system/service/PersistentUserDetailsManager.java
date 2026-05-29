package com.resolver.resource_conflict_system.service;

import com.resolver.resource_conflict_system.entity.UserAccountEntity;
import com.resolver.resource_conflict_system.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class PersistentUserDetailsManager implements UserDetailsManager, UserDetailsPasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PersistentUserDetailsManager(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccountEntity account = userRepository.findById(java.util.Objects.requireNonNull(username, "username must not be null"))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .roles(splitRoles(account.getRolesCsv()))
                .build();
    }

    @Override
    @Transactional
    public void createUser(UserDetails user) {
        String username = java.util.Objects.requireNonNull(user.getUsername(), "username must not be null");
        if (userExists(username)) {
            throw new DataIntegrityViolationException("Username already exists: " + username);
        }
        userRepository.save(new UserAccountEntity(
                username,
                user.getPassword(),
                joinRoles(user.getAuthorities())));
        userRepository.findById(username).ifPresent(account -> {
            account.setDisplayName(username);
            userRepository.save(account);
        });
    }

    @Override
    @Transactional
    public void updateUser(UserDetails user) {
        String username = java.util.Objects.requireNonNull(user.getUsername(), "username must not be null");
        UserAccountEntity account = userRepository.findById(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        account.setPasswordHash(java.util.Objects.requireNonNull(user.getPassword(), "password must not be null"));
        account.setRolesCsv(joinRoles(user.getAuthorities()));
        userRepository.save(account);
    }

    @Override
    @Transactional
    public void deleteUser(String username) {
        userRepository.deleteById(java.util.Objects.requireNonNull(username, "username must not be null"));
    }

    @Override
    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authenticated user available.");
        }
        String username = java.util.Objects.requireNonNull(authentication.getName(), "username must not be null");
        UserAccountEntity account = userRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        if (!passwordEncoder.matches(oldPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("Old password does not match.");
        }
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userExists(String username) {
        return userRepository.existsById(java.util.Objects.requireNonNull(username, "username must not be null"));
    }

    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        String username = java.util.Objects.requireNonNull(user.getUsername(), "username must not be null");
        UserAccountEntity account = userRepository.findById(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        account.setPasswordHash(java.util.Objects.requireNonNull(newPassword, "newPassword must not be null"));
        userRepository.save(account);
        return loadUserByUsername(username);
    }

    private static String joinRoles(Collection<? extends GrantedAuthority> authorities) {
        return AuthorityUtils.authorityListToSet(authorities).stream()
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private static String[] splitRoles(String rolesCsv) {
        if (rolesCsv == null || rolesCsv.isBlank()) {
            return new String[] {"USER"};
        }
        return Arrays.stream(rolesCsv.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .toArray(String[]::new);
    }
}