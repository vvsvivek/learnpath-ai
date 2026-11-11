package com.vrsec.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_requests")
public class CourseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentEmail;

    private String studentName;
    private String studentBranch;
    private String studentYear;
    private String studentSemester;

    @Column(nullable = false)
    private String suggestedTitle;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private String status = "PENDING"; // "PENDING", "APPROVED", "REJECTED"

    @Column(name = "approved_course_id")
    private Long approvedCourseId;

    private String reviewedBy;
    private String reviewNotes;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;

    public CourseRequest() {}

    public CourseRequest(String studentEmail, String studentName, String studentBranch, String studentYear, String studentSemester, String suggestedTitle, String reason) {
        this.studentEmail = studentEmail != null ? studentEmail.trim().toLowerCase() : "";
        this.studentName = studentName;
        this.studentBranch = studentBranch;
        this.studentYear = studentYear;
        this.studentSemester = studentSemester;
        this.suggestedTitle = suggestedTitle != null ? suggestedTitle.trim() : "";
        this.reason = reason != null ? reason.trim() : "";
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail != null ? studentEmail.trim().toLowerCase() : ""; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentBranch() { return studentBranch; }
    public void setStudentBranch(String studentBranch) { this.studentBranch = studentBranch; }

    public String getStudentYear() { return studentYear; }
    public void setStudentYear(String studentYear) { this.studentYear = studentYear; }

    public String getStudentSemester() { return studentSemester; }
    public void setStudentSemester(String studentSemester) { this.studentSemester = studentSemester; }

    public String getSuggestedTitle() { return suggestedTitle; }
    public void setSuggestedTitle(String suggestedTitle) { this.suggestedTitle = suggestedTitle; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getApprovedCourseId() { return approvedCourseId; }
    public void setApprovedCourseId(Long approvedCourseId) { this.approvedCourseId = approvedCourseId; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
