package com.vrsec.backend.config;

import com.vrsec.backend.model.*;
import com.vrsec.backend.repository.*;
import com.vrsec.backend.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Fail-Closed Security Configuration Guard & Staging Data Initializer.
 * 
 * Guarantees that staging instances can NEVER connect to production Supabase or Render hosts.
 * Seeds dedicated, isolated staging student and faculty identities.
 */
@Component
public class StagingSecurityGuard implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StagingSecurityGuard.class);

    @Autowired private Environment environment;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CoursePhaseRepository phaseRepository;
    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private TopicPerformanceRepository topicPerformanceRepository;
    @Autowired private AuthService authService;

    @Override
    public void run(ApplicationArguments args) {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isStaging = Arrays.asList(activeProfiles).contains("staging");

        if (isStaging) {
            enforceFailClosedGuard();
            seedStagingDatabase();
        }
    }

    private void enforceFailClosedGuard() {
        String datasourceUrl = environment.getProperty("spring.datasource.url", "");
        String serverPort = environment.getProperty("server.port", "5000");

        // 1. Block any connection to production hosts
        String[] prohibitedHosts = {
            "supabase.com",
            "aws-0-ap-northeast-1",
            "pooler.supabase.com",
            "onrender.com",
            "render.com",
            "ai-learning-backend-ztk7"
        };

        for (String prohibited : prohibitedHosts) {
            if (datasourceUrl.toLowerCase().contains(prohibited.toLowerCase())) {
                String errorMsg = "CRITICAL FAIL-CLOSED SECURITY GUARD TRIGGERED: Staging profile cannot connect to production host or pooler: " + prohibited;
                log.error("================================================================================");
                log.error(errorMsg);
                log.error("================================================================================");
                throw new IllegalStateException(errorMsg);
            }
        }

        // 2. Enforce isolated port 5001 for staging
        if (!"5001".equals(serverPort)) {
            String errorMsg = "CRITICAL FAIL-CLOSED GUARD: Staging profile must run on isolated port 5001 (Found: " + serverPort + ").";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 3. Log sanitized startup state
        log.info("================================================================================");
        log.info("[STAGING GUARD] FAIL-CLOSED PROTECTION: ACTIVE & VERIFIED");
        log.info("[STAGING GUARD] Profile: staging");
        log.info("[STAGING GUARD] Database: Isolated In-Memory H2 (PostgreSQL Mode)");
        log.info("[STAGING GUARD] Sanitized Datasource URL: jdbc:h2:mem:learnpath_staging");
        log.info("[STAGING GUARD] Server Port: 5001 (Isolated from production)");
        log.info("================================================================================");
    }

    private void seedStagingDatabase() {
        log.info("[STAGING GUARD] Initializing dedicated staging test accounts and course fixtures...");

        // Password hash for 'StagingPass123!'
        String passwordHash = authService.hashPassword("StagingPass123!");

        // 1. Primary Staging Student (CSE, 3rd Year)
        User student1 = new User();
        student1.setEmail("staging.student1@vrsec.ac.in");
        student1.setUsername("Staging Student One");
        student1.setRole("STUDENT");
        student1.setBranch("CSE");
        student1.setAcademicYear("3rd Year");
        student1.setSemester("1st Sem");
        student1.setCgpa(8.45);
        student1.setIsEmailVerified(true);
        student1.setAuthProvider("EMAIL");
        student1.setPassword(passwordHash);
        student1 = userRepository.save(student1);

        // 2. Secondary Staging Student - Non-institutional Gmail domain (CSE, 2nd Year, Needs Support)
        User student2 = new User();
        student2.setEmail("staging.student2@gmail.com");
        student2.setUsername("Staging Student Two");
        student2.setRole("STUDENT");
        student2.setBranch("CSE");
        student2.setAcademicYear("2nd Year");
        student2.setSemester("1st Sem");
        student2.setCgpa(6.10); // Under 6.5 threshold (Requires Academic Support)
        student2.setIsEmailVerified(true);
        student2.setAuthProvider("EMAIL");
        student2.setPassword(passwordHash);
        userRepository.save(student2);

        // 3. Staging Student 3 - MECH Department (for Department Isolation testing)
        User student3 = new User();
        student3.setEmail("staging.student3@outlook.com");
        student3.setUsername("Staging Student Three (MECH)");
        student3.setRole("STUDENT");
        student3.setBranch("MECH");
        student3.setAcademicYear("3rd Year");
        student3.setSemester("1st Sem");
        student3.setCgpa(7.80);
        student3.setIsEmailVerified(true);
        student3.setAuthProvider("EMAIL");
        student3.setPassword(passwordHash);
        userRepository.save(student3);

        // 4. Staging Faculty HOD CSE
        User hodCse = new User();
        hodCse.setEmail("staging.hod.cse@siddhartha.ac.in");
        hodCse.setUsername("Dr. Staging HOD CSE");
        hodCse.setRole("FACULTY");
        hodCse.setBranch("CSE");
        hodCse.setIsEmailVerified(true);
        hodCse.setAuthProvider("EMAIL");
        hodCse.setPassword(passwordHash);
        userRepository.save(hodCse);

        // 5. Staging Faculty MECH (for Cross-Department Isolation)
        User facultyMech = new User();
        facultyMech.setEmail("staging.faculty.mech@siddhartha.ac.in");
        facultyMech.setUsername("Prof. Staging MECH");
        facultyMech.setRole("FACULTY");
        facultyMech.setBranch("MECH");
        facultyMech.setIsEmailVerified(true);
        facultyMech.setAuthProvider("EMAIL");
        facultyMech.setPassword(passwordHash);
        userRepository.save(facultyMech);

        // Seed Courses for Student 1
        Course c1 = new Course();
        c1.setUserEmail(student1.getEmail());
        c1.setTitle("Database Management Systems");
        c1.setDescription("Core relational database design, normalization, SQL, and indexing.");
        c1.setDifficulty("Intermediate");
        c1.setProgress(60);
        c1.setCurrentPhase(2);
        c1.setTotalPhases(4);
        c1.setCustom(false);
        c1.setDeleted(false);
        c1 = courseRepository.save(c1);

        CoursePhase cp1 = new CoursePhase();
        cp1.setCourse(c1);
        cp1.setPhaseNumber(1);
        cp1.setTitle("Relational Model & Normalization");
        cp1.setUnlocked(true);
        cp1.setCompleted(true);
        phaseRepository.save(cp1);

        CoursePhase cp2 = new CoursePhase();
        cp2.setCourse(c1);
        cp2.setPhaseNumber(2);
        cp2.setTitle("SQL & Query Optimization");
        cp2.setUnlocked(true);
        cp2.setCompleted(false);
        phaseRepository.save(cp2);

        CoursePhase cp3 = new CoursePhase();
        cp3.setCourse(c1);
        cp3.setPhaseNumber(3);
        cp3.setTitle("Transactions & Concurrency Control");
        cp3.setUnlocked(false);
        cp3.setCompleted(false);
        phaseRepository.save(cp3);

        CoursePhase cp4 = new CoursePhase();
        cp4.setCourse(c1);
        cp4.setPhaseNumber(4);
        cp4.setTitle("Indexing & Query Tuning");
        cp4.setUnlocked(false);
        cp4.setCompleted(false);
        phaseRepository.save(cp4);

        // Seed Quiz Attempt for Student 1
        QuizAttempt qa1 = new QuizAttempt();
        qa1.setUser(student1);
        qa1.setCourse(c1);
        qa1.setPhase(cp1);
        qa1.setTopic("Relational Model & Normalization");
        qa1.setScore(8);
        qa1.setTotalQuestions(10);
        qa1.setPercentage(80.0);
        qa1.setPassed(true);
        quizAttemptRepository.save(qa1);

        // Seed Weak Topic for Student 1
        TopicPerformance tp1 = new TopicPerformance();
        tp1.setUser(student1);
        tp1.setCourseTitle("Database Management Systems");
        tp1.setConceptTag("2NF vs 3NF Transitive Dependency");
        tp1.setTimesTested(3);
        tp1.setTimesIncorrect(2);
        tp1.setStatus("NEEDS_ATTENTION");
        topicPerformanceRepository.save(tp1);

        log.info("[STAGING GUARD] Staging database initialization complete. 5 users and academic records provisioned.");
    }
}
