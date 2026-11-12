package com.vrsec.backend;

import com.vrsec.backend.config.StagingSecurityGuard;
import com.vrsec.backend.model.User;
import com.vrsec.backend.repository.*;
import com.vrsec.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class StagingSecurityGuardTest {

    private StagingSecurityGuard guard;
    private Map<String, User> userDb;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        guard = new StagingSecurityGuard();
        userDb = new ConcurrentHashMap<>();
        authService = new AuthService();

        UserRepository userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        User u = (User) args[0];
                        if (u.getId() == null) u.setId((long) (userDb.size() + 1));
                        userDb.put(u.getEmail(), u);
                        return u;
                    }
                    if ("findByEmail".equals(name)) {
                        return Optional.ofNullable(userDb.get(args[0]));
                    }
                    return null;
                }
        );

        CourseRepository courseRepository = (CourseRepository) Proxy.newProxyInstance(
                CourseRepository.class.getClassLoader(),
                new Class<?>[]{CourseRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) return args[0];
                    return null;
                }
        );

        CoursePhaseRepository phaseRepository = (CoursePhaseRepository) Proxy.newProxyInstance(
                CoursePhaseRepository.class.getClassLoader(),
                new Class<?>[]{CoursePhaseRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) return args[0];
                    return null;
                }
        );

        QuizAttemptRepository quizAttemptRepository = (QuizAttemptRepository) Proxy.newProxyInstance(
                QuizAttemptRepository.class.getClassLoader(),
                new Class<?>[]{QuizAttemptRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) return args[0];
                    return null;
                }
        );

        TopicPerformanceRepository topicPerformanceRepository = (TopicPerformanceRepository) Proxy.newProxyInstance(
                TopicPerformanceRepository.class.getClassLoader(),
                new Class<?>[]{TopicPerformanceRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) return args[0];
                    return null;
                }
        );

        ReflectionTestUtils.setField(guard, "userRepository", userRepository);
        ReflectionTestUtils.setField(guard, "courseRepository", courseRepository);
        ReflectionTestUtils.setField(guard, "phaseRepository", phaseRepository);
        ReflectionTestUtils.setField(guard, "quizAttemptRepository", quizAttemptRepository);
        ReflectionTestUtils.setField(guard, "topicPerformanceRepository", topicPerformanceRepository);
        ReflectionTestUtils.setField(guard, "authService", authService);
    }

    @Test
    void testFailClosedRejectsProductionSupabaseUrl() {
        Environment env = (Environment) Proxy.newProxyInstance(
                Environment.class.getClassLoader(),
                new Class<?>[]{Environment.class},
                (proxy, method, args) -> {
                    if ("getActiveProfiles".equals(method.getName())) return new String[]{"staging"};
                    if ("getProperty".equals(method.getName())) {
                        String prop = (String) args[0];
                        if ("spring.datasource.url".equals(prop)) return "jdbc:postgresql://aws-0-ap-northeast-1.pooler.supabase.com:5432/postgres";
                        if ("server.port".equals(prop)) return "5001";
                    }
                    return null;
                }
        );
        ReflectionTestUtils.setField(guard, "environment", env);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> guard.run(null));
        assertTrue(ex.getMessage().contains("FAIL-CLOSED SECURITY GUARD TRIGGERED"));
    }

    @Test
    void testFailClosedRejectsProductionPort5000() {
        Environment env = (Environment) Proxy.newProxyInstance(
                Environment.class.getClassLoader(),
                new Class<?>[]{Environment.class},
                (proxy, method, args) -> {
                    if ("getActiveProfiles".equals(method.getName())) return new String[]{"staging"};
                    if ("getProperty".equals(method.getName())) {
                        String prop = (String) args[0];
                        if ("spring.datasource.url".equals(prop)) return "jdbc:h2:mem:learnpath_staging";
                        if ("server.port".equals(prop)) return "5000"; // Production port
                    }
                    return null;
                }
        );
        ReflectionTestUtils.setField(guard, "environment", env);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> guard.run(null));
        assertTrue(ex.getMessage().contains("must run on isolated port 5001"));
    }

    @Test
    void testStagingInitializationSuccessAndSeedData() {
        Environment env = (Environment) Proxy.newProxyInstance(
                Environment.class.getClassLoader(),
                new Class<?>[]{Environment.class},
                (proxy, method, args) -> {
                    if ("getActiveProfiles".equals(method.getName())) return new String[]{"staging"};
                    if ("getProperty".equals(method.getName())) {
                        String prop = (String) args[0];
                        if ("spring.datasource.url".equals(prop)) return "jdbc:h2:mem:learnpath_staging";
                        if ("server.port".equals(prop)) return "5001";
                    }
                    return null;
                }
        );
        ReflectionTestUtils.setField(guard, "environment", env);

        assertDoesNotThrow(() -> guard.run(null));

        assertEquals(5, userDb.size());
        assertTrue(userDb.containsKey("staging.student1@vrsec.ac.in"));
        assertTrue(userDb.containsKey("staging.student2@gmail.com"));
        assertTrue(userDb.containsKey("staging.student3@outlook.com"));
        assertTrue(userDb.containsKey("staging.hod.cse@siddhartha.ac.in"));
        assertTrue(userDb.containsKey("staging.faculty.mech@siddhartha.ac.in"));
    }
}
