package com.vrsec.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_phases")
public class CoursePhase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id")
    private Course course;

    private Integer phaseNumber;
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String learningObjectives;

    @Column(columnDefinition = "TEXT")
    private String studyMaterial;

    private String estimatedDuration = "2-3 Hours";

    private Boolean isUnlocked = false;
    private Boolean isCompleted = false;
    private LocalDateTime completedAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public Integer getPhaseNumber() { return phaseNumber; }
    public void setPhaseNumber(Integer phaseNumber) { this.phaseNumber = phaseNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLearningObjectives() { return learningObjectives; }
    public void setLearningObjectives(String learningObjectives) { this.learningObjectives = learningObjectives; }
    public String getStudyMaterial() { return studyMaterial; }
    public void setStudyMaterial(String studyMaterial) { this.studyMaterial = studyMaterial; }
    public String getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(String estimatedDuration) { this.estimatedDuration = estimatedDuration; }
    public boolean isUnlocked() { return isUnlocked != null && isUnlocked; }
    public Boolean getIsUnlocked() { return isUnlocked; }
    public void setUnlocked(Boolean unlocked) { isUnlocked = unlocked != null ? unlocked : false; }
    public boolean isCompleted() { return isCompleted != null && isCompleted; }
    public Boolean getIsCompleted() { return isCompleted; }
    public void setCompleted(Boolean completed) { isCompleted = completed != null ? completed : false; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
