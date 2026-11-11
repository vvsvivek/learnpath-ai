package com.vrsec.backend.controller;

import com.vrsec.backend.exception.EmailDeliveryException;
import com.vrsec.backend.model.*;
import com.vrsec.backend.repository.*;
import com.vrsec.backend.service.AuthService;
import com.vrsec.backend.service.JwtService;
import com.vrsec.backend.service.LearningPathService;
import com.vrsec.backend.service.SubjectCurriculumRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/main")
@CrossOrigin(origins = "*")
@Transactional(readOnly = true)
public class ApiController {

    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CoursePhaseRepository phaseRepository;
    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private TopicPerformanceRepository topicPerformanceRepository;
    @Autowired private LearningPathService learningPathService;
    @Autowired private AuthService authService;
    @Autowired private JwtService jwtService;
    @Autowired private ApprovedCourseRepository approvedCourseRepository;
    @Autowired private CourseRequestRepository courseRequestRepository;

    // =========================================================================
    // 1. AUTH COMPATIBILITY ENDPOINTS (For legacy clients calling /api/main/auth)
    // =========================================================================
    @Transactional(noRollbackFor = {IllegalArgumentException.class, EmailDeliveryException.class})
    @PostMapping({"/auth/send-otp", "/send-otp"})
    public ResponseEntity<?> legacySendOtp(@RequestBody Map<String, String> req) {
        try {
            String email = req.get("email");
            String role = req.get("role");
            String mode = req.get("mode");
            return ResponseEntity.ok(authService.sendOtp(email, role, mode));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (EmailDeliveryException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @Transactional(noRollbackFor = {IllegalArgumentException.class})
    @PostMapping({"/auth", "/auth/verify-otp"})
    public ResponseEntity<?> legacyVerifyOtp(@RequestBody Map<String, String> req) {
        try {
            String email = req.get("email");
            String otp = req.get("otp");
            String username = req.get("username");
            String password = req.get("password");
            String role = req.get("role");
            String mode = req.get("mode");
            return ResponseEntity.ok(authService.verifyOtp(email, otp, username, password, role, mode));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    // =========================================================================
    // 2. STUDENT DASHBOARD (Real DB records, Dynamic Recommendations)
    // =========================================================================
    @GetMapping({"/dashboard", "/student/dashboard"})
    public ResponseEntity<?> getDashboard(@RequestParam String email) {
        try {
            String cleanEmail = email != null ? email.trim().toLowerCase() : "";
            User student = userRepository.findByEmail(cleanEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + cleanEmail));

            List<Course> allCourses = courseRepository.findByUserEmailIgnoreCase(cleanEmail);
            List<Course> activeCourses = allCourses.stream().filter(c -> !c.isDeleted()).collect(Collectors.toList());
            List<Course> deletedCourses = allCourses.stream().filter(Course::isDeleted).collect(Collectors.toList());

            List<QuizAttempt> userAttempts = quizAttemptRepository.findByUserIdOrderByCreatedAtDesc(student.getId());
            List<TopicPerformance> weakTopics = topicPerformanceRepository.findByUserIdAndStatus(student.getId(), "NEEDS_ATTENTION");

            // Format active courses with phase details
            List<Map<String, Object>> formattedCourses = new ArrayList<>();
            for (Course c : activeCourses) {
                Map<String, Object> cmap = new HashMap<>();
                cmap.put("id", c.getId());
                cmap.put("title", c.getTitle());
                cmap.put("description", c.getDescription());
                cmap.put("progress", c.getProgress());
                cmap.put("currentPhase", c.getCurrentPhase());
                cmap.put("totalPhases", c.getTotalPhases());
                cmap.put("difficulty", c.getDifficulty());

                List<CoursePhase> phases = phaseRepository.findByCourseIdOrderByPhaseNumberAsc(c.getId());
                List<Map<String, Object>> pList = new ArrayList<>();
                for (CoursePhase p : phases) {
                    Map<String, Object> pmap = new HashMap<>();
                    pmap.put("id", p.getId());
                    pmap.put("phaseNumber", p.getPhaseNumber());
                    pmap.put("title", p.getTitle());
                    pmap.put("objectives", p.getLearningObjectives());
                    pmap.put("duration", p.getEstimatedDuration());
                    pmap.put("isUnlocked", p.isUnlocked());
                    pmap.put("isCompleted", p.isCompleted());
                    pList.add(pmap);
                }
                cmap.put("phases", pList);
                formattedCourses.add(cmap);
            }

            // Generate personalized recommendations
            List<Map<String, Object>> recommendations = learningPathService.getPersonalizedRecommendations(student);

            Map<String, Object> response = new HashMap<>();
            response.put("profile", authService.sanitizeUser(student));
            response.put("activeCourses", formattedCourses);
            response.put("deletedCourses", deletedCourses);
            response.put("recommendations", recommendations);
            response.put("weakTopics", weakTopics);
            response.put("totalCompletedCourses", activeCourses.stream().filter(c -> c.getProgress() != null && c.getProgress() == 100).count());
            response.put("cgpa", student.getCgpa());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Error loading dashboard: " + e.getMessage()));
        }
    }

    // =========================================================================
    // 3. APPROVED COURSE CATALOGUE & SELECTION (NO ARBITRARY COURSES ALLOWED)
    // =========================================================================

    /**
     * Retrieves the approved course catalogue with eligibility filtering and search.
     */
    @GetMapping("/catalogue")
    public ResponseEntity<?> getCourseCatalogue(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "all") String scope) {
        try {
            learningPathService.syncApprovedCatalogue();
            List<ApprovedCourse> allActive = approvedCourseRepository.findByActiveTrueOrderByTitleAsc();

            User student = null;
            if (email != null && !email.trim().isEmpty()) {
                student = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
            }

            String sBranch = student != null && student.getBranch() != null ? LearningPathService.normalizeBranch(student.getBranch()) : (branch != null ? LearningPathService.normalizeBranch(branch) : null);
            String sYear = student != null && student.getAcademicYear() != null ? LearningPathService.normalizeYear(student.getAcademicYear()) : (year != null ? LearningPathService.normalizeYear(year) : null);
            String sSem = student != null && student.getSemester() != null ? LearningPathService.normalizeSemester(student.getSemester()) : (semester != null ? LearningPathService.normalizeSemester(semester) : null);

            List<Map<String, Object>> result = new ArrayList<>();
            for (ApprovedCourse ac : allActive) {
                boolean branchMatch = sBranch == null || ac.getBranch().equalsIgnoreCase(sBranch)
                        || ac.getBranch().equalsIgnoreCase("SHARED") || ac.getBranch().equalsIgnoreCase("ALL");
                boolean yearMatch = sYear == null || ac.getAcademicYear().equalsIgnoreCase(sYear)
                        || ac.getAcademicYear().equalsIgnoreCase("ANY");

                boolean isEligible = branchMatch && yearMatch;

                if ("eligible".equalsIgnoreCase(scope) && !isEligible) {
                    continue;
                }

                if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("ALL")) {
                    if (!ac.getCategory().equalsIgnoreCase(category.trim())) {
                        continue;
                    }
                }

                if (search != null && !search.trim().isEmpty()) {
                    String q = search.trim().toLowerCase();
                    boolean matches = ac.getTitle().toLowerCase().contains(q)
                            || (ac.getDescription() != null && ac.getDescription().toLowerCase().contains(q))
                            || (ac.getCode() != null && ac.getCode().toLowerCase().contains(q))
                            || (ac.getBranch() != null && ac.getBranch().toLowerCase().contains(q));
                    if (!matches) {
                        continue;
                    }
                }

                Map<String, Object> map = new HashMap<>();
                map.put("id", ac.getId());
                map.put("code", ac.getCode());
                map.put("title", ac.getTitle());
                map.put("description", ac.getDescription());
                map.put("branch", ac.getBranch());
                map.put("academicYear", ac.getAcademicYear());
                map.put("semester", ac.getSemester());
                map.put("difficulty", ac.getDifficulty());
                map.put("duration", ac.getDuration());
                map.put("category", ac.getCategory());
                map.put("isEligible", isEligible);

                boolean isSyllabusVerified = SubjectCurriculumRegistry.isSyllabusVerified(ac.getTitle());
                map.put("isSyllabusVerified", isSyllabusVerified);
                map.put("curriculumSource", isSyllabusVerified ? "Syllabus-Aligned" : "AICTE Domain-Synthesized");
                result.add(map);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Failed to retrieve catalogue: " + e.getMessage()));
        }
    }

    /**
     * Enrolls student in a course ONLY IF it exists in the approved catalogue.
     * Rejects personal names, random phrases, and unapproved courses.
     */
    @Transactional
    @PostMapping({"/course/select", "/generate-path", "/course/enroll"})
    public ResponseEntity<?> selectOrGenerateCourse(@RequestBody Map<String, Object> req) {
        try {
            String email = (String) req.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Email is required."));
            }

            User student = userRepository.findByEmail(email.trim().toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found."));

            // Look up approved course
            ApprovedCourse approvedCourse = null;
            if (req.get("approvedCourseId") != null) {
                Long acid = Long.valueOf(req.get("approvedCourseId").toString());
                approvedCourse = approvedCourseRepository.findByIdAndActiveTrue(acid).orElse(null);
            } else if (req.get("courseId") != null) {
                try {
                    Long cid = Long.valueOf(req.get("courseId").toString());
                    approvedCourse = approvedCourseRepository.findByIdAndActiveTrue(cid).orElse(null);
                } catch (Exception ignored) {}
            }

            if (approvedCourse == null) {
                String subject = (String) req.get("subject");
                if (subject == null) subject = (String) req.get("courseTitle");
                if (subject == null) subject = (String) req.get("title");
                if (subject == null) subject = (String) req.get("code");

                if (subject != null && !subject.trim().isEmpty()) {
                    approvedCourse = approvedCourseRepository.findByTitleIgnoreCaseAndActiveTrue(subject.trim()).orElse(null);
                    if (approvedCourse == null) {
                        approvedCourse = approvedCourseRepository.findByCodeIgnoreCaseAndActiveTrue(subject.trim()).orElse(null);
                    }
                    if (approvedCourse == null) {
                        List<ApprovedCourse> activeList = approvedCourseRepository.findByActiveTrue();
                        if (activeList != null) {
                            String clean = subject.trim().toLowerCase();
                            for (ApprovedCourse ac : activeList) {
                                String actLower = ac.getTitle().toLowerCase();
                                if (actLower.contains(clean) || clean.contains(actLower)
                                        || (clean.contains("data struct") && actLower.contains("data struct"))) {
                                    approvedCourse = ac;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // CRITICAL VALIDATION: Reject arbitrary text, personal names, and unapproved courses
            if (approvedCourse == null) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error",
                        "The selected course is not in the approved catalogue. Arbitrary course titles are not permitted. Please choose an approved course or submit a Course Request."));
            }

            // ACADEMIC ELIGIBILITY VALIDATION: Verify cohort eligibility on the backend
            String sBranch = student.getBranch() != null ? LearningPathService.normalizeBranch(student.getBranch()) : null;
            String cBranch = approvedCourse.getBranch();
            String cCat = approvedCourse.getCategory() != null ? approvedCourse.getCategory().toUpperCase() : "CORE";

            boolean isUniversal = "SHARED".equalsIgnoreCase(cBranch) || "ALL".equalsIgnoreCase(cBranch)
                    || "FOUNDATION".equalsIgnoreCase(cCat) || "HONORS".equalsIgnoreCase(cCat)
                    || "REMEDIAL".equalsIgnoreCase(cCat) || "ELECTIVE".equalsIgnoreCase(cCat);
            boolean branchEligible = isUniversal || (sBranch != null && sBranch.equalsIgnoreCase(cBranch));

            if (!branchEligible) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error",
                        "Academic eligibility check failed: The course '" + approvedCourse.getTitle() + "' is designated for the " + cBranch + " department. Please select a course eligible for your branch (" + sBranch + ") or submit a Course Request."));
            }

            Course course = learningPathService.generateSubjectLearningPath(student, approvedCourse.getTitle());

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Learning path generated for " + course.getTitle());
            res.put("courseId", course.getId());
            res.put("title", course.getTitle());
            res.put("totalPhases", course.getTotalPhases());
            res.put("difficulty", approvedCourse.getDifficulty());
            res.put("category", approvedCourse.getCategory());
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Failed to generate learning path: " + e.getMessage()));
        }
    }

    /**
     * Submits a Course Request when a student cannot find a course in the approved catalogue.
     * Saved as PENDING for faculty review. Does NOT generate a learning path.
     */
    @Transactional
    @PostMapping("/course/request")
    public ResponseEntity<?> requestCourse(@RequestBody Map<String, String> req) {
        try {
            String email = req.get("email");
            String title = req.get("title");
            if (title == null) title = req.get("suggestedTitle");
            String reason = req.get("reason");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Email is required."));
            }
            if (title == null || title.trim().length() < 3) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "A valid course title (at least 3 characters) is required."));
            }

            User student = userRepository.findByEmail(email.trim().toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found."));

            String cleanTitle = title.trim();

            // Check if already in approved catalogue
            if (approvedCourseRepository.existsByTitleIgnoreCaseAndActiveTrue(cleanTitle)) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error",
                        "This course is already available in the approved catalogue. You can enroll in it directly from the catalogue."));
            }

            // Check if student already has a pending request
            List<CourseRequest> userRequests = courseRequestRepository.findByStudentEmailIgnoreCaseOrderByCreatedAtDesc(student.getEmail());
            boolean alreadyPending = userRequests.stream()
                    .anyMatch(r -> r.getSuggestedTitle().equalsIgnoreCase(cleanTitle) && "PENDING".equalsIgnoreCase(r.getStatus()));
            if (alreadyPending) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error",
                        "You already have a pending request for this course. Please wait for faculty review."));
            }

            CourseRequest cr = new CourseRequest(
                    student.getEmail(),
                    student.getUsername(),
                    student.getBranch(),
                    student.getAcademicYear(),
                    student.getSemester(),
                    cleanTitle,
                    reason
            );
            CourseRequest saved = courseRequestRepository.save(cr);

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Course request submitted successfully. It is pending administrator review and approval.");
            res.put("requestId", saved.getId());
            res.put("status", saved.getStatus());
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Failed to submit course request: " + e.getMessage()));
        }
    }

    /**
     * Retrieves course requests for a student or administrator.
     */
    @GetMapping("/course/requests")
    public ResponseEntity<?> getCourseRequests(@RequestParam(required = false) String email) {
        try {
            List<CourseRequest> list;
            if (email != null && !email.trim().isEmpty()) {
                list = courseRequestRepository.findByStudentEmailIgnoreCaseOrderByCreatedAtDesc(email.trim().toLowerCase());
            } else {
                list = courseRequestRepository.findAllByOrderByCreatedAtDesc();
            }
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Failed to retrieve course requests: " + e.getMessage()));
        }
    }

    /**
     * Approves a course request, making it available in the active approved course catalogue.
     */
    @Transactional
    @PostMapping("/course/request/approve")
    public ResponseEntity<?> approveCourseRequest(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> req) {
        try {
            Long requestId = req.get("requestId") != null ? Long.valueOf(req.get("requestId").toString()) : null;

            // SECURITY ENFORCEMENT: Only authenticated and verified faculty/admin can approve courses via JWT
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error",
                        "Authentication required: Missing or invalid Authorization header. A valid bearer token is required."));
            }

            String token = authHeader.substring(7).trim();
            Map<String, Object> claims = jwtService.validateAndExtractClaims(token);
            if (claims == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error",
                        "Authentication failed: Invalid or expired authentication token."));
            }

            String verifiedEmail = (String) claims.get("sub");
            if (verifiedEmail == null || verifiedEmail.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error",
                        "Authentication failed: Token missing subject claim."));
            }

            String cleanApprover = verifiedEmail.trim().toLowerCase();
            User approver = userRepository.findByEmail(cleanApprover).orElse(null);

            boolean isFacultyRole = approver != null && ("FACULTY".equalsIgnoreCase(approver.getRole()) || "ADMIN".equalsIgnoreCase(approver.getRole()));
            boolean isEligibleFacultyEmail = authService != null && authService.isEligibleFacultyEmail(cleanApprover);

            if (!isFacultyRole && !isEligibleFacultyEmail) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("error",
                        "Access denied: Only authorized faculty or administrators can approve course requests. Students cannot access approval privileges."));
            }

            String reviewer = approver != null && approver.getUsername() != null ? approver.getUsername() : cleanApprover;

            if (requestId == null) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Request ID is required."));
            }

            CourseRequest cr = courseRequestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalArgumentException("Course request not found."));

            cr.setStatus("APPROVED");
            cr.setReviewedBy(reviewer);
            cr.setReviewedAt(LocalDateTime.now());

            String code = (String) req.getOrDefault("code", "REQ-" + cr.getId());
            String desc = (String) req.getOrDefault("description", "Approved student curriculum request: " + cr.getReason());
            String branch = (String) req.getOrDefault("branch", cr.getStudentBranch() != null ? cr.getStudentBranch() : "SHARED");
            String year = (String) req.getOrDefault("year", cr.getStudentYear() != null ? cr.getStudentYear() : "ANY");
            String sem = (String) req.getOrDefault("semester", cr.getStudentSemester() != null ? cr.getStudentSemester() : "ANY");

            ApprovedCourse ac = new ApprovedCourse(
                    code,
                    cr.getSuggestedTitle(),
                    desc,
                    branch,
                    year,
                    sem,
                    "Intermediate",
                    "6 Weeks",
                    "ELECTIVE"
            );
            ApprovedCourse savedAc = approvedCourseRepository.save(ac);
            cr.setApprovedCourseId(savedAc.getId());
            courseRequestRepository.save(cr);

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Course request approved and added to active catalogue.");
            res.put("approvedCourse", savedAc);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Approval error: " + e.getMessage()));
        }
    }

    @GetMapping("/phase/details")
    public ResponseEntity<?> getPhaseDetails(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String subject,
            @RequestParam(defaultValue = "1") Integer phaseNumber,
            @RequestParam(required = false) String email) {
        try {
            CoursePhase targetPhase = null;
            if (courseId != null) {
                targetPhase = phaseRepository.findByCourseIdAndPhaseNumber(courseId, phaseNumber).orElse(null);
            }
            if (targetPhase == null && subject != null) {
                List<Course> courses = courseRepository.findAll().stream()
                        .filter(c -> c.getTitle().equalsIgnoreCase(subject.trim()) && !c.isDeleted())
                        .toList();
                if (!courses.isEmpty()) {
                    targetPhase = phaseRepository.findByCourseIdAndPhaseNumber(courses.get(0).getId(), phaseNumber).orElse(null);
                }
            }

            if (targetPhase == null) {
                return ResponseEntity.status(404).body(Collections.singletonMap("error", "Phase details not found."));
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", targetPhase.getId());
            map.put("courseId", targetPhase.getCourse().getId());
            map.put("courseTitle", targetPhase.getCourse().getTitle());
            map.put("phaseNumber", targetPhase.getPhaseNumber());
            map.put("title", targetPhase.getTitle());
            map.put("objectives", targetPhase.getLearningObjectives());
            map.put("studyMaterial", targetPhase.getStudyMaterial());
            map.put("duration", targetPhase.getEstimatedDuration());
            map.put("isUnlocked", targetPhase.isUnlocked());
            map.put("isCompleted", targetPhase.isCompleted());
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @Transactional
    @PostMapping("/course/toggle-delete")
    public ResponseEntity<?> toggleCourseDelete(@RequestBody Map<String, Object> req) {
        try {
            Long courseId = Long.valueOf(req.get("courseId").toString());
            boolean isDeleted = req.containsKey("isDeleted") ? (Boolean) req.get("isDeleted") : true;
            Optional<Course> opt = courseRepository.findById(courseId);
            if (opt.isPresent()) {
                Course c = opt.get();
                c.setDeleted(isDeleted);
                courseRepository.save(c);
                return ResponseEntity.ok(Collections.singletonMap("message", "Course visibility updated"));
            }
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // =========================================================================
    // 4. PROFILE UPDATE
    // =========================================================================
    @Transactional
    @PostMapping("/profile/update")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> req) {
        try {
            String email = (String) req.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Email is required."));
            }
            User user = userRepository.findByEmail(email.trim().toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

            if (req.get("username") != null && !req.get("username").toString().trim().isEmpty()) {
                user.setUsername(req.get("username").toString().trim());
            }
            if (req.get("branch") != null && !req.get("branch").toString().trim().isEmpty()) {
                user.setBranch(req.get("branch").toString().trim().toUpperCase());
            }
            if (req.get("academicYear") != null && !req.get("academicYear").toString().trim().isEmpty()) {
                user.setAcademicYear(req.get("academicYear").toString().trim());
            } else if (req.get("year") != null && !req.get("year").toString().trim().isEmpty()) {
                user.setAcademicYear(req.get("year").toString().trim());
            }
            if (req.get("semester") != null && !req.get("semester").toString().trim().isEmpty()) {
                user.setSemester(req.get("semester").toString().trim());
            } else if (req.get("sem") != null && !req.get("sem").toString().trim().isEmpty()) {
                user.setSemester(req.get("sem").toString().trim());
            }

            if (req.containsKey("cgpa") && req.get("cgpa") != null) {
                String cgpaStr = req.get("cgpa").toString().trim();
                if (cgpaStr.isEmpty()) {
                    throw new IllegalArgumentException("CGPA cannot be empty. Please enter a valid number between 0.00 and 10.00.");
                }
                double parsed;
                try {
                    parsed = Double.parseDouble(cgpaStr);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid CGPA format. Please enter a valid numerical value between 0.00 and 10.00.");
                }
                if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
                    throw new IllegalArgumentException("Invalid CGPA value.");
                }
                if (parsed < 0.0 || parsed > 10.0) {
                    throw new IllegalArgumentException("CGPA must be between 0.00 and 10.00.");
                }
                int dotIdx = cgpaStr.indexOf('.');
                if (dotIdx >= 0 && (cgpaStr.length() - dotIdx - 1) > 2) {
                    throw new IllegalArgumentException("CGPA allows up to 2 decimal places only.");
                }
                double rounded = Math.round(parsed * 100.0) / 100.0;
                user.setCgpa(rounded);
            }

            userRepository.save(user);
            return ResponseEntity.ok(authService.sanitizeUser(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
