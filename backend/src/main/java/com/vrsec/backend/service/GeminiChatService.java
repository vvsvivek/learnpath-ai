package com.vrsec.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrsec.backend.model.Course;
import com.vrsec.backend.model.QuizAttempt;
import com.vrsec.backend.model.User;
import com.vrsec.backend.repository.CourseRepository;
import com.vrsec.backend.repository.QuizAttemptRepository;
import com.vrsec.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeminiChatService {

    @Value("${gemini.api.key:YOUR_API_KEY_HERE}")
    private String geminiApiKey;

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiChatService(UserRepository userRepository, CourseRepository courseRepository, QuizAttemptRepository quizAttemptRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    // --- 1. SUBJECT-SPECIFIC 3-PHASE CURRICULUM GENERATOR ---
    public List<Map<String, Object>> generateCourseCurriculum(String subject) {
        String cleanSubject = subject != null ? subject.trim() : "General Studies";

        String jsonPrompt = "You are a university curriculum designer. Create a 3-phase academic syllabus specifically for the subject: '" + cleanSubject + "'. " +
                "Every phase title and every concept MUST be 100% specific to '" + cleanSubject + "'. Do NOT use generic software terms unless the subject is software engineering. " +
                "Each phase must have 3 concepts in 'studyMaterial' separated by '||', where each concept is formatted as 'Concept Name: Detailed explanation'. " +
                "Return ONLY a valid raw JSON array with no markdown formatting: " +
                "[{\"title\": \"Phase 1: Specific Title\", \"studyMaterial\": \"Concept 1: Explanation||Concept 2: Explanation||Concept 3: Explanation\"}, " +
                "{\"title\": \"Phase 2: Specific Title\", \"studyMaterial\": \"Concept 1: Explanation||Concept 2: Explanation||Concept 3: Explanation\"}, " +
                "{\"title\": \"Phase 3: Specific Title\", \"studyMaterial\": \"Concept 1: Explanation||Concept 2: Explanation||Concept 3: Explanation\"}]";

        String aiResponse = callGoogleGeminiApi(jsonPrompt);
        if (aiResponse != null) {
            try {
                int start = aiResponse.indexOf("[");
                int end = aiResponse.lastIndexOf("]");
                if (start != -1 && end != -1 && end > start) {
                    String extractedJson = aiResponse.substring(start, end + 1);
                    List<Map<String, Object>> parsed = objectMapper.readValue(extractedJson, new TypeReference<List<Map<String, Object>>>() {});
                    if (parsed != null && !parsed.isEmpty()) {
                        return parsed;
                    }
                }
            } catch (Exception e) {
                System.err.println("Curriculum JSON parse fallback triggered: " + e.getMessage());
            }
        }

        return buildSubjectSpecificCurriculum(cleanSubject);
    }

    // --- 2. SUBJECT & PHASE-SPECIFIC QUIZ GENERATOR ---
    public List<Map<String, Object>> generatePhaseQuiz(String courseTitle, String phaseTitle, String studyMaterial) {
        String quizPrompt = "Create a 3-question multiple-choice quiz specifically testing the course '" + courseTitle + "' and phase '" + phaseTitle + "' " +
                "based on these exact concepts: [" + (studyMaterial != null ? studyMaterial : phaseTitle) + "]. " +
                "Return ONLY a valid raw JSON array with no markdown formatting: " +
                "[{\"q\": \"Specific question?\", \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"], \"correct\": \"Option A\"}]";

        String aiResponse = callGoogleGeminiApi(quizPrompt);
        if (aiResponse != null) {
            try {
                int start = aiResponse.indexOf("[");
                int end = aiResponse.lastIndexOf("]");
                if (start != -1 && end != -1 && end > start) {
                    String extractedJson = aiResponse.substring(start, end + 1);
                    List<Map<String, Object>> parsed = objectMapper.readValue(extractedJson, new TypeReference<List<Map<String, Object>>>() {});
                    if (parsed != null && !parsed.isEmpty()) {
                        return parsed;
                    }
                }
            } catch (Exception e) {
                System.err.println("Quiz JSON parse fallback triggered: " + e.getMessage());
            }
        }

        return buildQuizFromStudyMaterial(courseTitle, phaseTitle, studyMaterial);
    }

    // --- 3. CONTEXT-AWARE AI TUTOR CHAT ---
    public String getGenerativeAIResponse(String email, String userMessage) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        User user = findUserByEmail(cleanEmail);
        List<Course> activeCourses = findActiveCourses(cleanEmail);
        List<QuizAttempt> quizzes = findUserQuizzes(user);

        String studentContext = buildSecureStudentContext(user, activeCourses, quizzes);

        String systemPrompt = "You are the 'LearnPath AI Tutor', a dedicated academic mentor for university students. " +
                "Format your responses cleanly using standard Markdown: use bold text for emphasis, bullet points for lists, and formatted code blocks for code snippets. " +
                "Keep your tone conversational, direct, and supportive. Avoid robotic disclaimers. " +
                "Below is the verified, real-time database record of the authenticated student. " +
                "If they ask about their enrolled courses, progress, profile, or quiz results, base your response strictly on this data. " +
                "If they ask general academic or technical questions, answer thoroughly with clear explanations and examples.\n\n" +
                "=== AUTHENTICATED STUDENT DATABASE CONTEXT ===\n" + studentContext + "\n=============================================\n\n" +
                "Student Question: " + userMessage;

        String apiReply = callGoogleGeminiApi(systemPrompt);
        if (apiReply != null && !apiReply.trim().isEmpty()) {
            return apiReply;
        }

        return buildDynamicAcademicResponse(user, activeCourses, quizzes, userMessage);
    }

    private String callGoogleGeminiApi(String prompt) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || "YOUR_API_KEY_HERE".equals(geminiApiKey.trim())) {
            return null;
        }

        String[] models = {"gemini-2.5-flash", "gemini-2.0-flash"};
        for (String model : models) {
            try {
                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + geminiApiKey.trim();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> textPart = Collections.singletonMap("text", prompt);
                Map<String, Object> contentObj = Collections.singletonMap("parts", Collections.singletonList(textPart));
                Map<String, Object> payload = Collections.singletonMap("contents", Collections.singletonList(contentObj));

                String requestBody = objectMapper.writeValueAsString(payload);
                HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

                Map response = restTemplate.postForObject(apiUrl, request, Map.class);
                if (response != null && response.containsKey("candidates")) {
                    List candidates = (List) response.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map firstCandidate = (Map) candidates.get(0);
                        Map content = (Map) firstCandidate.get("content");
                        if (content != null && content.containsKey("parts")) {
                            List parts = (List) content.get("parts");
                            if (parts != null && !parts.isEmpty()) {
                                Map firstPart = (Map) parts.get(0);
                                String text = (String) firstPart.get("text");
                                if (text != null && !text.trim().isEmpty()) {
                                    return text.trim();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Gemini API attempt with " + model + " failed: " + e.getMessage());
            }
        }
        return null;
    }

    // --- DOMAIN-AWARE SUBJECT CURRICULUM BUILDER ---
    private List<Map<String, Object>> buildSubjectSpecificCurriculum(String subject) {
        String s = subject.toLowerCase().trim();

        // 1. Constitution / Polity / Law / Civics
        if (s.contains("constitution") || s.contains("polity") || s.contains("civics") || s.contains("law")) {
            return Arrays.asList(
                phase("Phase 1: Historical Background & The Preamble",
                      "Making of the Constitution: Constituent Assembly debates, drafting committee, and constitutional sources||" +
                      "The Preamble: Sovereign, Socialist, Secular, Democratic Republic ideals along with Justice, Liberty, Equality, and Fraternity||" +
                      "Citizenship & Union Territories: Constitutional provisions under Articles 1 to 11 governing the Union and citizenship rights"),
                phase("Phase 2: Fundamental Rights, DPSP & Fundamental Duties",
                      "Fundamental Rights (Articles 12-35): Right to Equality, Freedom, Protection against Exploitation, Freedom of Religion, and Cultural Rights||" +
                      "Constitutional Remedies (Article 32): Writs of Habeas Corpus, Mandamus, Prohibition, Certiorari, and Quo-Warranto||" +
                      "DPSP & Fundamental Duties (Part IV & IVA): Directive Principles for socio-economic welfare and citizens' duties under Article 51A"),
                phase("Phase 3: Union & State Governance, Judiciary & Amendments",
                      "Parliamentary Executive & Legislature: Roles and powers of the President, Prime Minister, Council of Ministers, Lok Sabha, and Rajya Sabha||" +
                      "Independent Judiciary & Judicial Review: Structure and jurisdiction of the Supreme Court and High Courts||" +
                      "Federalism, Emergency & Amendments: Centre-State relations, Emergency provisions (Articles 352-360), and Amendment procedure (Article 368)")
            );
        }

        // 2. Python Programming
        if (s.contains("python")) {
            return Arrays.asList(
                phase("Phase 1: Python Syntax, Data Types & Control Flow",
                      "Variables & Dynamic Typing: Core primitives (int, float, str, bool), memory references, and type casting||" +
                      "Control Structures & Functions: Conditional branching, loops, *args/**kwargs, and lexical variable scoping||" +
                      "Built-in Data Structures: Lists, immutable tuples, hash-map dictionaries, sets, and list comprehensions"),
                phase("Phase 2: Object-Oriented Python & Functional Modules",
                      "Classes & Inheritance: Constructors (__init__), dunder methods, encapsulation, and method resolution order (MRO)||" +
                      "Iterators, Generators & Decorators: Lazy evaluation using yield, higher-order functions, and function wrappers||" +
                      "File Handling & Exception Management: Context managers (with statement), JSON/CSV parsing, and try-except-finally blocks"),
                phase("Phase 3: Libraries, Concurrency & Real-World Applications",
                      "Data Processing & Scientific Computing: Vectorized array operations with NumPy and tabular manipulation with Pandas||" +
                      "Web APIs & Automation: Building REST endpoints with FastAPI/Flask and HTTP requests handling||" +
                      "Testing, Packaging & AsyncIO: Virtual environments, unit testing with pytest, and asynchronous event loops")
            );
        }

        // 3. Java Programming
        if (s.contains("java") && !s.contains("javascript")) {
            return Arrays.asList(
                phase("Phase 1: Java Architecture, Syntax & OOP Pillars",
                      "JVM, JRE & Bytecode Execution: Classloader subsystem, runtime memory areas (Heap vs Stack), and Garbage Collection||" +
                      "Four Pillars of OOP: Encapsulation, Single/Multilevel Inheritance, Compile-time/Runtime Polymorphism, and Abstraction||" +
                      "Interfaces, Abstract Classes & Packages: Contract-driven design, access modifiers, and exception handling hierarchy"),
                phase("Phase 2: Java Collections Framework, Generics & Streams",
                      "List, Set & Queue Implementations: Internal workings of ArrayList, LinkedList, HashSet, TreeSet, and PriorityQueue||" +
                      "Hashing & Map Structures: HashMap bucket resolution, hashCode/equals contracts, and ConcurrentHashMap||" +
                      "Java 8+ Functional Programming: Lambda expressions, Functional Interfaces, Optional, and the Stream API pipeline"),
                phase("Phase 3: Multithreading, JDBC & Enterprise Spring Boot",
                      "Concurrency & Threads: Thread lifecycle, synchronized blocks, ExecutorService thread pools, and CompletableFuture||" +
                      "Database Connectivity (JDBC & JPA): Connection pooling, prepared statements, and ORM entity mapping||" +
                      "Enterprise REST Services: Dependency Injection, Spring Boot controllers, and microservice packaging")
            );
        }

        // 4. C Programming
        if (s.equals("c") || s.contains("c programming") || s.contains("foundations of c")) {
            return Arrays.asList(
                phase("Phase 1: C Fundamentals, Operators & Control Structures",
                      "Compilation Pipeline & Memory Layout: Preprocessor, compiler, assembler, linker stages, and basic data types||" +
                      "Operators & Decision Control: Bitwise operations, operator precedence, switch-case, and iterative loops||" +
                      "Modular Functions & Storage Classes: Call-by-value vs call-by-reference, recursion, and auto, extern, static, register variables"),
                phase("Phase 2: Arrays, Strings & Pointer Arithmetic",
                      "Multi-dimensional Arrays & Strings: Contiguous memory allocation, character arrays, and string manipulation functions||" +
                      "Pointers & Memory Addressing: Pointer declarations, dereferencing, pointer arithmetic, and function pointers||" +
                      "Dynamic Memory Management: Runtime heap allocation and deallocation using malloc(), calloc(), realloc(), and free()"),
                phase("Phase 3: Structures, Unions & File I/O Operations",
                      "User-Defined Data Types: Structures, structure padding, nested structures, bit-fields, and Unions||" +
                      "Self-Referential Structures: Building dynamic linked nodes using struct pointers||" +
                      "File Streams & Command Line Arguments: Sequential and random file access (fopen, fread, fwrite, fseek) and argc/argv")
            );
        }

        // 5. C++ / OOP
        if (s.contains("c++") || s.contains("object-oriented") || s.contains("oop")) {
            return Arrays.asList(
                phase("Phase 1: Classes, Objects & Memory Management in C++",
                      "Encapsulation & Constructors: Class blueprints, parameterized/copy constructors, destructors, and the 'this' pointer||" +
                      "References, Pointers & Dynamic Allocation: Reference variables, new/delete operators, and deep vs shallow copying||" +
                      "Friend Functions & Operator Overloading: Compile-time polymorphism by overloading unary and binary operators"),
                phase("Phase 2: Inheritance, Virtual Functions & Runtime Polymorphism",
                      "Inheritance Hierarchies: Single, multiple, multilevel, and diamond problem resolution using virtual base classes||" +
                      "Dynamic Binding & V-Tables: Virtual functions, vptr/vtable mechanisms, and method overriding||" +
                      "Abstract Classes & Interfaces: Pure virtual functions and designing extensible object architectures"),
                phase("Phase 3: Templates, Exception Handling & Standard Template Library (STL)",
                      "Generic Programming: Function templates and class templates for type-independent algorithms||" +
                      "STL Containers & Iterators: Vectors, Deques, Lists, Maps, Sets, and bidirectional/random-access iterators||" +
                      "STL Algorithms & Modern C++: Sorting, searching, smart pointers (unique_ptr, shared_ptr), and lambda expressions")
            );
        }

        // 6. Data Structures & Algorithms (DSA)
        if (s.contains("data structure") || s.contains("dsa") || s.contains("algorithm")) {
            return Arrays.asList(
                phase("Phase 1: Complexity Analysis & Linear Data Structures",
                      "Asymptotic Notations: Time and space complexity analysis using Big-O, Omega, and Theta notations||" +
                      "Arrays, Linked Lists & Two-Pointer Techniques: Singly, doubly, and circular linked lists alongside sliding window patterns||" +
                      "Stacks & Queues: LIFO/FIFO implementations, monotonic stacks (Next Greater Element, Stock Span), and circular queues"),
                phase("Phase 2: Trees, Heaps, Hashing & Graphs",
                      "Binary Trees & BST: Tree traversals (Inorder, Preorder, Postorder, Level-order), LCA, and balanced AVL/Red-Black trees||" +
                      "Heaps & Hash Tables: Priority queues, heap sort, hash functions, and collision resolution (chaining vs open addressing)||" +
                      "Graph Representations & Traversals: Adjacency lists/matrices, Breadth-First Search (BFS), Depth-First Search (DFS), and topological sort"),
                phase("Phase 3: Greedy Paradigms, Shortest Paths & Dynamic Programming",
                      "Sorting & Searching: Merge sort, Quick sort, and Binary Search on monotonic answer spaces||" +
                      "Greedy & Graph Algorithms: Activity selection, Job sequencing, Dijkstra's shortest path, and Minimum Spanning Trees (Prim/Kruskal)||" +
                      "Dynamic Programming (DP): Overlapping subproblems, memoization vs tabulation, Knapsack, LCS, and matrix DP")
            );
        }

        // 7. Database Management Systems (DBMS / SQL)
        if (s.contains("dbms") || s.contains("database") || s.contains("sql")) {
            return Arrays.asList(
                phase("Phase 1: Relational Data Model, ER Diagrams & SQL Foundations",
                      "Database Architecture & ER Modeling: Three-schema architecture, entities, attributes, cardinalities, and ER-to-Relational mapping||" +
                      "Relational Algebra & Keys: Selection, projection, Cartesian product, natural joins, and Super, Candidate, Primary, and Foreign keys||" +
                      "Structured Query Language (SQL): DDL, DML, complex INNER/OUTER joins, aggregate functions, GROUP BY, and nested subqueries"),
                phase("Phase 2: Functional Dependencies & Database Normalization",
                      "Redundancy & Update Anomalies: Insertion, deletion, and modification anomalies in unnormalized schemas||" +
                      "Armstrong's Axioms & Attribute Closure: Reflexivity, augmentation, transitivity, and canonical cover calculation||" +
                      "Normal Forms (1NF to BCNF): Step-by-step lossless decomposition into 1NF, 2NF, 3NF, and Boyce-Codd Normal Form (BCNF)"),
                phase("Phase 3: Transaction Management, Concurrency Control & Indexing",
                      "ACID Properties & Schedules: Atomicity, Consistency, Isolation, Durability, and conflict/view serializability||" +
                      "Concurrency Control Protocols: Two-Phase Locking (2PL), deadlock handling, and timestamp-based ordering||" +
                      "Indexing & Crash Recovery: B-Trees, B+ Trees, hash indexing, and ARIES write-ahead logging (WAL)")
            );
        }

        // 8. Operating Systems (OS)
        if (s.contains("operating system") || s.equals("os") || s.contains("kernel")) {
            return Arrays.asList(
                phase("Phase 1: OS Architecture, Processes & CPU Scheduling",
                      "System Calls & Kernel Modes: Dual-mode operation (user vs kernel mode), interrupts, and monolithic vs microkernel design||" +
                      "Process Management & Threads: Process Control Block (PCB), context switching, inter-process communication (IPC), and multithreading||" +
                      "CPU Scheduling Algorithms: Preemptive and non-preemptive FCFS, SJF/SRTF, Priority, and Round Robin scheduling metrics"),
                phase("Phase 2: Process Synchronization & Deadlock Management",
                      "Critical Section Problem: Race conditions, Peterson's solution, hardware atomic instructions, Mutex locks, and Semaphores||" +
                      "Classic Synchronization Problems: Producer-Consumer (Bounded Buffer), Readers-Writers, and Dining Philosophers||" +
                      "Deadlocks: Four Coffman conditions, Resource Allocation Graphs, Banker's safety algorithm, and deadlock recovery"),
                phase("Phase 3: Memory Management, Virtual Memory & File Systems",
                      "Contiguous Allocation & Paging: Internal vs external fragmentation, page tables, and Translation Lookaside Buffer (TLB)||" +
                      "Demand Paging & Page Replacement: Page faults, thrashing, and FIFO, Optimal, and LRU page replacement algorithms||" +
                      "Disk Scheduling & File Systems: Inode structures, directory allocation, and SCAN/C-SCAN/SSTF disk arm scheduling")
            );
        }

        // 9. Discrete Mathematics
        if (s.contains("discrete")) {
            return Arrays.asList(
                phase("Phase 1: Mathematical Logic & Proof Techniques",
                      "Propositional Logic: Logical connectives, truth tables, tautologies, equivalences, andConjunctive/Disjunctive Normal Forms||" +
                      "Predicate Logic & Quantifiers: Universal and existential quantifiers, rules of inference, and predicate resolution||" +
                      "Methods of Proof: Direct proof, contraposition, contradiction, and mathematical induction"),
                phase("Phase 2: Set Theory, Relations, Functions & Algebraic Structures",
                      "Sets & Binary Relations: Power sets, closures, equivalence relations, partial orderings (Posets), and Hasse diagrams||" +
                      "Functions & Cardinality: Injective, surjective, and bijective mappings, pigeonhole principle, and composition||" +
                      "Groups, Rings & Lattices: Semigroups, monoids, Abelian groups, subgroups, and Boolean algebra"),
                phase("Phase 3: Combinatorics, Recurrence Relations & Graph Theory",
                      "Counting Principles: Permutations, combinations, inclusion-exclusion principle, and generating functions||" +
                      "Recurrence Relations: Solving linear homogeneous and non-homogeneous recurrences using characteristic roots||" +
                      "Graph Theory & Trees: Eulerian and Hamiltonian paths, planar graphs, graph coloring, and spanning trees")
            );
        }

        // 10. Engineering Mathematics / Calculus / Linear Algebra
        if (s.contains("math") || s.contains("calculus") || s.contains("algebra")) {
            return Arrays.asList(
                phase("Phase 1: Linear Algebra, Matrices & Eigenvalue Problems",
                      "Matrix Rank & Linear Systems: Gauss elimination, Gauss-Jordan method, and consistency of linear equations (Ax = b)||" +
                      "Eigenvalues & Eigenvectors: Characteristic polynomials, Cayley-Hamilton theorem, and matrix diagonalization||" +
                      "Quadratic Forms: Orthogonal transformations and reduction of quadratic forms to canonical form"),
                phase("Phase 2: Differential & Multivariable Calculus",
                      "Mean Value Theorems: Rolle's theorem, Lagrange's and Cauchy's mean value theorems, and Taylor series expansions||" +
                      "Partial Derivatives & Jacobians: Total differentiation, chain rule, Jacobian determinants, and functional dependence||" +
                      "Maxima & Minima: Unconstrained extrema and constrained optimization using Lagrange multipliers"),
                phase("Phase 3: Multiple Integrals, Vector Calculus & Transforms",
                      "Double & Triple Integrals: Change of order of integration, polar/cylindrical coordinates, and volume calculations||" +
                      "Vector Differential & Integral Calculus: Gradient, divergence, curl, and Green's, Stokes', and Gauss Divergence theorems||" +
                      "Laplace & Fourier Transforms: Transform properties, convolution theorem, and solving differential equations")
            );
        }

        // 11. Full-Stack Web / Microservices / Cloud
        if (s.contains("full-stack") || s.contains("web") || s.contains("cloud") || s.contains("microservice")) {
            return Arrays.asList(
                phase("Phase 1: Frontend Component Architecture & State Management",
                      "Responsive UI & Component Lifecycle: Declarative UI rendering, props vs state, and hooks-based lifecycle management||" +
                      "Client-Side Routing & Asynchronous Fetching: REST API consumption, JWT token handling, and error boundaries||" +
                      "Design Systems & Accessibility: Clean minimal layouts, state persistence, and cross-platform responsiveness"),
                phase("Phase 2: Backend API Engineering & Relational Persistence",
                      "Layered Enterprise Architecture: Controller-Service-Repository separation, DTO validation, and RESTful conventions||" +
                      "ORM & Database Transactions: Entity relationships, connection pooling, query optimization, and schema migrations||" +
                      "Security & Authentication: Role-based access control (RBAC), password hashing, and CORS policies"),
                phase("Phase 3: Distributed Cloud Infrastructure & Scalability",
                      "Microservices & API Gateways: Service discovery, load balancing, fault tolerance, and circuit breakers||" +
                      "Cloud Compute & Storage (AWS): VPC networking, IAM roles, RDS/DynamoDB databases, and Auto Scaling groups||" +
                      "Containerization & CI/CD: Docker images, automated build pipelines, and zero-downtime cloud deployment")
            );
        }

        // 12. ECE: Electronic Circuits / Signals / DSP
        if (s.contains("circuit") || s.contains("electronic") || s.contains("ece") || s.contains("signal") || s.contains("dsp")) {
            return Arrays.asList(
                phase("Phase 1: Network Theorems, Semiconductor Devices & Amplifiers",
                      "Circuit Analysis Laws: Kirchhoff's Voltage and Current Laws (KVL/KCL), Thevenin's, Norton's, and Superposition theorems||" +
                      "P-N Junction Diodes & Transistors: Rectifiers, Zener voltage regulators, and BJT/MOSFET biasing configurations||" +
                      "Small-Signal & Operational Amplifiers: Frequency response, feedback topologies, and Op-Amp linear/non-linear applications"),
                phase("Phase 2: Continuous & Discrete-Time Signals and Systems",
                      "Signal Classification & LTI Systems: Impulse response, convolution sum/integral, causality, and stability analysis||" +
                      "Fourier Analysis & Sampling: Continuous/Discrete-Time Fourier Transform (CTFT/DTFT) and Nyquist-Shannon sampling theorem||" +
                      "Laplace & Z-Transforms: Region of convergence (ROC), poles and zeros, and inverse transform techniques"),
                phase("Phase 3: Digital Signal Processing (DSP) & Filter Design",
                      "Discrete Fourier Transform (DFT) & FFT: Radix-2 Fast Fourier Transform algorithms and circular convolution||" +
                      "Digital Filter Design (FIR & IIR): Butterworth/Chebyshev IIR filters and window-based linear-phase FIR filters||" +
                      "Hardware Implementation: Quantization noise, finite word-length effects, and DSP processor architectures")
            );
        }

        // 13. Mechanical: Thermodynamics / Heat Transfer / CAD
        if (s.contains("thermodynamic") || s.contains("heat") || s.contains("cad") || s.contains("machine") || s.contains("mech")) {
            return Arrays.asList(
                phase("Phase 1: Laws of Thermodynamics & Energy Conversion",
                      "Thermodynamic Systems & First Law: Open vs closed systems, work and heat transfer, internal energy, and steady-flow energy equations||" +
                      "Second Law & Entropy: Kelvin-Planck and Clausius statements, Carnot cycle efficiency, entropy generation, and exergy analysis||" +
                      "Power & Refrigeration Cycles: Otto, Diesel, Brayton gas cycles, Rankine vapor cycle, and vapor-compression refrigeration"),
                phase("Phase 2: Heat Transfer Mechanisms & Fluid Thermal Systems",
                      "Steady & Transient Conduction: Fourier's law, thermal resistance networks, fins, and lumped heat capacity systems||" +
                      "Forced & Natural Convection: Velocity and thermal boundary layers, dimensionless numbers (Reynolds, Prandtl, Nusselt)||" +
                      "Thermal Radiation & Heat Exchangers: Stefan-Boltzmann law, view factors, and LMTD/NTU heat exchanger design"),
                phase("Phase 3: Computer-Aided Design (CAD) & Structural Prototyping",
                      "Geometric Modeling: Parametric 2D sketching, 3D solid modeling (extrude, revolve, loft), and surface representation||" +
                      "Assembly Design & Kinematics: Geometric dimensioning and tolerancing (GD&T), mates, and interference detection||" +
                      "Finite Element Analysis (FEA) & CAM: Mesh generation, stress/strain simulation, and CNC toolpath generation")
            );
        }

        // 14. Dynamic Academic Curriculum for ANY Other Subject (History, Economics, Chemistry, AI, etc.)
        return Arrays.asList(
            phase("Phase 1: Core Foundations & Theoretical Framework of " + subject,
                  "Historical Context & Definitions: Origins, foundational terminology, and primary objectives of " + subject + "||" +
                  "Governing Principles & Models: Core theoretical axioms, classification frameworks, and conceptual structures in " + subject + "||" +
                  "Standard Methodologies: Analytical techniques and foundational rules used to study " + subject),
            phase("Phase 2: Structural Analysis & Applied Concepts in " + subject,
                  "Core Mechanisms & Processes: Detailed examination of how key components interact within " + subject + "||" +
                  "Comparative Case Studies: Evaluating real-world scenarios, applications, and domain challenges in " + subject + "||" +
                  "Problem-Solving Frameworks: Systematic evaluation, structured reasoning, and critical analysis for " + subject),
            phase("Phase 3: Advanced Perspectives, Ethics & Mastery of " + subject,
                  "Contemporary Developments: Modern advancements, policy or research trends, and emerging paradigms in " + subject + "||" +
                  "Practical Synthesis & Evaluation: Integrating multi-disciplinary insights and assessing real-world impact in " + subject + "||" +
                  "Comprehensive Review & Assessment: Mastering advanced examination standards and applied synthesis of " + subject)
        );
    }

    private Map<String, Object> phase(String title, String studyMaterial) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("studyMaterial", studyMaterial);
        return map;
    }

    // --- DYNAMIC QUIZ BUILDER DIRECTLY FROM THE PHASE'S STUDY MATERIAL ---
    private List<Map<String, Object>> buildQuizFromStudyMaterial(String courseTitle, String phaseTitle, String studyMaterial) {
        List<String[]> parsedConcepts = new ArrayList<>();
        if (studyMaterial != null && !studyMaterial.trim().isEmpty()) {
            String[] rawItems = studyMaterial.split("\\|\\|");
            for (String item : rawItems) {
                int colonIdx = item.indexOf(":");
                if (colonIdx != -1) {
                    String name = item.substring(0, colonIdx).trim();
                    String desc = item.substring(colonIdx + 1).trim();
                    parsedConcepts.add(new String[]{name, desc});
                } else if (!item.trim().isEmpty()) {
                    parsedConcepts.add(new String[]{item.trim(), "Core academic concept in " + courseTitle});
                }
            }
        }

        while (parsedConcepts.size() < 3) {
            int idx = parsedConcepts.size() + 1;
            parsedConcepts.add(new String[]{
                courseTitle + " Concept " + idx,
                "Essential principle covered under " + phaseTitle + " in " + courseTitle
            });
        }

        String c1Name = parsedConcepts.get(0)[0];
        String c1Desc = parsedConcepts.get(0)[1];
        String c2Name = parsedConcepts.get(1)[0];
        String c2Desc = parsedConcepts.get(1)[1];
        String c3Name = parsedConcepts.get(2)[0];
        String c3Desc = parsedConcepts.get(2)[1];

        List<Map<String, Object>> questions = new ArrayList<>();

        // Question 1: Tests Concept 1 Name from its description
        questions.add(createQuestion(
            "In " + courseTitle + " (" + phaseTitle + "), which concept specifically addresses: \"" + c1Desc + "\"?",
            Arrays.asList(c1Name, c2Name, c3Name, "Unrelated External Hypothesis"),
            c1Name
        ));

        // Question 2: Tests Concept 2 Description from its name
        questions.add(createQuestion(
            "What is the primary focus of '" + c2Name + "' within " + courseTitle + "?",
            Arrays.asList(
                c2Desc,
                "Eliminating all foundational rules of " + courseTitle,
                "Bypassing analytical verification completely",
                "Replacing " + courseTitle + " with arbitrary assumptions"
            ),
            c2Desc
        ));

        // Question 3: Tests Concept 3 Name & Description
        questions.add(createQuestion(
            "Which of the following accurately describes '" + c3Name + "' covered in " + phaseTitle + "?",
            Arrays.asList(
                c3Desc,
                "It has no relevance to " + courseTitle,
                "It only applies to deprecated legacy systems outside " + courseTitle,
                "It contradicts the core syllabus of " + phaseTitle
            ),
            c3Desc
        ));

        return questions;
    }

    private Map<String, Object> createQuestion(String q, List<String> options, String correct) {
        Map<String, Object> map = new HashMap<>();
        map.put("q", q);
        map.put("options", options);
        map.put("correct", correct);
        return map;
    }

    private User findUserByEmail(String cleanEmail) {
        if (cleanEmail.isEmpty()) return null;
        return userRepository.findAll().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().trim().equalsIgnoreCase(cleanEmail))
                .findFirst()
                .orElse(null);
    }

    private List<Course> findActiveCourses(String cleanEmail) {
        if (cleanEmail.isEmpty()) return Collections.emptyList();
        return courseRepository.findAll().stream()
                .filter(c -> c.getUserEmail() != null && c.getUserEmail().trim().equalsIgnoreCase(cleanEmail) && !c.isDeleted())
                .collect(Collectors.toList());
    }

    private List<QuizAttempt> findUserQuizzes(User user) {
        if (user == null || user.getId() == null) return Collections.emptyList();
        return quizAttemptRepository.findAll().stream()
                .filter(q -> q.getUser() != null && user.getId().equals(q.getUser().getId()))
                .collect(Collectors.toList());
    }

    private String buildSecureStudentContext(User user, List<Course> activeCourses, List<QuizAttempt> quizzes) {
        if (user == null) return "User not authenticated.";

        StringBuilder context = new StringBuilder();
        context.append("Name: ").append(user.getUsername()).append("\n");
        context.append("Email: ").append(user.getEmail()).append("\n");
        context.append("Role: ").append(user.getRole()).append("\n");
        context.append("Branch: ").append(user.getBranch() != null ? user.getBranch() : "CSE")
               .append(" (").append(user.getAcademicYear() != null ? user.getAcademicYear() : "3rd Year")
               .append(", ").append(user.getSemester() != null ? user.getSemester() : "1st Sem").append(")\n");
        context.append("Current CGPA: ").append(user.getCgpa() != null ? user.getCgpa() : 0.0).append("\n\n");

        context.append("Enrolled Courses:\n");
        if (activeCourses.isEmpty()) {
            context.append("- None currently enrolled.\n");
        } else {
            for (Course c : activeCourses) {
                context.append("- ").append(c.getTitle()).append("\n");
            }
        }

        context.append("\nRecent Quiz Results:\n");
        if (quizzes.isEmpty()) {
            context.append("- No quizzes attempted yet.\n");
        } else {
            for (QuizAttempt q : quizzes) {
                context.append("- Topic: ").append(q.getTopic()).append(" | Score: ").append(q.getScore()).append("/").append(q.getTotalQuestions()).append("\n");
            }
        }

        return context.toString();
    }

    private String buildDynamicAcademicResponse(User user, List<Course> activeCourses, List<QuizAttempt> quizzes, String rawMessage) {
        String msg = rawMessage != null ? rawMessage.trim() : "";
        String lower = msg.toLowerCase();
        String name = (user != null && user.getUsername() != null) ? user.getUsername() : "Student";
        String branch = (user != null && user.getBranch() != null) ? user.getBranch() : "Engineering";
        String year = (user != null && user.getAcademicYear() != null) ? user.getAcademicYear() : "current";
        String sem = (user != null && user.getSemester() != null) ? user.getSemester() : "current";
        double cgpa = (user != null && user.getCgpa() != null) ? user.getCgpa() : 0.0;

        String courseNames = activeCourses.isEmpty()
                ? "no active courses yet (you can generate one from the Home tab)"
                : activeCourses.stream().map(Course::getTitle).collect(Collectors.joining(", "));

        if (lower.equals("hi") || lower.equals("hello") || lower.equals("hey") || lower.startsWith("hello ") || lower.startsWith("hi ")) {
            return "Hello **" + name + "**! Welcome to your LearnPath AI Tutor.\n\n" +
                   "Here is a quick snapshot of your current academic status:\n" +
                   "* **Branch & Cohort:** " + branch + " (" + year + " Year, " + sem + ")\n" +
                   "* **Current CGPA:** " + cgpa + " / 10\n" +
                   "* **Active Learning Paths:** " + courseNames + "\n\n" +
                   "Ask me about any subject (such as **Constitution**, **Python**, **Java**, **DBMS**, **DSA**, **Operating Systems**) or ask about your **courses, quizzes, and study plan**!";
        }

        if (lower.contains("my course") || lower.contains("enrolled") || lower.contains("my profile") ||
            lower.contains("who am i") || lower.contains("my progress") || lower.contains("my cgpa") ||
            lower.contains("quiz") || lower.contains("learning path") || lower.contains("status")) {

            StringBuilder sb = new StringBuilder();
            sb.append("### Academic Profile & Learning Path Summary for ").append(name).append("\n\n");
            sb.append("* **Email:** ").append(user != null ? user.getEmail() : "N/A").append("\n");
            sb.append("* **Department / Branch:** ").append(branch).append("\n");
            sb.append("* **Academic Cohort:** ").append(year).append(" Year, ").append(sem).append("\n");
            sb.append("* **CGPA Trajectory:** ").append(cgpa).append(" / 10.0\n\n");

            sb.append("#### Current Enrolled Courses:\n");
            if (activeCourses.isEmpty()) {
                sb.append("* You currently have no active courses enrolled. Head over to the **Home** tab to add a recommended subject or generate a custom syllabus.\n\n");
            } else {
                for (Course c : activeCourses) {
                    sb.append("* **").append(c.getTitle()).append("** — Active 3-Phase Curriculum\n");
                }
                sb.append("\n");
            }

            sb.append("#### Recent Quiz Attempts:\n");
            if (quizzes.isEmpty()) {
                sb.append("* No phase quizzes submitted yet. Open any course phase and click **Take Phase Quiz** to test your mastery!");
            } else {
                for (QuizAttempt q : quizzes) {
                    sb.append("* **").append(q.getTopic()).append("**: Scored **").append(q.getScore()).append("/").append(q.getTotalQuestions()).append("**\n");
                }
            }
            return sb.toString();
        }

        // Pull directly from our domain-aware curriculum builder so chat explanations are also 100% subject-specific
        List<Map<String, Object>> phases = buildSubjectSpecificCurriculum(msg);
        StringBuilder sb = new StringBuilder();
        sb.append("### Comprehensive Study Guide: ").append(msg).append("\n\n");
        for (Map<String, Object> p : phases) {
            sb.append("#### ").append(p.get("title")).append("\n");
            String[] concepts = String.valueOf(p.get("studyMaterial")).split("\\|\\|");
            for (String c : concepts) {
                sb.append("* **").append(c.replace(":", ":**")).append("\n");
            }
            sb.append("\n");
        }
        sb.append("You can generate a full interactive course path and quizzes for **").append(msg).append("** directly from the **Home** tab!");
        return sb.toString();
    }
}