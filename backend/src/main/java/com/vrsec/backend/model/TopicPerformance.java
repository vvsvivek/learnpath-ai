package com.vrsec.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "topic_performance")
public class TopicPerformance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String courseTitle;

    @Column(nullable = false)
    private String conceptTag;

    private int timesTested = 1;
    private int timesIncorrect = 0;

    private String status = "NEEDS_ATTENTION"; // "NEEDS_ATTENTION", "DEVELOPING", "MASTERED"

    private LocalDateTime lastEvaluatedAt = LocalDateTime.now();

    public void recordResult(boolean correct) {
        this.timesTested++;
        if (!correct) {
            this.timesIncorrect++;
        }
        double failRate = (double) timesIncorrect / timesTested;
        if (failRate >= 0.40) {
            this.status = "NEEDS_ATTENTION";
        } else if (failRate >= 0.15) {
            this.status = "DEVELOPING";
        } else {
            this.status = "MASTERED";
        }
        this.lastEvaluatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public String getConceptTag() { return conceptTag; }
    public void setConceptTag(String conceptTag) { this.conceptTag = conceptTag; }
    public int getTimesTested() { return timesTested; }
    public void setTimesTested(int timesTested) { this.timesTested = timesTested; }
    public int getTimesIncorrect() { return timesIncorrect; }
    public void setTimesIncorrect(int timesIncorrect) { this.timesIncorrect = timesIncorrect; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastEvaluatedAt() { return lastEvaluatedAt; }
    public void setLastEvaluatedAt(LocalDateTime lastEvaluatedAt) { this.lastEvaluatedAt = lastEvaluatedAt; }
}
