package com.resolver.resource_conflict_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "users")
public class UserAccountEntity {

    @Id
    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String rolesCsv;

    @Column(nullable = false)
    private String skillsCsv = "";

    @Column(nullable = false)
    private String displayName = "";

    @Column(nullable = false)
    private String location = "";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salaryPerHour = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean skillsVerified;

    @Column(nullable = false)
    private boolean locationVerified;

    @Column(nullable = false)
    private boolean salaryVerified;

    protected UserAccountEntity() {
    }

    public UserAccountEntity(String username, String passwordHash, String rolesCsv) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.rolesCsv = rolesCsv;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRolesCsv() {
        return rolesCsv;
    }

    public void setRolesCsv(String rolesCsv) {
        this.rolesCsv = rolesCsv;
    }

    public String getSkillsCsv() {
        return skillsCsv;
    }

    public void setSkillsCsv(String skillsCsv) {
        this.skillsCsv = skillsCsv == null ? "" : skillsCsv;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName == null ? "" : displayName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location == null ? "" : location;
    }

    public BigDecimal getSalaryPerHour() {
        return salaryPerHour;
    }

    public void setSalaryPerHour(BigDecimal salaryPerHour) {
        this.salaryPerHour = salaryPerHour == null ? BigDecimal.ZERO : salaryPerHour;
    }

    public boolean isSkillsVerified() {
        return skillsVerified;
    }

    public void setSkillsVerified(boolean skillsVerified) {
        this.skillsVerified = skillsVerified;
    }

    public boolean isLocationVerified() {
        return locationVerified;
    }

    public void setLocationVerified(boolean locationVerified) {
        this.locationVerified = locationVerified;
    }

    public boolean isSalaryVerified() {
        return salaryVerified;
    }

    public void setSalaryVerified(boolean salaryVerified) {
        this.salaryVerified = salaryVerified;
    }
}