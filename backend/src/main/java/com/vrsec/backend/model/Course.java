package com.vrsec.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer progress = 0;
    private String difficulty = "Standard";
    private String userEmail;

    private Integer currentPhase = 1;
    private Integer totalPhases = 4;
    @Column(name = "is_custom")
    private Boolean isCustom = false;

    @Column(name = "is_deleted")
    private Boolean deleted = false;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (progress == null) progress = 0;
        if (currentPhase == null) currentPhase = 1;
        if (totalPhases == null) totalPhases = 4;
        if (difficulty == null) difficulty = "Standard";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail != null ? userEmail.trim().toLowerCase() : null; }
    public Integer getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(Integer currentPhase) { this.currentPhase = currentPhase; }
    public Integer getTotalPhases() { return totalPhases; }
    public void setTotalPhases(Integer totalPhases) { this.totalPhases = totalPhases; }
    public boolean isCustom() { return isCustom != null && isCustom; }
    public Boolean getIsCustom() { return isCustom; }
    public void setCustom(Boolean custom) { isCustom = custom != null ? custom : false; }
    public boolean isDeleted() { return deleted != null && deleted; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted != null ? deleted : false; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
