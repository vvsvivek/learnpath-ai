package com.vrsec.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    private String password;
    private String username; 
    private String role; // "STUDENT", "FACULTY", "ADMIN"

    private String branch;
    private String academicYear;
    private String semester;
    private Double cgpa = 0.0;

    private Boolean isEmailVerified = false;
    private String authProvider = "EMAIL"; // "EMAIL", "GOOGLE"

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (cgpa == null) cgpa = 0.0;
        if (isEmailVerified == null) isEmailVerified = false;
        if (authProvider == null) authProvider = "EMAIL";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Role helper methods
    public boolean isFaculty() {
        return "FACULTY".equalsIgnoreCase(role) ||
               (email != null && (email.toLowerCase().endsWith("@siddhartha.ac.in") || email.equalsIgnoreCase("vivekvaranasi99@gmail.com")));
    }

    public boolean isEligibleStudent() {
        return "STUDENT".equalsIgnoreCase(role) &&
               email != null && email.toLowerCase().endsWith("@vrsec.ac.in");
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email != null ? email.trim().toLowerCase() : null; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role != null ? role.toUpperCase() : "STUDENT"; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public Double getCgpa() { return cgpa; }
    public void setCgpa(Double cgpa) { this.cgpa = cgpa; }
    public Boolean getIsEmailVerified() { return isEmailVerified; }
    public void setIsEmailVerified(Boolean emailVerified) { isEmailVerified = emailVerified; }
    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
