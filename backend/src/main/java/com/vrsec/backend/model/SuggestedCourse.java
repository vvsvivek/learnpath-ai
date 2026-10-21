package com.vrsec.backend.model;

import jakarta.persistence.*;

@Entity
public class SuggestedCourse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String targetSemester;
    private String facultyName;
    private String facultyEmail;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTargetSemester() { return targetSemester; }
    public void setTargetSemester(String targetSemester) { this.targetSemester = targetSemester; }
    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }
    public String getFacultyEmail() { return facultyEmail; }
    public void setFacultyEmail(String facultyEmail) { this.facultyEmail = facultyEmail; }
}