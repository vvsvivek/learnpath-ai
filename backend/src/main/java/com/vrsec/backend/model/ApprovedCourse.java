package com.vrsec.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "approved_courses")
public class ApprovedCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_branch")
    private String branch = "SHARED"; // "CSE", "IT", "ECE", "EEE", "MECH", "CIVIL", "AIML", "SHARED", "ALL"

    @Column(name = "academic_year")
    private String academicYear = "1st Year"; // "1st Year", "2nd Year", "3rd Year", "4th Year", "ANY"

    private String semester = "1st Sem"; // "1st Sem", "2nd Sem", "ANY"

    private String difficulty = "Intermediate"; // "Foundational", "Intermediate", "Advanced", "Specialized", "Challenging", "Revision"

    private String duration = "6 Weeks";

    private String category = "CORE"; // "CORE", "FOUNDATION", "HONORS", "REMEDIAL", "ELECTIVE"

    @Column(name = "is_active")
    private Boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    public ApprovedCourse() {}

    public ApprovedCourse(String code, String title, String description, String branch, String academicYear, String semester, String difficulty, String duration, String category) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.branch = branch;
        this.academicYear = academicYear;
        this.semester = semester;
        this.difficulty = difficulty;
        this.duration = duration;
        this.category = category;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public boolean isActive() { return active != null && active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
