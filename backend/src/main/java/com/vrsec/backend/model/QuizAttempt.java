package com.vrsec.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "phase_id")
    private CoursePhase phase;

    private Integer score = 0;
    private Integer totalQuestions = 10;
    private Double percentage = 0.0;
    private Boolean passed = false;

    private String topic;
    private Integer attemptNumber = 1;

    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (totalQuestions == null || totalQuestions == 0) totalQuestions = 10;
        if (score == null) score = 0;
        this.percentage = ((double) score / totalQuestions) * 100.0;
        // Strictly > 60% rule: score / totalQuestions > 0.60
        this.passed = this.percentage > 60.0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public CoursePhase getPhase() { return phase; }
    public void setPhase(CoursePhase phase) { this.phase = phase; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { 
        this.score = score;
        if (this.totalQuestions != null && this.totalQuestions > 0) {
            this.percentage = ((double) score / this.totalQuestions) * 100.0;
            this.passed = this.percentage > 60.0;
        }
    }
    public Integer getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Integer totalQuestions) { 
        this.totalQuestions = totalQuestions;
        if (totalQuestions != null && totalQuestions > 0 && this.score != null) {
            this.percentage = ((double) this.score / totalQuestions) * 100.0;
            this.passed = this.percentage > 60.0;
        }
    }
    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
