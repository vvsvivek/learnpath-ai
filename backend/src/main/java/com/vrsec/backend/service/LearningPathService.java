package com.vrsec.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrsec.backend.model.*;
import com.vrsec.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LearningPathService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CoursePhaseRepository phaseRepository;
    @Autowired private QuizQuestionRepository questionRepository;
    @Autowired private QuizAttemptRepository attemptRepository;
    @Autowired private TopicPerformanceRepository topicPerformanceRepository;
    @Autowired private SuggestedCourseRepository suggestedCourseRepository;
    @Autowired private ApprovedCourseRepository approvedCourseRepository;
    @Autowired private CourseRequestRepository courseRequestRepository;

    @PostConstruct
    public void initCatalogue() {
        syncApprovedCatalogue();
    }

    @Transactional
    public void syncApprovedCatalogue() {
        if (approvedCourseRepository == null) return;
        try {
            int index = 101;
            for (CatalogueCourse cc : COURSE_CATALOGUE) {
                String prefix = cc.getBranch().equalsIgnoreCase("SHARED") ? "ENG" : cc.getBranch();
                if (prefix.equalsIgnoreCase("ALL")) prefix = "GEN";
                String code = prefix + "-" + (index++);

                // Idempotent seeding: Only insert if not already present. Never overwrites existing/faculty changes.
                if (!approvedCourseRepository.existsByTitleIgnoreCaseAndActiveTrue(cc.getTitle())) {
                    ApprovedCourse ac = new ApprovedCourse(
                            code,
                            cc.getTitle(),
                            cc.getDescription(),
                            cc.getBranch(),
                            cc.getAcademicYear(),
                            cc.getSemester(),
                            cc.getDifficulty(),
                            cc.getDuration(),
                            cc.getCategory()
                    );
                    approvedCourseRepository.save(ac);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not sync approved catalogue: " + e.getMessage());
        }
    }

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================================================================
    // 1. DYNAMIC PERSONALIZED COURSE RECOMMENDATIONS & CATALOGUE ENGINE
    // =========================================================================

    public static class CatalogueCourse {
        private final String title;
        private final String description;
        private final String branch; // "CSE", "IT", "ECE", "EEE", "MECH", "CIVIL", "AIML", "AIDS", "SHARED", "ALL"
        private final String academicYear; // "1st Year", "2nd Year", "3rd Year", "4th Year", "ANY"
        private final String semester; // "1st Sem", "2nd Sem", "ANY"
        private final String difficulty; // "Foundational", "Intermediate", "Advanced", "Specialized", "Challenging", "Revision"
        private final String duration;
        private final String category; // "CORE", "FOUNDATION", "HONORS", "REMEDIAL"

        public CatalogueCourse(String title, String description, String branch, String academicYear, String semester, String difficulty, String duration, String category) {
            this.title = title;
            this.description = description;
            this.branch = branch;
            this.academicYear = academicYear;
            this.semester = semester;
            this.difficulty = difficulty;
            this.duration = duration;
            this.category = category;
        }

        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getBranch() { return branch; }
        public String getAcademicYear() { return academicYear; }
        public String getSemester() { return semester; }
        public String getDifficulty() { return difficulty; }
        public String getDuration() { return duration; }
        public String getCategory() { return category; }
    }

    public static final List<CatalogueCourse> COURSE_CATALOGUE = new ArrayList<>();

    static {
        // --- 1ST YEAR SHARED ENGINEERING FOUNDATIONS ---
        COURSE_CATALOGUE.add(new CatalogueCourse("Engineering Mathematics & Calculus", "Essential differential and integral calculus, ordinary differential equations, and infinite series.", "SHARED", "1st Year", "1st Sem", "Foundational", "4 Weeks", "FOUNDATION"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Programming for Problem Solving using C", "Algorithmic thinking, syntax, arrays, functions, pointers, and memory layout.", "SHARED", "1st Year", "1st Sem", "Foundational", "4 Weeks", "FOUNDATION"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Engineering Physics & Semiconductor Basics", "Applied wave optics, lasers, fiber physics, quantum mechanics, and semiconductors.", "SHARED", "1st Year", "1st Sem", "Foundational", "4 Weeks", "FOUNDATION"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Advanced Calculus & Linear Algebra", "Vector calculus, matrices, eigenvalues, eigenvectors, and multivariable functions.", "SHARED", "1st Year", "2nd Sem", "Foundational", "4 Weeks", "FOUNDATION"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Python Programming for Engineering Applications", "Applied scripting, functional programming, numerical packages, and data visualization.", "SHARED", "1st Year", "2nd Sem", "Foundational", "4 Weeks", "FOUNDATION"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Basic Electrical & Electronics Engineering", "Fundamental circuit laws, AC/DC machines, semiconductor diodes, and op-amps.", "SHARED", "1st Year", "2nd Sem", "Foundational", "4 Weeks", "FOUNDATION"));

        // --- COMPUTER SCIENCE & ENGINEERING (CSE) ---
        // 2nd Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Data Structures & Algorithmic Complexity", "Arrays, linked lists, trees, graphs, hashing, recursion, and Big-O efficiency analysis.", "CSE", "2nd Year", "1st Sem", "Intermediate", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Object-Oriented Programming with Java & C++", "Encapsulation, inheritance, polymorphism, templates, exception handling, and collections framework.", "CSE", "2nd Year", "1st Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Discrete Mathematical Structures & Graph Theory", "Propositional logic, set theory, combinatorics, recurrence relations, and graph algorithms.", "CSE", "2nd Year", "1st Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Database Management Systems & SQL Mastery", "Relational schema design, normalization, ACID transactions, concurrency, and indexing.", "CSE", "2nd Year", "2nd Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Computer Organization & Architecture", "Instruction set architectures, CPU datapath, pipelining, and cache memory hierarchy.", "CSE", "2nd Year", "2nd Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Design & Analysis of Algorithms", "Divide and conquer, greedy strategies, dynamic programming, and NP-completeness theory.", "CSE", "2nd Year", "2nd Sem", "Advanced", "6 Weeks", "CORE"));
        // 3rd Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Operating Systems & Kernel Process Modeling", "Process scheduling, thread synchronization, deadlocks, paging, and virtual memory.", "CSE", "3rd Year", "1st Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Computer Networks & Protocol Architectures", "OSI/TCP-IP models, packet routing, congestion control, and network socket programming.", "CSE", "3rd Year", "1st Sem", "Advanced", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Formal Languages & Automata Theory", "DFA/NFA, context-free grammars, pushdown automata, and Turing machine computability.", "CSE", "3rd Year", "1st Sem", "Advanced", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Full-Stack Microservices & Cloud Architectures", "REST APIs, containerization, microservice patterns, and distributed state management.", "CSE", "3rd Year", "2nd Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Compiler Design & Lexical Optimization", "Lexical parsing, syntax-directed translation, symbol tables, and intermediate code generation.", "CSE", "3rd Year", "2nd Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Software Engineering & Agile Methodologies", "UML modeling, design patterns, test-driven development, and CI/CD pipelines.", "CSE", "3rd Year", "2nd Sem", "Intermediate", "4 Weeks", "CORE"));
        // 4th Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Distributed Systems & Cloud Computing Architectures", "Consensus protocols, cloud infrastructure, replication, and distributed storage.", "CSE", "4th Year", "1st Sem", "Specialized", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Information & Cyber Security Engineering", "Applied cryptography, network penetration testing, vulnerabilities, and secure coding.", "CSE", "4th Year", "1st Sem", "Specialized", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("High-Performance Computing & Parallel Architectures", "MPI, OpenMP, GPU programming with CUDA, and cluster scalability.", "CSE", "4th Year", "2nd Sem", "Specialized", "6 Weeks", "CORE"));

        // --- INFORMATION TECHNOLOGY (IT) ---
        COURSE_CATALOGUE.add(new CatalogueCourse("Data Structures & Practical Algorithms", "Core linear/non-linear structures, search optimization, and practical algorithmic complexity.", "IT", "2nd Year", "1st Sem", "Intermediate", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Object-Oriented Software Design", "Enterprise Java, design patterns, SOLID principles, and modular application development.", "IT", "2nd Year", "1st Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Web Technologies & Interactive UI Development", "Modern JavaScript, DOM manipulation, responsive layouts, and REST integration.", "IT", "2nd Year", "2nd Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Relational Databases & Enterprise Data Modeling", "Enterprise SQL, query tuning, normalization, transactions, and stored procedures.", "IT", "2nd Year", "2nd Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Computer Networks & Network Administration", "Routing protocols, subnetting, network security, and Linux administration.", "IT", "3rd Year", "1st Sem", "Advanced", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Cloud Infrastructure & DevOps Automation", "Docker containers, Kubernetes orchestration, CI/CD pipelines, and AWS architecture.", "IT", "3rd Year", "2nd Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Enterprise Cloud Architecture & Data Engineering", "Distributed streaming, big data warehousing, and serverless architectural patterns.", "IT", "4th Year", "1st Sem", "Specialized", "6 Weeks", "CORE"));

        // --- ELECTRONICS & COMMUNICATION ENGINEERING (ECE) ---
        // 2nd Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Electronic Devices & Circuit Theory", "PN junctions, BJT/MOSFET modeling, biasing techniques, and small-signal amplifiers.", "ECE", "2nd Year", "1st Sem", "Intermediate", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Digital Logic Design & Verilog Modeling", "Combinational logic, sequential circuits, finite state machines, and HDL simulation.", "ECE", "2nd Year", "1st Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Signals & Systems Analysis", "LTI systems, convolution, continuous/discrete Fourier transforms, and Z-transforms.", "ECE", "2nd Year", "1st Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Analog Circuits & Operational Amplifiers", "Op-amp configurations, active filters, feedback amplifiers, and sinusoidal oscillators.", "ECE", "2nd Year", "2nd Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Electromagnetic Waves & Transmission Lines", "Maxwell's equations, wave propagation in dielectrics, and transmission line matching.", "ECE", "2nd Year", "2nd Sem", "Advanced", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Probability Theory & Stochastic Processes", "Random variables, Gaussian processes, and noise analysis in communications.", "ECE", "2nd Year", "2nd Sem", "Intermediate", "4 Weeks", "CORE"));
        // 3rd Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Digital Signal Processing (DSP) & Transform Analysis", "DFT, FFT algorithms, FIR/IIR digital filter design, and spectral analysis.", "ECE", "3rd Year", "1st Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Microprocessors, Microcontrollers & Interfacing", "ARM/8086 architectures, memory interfacing, timers, interrupts, and peripherals.", "ECE", "3rd Year", "1st Sem", "Advanced", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Analog & Digital Communication Systems", "AM, FM, QAM modulations, PCM, digital demodulation, and SNR performance.", "ECE", "3rd Year", "1st Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("VLSI Design & CMOS Circuit Implementation", "CMOS layout, stick diagrams, propagation delay modeling, and Verilog synthesis.", "ECE", "3rd Year", "2nd Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Antenna Theory & Microwave Engineering", "Radiation patterns, dipoles, microstrip antennas, waveguides, and S-parameters.", "ECE", "3rd Year", "2nd Sem", "Advanced", "5 Weeks", "CORE"));
        // 4th Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Embedded Real-Time Operating Systems (RTOS)", "Task scheduling, inter-process communication, FreeRTOS, and IoT edge devices.", "ECE", "4th Year", "1st Sem", "Specialized", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Optical Fiber & Wireless Cellular Networks", "Fiber dispersion, optical transmitters, 5G wireless standards, and MIMO beamforming.", "ECE", "4th Year", "2nd Sem", "Specialized", "6 Weeks", "CORE"));

        // --- ELECTRICAL & ELECTRONICS ENGINEERING (EEE) ---
        // 2nd Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Electric Circuit Theory & Network Analysis", "Kirchhoff's laws, Thevenin/Norton theorems, AC circuit resonance, and transient response.", "EEE", "2nd Year", "1st Sem", "Intermediate", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("DC Machines & Transformers", "DC generators and motors, speed control, single-phase and three-phase transformers.", "EEE", "2nd Year", "1st Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Electromagnetic Field Theory", "Coulomb's law, Biot-Savart law, magnetic circuits, and time-varying fields.", "EEE", "2nd Year", "1st Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("AC Electrical Machines & Induction Motors", "Three-phase induction motors, synchronous machines, and circle diagrams.", "EEE", "2nd Year", "2nd Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Analog Electronics for Electrical Engineers", "Diode rectifiers, power BJTs, op-amps, and industrial switching circuits.", "EEE", "2nd Year", "2nd Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Power Systems Generation & Economics", "Hydro, thermal, nuclear power generation, load duration curves, and tariff calculation.", "EEE", "2nd Year", "2nd Sem", "Advanced", "5 Weeks", "CORE"));
        // 3rd Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Power Electronics & Solid-State Switching", "SCR, MOSFET, IGBT characteristics, phase-controlled rectifiers, choppers, and inverters.", "EEE", "3rd Year", "1st Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Transmission & Distribution of Electrical Power", "Line constants, Ferranti effect, cable insulation, and substation layout.", "EEE", "3rd Year", "1st Sem", "Advanced", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Control Systems Engineering & State Variable Analysis", "Transfer functions, Routh-Hurwitz, Nyquist stability, Bode plots, and state-space.", "EEE", "3rd Year", "1st Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Power System Analysis & Fault Diagnostics", "Symmetrical and unsymmetrical faults, Gauss-Seidel and Newton-Raphson load flow.", "EEE", "3rd Year", "2nd Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Microcontroller Applications in Power Systems", "Digital control of drives, PWM waveform generation, and sensor telemetry.", "EEE", "3rd Year", "2nd Sem", "Advanced", "5 Weeks", "CORE"));
        // 4th Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Power System Protection & Switchgear", "Differential protection, numerical distance relays, and circuit breaker arc extinction.", "EEE", "4th Year", "1st Sem", "Specialized", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Electric Vehicle Powertrains & Renewable Energy", "EV motor controllers, battery management systems, and solar PV/wind grid integration.", "EEE", "4th Year", "2nd Sem", "Specialized", "6 Weeks", "CORE"));

        // --- MECHANICAL ENGINEERING (MECH) ---
        // 2nd Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Mechanics of Solids & Strength of Materials", "Stress, strain, Mohr's circle, shear force and bending moment diagrams, torsion of shafts.", "MECH", "2nd Year", "1st Sem", "Intermediate", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Thermodynamics & Heat Transfer Fundamentals", "First and second laws of thermodynamics, Carnot cycle, entropy, and heat engines.", "MECH", "2nd Year", "1st Sem", "Intermediate", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Material Science & Metallurgy for Engineers", "Crystal structures, iron-carbon equilibrium diagram, TTT curves, and heat treatment.", "MECH", "2nd Year", "1st Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Fluid Mechanics & Hydraulic Machinery", "Fluid statics, Bernoulli's equation, boundary layer theory, Pelton/Francis turbines, and pumps.", "MECH", "2nd Year", "2nd Sem", "Intermediate", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Kinematics of Machinery & Mechanism Synthesis", "Velocity and acceleration analysis of mechanisms, cam profiles, and gear trains.", "MECH", "2nd Year", "2nd Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Manufacturing Processes & Machine Tool Technology", "Foundry casting, fusion welding, sheet metal forming, and lathe/milling operations.", "MECH", "2nd Year", "2nd Sem", "Intermediate", "5 Weeks", "CORE"));
        // 3rd Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Dynamics of Machinery & Vibration Analysis", "Balancing of rotating masses, longitudinal/transverse vibrations, and gyroscopic couples.", "MECH", "3rd Year", "1st Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Applied Thermal Engineering & IC Engines", "Four-stroke/two-stroke engine cycles, combustion analysis, refrigeration, and air conditioning.", "MECH", "3rd Year", "1st Sem", "Advanced", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Design of Machine Elements & Fatigue Analysis", "Design of shafts, keys, couplings, welded joints, springs, and bearings under dynamic loading.", "MECH", "3rd Year", "1st Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Heat & Mass Transfer Engineering", "Steady/unsteady conduction, convective heat transfer coefficients, radiation, and heat exchangers.", "MECH", "3rd Year", "2nd Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("CAD/CAM & Finite Element Modeling (FEA)", "Solid modeling, NC part programming, 1D/2D finite element formulations, and meshing.", "MECH", "3rd Year", "2nd Sem", "Advanced", "5 Weeks", "CORE"));
        // 4th Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Robotics & Industrial Automation Systems", "Forward/inverse kinematics, hydraulic/pneumatic actuators, and automated assembly.", "MECH", "4th Year", "1st Sem", "Specialized", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Automobile Engineering & Electric Vehicle Dynamics", "Chassis and suspension geometry, braking systems, EV battery cooling, and aerodynamics.", "MECH", "4th Year", "2nd Sem", "Specialized", "6 Weeks", "CORE"));

        // --- CIVIL ENGINEERING (CIVIL) ---
        // 2nd Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Mechanics of Materials & Structural Foundations", "Stress-strain relationships, bending and shear stresses in beams, and deflection theorems.", "CIVIL", "2nd Year", "1st Sem", "Intermediate", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Surveying & Geomatics Engineering", "Theodolite surveying, leveling, contouring, curves, and electronic total stations.", "CIVIL", "2nd Year", "1st Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Fluid Mechanics & Open Channel Flow", "Hydrostatic forces, laminar and turbulent flow, Manning's equation, and hydraulic jumps.", "CIVIL", "2nd Year", "1st Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Structural Analysis & Energy Methods", "Indeterminate structures, slope deflection method, moment distribution, and influence lines.", "CIVIL", "2nd Year", "2nd Sem", "Intermediate", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Building Materials, Construction & Concrete Technology", "Properties of cement, aggregates, workability, concrete mix design, and curing methods.", "CIVIL", "2nd Year", "2nd Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Hydraulic Engineering & Water Resources", "Hydrologic cycle, hydrographs, flood routing, dam design, and irrigation networks.", "CIVIL", "2nd Year", "2nd Sem", "Intermediate", "5 Weeks", "CORE"));
        // 3rd Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Design of Reinforced Concrete Structures (RCC)", "Limit state design of singly/doubly reinforced beams, one-way/two-way slabs, and columns.", "CIVIL", "3rd Year", "1st Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Geotechnical Engineering & Soil Mechanics", "Phase relations, Atterberg limits, soil compaction, permeability, and shear strength parameters.", "CIVIL", "3rd Year", "1st Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Transportation & Highway Engineering", "Highway alignment, sight distances, flexible/rigid pavement design, and traffic engineering.", "CIVIL", "3rd Year", "1st Sem", "Advanced", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Design of Steel Structures", "Bolted and welded joints, tension and compression members, and steel roof trusses.", "CIVIL", "3rd Year", "2nd Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Foundation Engineering & Slope Stability", "Terzaghi's bearing capacity, shallow and deep pile foundations, and earth pressure theories.", "CIVIL", "3rd Year", "2nd Sem", "Advanced", "5 Weeks", "CORE"));
        // 4th Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Environmental Engineering & Wastewater Treatment", "Water treatment design, BOD/COD testing, activated sludge processes, and air pollution.", "CIVIL", "4th Year", "1st Sem", "Specialized", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Construction Management, Estimation & Safety", "Quantity surveying, rate analysis, CPM/PERT scheduling, and safety standards.", "CIVIL", "4th Year", "2nd Sem", "Specialized", "6 Weeks", "CORE"));

        // --- ARTIFICIAL INTELLIGENCE & MACHINE LEARNING (AIML / AIDS) ---
        // 2nd Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Mathematical Foundations of AI & Linear Algebra", "Vector spaces, eigen decomposition, SVD, multivariable calculus, and gradient descent.", "AIML", "2nd Year", "1st Sem", "Intermediate", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Data Structures & Algorithms with Python", "Pythonic data structures, algorithm efficiency, graph representations, and recursions.", "AIML", "2nd Year", "1st Sem", "Intermediate", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Probability & Statistics for Machine Learning", "Bayesian probability, random variables, hypothesis testing, and maximum likelihood estimation.", "AIML", "2nd Year", "1st Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Foundations of Artificial Intelligence & Search", "State-space graphs, informed A* search, minimax game trees, and constraint satisfaction.", "AIML", "2nd Year", "2nd Sem", "Intermediate", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Machine Learning Fundamentals & Scikit-Learn", "Linear/logistic regression, decision trees, ensemble methods, and unsupervised clustering.", "AIML", "2nd Year", "2nd Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Database Systems & Data Warehousing", "SQL querying, schema design, NoSQL document stores, and dimensional modeling.", "AIML", "2nd Year", "2nd Sem", "Intermediate", "5 Weeks", "CORE"));
        // 3rd Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Deep Learning & Neural Network Architectures", "Backpropagation calculus, CNNs, RNNs, LSTMs, and PyTorch model implementation.", "AIML", "3rd Year", "1st Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Natural Language Processing (NLP) & Text Mining", "Tokenization, TF-IDF, Word2Vec, Transformer attention heads, and sentiment analysis.", "AIML", "3rd Year", "1st Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Data Engineering Pipelines & Distributed Computing", "Data ingestion with Kafka, distributed processing with Apache Spark, and cloud data lakes.", "AIML", "3rd Year", "1st Sem", "Advanced", "5 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Computer Vision & Visual Perception Systems", "Convolutional feature extraction, YOLO object detection, and image segmentation.", "AIML", "3rd Year", "2nd Sem", "Advanced", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Generative AI, LLMs & Prompt Engineering", "Transformer decoder architectures, LoRA fine-tuning, RAG pipelines, and vector databases.", "AIML", "3rd Year", "2nd Sem", "Advanced", "6 Weeks", "CORE"));
        // 4th Year
        COURSE_CATALOGUE.add(new CatalogueCourse("Reinforcement Learning & Autonomous Agents", "Markov decision processes, Q-learning, policy gradients, and actor-critic algorithms.", "AIML", "4th Year", "1st Sem", "Specialized", "6 Weeks", "CORE"));
        COURSE_CATALOGUE.add(new CatalogueCourse("MLOps: Production Machine Learning & Model Serving", "MLflow tracking, Dockerized model APIs, latency optimization, and continuous monitoring.", "AIML", "4th Year", "2nd Sem", "Specialized", "6 Weeks", "CORE"));

        // --- HONORS & REMEDIAL TRACKS ---
        COURSE_CATALOGUE.add(new CatalogueCourse("Competitive Programming & Advanced Algorithmic Mastery", "Advanced graph algorithms, segment trees, dynamic programming on trees, and competitive contest problem-solving.", "ALL", "ANY", "ANY", "Challenging", "6 Weeks", "HONORS"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Research Methodologies & Systems Engineering", "Literature review frameworks, experimental design, technical publishing, and patent drafting.", "ALL", "ANY", "ANY", "Challenging", "6 Weeks", "HONORS"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Core Academic Engineering Foundations Booster", "Targeted concept reinforcement to elevate academic fundamentals and exam performance.", "ALL", "ANY", "ANY", "Revision", "3 Weeks", "REMEDIAL"));
        COURSE_CATALOGUE.add(new CatalogueCourse("Engineering Mathematics & Analytical Problem Solving Track", "Calculus, matrix operations, and analytical problem-solving booster for academic recovery.", "ALL", "ANY", "ANY", "Revision", "4 Weeks", "REMEDIAL"));
    }

    public static String normalizeBranch(String branch) {
        if (branch == null || branch.trim().isEmpty()) return "CSE";
        String b = branch.trim().toUpperCase();
        if (b.contains("MECH") || b.contains("MECHANICAL")) return "MECH";
        if (b.contains("CIVIL")) return "CIVIL";
        if (b.contains("EEE") || b.contains("ELECTRICAL")) return "EEE";
        if (b.contains("ECE") || b.contains("ELECTRONICS") || b.contains("COMMUNICATION")) return "ECE";
        if (b.contains("AIML") || b.contains("AI & ML") || b.contains("AI/ML") || (b.contains("ARTIFICIAL") && b.contains("MACHINE"))) return "AIML";
        if (b.contains("AIDS") || b.contains("AI & DS") || b.contains("AI/DS") || (b.contains("ARTIFICIAL") && b.contains("DATA"))) return "AIML";
        if (b.contains("IT") || b.contains("INFORMATION")) return "IT";
        if (b.contains("CSE") || b.contains("COMPUTER")) return "CSE";
        return b;
    }

    public static String normalizeYear(String year) {
        if (year == null || year.trim().isEmpty()) return "2nd Year";
        String y = year.trim().toLowerCase();
        if (y.contains("1") || y.contains("first")) return "1st Year";
        if (y.contains("2") || y.contains("second")) return "2nd Year";
        if (y.contains("3") || y.contains("third")) return "3rd Year";
        if (y.contains("4") || y.contains("fourth")) return "4th Year";
        return "2nd Year";
    }

    public static String normalizeSemester(String sem) {
        if (sem == null || sem.trim().isEmpty()) return "1st Sem";
        String s = sem.trim().toLowerCase();
        if (s.contains("2") || s.contains("second") || s.contains("sem 2")) return "2nd Sem";
        return "1st Sem";
    }

    public List<Map<String, Object>> getPersonalizedRecommendations(User student) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        Set<String> addedTitles = new HashSet<>();

        String email = student != null && student.getEmail() != null ? student.getEmail().trim().toLowerCase() : "";
        String branch = normalizeBranch(student != null ? student.getBranch() : null);
        String year = normalizeYear(student != null ? student.getAcademicYear() : null);
        String sem = normalizeSemester(student != null ? student.getSemester() : null);
        Double cgpa = student != null ? student.getCgpa() : null;

        // Retrieve existing enrolled courses to prevent duplicate recommendations
        Set<String> enrolledTitles = new HashSet<>();
        if (!email.isEmpty()) {
            enrolledTitles = courseRepository.findByUserEmailIgnoreCaseAndDeletedFalse(email)
                    .stream().map(c -> c.getTitle().trim().toLowerCase()).collect(Collectors.toSet());
        }

        // 1. Faculty-Assigned Recommendations (Top Priority for the student's cohort)
        if (email.endsWith("@vrsec.ac.in") || (student != null && student.isEligibleStudent())) {
            List<SuggestedCourse> facultyPicks = suggestedCourseRepository.findAll().stream()
                .filter(sc -> sc.getTargetBranch() == null || sc.getTargetBranch().equalsIgnoreCase("ALL") || normalizeBranch(sc.getTargetBranch()).equalsIgnoreCase(branch))
                .filter(sc -> sc.getTargetYear() == null || sc.getTargetYear().equalsIgnoreCase("ALL") || normalizeYear(sc.getTargetYear()).equalsIgnoreCase(year))
                .filter(sc -> sc.getTargetSemester() == null || sc.getTargetSemester().equalsIgnoreCase("ALL") || normalizeSemester(sc.getTargetSemester()).equalsIgnoreCase(sem))
                .collect(Collectors.toList());

            for (SuggestedCourse sc : facultyPicks) {
                String cleanT = sc.getTitle().trim().toLowerCase();
                if (!enrolledTitles.contains(cleanT) && addedTitles.add(cleanT)) {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("title", sc.getTitle());
                    rec.put("description", sc.getDescription() != null ? sc.getDescription() : "Assigned department curriculum module.");
                    rec.put("reason", "Mandatory Faculty Assignment by " + (sc.getFacultyName() != null ? sc.getFacultyName() : "Department Faculty"));
                    rec.put("suggestedBy", sc.getFacultyName() != null ? sc.getFacultyName() : "Faculty");
                    rec.put("difficulty", "Advanced");
                    rec.put("duration", "4-6 Weeks");
                    rec.put("isFacultyAssigned", true);
                    rec.put("priority", sc.getPriority() != null ? sc.getPriority() : "HIGH");
                    recommendations.add(rec);
                }
            }
        }

        // 2. Adaptive AI Recommendations (Weak Topics detected from recent quiz attempts)
        if (student != null && student.getId() != null) {
            List<TopicPerformance> weakTopics = topicPerformanceRepository.findByUserIdAndStatus(student.getId(), "NEEDS_ATTENTION");
            for (TopicPerformance tp : weakTopics) {
                String remedialTitle = tp.getCourseTitle() + ": Remedial Booster on " + tp.getConceptTag();
                String cleanT = remedialTitle.toLowerCase();
                if (!enrolledTitles.contains(cleanT) && addedTitles.add(cleanT)) {
                    Map<String, Object> remedial = new HashMap<>();
                    remedial.put("title", remedialTitle);
                    remedial.put("description", "Targeted reinforcement modules to overcome struggles in " + tp.getConceptTag() + " based on your recent quiz performance.");
                    remedial.put("reason", "Performance Analysis: Weakness detected in " + tp.getConceptTag());
                    remedial.put("suggestedBy", "Adaptive AI");
                    remedial.put("difficulty", "Remedial");
                    remedial.put("duration", "1-2 Weeks");
                    remedial.put("isFacultyAssigned", false);
                    recommendations.add(remedial);
                }
            }
        }

        // 3. TIER 1: Exact Match (Branch + Academic Year + Semester)
        List<CatalogueCourse> exactMatches = COURSE_CATALOGUE.stream()
            .filter(c -> c.getBranch().equalsIgnoreCase(branch) && c.getAcademicYear().equalsIgnoreCase(year) && c.getSemester().equalsIgnoreCase(sem))
            .collect(Collectors.toList());

        for (CatalogueCourse cc : exactMatches) {
            String cleanT = cc.getTitle().toLowerCase();
            if (!enrolledTitles.contains(cleanT) && addedTitles.add(cleanT)) {
                recommendations.add(createCatalogueRec(cc, "Core " + year + " requirement: Exactly matches your " + branch + " (" + sem + ") curriculum.", "Curriculum AI"));
            }
        }

        // 4. TIER 2: Current Year Broader Match (Same Branch + Same Year, other semester)
        if (recommendations.size() < 4) {
            List<CatalogueCourse> yearMatches = COURSE_CATALOGUE.stream()
                .filter(c -> c.getBranch().equalsIgnoreCase(branch) && c.getAcademicYear().equalsIgnoreCase(year) && !c.getSemester().equalsIgnoreCase(sem))
                .collect(Collectors.toList());

            for (CatalogueCourse cc : yearMatches) {
                String cleanT = cc.getTitle().toLowerCase();
                if (!enrolledTitles.contains(cleanT) && addedTitles.add(cleanT)) {
                    recommendations.add(createCatalogueRec(cc, "Relevant to your " + branch + " " + year + " academic curriculum.", "Curriculum AI"));
                    if (recommendations.size() >= 5) break;
                }
            }
        }

        // 5. TIER 3: Shared 1st Year Engineering Foundations (only for 1st Year or when relevant)
        if (year.equalsIgnoreCase("1st Year") || recommendations.size() < 3) {
            List<CatalogueCourse> sharedFoundations = COURSE_CATALOGUE.stream()
                .filter(c -> c.getBranch().equalsIgnoreCase("SHARED") && (c.getAcademicYear().equalsIgnoreCase(year) || c.getAcademicYear().equalsIgnoreCase("ANY")))
                .collect(Collectors.toList());

            for (CatalogueCourse cc : sharedFoundations) {
                String cleanT = cc.getTitle().toLowerCase();
                if (!enrolledTitles.contains(cleanT) && addedTitles.add(cleanT)) {
                    recommendations.add(createCatalogueRec(cc, "Foundational engineering core for " + year + " students.", "Foundations AI"));
                    if (recommendations.size() >= 4) break;
                }
            }
        }

        // 6. TIER 4: Branch Core Specialization (STRICTLY same branch, never cross-branch!)
        if (recommendations.size() < 4) {
            List<CatalogueCourse> branchTracks = COURSE_CATALOGUE.stream()
                .filter(c -> c.getBranch().equalsIgnoreCase(branch))
                .collect(Collectors.toList());

            for (CatalogueCourse cc : branchTracks) {
                String cleanT = cc.getTitle().toLowerCase();
                if (!enrolledTitles.contains(cleanT) && addedTitles.add(cleanT)) {
                    recommendations.add(createCatalogueRec(cc, "Recommended for " + branch + " engineering domain expertise.", "Department AI"));
                    if (recommendations.size() >= 5) break;
                }
            }
        }

        // 7. CGPA Performance-Tailored Tracks (Honors vs Remedial)
        if (cgpa != null && cgpa > 0.0) {
            if (cgpa >= 8.5) {
                CatalogueCourse honors = COURSE_CATALOGUE.stream()
                    .filter(c -> c.getCategory().equalsIgnoreCase("HONORS"))
                    .findFirst().orElse(null);
                if (honors != null) {
                    String cleanT = honors.getTitle().toLowerCase();
                    if (!enrolledTitles.contains(cleanT) && addedTitles.add(cleanT)) {
                        recommendations.add(createCatalogueRec(honors, "Honors Track: Advanced challenge tailored for high CGPA (" + String.format("%.2f", cgpa) + ") students.", "Honors AI"));
                    }
                }
            } else if (cgpa < 7.0) {
                CatalogueCourse remedial = COURSE_CATALOGUE.stream()
                    .filter(c -> c.getCategory().equalsIgnoreCase("REMEDIAL"))
                    .findFirst().orElse(null);
                if (remedial != null) {
                    String cleanT = remedial.getTitle().toLowerCase();
                    if (!enrolledTitles.contains(cleanT) && addedTitles.add(cleanT)) {
                        recommendations.add(createCatalogueRec(remedial, "Academic Booster: Targeted concept reinforcement for CGPA (" + String.format("%.2f", cgpa) + ").", "Adaptive AI"));
                    }
                }
            }
        }

        return recommendations;
    }

    private Map<String, Object> createCatalogueRec(CatalogueCourse cc, String reason, String suggestedBy) {
        Map<String, Object> m = new HashMap<>();
        m.put("title", cc.getTitle());
        m.put("description", cc.getDescription());
        m.put("reason", reason);
        m.put("suggestedBy", suggestedBy);
        m.put("difficulty", cc.getDifficulty());
        m.put("duration", cc.getDuration());
        m.put("isFacultyAssigned", false);
        return m;
    }

    // =========================================================================
    // 2. DYNAMIC SUBJECT-SPECIFIC LEARNING PATH GENERATION (5 to 8 PHASES)
    // =========================================================================
    @Transactional
    public Course generateSubjectLearningPath(User student, String subjectName) {
        String cleanSubject = subjectName != null ? subjectName.trim() : "";
        if (cleanSubject.isEmpty()) {
            throw new IllegalArgumentException("Course title cannot be empty.");
        }

        // Validate course existence against the approved catalogue
        if (approvedCourseRepository != null && approvedCourseRepository.count() > 0) {
            ApprovedCourse approved = approvedCourseRepository.findByTitleIgnoreCaseAndActiveTrue(cleanSubject).orElse(null);
            if (approved == null) {
                approved = approvedCourseRepository.findByCodeIgnoreCaseAndActiveTrue(cleanSubject).orElse(null);
            }
            if (approved == null) {
                List<ApprovedCourse> activeList = approvedCourseRepository.findByActiveTrue();
                if (activeList != null) {
                    String clean = cleanSubject.toLowerCase();
                    for (ApprovedCourse ac : activeList) {
                        String actLower = ac.getTitle().toLowerCase();
                        if (actLower.contains(clean) || clean.contains(actLower)
                                || (clean.contains("data struct") && actLower.contains("data struct"))) {
                            approved = ac;
                            break;
                        }
                    }
                }
            }
            if (approved == null) {
                throw new IllegalArgumentException("The course '" + cleanSubject + "' is not in the approved catalogue. Arbitrary course generation is not permitted. Please select an approved course from the catalogue or submit a Course Request.");
            }
            cleanSubject = approved.getTitle();
        }

        final String targetSubject = cleanSubject;

        // Check if student already enrolled in this exact course
        Optional<Course> existing = courseRepository.findByUserEmailIgnoreCaseAndDeletedFalse(student.getEmail())
                .stream().filter(c -> c.getTitle().equalsIgnoreCase(targetSubject)).findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }

        List<Map<String, Object>> phasesData = fetchCurriculumFromGemini(targetSubject, student);
        if (phasesData == null || phasesData.isEmpty()) {
            phasesData = SubjectCurriculumRegistry.getCurriculumForSubject(targetSubject, student);
        }

        if (phasesData == null || phasesData.isEmpty()) {
            throw new IllegalStateException("Curriculum generation is currently unavailable for " + targetSubject + ". Please try again later.");
        }

        Course course = new Course();
        course.setTitle(targetSubject);
        course.setDescription("Comprehensive AI-generated curriculum specifically structured for " + targetSubject);
        course.setProgress(0);
        course.setCurrentPhase(1);
        course.setTotalPhases(phasesData.size());
        course.setDifficulty("Adaptive");
        course.setUserEmail(student.getEmail());
        course.setCustom(true);
        course.setDeleted(false);
        Course savedCourse = courseRepository.save(course);

        int phaseNum = 1;
        for (Map<String, Object> pMap : phasesData) {
            CoursePhase phase = new CoursePhase();
            phase.setCourse(savedCourse);
            phase.setPhaseNumber(phaseNum);
            phase.setTitle((String) pMap.getOrDefault("title", "Phase " + phaseNum));
            phase.setLearningObjectives((String) pMap.getOrDefault("objectives", "Master fundamental and practical concepts."));
            phase.setStudyMaterial((String) pMap.getOrDefault("studyMaterial", "Detailed academic content."));
            phase.setEstimatedDuration((String) pMap.getOrDefault("duration", "3-4 Hours"));
            // Phase 1 is unlocked by default; others are locked until quiz is passed (>60%)
            phase.setUnlocked(phaseNum == 1);
            phase.setCompleted(false);
            phaseRepository.save(phase);
            phaseNum++;
        }

        return savedCourse;
    }

    // =========================================================================
    // 3. EXACTLY 10-QUESTION QUIZ GENERATION PER PHASE
    // =========================================================================
    @Transactional
    public List<Map<String, Object>> getOrCreatePhaseQuiz(Long courseId, Integer phaseNumber) {
        CoursePhase phase = phaseRepository.findByCourseIdAndPhaseNumber(courseId, phaseNumber)
                .orElseThrow(() -> new IllegalArgumentException("Course phase not found."));

        List<QuizQuestion> existingQuestions = questionRepository.findByPhaseIdOrderByIdAsc(phase.getId());
        if (existingQuestions.size() >= 10) {
            if (!isLegacyDummyQuiz(existingQuestions)) {
                return maskQuestionsForClient(existingQuestions);
            }
            // Purge legacy dummy questions
            questionRepository.deleteAll(existingQuestions);
            questionRepository.flush();
        }

        // Retrieve student profile context for tailored questions
        Course course = phase.getCourse();
        User student = null;
        if (course != null && course.getUserEmail() != null) {
            student = userRepository.findByEmailIgnoreCase(course.getUserEmail()).orElse(null);
        }

        // Generate 10 brand-new tailored questions using Gemini / domain bank
        List<QuizQuestion> generated = generate10QuestionsForPhase(phase, student);
        questionRepository.saveAll(generated);
        return maskQuestionsForClient(generated);
    }

    private List<Map<String, Object>> maskQuestionsForClient(List<QuizQuestion> questions) {
        List<Map<String, Object>> list = new ArrayList<>();
        int index = 1;
        for (QuizQuestion q : questions) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", q.getId());
            map.put("questionIndex", index++);
            map.put("questionText", q.getQuestionText());
            map.put("options", Arrays.asList(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()));
            map.put("conceptTag", q.getConceptTag());
            // CRITICAL SECURITY: Never expose correctOption or explanation to client before submission!
            list.add(map);
        }
        return list;
    }

    // =========================================================================
    // 4. SUBMIT QUIZ & STRICTLY > 60% AUTOMATIC PHASE UNLOCKING
    // =========================================================================
    @Transactional
    public Map<String, Object> submitQuizAnswers(User student, Long courseId, Integer phaseNumber, Map<Long, String> submittedAnswers) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found."));
        CoursePhase phase = phaseRepository.findByCourseIdAndPhaseNumber(courseId, phaseNumber)
                .orElseThrow(() -> new IllegalArgumentException("Phase not found."));

        List<QuizQuestion> questions = questionRepository.findByPhaseIdOrderByIdAsc(phase.getId());
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("No quiz configured for this phase.");
        }

        int score = 0;
        int total = questions.size();
        List<Map<String, Object>> review = new ArrayList<>();
        List<String> weakConcepts = new ArrayList<>();

        for (QuizQuestion q : questions) {
            String studentSelected = submittedAnswers != null ? submittedAnswers.get(q.getId()) : null;
            if (studentSelected != null) studentSelected = studentSelected.trim().toUpperCase();

            boolean isCorrect = q.getCorrectOption() != null && q.getCorrectOption().equalsIgnoreCase(studentSelected);
            if (isCorrect) {
                score++;
            } else {
                if (q.getConceptTag() != null) {
                    weakConcepts.add(q.getConceptTag());
                }
            }

            Map<String, Object> revItem = new HashMap<>();
            revItem.put("questionId", q.getId());
            revItem.put("questionText", q.getQuestionText());
            revItem.put("selectedOption", studentSelected);
            revItem.put("correctOption", q.getCorrectOption());
            revItem.put("isCorrect", isCorrect);
            revItem.put("explanation", q.getExplanation());
            revItem.put("conceptTag", q.getConceptTag());
            review.add(revItem);
        }

        // STRICT REQUIREMENT: score > 60% (Since total = 10, strictly score >= 7, percentage > 60%)
        double percentage = Math.round(((double) score / total) * 100.0);
        boolean passed = percentage > 60.0;

        // Save Attempt
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUser(student);
        attempt.setCourse(course);
        attempt.setPhase(phase);
        attempt.setScore(score);
        attempt.setTotalQuestions(total);
        attempt.setPercentage(percentage);
        attempt.setPassed(passed);
        attempt.setTopic(course.getTitle() + " | " + phase.getTitle());
        attemptRepository.save(attempt);

        boolean nextPhaseUnlocked = false;
        boolean courseCompleted = false;

        if (passed) {
            phase.setCompleted(true);
            phase.setCompletedAt(LocalDateTime.now());
            phaseRepository.save(phase);

            // Attempt to unlock next phase
            Optional<CoursePhase> nextPhaseOpt = phaseRepository.findByCourseIdAndPhaseNumber(courseId, phaseNumber + 1);
            if (nextPhaseOpt.isPresent()) {
                CoursePhase nextPhase = nextPhaseOpt.get();
                nextPhase.setUnlocked(true);
                phaseRepository.save(nextPhase);
                course.setCurrentPhase(phaseNumber + 1);
                nextPhaseUnlocked = true;
            } else {
                // Final phase passed! Course completed!
                courseCompleted = true;
            }

            // Update course progress
            List<CoursePhase> allPhases = phaseRepository.findByCourseIdOrderByPhaseNumberAsc(courseId);
            long completedCount = allPhases.stream().filter(CoursePhase::isCompleted).count();
            int progressPct = (int) Math.round(((double) completedCount / allPhases.size()) * 100.0);
            course.setProgress(progressPct);
            courseRepository.save(course);
        }

        // Update topic performance records safely after attempt and phase are saved
        try {
            for (String weakConcept : weakConcepts) {
                updateTopicPerformance(student, course.getTitle(), weakConcept, false);
            }
        } catch (Exception ignored) {
            // Secondary tracking metric; never fail the student's submission
        }

        Map<String, Object> response = new HashMap<>();
        response.put("passed", passed);
        response.put("score", score);
        response.put("totalQuestions", total);
        response.put("percentage", percentage);
        response.put("nextPhaseUnlocked", nextPhaseUnlocked);
        response.put("courseCompleted", courseCompleted);
        response.put("weakConcepts", weakConcepts);
        response.put("review", review);

        if (passed) {
            response.put("feedback", "Outstanding work! You scored " + score + "/" + total + " (" + percentage + "%). The next phase has been unlocked!");
        } else {
            response.put("feedback", "You scored " + score + "/" + total + " (" + percentage + "%). A score strictly greater than 60% (at least 7/10) is required to unlock the next phase. Please review the revision concepts below and try again.");
        }

        return response;
    }

    private void updateTopicPerformance(User user, String courseTitle, String conceptTag, boolean isCorrect) {
        if (user == null || user.getId() == null || conceptTag == null || conceptTag.trim().isEmpty()) {
            return;
        }
        try {
            TopicPerformance tp = topicPerformanceRepository
                    .findByUserIdAndCourseTitleAndConceptTag(user.getId(), courseTitle, conceptTag)
                    .orElse(null);
            if (tp == null) {
                tp = new TopicPerformance();
                tp.setUser(user);
                tp.setCourseTitle(courseTitle != null ? courseTitle : "General");
                tp.setConceptTag(conceptTag);
                tp.setTimesTested(0);
                tp.setTimesIncorrect(0);
            }
            tp.recordResult(isCorrect);
            topicPerformanceRepository.save(tp);
        } catch (Exception e) {
            System.err.println("Warning: Could not update topic performance: " + e.getMessage());
        }
    }

    // =========================================================================
    // 5. GEMINI API CURRICULUM & QUIZ ENGINE WITH RESILIENT FALLBACKS
    // =========================================================================
    private List<Map<String, Object>> fetchCurriculumFromGemini(String subject, User student) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.contains("YOUR_")) {
            return null;
        }

        String branchInfo = student != null && student.getBranch() != null ? student.getBranch() : "CSE";
        String yearInfo = student != null && student.getAcademicYear() != null ? student.getAcademicYear() : "2nd Year";
        String semInfo = student != null && student.getSemester() != null ? student.getSemester() : "1st Sem";
        String cgpaInfo = student != null && student.getCgpa() != null && student.getCgpa() > 0 ? String.format("%.2f", student.getCgpa()) : "Standard";

        String prompt = "You are an elite university curriculum designer. Create an in-depth 6-phase academic learning path specifically for: '" + subject + "' for a " 
                + branchInfo + " engineering student (" + yearInfo + ", " + semInfo + ", Academic Standing CGPA: " + cgpaInfo + ").\n"
                + "Return ONLY a valid JSON array containing exactly 6 phase objects with this schema:\n"
                + "[\n"
                + "  {\n"
                + "    \"title\": \"Phase 1: Specific Title\",\n"
                + "    \"objectives\": \"Clear learning outcomes for this phase\",\n"
                + "    \"duration\": \"2-3 Hours\",\n"
                + "    \"studyMaterial\": \"Detailed comprehensive explanations, definitions, code snippets or practical examples, real-world applications, and practice questions.\"\n"
                + "  }\n"
                + "]\n"
                + "Do NOT wrap in markdown backticks. Return raw JSON array only.";

        String aiResponse = callGemini(prompt);
        if (aiResponse != null) {
            try {
                int start = aiResponse.indexOf("[");
                int end = aiResponse.lastIndexOf("]");
                if (start != -1 && end > start) {
                    return objectMapper.readValue(aiResponse.substring(start, end + 1), new TypeReference<List<Map<String, Object>>>() {});
                }
            } catch (Exception e) {
                System.err.println("Gemini curriculum JSON parse error: " + e.getMessage());
            }
        }
        return null;
    }

    public List<QuizQuestion> generate10QuestionsForPhase(CoursePhase phase, User student) {
        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty() && !geminiApiKey.contains("YOUR_")) {
            List<QuizQuestion> geminiQuestions = fetchQuizFromGemini(phase, student);
            if (geminiQuestions != null && geminiQuestions.size() == 10 && validateQuizQuestions(geminiQuestions)) {
                return geminiQuestions;
            }
        }

        // Resilient Subject-Specific Question Bank
        return SubjectQuestionBank.getQuestionsForPhase(phase, student);
    }

    public List<QuizQuestion> generate10QuestionsForPhase(CoursePhase phase) {
        return generate10QuestionsForPhase(phase, null);
    }

    private List<QuizQuestion> fetchQuizFromGemini(CoursePhase phase, User student) {
        String courseTitle = (phase.getCourse() != null && phase.getCourse().getTitle() != null)
                ? phase.getCourse().getTitle()
                : "Engineering";
        String branch = (student != null && student.getBranch() != null) ? student.getBranch() : "Engineering";
        String year = (student != null && student.getAcademicYear() != null) ? student.getAcademicYear() : "2nd Year";
        String sem = (student != null && student.getSemester() != null) ? student.getSemester() : "1st Sem";

        String prompt = "You are an elite university examiner in " + branch + " (" + year + ", " + sem + ").\n"
                + "Generate EXACTLY 10 challenging, unique, academically rigorous multiple-choice questions for the course '" 
                + courseTitle + "', Phase " + phase.getPhaseNumber() + ": '" + phase.getTitle() + "'.\n"
                + "Context and study material:\n[" + (phase.getStudyMaterial() != null ? phase.getStudyMaterial() : phase.getTitle()) + "]\n\n"
                + "STRICT REQUIREMENTS:\n"
                + "1. Exactly 10 questions.\n"
                + "2. Exactly 4 distinct options per question (Option A, B, C, D). No duplicate options.\n"
                + "3. The correct answer must be balanced across A, B, C, and D (do NOT make all or most answers A!).\n"
                + "4. Provide an in-depth academic explanation for why the correct option is right.\n"
                + "5. Provide a specific conceptTag for the subtopic tested.\n"
                + "6. Return ONLY a valid raw JSON array of 10 objects with this exact schema:\n"
                + "[\n"
                + "  {\n"
                + "    \"q\": \"Question text?\",\n"
                + "    \"options\": [\"Option A text\", \"Option B text\", \"Option C text\", \"Option D text\"],\n"
                + "    \"correct\": \"B\",\n"
                + "    \"explanation\": \"Detailed rationale...\",\n"
                + "    \"conceptTag\": \"Subtopic name\"\n"
                + "  }\n"
                + "]\n"
                + "Do NOT wrap in markdown or backticks. Raw JSON array only.";

        String aiResponse = callGemini(prompt);
        if (aiResponse == null) return null;

        try {
            int start = aiResponse.indexOf("[");
            int end = aiResponse.lastIndexOf("]");
            if (start != -1 && end > start) {
                List<Map<String, Object>> parsed = objectMapper.readValue(aiResponse.substring(start, end + 1), new TypeReference<List<Map<String, Object>>>() {});
                if (parsed != null && parsed.size() >= 10) {
                    List<QuizQuestion> questions = new ArrayList<>();
                    for (int i = 0; i < 10; i++) {
                        Map<String, Object> item = parsed.get(i);
                        List<String> opts = (List<String>) item.get("options");
                        if (opts != null && opts.size() >= 4) {
                            Set<String> distinct = new HashSet<>(opts.subList(0, 4));
                            if (distinct.size() < 4) return null; // Reject duplicate options

                            String correct = String.valueOf(item.getOrDefault("correct", "A")).trim().toUpperCase();
                            if (!List.of("A", "B", "C", "D").contains(correct)) {
                                correct = "A";
                            }

                            QuizQuestion qq = new QuizQuestion();
                            qq.setPhase(phase);
                            qq.setCourse(phase.getCourse());
                            qq.setQuestionText((String) item.get("q"));
                            qq.setOptionA(opts.get(0));
                            qq.setOptionB(opts.get(1));
                            qq.setOptionC(opts.get(2));
                            qq.setOptionD(opts.get(3));
                            qq.setCorrectOption(correct);
                            qq.setExplanation((String) item.getOrDefault("explanation", "Verified academic concept."));
                            qq.setConceptTag((String) item.getOrDefault("conceptTag", phase.getTitle()));
                            questions.add(qq);
                        }
                    }
                    if (questions.size() == 10) return questions;
                }
            }
        } catch (Exception e) {
            System.err.println("Gemini quiz JSON parse error: " + e.getMessage());
        }
        return null;
    }

    public boolean validateQuizQuestions(List<QuizQuestion> questions) {
        if (questions == null || questions.size() != 10) return false;

        Set<String> questionTexts = new HashSet<>();
        Set<String> correctOptions = new HashSet<>();

        for (QuizQuestion q : questions) {
            if (q.getQuestionText() == null || q.getQuestionText().trim().isEmpty()) return false;
            if (q.getOptionA() == null || q.getOptionB() == null || q.getOptionC() == null || q.getOptionD() == null) return false;

            Set<String> opts = new HashSet<>(Arrays.asList(
                    q.getOptionA().trim(), q.getOptionB().trim(), q.getOptionC().trim(), q.getOptionD().trim()));
            if (opts.size() < 4) return false;

            if (q.getCorrectOption() == null || !List.of("A", "B", "C", "D").contains(q.getCorrectOption().toUpperCase())) return false;
            correctOptions.add(q.getCorrectOption().toUpperCase());

            questionTexts.add(q.getQuestionText().trim().toLowerCase());
        }

        if (questionTexts.size() < 10) return false;
        if (correctOptions.size() < 2) return false;

        return true;
    }

    public boolean isLegacyDummyQuiz(List<QuizQuestion> questions) {
        if (questions == null || questions.isEmpty()) return false;

        long countOptionAOptimized = questions.stream()
                .filter(q -> q.getOptionA() != null && q.getOptionA().contains("It provides an optimized, mathematically sound architectural foundation"))
                .count();
        if (countOptionAOptimized >= 5) return true;

        long countGenericPrompt = questions.stream()
                .filter(q -> q.getQuestionText() != null && q.getQuestionText().contains("Which statement correctly describes the primary principle of"))
                .count();
        if (countGenericPrompt >= 5) return true;

        long countAllA = questions.stream()
                .filter(q -> "A".equalsIgnoreCase(q.getCorrectOption()))
                .count();
        if (questions.size() >= 10 && countAllA == questions.size()) {
            String firstOptA = questions.get(0).getOptionA();
            long identicalA = questions.stream()
                    .filter(q -> Objects.equals(q.getOptionA(), firstOptA))
                    .count();
            if (identicalA >= 5) return true;
        }

        return false;
    }

    private String callGemini(String prompt) {
        String[] models = {"gemini-2.5-flash", "gemini-2.0-flash"};
        for (String model : models) {
            try {
                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + geminiApiKey.trim();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> payload = Map.of("contents", Collections.singletonList(
                    Map.of("parts", Collections.singletonList(Map.of("text", prompt)))
                ));

                HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
                Map response = restTemplate.postForObject(apiUrl, request, Map.class);
                if (response != null && response.containsKey("candidates")) {
                    List candidates = (List) response.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map first = (Map) candidates.get(0);
                        Map content = (Map) first.get("content");
                        if (content != null && content.containsKey("parts")) {
                            List parts = (List) content.get("parts");
                            if (parts != null && !parts.isEmpty()) {
                                return ((String) ((Map) parts.get(0)).get("text")).trim();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Gemini API error with " + model + ": " + e.getMessage());
            }
        }
        return null;
    }

    public List<Map<String, Object>> buildFallbackCurriculum(String subject) {
        return SubjectCurriculumRegistry.getCurriculumForSubject(subject, null);
    }

    public List<QuizQuestion> buildDomainAware10Questions(CoursePhase phase) {
        return SubjectQuestionBank.getQuestionsForPhase(phase, null);
    }
}
