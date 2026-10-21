package com.vrsec.backend.model;

import jakarta.persistence.*;

@Entity
public class CoursePhase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Course course;

    private Integer phaseNumber;
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String studyMaterial;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public Integer getPhaseNumber() { return phaseNumber; }
    public void setPhaseNumber(Integer phaseNumber) { this.phaseNumber = phaseNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStudyMaterial() { return studyMaterial; }
    public void setStudyMaterial(String studyMaterial) { this.studyMaterial = studyMaterial; }
}