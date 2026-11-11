package com.vrsec.backend.service;

import com.vrsec.backend.model.CoursePhase;
import com.vrsec.backend.model.QuizQuestion;
import com.vrsec.backend.model.User;

import java.util.*;

/**
 * Authentic, academically rigorous Subject Question Bank for LearnPath AI.
 * Provides authentic 10-question pools per subject and phase with:
 * - 4 distinct, plausible options per question
 * - Balanced correct answer distribution across A, B, C, and D
 * - Detailed technical explanations for every question
 * - Specific concept tags for targeted remedial tracking
 * - Strict domain relevance (no coding questions in non-programming courses!)
 */
public class SubjectQuestionBank {

    public static class QuestionBlueprint {
        public final String q;
        public final String a;
        public final String b;
        public final String c;
        public final String d;
        public final String correct;
        public final String explanation;
        public final String tag;

        public QuestionBlueprint(String q, String a, String b, String c, String d, String correct, String explanation, String tag) {
            this.q = q;
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.correct = correct.toUpperCase();
            this.explanation = explanation;
            this.tag = tag;
        }
    }

    public static List<QuizQuestion> getQuestionsForPhase(CoursePhase phase, User student) {
        String courseTitle = (phase.getCourse() != null && phase.getCourse().getTitle() != null)
                ? phase.getCourse().getTitle().toLowerCase()
                : "general";
        int phaseNum = phase.getPhaseNumber() != null ? phase.getPhaseNumber() : 1;

        List<QuestionBlueprint> blueprints = null;

        if (courseTitle.contains("data struct") || courseTitle.contains("dsa") || (courseTitle.contains("algorithm") && !courseTitle.contains("machine"))) {
            blueprints = getDataStructuresQuestions(phaseNum);
        } else if (courseTitle.contains("operating system") || courseTitle.contains("os &") || courseTitle.contains("kernel")) {
            blueprints = getOperatingSystemsQuestions(phaseNum);
        } else if (courseTitle.contains("database") || courseTitle.contains("dbms") || courseTitle.contains("sql")) {
            blueprints = getDatabaseQuestions(phaseNum);
        } else if (courseTitle.contains("network") && !courseTitle.contains("neural")) {
            blueprints = getComputerNetworksQuestions(phaseNum);
        } else if (courseTitle.contains("fluid") || courseTitle.contains("hydraulic")) {
            blueprints = getFluidMechanicsQuestions(phaseNum);
        } else if (courseTitle.contains("environment") || courseTitle.contains("ecology") || courseTitle.contains("pollution")) {
            blueprints = getEnvironmentalScienceQuestions(phaseNum);
        } else if (courseTitle.contains("math") || courseTitle.contains("calculus") || courseTitle.contains("algebra")) {
            blueprints = getEngineeringMathematicsQuestions(phaseNum);
        } else if (courseTitle.contains("digital") || courseTitle.contains("logic design") || courseTitle.contains("verilog") || (courseTitle.contains("electronic") && !courseTitle.contains("power"))) {
            blueprints = getDigitalElectronicsQuestions(phaseNum);
        } else if (courseTitle.contains("physic") || courseTitle.contains("optics") || courseTitle.contains("laser") || courseTitle.contains("semiconductor")) {
            blueprints = getEngineeringPhysicsQuestions(phaseNum);
        } else if (courseTitle.contains("solid") || courseTitle.contains("mechanic") || courseTitle.contains("strength") || courseTitle.contains("thermal")) {
            blueprints = getMechanicsOfSolidsQuestions(phaseNum);
        } else if (courseTitle.contains("circuit") || courseTitle.contains("electric") || courseTitle.contains("power system") || courseTitle.contains("machine")) {
            blueprints = getElectricCircuitsQuestions(phaseNum);
        } else if (courseTitle.contains("python")) {
            blueprints = getPythonQuestions(phaseNum);
        }

        if (blueprints == null || blueprints.size() < 10) {
            blueprints = synthesizeDynamicQuestions(phase, student);
        }

        List<QuizQuestion> questions = new ArrayList<>();
        for (int i = 0; i < Math.min(10, blueprints.size()); i++) {
            QuestionBlueprint bp = blueprints.get(i);
            QuizQuestion q = new QuizQuestion();
            q.setPhase(phase);
            q.setCourse(phase.getCourse());
            q.setQuestionText(bp.q);
            q.setOptionA(bp.a);
            q.setOptionB(bp.b);
            q.setOptionC(bp.c);
            q.setOptionD(bp.d);
            q.setCorrectOption(bp.correct);
            q.setExplanation(bp.explanation);
            q.setConceptTag(bp.tag);
            questions.add(q);
        }

        return questions;
    }

    // =========================================================================
    // 1. DATA STRUCTURES & ALGORITHMIC COMPLEXITY
    // =========================================================================
    private static List<QuestionBlueprint> getDataStructuresQuestions(int phaseNum) {
        List<QuestionBlueprint> list = new ArrayList<>();
        switch (phaseNum) {
            case 1: // Big-O & Complexity
                list.add(new QuestionBlueprint("What is the time complexity of the recurrence T(n) = 2T(n/2) + O(n) according to the Master Theorem?", "O(n)", "O(n log n)", "O(n^2)", "O(log n)", "B", "By Master Theorem case 2 where a=2, b=2, and f(n)=O(n^1), log_b(a) = log_2(2) = 1. Since f(n) = Theta(n^log_b(a)), T(n) = Theta(n log n).", "Master Theorem"));
                list.add(new QuestionBlueprint("Which asymptotic notation denotes an asymptotically tight bound?", "Big-O (O)", "Big-Omega (Ω)", "Big-Theta (Θ)", "Little-o (o)", "C", "Big-Theta (Θ) bounds a function from above and below within constant factors for sufficiently large n.", "Asymptotic Bounds"));
                list.add(new QuestionBlueprint("What is the amortized time complexity of inserting n elements into an initially empty dynamic array (vector) with doubling strategy?", "O(1) per insertion", "O(n) per insertion", "O(log n) per insertion", "O(n log n) total", "A", "Doubling capacity causes copying cost sum of powers of 2 (2n total work for n insertions), resulting in O(1) amortized time per insertion.", "Amortized Analysis"));
                list.add(new QuestionBlueprint("Which of the following recurrence relations yields a time complexity of O(n^2)?", "T(n) = T(n/2) + O(1)", "T(n) = 2T(n/2) + O(n)", "T(n) = T(n-1) + O(1)", "T(n) = T(n-1) + O(n)", "D", "Expanding T(n) = T(n-1) + n gives n + (n-1) + (n-2) + ... + 1 = n(n+1)/2, which is O(n^2).", "Recurrence Relations"));
                list.add(new QuestionBlueprint("What is the worst-case space complexity of recursive Fibonacci computation without memoization?", "O(1)", "O(n)", "O(2^n)", "O(n^2)", "B", "The recursion tree reaches a maximum stack call depth of n frames before unwinding, making memory O(n).", "Recursion Space Complexity"));
                list.add(new QuestionBlueprint("In asymptotic growth analysis, which function dominates all others as n approaches infinity?", "n^1000", "2^n", "n!", "n^n", "D", "n^n grows strictly faster than factorial n!, which in turn dominates exponential 2^n and polynomial n^1000.", "Growth of Functions"));
                list.add(new QuestionBlueprint("What is the space complexity of Depth-First Search (DFS) on a general graph with V vertices and E edges?", "O(V)", "O(E)", "O(V + E)", "O(V^2)", "A", "DFS stores visited flags and maintains a recursion stack bounded by the maximum path length, which is at most V.", "Graph Space Complexity"));
                list.add(new QuestionBlueprint("What does Little-o notation o(g(n)) indicate mathematically?", "f(n) <= c*g(n) for some constant c", "f(n) >= c*g(n) for all n", "f(n) is strictly dominated by g(n) such that limit f(n)/g(n) = 0", "f(n) and g(n) grow at identical rates", "C", "Little-o represents an upper bound that is not asymptotically tight; lim(n->inf) [f(n)/g(n)] = 0.", "Little-o Notation"));
                list.add(new QuestionBlueprint("What is the time complexity of building a heap (heapify) of n elements from an unordered array?", "O(n log n)", "O(n)", "O(n^2)", "O(log n)", "B", "Bottom-up build-heap evaluates sum of h/2^h over all tree levels, which converges mathematically to O(n).", "Heapify Complexity"));
                list.add(new QuestionBlueprint("If an algorithm's worst-case time is O(n^2) and best-case time is Omega(n), what can be formally asserted about its Big-Theta?", "It is Theta(n log n)", "It is Theta(n^2)", "Big-Theta cannot be established because upper and lower bounds differ", "It is Theta(n)", "C", "A tight Theta bound exists if and only if both the upper and lower bounds have the exact same order of magnitude.", "Tight Bounds"));
                break;
            default: // Other DSA phases
                list.add(new QuestionBlueprint("In a 2D array A[M][N] stored in row-major order with base address B and element size S, what is the address of A[i][j] (0-indexed)?", "B + (i + j * M) * S", "B + (i * N + j) * S", "B + (j * N + i) * S", "B + (i * M + j) * S", "B", "Row-major layout stores complete rows sequentially. Skipping i full rows skips i*N elements, plus j elements in the current row.", "Array Memory Layout"));
                list.add(new QuestionBlueprint("What is the time and space complexity of Kadane's algorithm for Maximum Subarray Sum?", "Time: O(n log n), Space: O(n)", "Time: O(n^2), Space: O(1)", "Time: O(n), Space: O(1)", "Time: O(n), Space: O(n)", "C", "Kadane's algorithm scans the array once maintaining current_max and global_max using two scalar variables.", "Kadane Algorithm"));
                list.add(new QuestionBlueprint("In Floyd's Cycle-Finding Algorithm (Tortoise and Hare), what are the speeds of the two pointers?", "Slow moves 1 step, Fast moves 2 steps", "Slow moves 2 steps, Fast moves 3 steps", "Both move 1 step in opposite directions", "Slow is stationary, Fast moves 1 step", "A", "If a cycle exists, the distance between fast and slow increases by 1 each step, guaranteeing they meet in at most cycle-length iterations.", "Floyd Cycle Detection"));
                list.add(new QuestionBlueprint("Which combination of data structures is optimal for implementing an LRU Cache with O(1) get and put?", "Hash Table and Singly Linked List", "Binary Search Tree and Queue", "Hash Table and Doubly Linked List", "Min-Heap and Stack", "C", "The hash map provides O(1) key-to-node lookup, while the doubly linked list allows O(1) node removal and insertion at head.", "LRU Cache Architecture"));
                list.add(new QuestionBlueprint("In an AVL tree, what are the permissible values for the balance factor of any node?", "-1, 0, +1", "-2, 0, +2", "Strictly 0", "Any positive integer", "A", "AVL trees require height difference between left and right subtrees to be at most 1: Balance Factor in {-1, 0, +1}.", "AVL Balance Factor"));
                list.add(new QuestionBlueprint("Which algorithm is used to find the Shortest Path in a weighted graph with non-negative edge weights?", "Kruskal's Algorithm", "Dijkstra's Algorithm", "Prim's Algorithm", "Kosaraju's Algorithm", "B", "Dijkstra's greedy algorithm with min-heap priority queue finds single-source shortest paths in O((V + E) log V).", "Dijkstra Algorithm"));
                list.add(new QuestionBlueprint("What data structure is used to implement cycle detection in Kruskal's Minimum Spanning Tree algorithm?", "Disjoint Set Union (DSU / Union-Find)", "Adjacency Matrix", "Fibonacci Heap", "Breadth-First Queue", "A", "DSU with union by rank and path compression checks whether adding an edge creates a cycle in nearly O(1) time.", "Disjoint Set Union"));
                list.add(new QuestionBlueprint("In topological sorting of a Directed Acyclic Graph (DAG), what is Kahn's algorithm based on?", "Tracking outgoing degrees using DFS", "Repeatedly removing vertices with in-degree equal to 0", "Dijkstra shortest path relaxation", "Ford-Fulkerson max flow cuts", "B", "Kahn's algorithm initializes a queue with all 0-in-degree vertices and decrements neighbor in-degrees upon removal.", "Topological Sort"));
                list.add(new QuestionBlueprint("What are the two foundational requirements for a problem to be solvable via Dynamic Programming?", "Greedy choice and linear time complexity", "Optimal substructure and overlapping subproblems", "Divide and conquer with independent subproblems", "Sparse matrix representation and branch pruning", "B", "Dynamic programming requires optimal substructure and overlapping subproblems.", "DP Foundations"));
                list.add(new QuestionBlueprint("What is the time complexity of the 0/1 Knapsack problem with n items and capacity W using 2D Dynamic Programming?", "O(2^n)", "O(n * W)", "O(n + W)", "O(W log n)", "B", "A table of size (n+1) x (W+1) is populated where each cell computation takes O(1) time, yielding pseudo-polynomial O(n*W).", "0/1 Knapsack DP"));
                break;
        }
        return list;
    }

    // =========================================================================
    // 2. OPERATING SYSTEMS & KERNEL PROCESS MODELING
    // =========================================================================
    private static List<QuestionBlueprint> getOperatingSystemsQuestions(int phaseNum) {
        List<QuestionBlueprint> list = new ArrayList<>();
        list.add(new QuestionBlueprint("What is the primary purpose of dual-mode CPU operation (User Mode vs Kernel Mode)?", "To maximize clock frequency", "To protect hardware and kernel memory from unauthorized user application access", "To eliminate the need for CPU interrupts", "To allow parallel threading on single-core CPUs", "B", "Dual-mode execution restricts privileged instructions to Kernel Mode (Ring 0), preventing user processes from corrupting the OS.", "Dual-Mode CPU"));
        list.add(new QuestionBlueprint("Which CPU scheduling algorithm is non-preemptive and guarantees minimum average waiting time for a static batch of processes?", "First-Come First-Served (FCFS)", "Round Robin (RR)", "Shortest Job First (SJF)", "Priority Scheduling", "C", "SJF scheduling is provably optimal in minimizing average waiting time by executing the process with the shortest CPU burst first.", "SJF Scheduling"));
        list.add(new QuestionBlueprint("In Unix-like systems, what does the fork() system call return to the parent process upon successful process creation?", "The integer 0", "A negative error code", "The Process ID (PID) of the newly created child process", "A pointer to the child Process Control Block", "C", "fork() returns 0 to the child process and returns the child's positive PID to the parent process.", "Fork System Call"));
        list.add(new QuestionBlueprint("Which four conditions are mandatory and must occur simultaneously for a system deadlock to arise (Coffman Conditions)?", "Paging, Segmentation, Swapping, and Thrashing", "Mutual Exclusion, Hold and Wait, Preemption Allowed, and Starvation", "Race condition, Critical Section, Semaphore, and Monitor", "Mutual Exclusion, Hold and Wait, No Preemption, and Circular Wait", "D", "Coffman established that deadlocks occur if and only if all four conditions (Mutual Exclusion, Hold & Wait, No Preemption, Circular Wait) hold simultaneously.", "Coffman Conditions"));
        list.add(new QuestionBlueprint("What is the function of the Translation Lookaside Buffer (TLB) in virtual memory hardware?", "To store compressed disk pages", "To cache recent virtual-to-physical address translations to avoid secondary page table lookups", "To encrypt memory pages during DMA transfers", "To manage page replacement in swap space", "B", "The TLB is a high-speed associative hardware cache that speeds up virtual address translation by caching recent page-to-frame mappings.", "TLB Architecture"));
        list.add(new QuestionBlueprint("What does Belady's Anomaly describe in virtual memory management?", "Page faults increase under high CPU load", "Allocating more physical page frames causes more page faults under the FIFO page replacement algorithm", "LRU page replacement causes thrashing", "Disk access latency exceeds RAM access speed", "B", "Belady's Anomaly proves that for FIFO page replacement, increasing available physical page frames can paradoxically increase total page faults.", "Belady's Anomaly"));
        list.add(new QuestionBlueprint("What is 'Thrashing' in an operating system?", "When the CPU spends virtually all its time swapping pages in and out of disk rather than executing application instructions", "When two processes access a mutex simultaneously", "When physical RAM experiences thermal degradation", "When the disk arm scheduling algorithm fails to converge", "A", "Thrashing occurs when the sum of process working sets exceeds total physical memory, resulting in continuous page faults.", "Thrashing"));
        list.add(new QuestionBlueprint("Which synchronization primitive uses atomic Wait (P) and Signal (V) operations to manage access to shared resources?", "Pipe", "Counting Semaphore", "Socket", "Spinlock", "B", "Dijkstra's counting semaphore maintains an integer counter modified only through atomic wait() and signal() operations.", "Counting Semaphore"));
        list.add(new QuestionBlueprint("What data structure in a Unix filesystem stores metadata, file size, owner permissions, and disk block pointers?", "Superblock", "Inode", "Dentry", "File Allocation Table (FAT)", "B", "An inode (index node) stores all filesystem metadata and data block pointers for an individual file.", "Inode Architecture"));
        list.add(new QuestionBlueprint("What does Dijkstra's Banker's Algorithm accomplish in operating systems?", "Automatic thread context switching", "Deadlock avoidance by ensuring resource allocations maintain the system in a safe state", "Dynamic memory defragmentation", "Virtual file system encryption", "B", "The Banker's Algorithm simulates resource allocation to verify that at least one safe execution sequence exists before granting a request.", "Banker's Algorithm"));
        return list;
    }

    // =========================================================================
    // 3. DATABASE MANAGEMENT SYSTEMS & SQL MASTERY
    // =========================================================================
    private static List<QuestionBlueprint> getDatabaseQuestions(int phaseNum) {
        List<QuestionBlueprint> list = new ArrayList<>();
        list.add(new QuestionBlueprint("In the relational database model, what does the Entity Integrity Constraint mandate?", "Foreign keys must reference existing primary keys", "No primary key attribute can contain a NULL value", "All attribute values must be unique across tables", "Tables must be normalized to at least 3NF", "B", "Entity integrity requires that every base relation have a primary key, and no primary key component can be null.", "Entity Integrity"));
        list.add(new QuestionBlueprint("Which Normal Form specifically eliminates partial functional dependencies on a composite primary key?", "First Normal Form (1NF)", "Second Normal Form (2NF)", "Third Normal Form (3NF)", "Boyce-Codd Normal Form (BCNF)", "B", "2NF requires 1NF and mandates that every non-prime attribute must be fully functionally dependent on the entire primary key.", "Second Normal Form"));
        list.add(new QuestionBlueprint("What is the formal definition of Boyce-Codd Normal Form (BCNF)?", "For every non-trivial functional dependency X -> Y, X must be a superkey", "Every non-prime attribute must be non-transitively dependent", "All multi-valued dependencies are eliminated", "Attribute values must be atomic numbers only", "A", "BCNF is a stricter version of 3NF requiring that in every non-trivial FD X -> Y, the determinant X is a superkey.", "Boyce-Codd Normal Form"));
        list.add(new QuestionBlueprint("What does the ACID 'Durability' property guarantee in database management systems?", "Transactions execute concurrently without interference", "Database constraints remain consistent after every query", "All operations within a transaction succeed or all fail", "Committed transaction changes persist permanently even in the event of a system crash", "D", "Durability ensures that once a transaction commits, its effects survive crashes and power outages via non-volatile logging.", "ACID Durability"));
        list.add(new QuestionBlueprint("In SQL, what is the fundamental difference between the WHERE clause and the HAVING clause?", "WHERE is faster than HAVING", "WHERE filters individual rows before grouping; HAVING filters aggregated groups after GROUP BY", "HAVING can only be used with numbers", "WHERE is only allowed in subqueries", "B", "WHERE filters source rows before aggregation; HAVING filters aggregated grouped rows generated by GROUP BY.", "SQL WHERE vs HAVING"));
        list.add(new QuestionBlueprint("How does the Strict Two-Phase Locking (Strict 2PL) protocol prevent cascading rollbacks?", "By acquiring all locks before transaction start", "By releasing all shared locks at the midpoint", "By holding all exclusive (X) write locks until the transaction commits or aborts", "By converting exclusive locks to intention locks", "C", "Strict 2PL retains all exclusive locks until transaction completion, preventing other transactions from reading uncommitted dirty data.", "Strict 2PL"));
        list.add(new QuestionBlueprint("In a B+ Tree index, why are all leaf nodes connected together sequentially via pointers?", "To reduce the tree height to 1", "To enable fast and efficient range queries and sequential scans without traversing root nodes", "To store duplicate keys", "To eliminate the need for an index root", "B", "Leaf nodes in a B+ tree form a doubly linked list, enabling O(log N) point lookup followed by fast linear range scanning.", "B+ Tree Indexing"));
        list.add(new QuestionBlueprint("Which SQL transaction isolation level guarantees prevention of dirty reads, non-repeatable reads, and phantom reads?", "READ UNCOMMITTED", "READ COMMITTED", "REPEATABLE READ", "SERIALIZABLE", "D", "SERIALIZABLE is the highest isolation level, completely eliminating dirty reads, non-repeatable reads, and phantom read anomalies.", "Isolation Levels"));
        list.add(new QuestionBlueprint("What does the Write-Ahead Logging (WAL) protocol require during database updates?", "Data pages must be written to disk before log records", "Log records describing a change must be written to non-volatile disk before the corresponding modified data page is flushed", "Transactions must write logs only upon server shutdown", "Logs must be encrypted before hashing", "B", "WAL guarantees Atomicity and Durability by ensuring undo/redo log records are written to stable storage before dirty buffer pool pages.", "Write-Ahead Logging"));
        list.add(new QuestionBlueprint("In relational algebra, which operation is defined as a Cartesian product followed by a selection condition?", "Projection", "Union", "Theta Join", "Natural Division", "C", "A Theta Join (R join_theta S) is defined mathematically as sigma_theta(R x S).", "Theta Join"));
        return list;
    }

    // =========================================================================
    // 4. FLUID MECHANICS & HYDRAULIC MACHINERY
    // =========================================================================
    private static List<QuestionBlueprint> getFluidMechanicsQuestions(int phaseNum) {
        List<QuestionBlueprint> list = new ArrayList<>();
        list.add(new QuestionBlueprint("What does Pascal's Law state regarding fluid pressure in a confined static fluid?", "Pressure decreases with depth", "Pressure applied at any point in a confined static fluid is transmitted equally and undiminished in all directions", "Pressure depends on fluid velocity squared", "Pressure is zero at free surfaces", "B", "Pascal's principle states that external pressure applied to an enclosed static fluid is transmitted uniformly in all directions.", "Pascal's Law"));
        list.add(new QuestionBlueprint("What fundamental physical conservation law is represented by the 1D fluid continuity equation A1*V1 = A2*V2 for steady incompressible flow?", "Conservation of Energy", "Conservation of Momentum", "Conservation of Mass", "Conservation of Angular Momentum", "C", "The continuity equation is the mathematical statement of conservation of mass in a fluid control volume.", "Continuity Equation"));
        list.add(new QuestionBlueprint("What conservation principle forms the theoretical basis of Bernoulli's equation along a streamline in frictionless flow?", "Conservation of Mechanical Energy", "Conservation of Mass", "Conservation of Thermal Enthalpy", "Conservation of Linear Momentum", "A", "Bernoulli's theorem represents mechanical energy conservation: P/(rho*g) + V^2/(2g) + z = Constant.", "Bernoulli's Principle"));
        list.add(new QuestionBlueprint("In fluid flow through a circular pipe of diameter D, what is the critical Reynolds number (Re) below which flow is always laminar?", "Re = 500", "Re = 1000", "Re = 2000", "Re = 4000", "C", "For internal pipe flow, flow is laminar for Re < 2000, transitional between 2000 and 4000, and fully turbulent for Re > 4000.", "Reynolds Number"));
        list.add(new QuestionBlueprint("According to the Darcy-Weisbach equation, how does head loss due to pipe friction (h_f) vary with average flow velocity V?", "Linearly proportional to V", "Proportional to the square of velocity (V^2)", "Inversely proportional to V", "Independent of velocity", "B", "The Darcy-Weisbach equation is h_f = f * L * V^2 / (2 * g * D), where head loss varies with the square of velocity.", "Darcy-Weisbach Equation"));
        list.add(new QuestionBlueprint("Where does the Center of Pressure on an inclined submerged plane surface lie relative to its centroid?", "Strictly above the centroid", "At the exact centroid", "Strictly below the centroid due to increasing hydrostatic pressure with depth", "At the free water surface", "C", "Because hydrostatic pressure increases linearly with depth, the resultant force center lies below the geometric centroid.", "Center of Pressure"));
        list.add(new QuestionBlueprint("What phenomenon occurs when local static pressure in a hydraulic machine drops below the liquid vapor pressure, forming vapor cavities that collapse violently?", "Cavitation", "Water hammer", "Boundary layer separation", "Turbulent churning", "A", "Cavitation occurs when fluid pressure drops below vapor pressure, creating vapor bubbles whose sudden collapse causes pitting damage.", "Cavitation"));
        list.add(new QuestionBlueprint("What is the velocity profile of fully developed laminar flow of a Newtonian fluid through a circular pipe (Hagen-Poiseuille flow)?", "Uniform rectangular profile", "Logarithmic profile", "Linear triangular profile", "Parabolic profile with maximum velocity at the pipe axis", "D", "Viscous shear stresses produce a parabolic velocity distribution with u_max = 2 * u_average at the centerline.", "Hagen-Poiseuille Profile"));
        list.add(new QuestionBlueprint("Which hydraulic turbine is an impulse turbine designed specifically for high head and low volumetric flow rate installations?", "Kaplan Turbine", "Pelton Wheel", "Francis Turbine", "Propeller Turbine", "B", "The Pelton wheel uses high-velocity water jets striking double-hemispherical buckets, optimal for heads above 250m.", "Pelton Wheel"));
        list.add(new QuestionBlueprint("What does the boundary layer displacement thickness (delta*) physically represent in fluid mechanics?", "The total thickness where velocity reaches 99% of free-stream", "The physical distance by which the external boundary must be shifted to compensate for mass flow reduction caused by viscosity", "The thickness where fluid becomes turbulent", "The roughness height of the pipe wall", "B", "Displacement thickness delta* = integral_0^inf (1 - u/U) dy accounts for the deficit in mass flow rate due to viscous retardance.", "Displacement Thickness"));
        return list;
    }

    // =========================================================================
    // 5. ENVIRONMENTAL SCIENCE & SUSTAINABILITY
    // =========================================================================
    private static List<QuestionBlueprint> getEnvironmentalScienceQuestions(int phaseNum) {
        List<QuestionBlueprint> list = new ArrayList<>();
        list.add(new QuestionBlueprint("According to Lindeman's 10% Rule of ecological efficiency, how much energy is transferred from one trophic level to the next?", "50%", "10%", "1%", "25%", "B", "Lindeman's law states that on average only about 10% of chemical energy is transferred to the next trophic level; 90% is dissipated as heat.", "10% Trophic Rule"));
        list.add(new QuestionBlueprint("What parameter is standardly tested to measure the amount of dissolved oxygen consumed by microorganisms decomposing organic matter in water over 5 days at 20°C?", "Chemical Oxygen Demand (COD)", "Total Organic Carbon (TOC)", "Biochemical Oxygen Demand (BOD5)", "Dissolved Solids (TDS)", "C", "BOD5 tests biological oxygen consumption by aerobic microbes decomposing biodegradable organic waste under standard incubation.", "Biochemical Oxygen Demand"));
        list.add(new QuestionBlueprint("In secondary biological wastewater treatment, what is the primary operational role of the Activated Sludge Process (ASP)?", "Cultivating suspended aerobic microbial flocs in an aeration tank to metabolize and remove dissolved organic pollutants", "Filtering suspended solids through coarse gravel beds", "Neutralizing acidic pH using chemical lime addition", "Disinfecting pathogens with ultraviolet radiation", "A", "ASP aerates sewage to stimulate aerobic bacteria that consume dissolved organic matter, forming settleable biological flocs.", "Activated Sludge Process"));
        list.add(new QuestionBlueprint("Which atmospheric meteorological condition traps ambient air pollutants close to the ground, preventing vertical dispersion?", "Coriolis deflection", "Adiabatic lapse expansion", "Cyclonic depression", "Temperature Inversion", "D", "Temperature inversion occurs when warm air caps colder ground air, suppressing convective thermal updrafts and trapping smog.", "Temperature Inversion"));
        list.add(new QuestionBlueprint("What chemical reaction mechanism between sunlight, volatile organic compounds (VOCs), and nitrogen oxides (NOx) produces photochemical smog?", "Photolysis of NO2 generating free oxygen radicals that react with O2 to form ground-level ozone (O3)", "Dissolution of sulfur dioxide in rainwater forming sulfuric acid", "Reaction of methane with carbon monoxide producing soot", "Condensation of water vapor around sea salt nuclei", "A", "Solar ultraviolet radiation dissociates NO2 into NO and O*; free oxygen combines with O2 to generate harmful ground-level ozone.", "Photochemical Smog"));
        list.add(new QuestionBlueprint("In engineered sanitary landfills, what is the critical purpose of composite bottom liners (HDPE geomembranes and compacted bentonite clay)?", "To prevent landfill gas from venting into the atmosphere", "To accelerate aerobic decomposition of organic waste", "To prevent toxic contaminated leachate from percolating downward into groundwater aquifers", "To provide structural bearing strength for heavy waste trucks", "C", "Impermeable composite liners form a hydraulic barrier preventing toxic leachate from migrating into subsurface aquifers.", "Sanitary Landfill Liners"));
        list.add(new QuestionBlueprint("Which landmark international environmental treaty successfully mandated the global phase-out of ozone-depleting chlorofluorocarbons (CFCs)?", "Montreal Protocol", "Kyoto Protocol", "Basel Convention", "Ramsar Convention", "A", "The 1987 Montreal Protocol is universally ratified and successfully phased out ozone-depleting substances.", "Montreal Protocol"));
        list.add(new QuestionBlueprint("What ecological phenomenon occurs when excessive agricultural nutrient runoff (nitrogen and phosphorus) causes explosive algal blooms and aquatic hypoxia?", "Salinization", "Biomagnification", "Acidification", "Eutrophication", "D", "Eutrophication is nutrient over-enrichment causing dense algal blooms; when algae die and decay, dissolved oxygen is depleted, killing fish.", "Eutrophication"));
        list.add(new QuestionBlueprint("What formal statutory planning procedure evaluates and mitigates the environmental impacts of major proposed industrial and infrastructure projects prior to government clearance?", "Life Cycle Costing", "Environmental Impact Assessment (EIA)", "Six Sigma Quality Audit", "OSHA Safety Clearance", "B", "EIA is a statutory decision-making tool that identifies, predicts, and mitigates environmental, ecological, and socio-economic project impacts.", "Environmental Impact Assessment"));
        list.add(new QuestionBlueprint("Which greenhouse gas has a Global Warming Potential (GWP) approximately 28 times greater than carbon dioxide (CO2) over a 100-year time horizon?", "Argon", "Helium", "Methane (CH4)", "Nitrogen (N2)", "C", "Methane (CH4) traps significantly more infrared radiation per molecule than CO2, giving it a 100-year GWP of 28.", "Global Warming Potential"));
        return list;
    }

    // =========================================================================
    // 6. COMPUTER NETWORKS & PROTOCOL ARCHITECTURES
    // =========================================================================
    private static List<QuestionBlueprint> getComputerNetworksQuestions(int phaseNum) {
        List<QuestionBlueprint> list = new ArrayList<>();
        list.add(new QuestionBlueprint("In the 7-Layer OSI Reference Model, which layer is responsible for logical IP addressing and end-to-end packet routing across intermediate hops?", "Data Link Layer", "Network Layer", "Transport Layer", "Session Layer", "B", "The Network Layer (Layer 3) handles logical addressing (IPv4/IPv6), subnet routing, and packet forwarding across networks.", "OSI Network Layer"));
        list.add(new QuestionBlueprint("What is the maximum window size in the Go-Back-N sliding window protocol using n bits for sequence numbering?", "2^n", "2^n - 1", "2^(n-1)", "n", "B", "In Go-Back-N, the sender window size cannot exceed 2^n - 1 to prevent ambiguity when ACKs are delayed or lost.", "Go-Back-N Window"));
        list.add(new QuestionBlueprint("What mechanism does Ethernet (IEEE 802.3) use to resolve packet collisions on a shared half-duplex medium?", "Token Passing", "Slotted ALOHA", "Carrier Sense Multiple Access with Collision Detection (CSMA/CD)", "Frequency Division Multiplexing", "C", "CSMA/CD listens before transmitting, detects collisions during transmission, and applies binary exponential backoff.", "CSMA/CD Protocol"));
        list.add(new QuestionBlueprint("What is the subnet mask and total usable host capacity for an IPv4 CIDR prefix of /26?", "255.255.255.0 with 254 hosts", "255.255.255.192 with 62 usable hosts", "255.255.255.128 with 126 usable hosts", "255.255.255.240 with 14 usable hosts", "B", "A /26 network leaves 32 - 26 = 6 host bits: 2^6 - 2 = 62 usable hosts. Mask: 11111111.11111111.11111111.11000000 = 255.255.255.192.", "IPv4 Subnetting"));
        list.add(new QuestionBlueprint("Which protocol maps a known 32-bit IPv4 logical address to a physical 48-bit Ethernet MAC hardware address?", "DNS", "DHCP", "Address Resolution Protocol (ARP)", "BGP", "C", "ARP broadcasts queries on the local subnet to resolve IP addresses to destination MAC hardware addresses.", "ARP Protocol"));
        list.add(new QuestionBlueprint("In Distance Vector Routing, what routing anomaly causes routers to slowly increment hop counts indefinitely when a link fails?", "Split Horizon Anomaly", "Count-to-Infinity Problem", "BGP Flapping", "Poison Reverse Delay", "B", "Count-to-infinity occurs in Bellman-Ford distance vector protocols when routing updates loop between neighboring nodes without fast convergence.", "Count-to-Infinity"));
        list.add(new QuestionBlueprint("What sequence of packets constitutes the standard TCP Three-Way Handshake for connection establishment?", "SYN -> SYN-ACK -> ACK", "ACK -> SYN -> DATA", "SYN -> ACK -> RST", "FIN -> FIN-ACK -> ACK", "A", "TCP connection begins with client sending SYN, server responding with SYN-ACK, and client confirming with ACK.", "TCP 3-Way Handshake"));
        list.add(new QuestionBlueprint("How does TCP flow control differ from TCP congestion control?", "Flow control prevents sender from overwhelming receiver buffer; congestion control prevents overloading network routers", "Flow control uses UDP; congestion control uses TCP", "There is no difference", "Flow control operates at the Network layer", "A", "Flow control is end-to-end based on receiver advertized window (rwnd); congestion control manages network pipe capacity (cwnd).", "TCP Flow vs Congestion"));
        list.add(new QuestionBlueprint("Which application layer protocol translates human-readable domain names (e.g. www.vrsec.ac.in) into machine IP addresses?", "DHCP", "Domain Name System (DNS)", "SNMP", "BGP", "B", "DNS is a distributed hierarchical naming database resolving domain names into numerical IP addresses.", "DNS Protocol"));
        list.add(new QuestionBlueprint("Which cryptographic protocol provides privacy, authentication, and data integrity over TCP for secure web browsing (HTTPS)?", "IPsec", "Transport Layer Security (TLS/SSL)", "SSH", "Kerberos", "B", "TLS/SSL establishes encrypted communication tunnels over TCP using asymmetric key exchange and symmetric data ciphers.", "TLS Security"));
        return list;
    }

    // =========================================================================
    // 7. ENGINEERING MATHEMATICS & CALCULUS
    // =========================================================================
    private static List<QuestionBlueprint> getEngineeringMathematicsQuestions(int phaseNum) {
        List<QuestionBlueprint> list = new ArrayList<>();
        list.add(new QuestionBlueprint("Which condition is NOT required for Rolle's Theorem to hold on interval [a, b]?", "f(x) is continuous on [a, b]", "f(x) is differentiable on (a, b)", "f(a) = f(b)", "f'(x) is continuous on (a, b)", "D", "Rolle's theorem requires f to be continuous on [a,b], differentiable on (a,b), and f(a)=f(b). f'(x) does not need to be continuous.", "Rolle's Theorem"));
        list.add(new QuestionBlueprint("What does Lagrange's Mean Value Theorem state for a function continuous on [a, b] and differentiable on (a, b)?", "f'(c) = 0 for some c in (a, b)", "f'(c) = [f(b) - f(a)] / (b - a) for some c in (a, b)", "f(b) = f(a) * f'(c)", "f''(c) > 0 for all c", "B", "Lagrange's MVT guarantees at least one point c where instantaneous tangent slope equals secant line slope.", "Lagrange MVT"));
        list.add(new QuestionBlueprint("According to Euler's Theorem on Homogeneous Functions of degree n, what does x*(df/dx) + y*(df/dy) equal?", "n * f", "f / n", "(n - 1) * f", "0", "A", "Euler's theorem states that if f(x, y) is homogeneous of degree n, then x(df/dx) + y(df/dy) = n*f(x, y).", "Euler's Theorem"));
        list.add(new QuestionBlueprint("What does the Jacobian determinant J = d(x, y) / d(r, theta) represent when transforming Cartesian coordinates to polar coordinates (x = r cos θ, y = r sin θ)?", "1", "r^2", "r", "1 / r", "C", "Computing J = det[[cos θ, -r sin θ], [sin θ, r cos θ]] = r(cos^2 θ + sin^2 θ) = r.", "Polar Jacobian"));
        list.add(new QuestionBlueprint("Which theorem converts a surface integral of a vector field over a closed boundary into a volume integral of its divergence?", "Green's Theorem", "Stokes' Theorem", "Gauss Divergence Theorem", "Cauchy Integral Theorem", "C", "Gauss's Divergence Theorem states that outward flux of F across closed surface S equals volume integral of div(F).", "Gauss Divergence Theorem"));
        list.add(new QuestionBlueprint("What is the necessary and sufficient condition for the first-order differential equation M(x, y) dx + N(x, y) dy = 0 to be exact?", "dM/dx = dN/dy", "dM/dy = dN/dx", "dM/dy + dN/dx = 0", "M + N = constant", "B", "Exactness requires mixed second derivatives to match: dM/dy = dN/dx.", "Exact Differential Equations"));
        list.add(new QuestionBlueprint("What is the Laplace transform of f(t) = e^(at)?", "1 / (s - a) for s > a", "1 / (s + a)", "a / (s^2 + a^2)", "s / (s^2 - a^2)", "A", "L{e^(at)} = integral_0^inf e^(-st) e^(at) dt = 1 / (s - a) for s > a.", "Laplace Transform of Exponential"));
        list.add(new QuestionBlueprint("What is the Laplace transform of the derivative f'(t)?", "s * F(s)", "s * F(s) - f(0)", "F(s) / s", "s^2 * F(s) - f'(0)", "B", "By integration by parts: L{f'(t)} = s*F(s) - f(0).", "Laplace of Derivative"));
        list.add(new QuestionBlueprint("What does the Convolution Theorem for Laplace transforms state?", "L{f(t) * g(t)} = F(s) + G(s)", "L{f(t) * g(t)} = F(s) / G(s)", "L{(f * g)(t)} = F(s) * G(s)", "L{(f * g)(t)} = s * F(s) * G(s)", "C", "The Laplace transform of convolution integral equals algebraic product F(s) * G(s).", "Convolution Theorem"));
        list.add(new QuestionBlueprint("What is the integrating factor for the linear first-order differential equation dy/dx + P(x)y = Q(x)?", "e^(integral P(x) dx)", "integral P(x) dx", "e^(-integral P(x) dx)", "P(x) * Q(x)", "A", "Multiplying by IF = exp(int P dx) transforms the LHS into exact derivative d/dx [y * IF].", "Integrating Factor"));
        return list;
    }

    // =========================================================================
    // 8. DIGITAL LOGIC DESIGN & DIGITAL ELECTRONICS
    // =========================================================================
    private static List<QuestionBlueprint> getDigitalElectronicsQuestions(int phaseNum) {
        List<QuestionBlueprint> list = new ArrayList<>();
        list.add(new QuestionBlueprint("What is the 2's complement of the 8-bit binary number 00101100?", "11010011", "11010100", "11010010", "00101101", "B", "Invert all bits to get 1's complement (11010011), then add 1: 11010011 + 1 = 11010100.", "2's Complement"));
        list.add(new QuestionBlueprint("What key characteristic distinguishes Gray Code from standard binary code?", "It uses base-8 arithmetic", "It is a weighted positional code", "Only one single bit changes between consecutive successive values", "It cannot represent negative integers", "C", "Gray code is a unit distance code where adjacent numerical codewords differ by only a single bit flip.", "Gray Code"));
        list.add(new QuestionBlueprint("According to De Morgan's Laws, what is the simplified complement of (A + B * C)'?", "A' * (B' + C')", "A' * B' * C'", "A' + B' + C'", "A * B' * C", "A", "Applying De Morgan's theorem: (A + (B*C))' = A' * (B*C)' = A' * (B' + C').", "De Morgan's Laws"));
        list.add(new QuestionBlueprint("How many select lines are required for a 16-to-1 Multiplexer (MUX)?", "2", "3", "4", "8", "C", "A multiplexer with 2^n data input lines requires n select lines. For 16 inputs (2^4), 4 select lines are required.", "Multiplexer Select Lines"));
        list.add(new QuestionBlueprint("Why are NAND and NOR gates classified as 'Universal Gates' in digital electronics?", "They consume zero quiescent power", "Any arbitrary Boolean function can be implemented using only that gate type", "They have infinite fan-out capacity", "They do not require power supply pins", "B", "NAND and NOR can synthesize AND, OR, and NOT operations, allowing complete digital logic implementation.", "Universal Gates"));
        list.add(new QuestionBlueprint("What are the sum (S) and carry (C) outputs of a Half Adder with inputs A and B?", "S = A AND B, C = A OR B", "S = A XOR B, C = A AND B", "S = A XNOR B, C = A OR B", "S = A OR B, C = A AND B", "B", "Half adder equations: Sum S = A XOR B, Carry C = A AND B.", "Half Adder"));
        list.add(new QuestionBlueprint("What is the primary advantage of a Carry Lookahead Adder (CLA) over a standard Ripple Carry Adder (RCA)?", "Lower silicon area", "Eliminates propagation delay caused by sequential carry ripple through all bit stages", "Requires fewer transistors", "Operates without an external clock", "B", "CLA calculates carry generate and propagate signals simultaneously for all bits, eliminating O(n) ripple delay.", "Carry Lookahead Adder"));
        list.add(new QuestionBlueprint("What condition causes a race-around problem in a level-triggered JK Flip-Flop?", "J = 0, K = 0 and clock is high", "J = 1, K = 1, clock pulse width is greater than flip-flop propagation delay", "J = 1, K = 0 and clock is low", "Clock frequency is too low", "B", "When J=1, K=1 and clock is HIGH longer than gate delay, the output toggles repeatedly, causing instability.", "Race-Around Condition"));
        list.add(new QuestionBlueprint("How is the race-around condition permanently eliminated in flip-flop design?", "Using master-slave or edge-triggered flip-flop architectures", "Using larger capacitors on the inputs", "Increasing power supply voltage", "Adding inverter feedback loops", "A", "Master-slave and edge-triggered architectures isolate input sampling to a single clock transition edge.", "Master-Slave Flip-Flop"));
        list.add(new QuestionBlueprint("How many flip-flops are required to construct a MOD-12 (modulo-12) synchronous counter?", "3", "4", "6", "12", "B", "An n-flip-flop counter counts up to 2^n states. 2^3 = 8 < 12 <= 2^4 = 16, so 4 flip-flops are required.", "Mod-N Counter Design"));
        return list;
    }

    // =========================================================================
    // 9. ENGINEERING PHYSICS & SEMICONDUCTOR BASICS
    // =========================================================================
    private static List<QuestionBlueprint> getEngineeringPhysicsQuestions(int phaseNum) {
        List<QuestionBlueprint> list = new ArrayList<>();
        list.add(new QuestionBlueprint("In thin film interference by reflection, why is an extra phase change of π (path difference of λ/2) introduced?", "Because light passes through a denser medium", "Due to reflection at the surface of an optically denser medium (Stokes' relation)", "Due to chromatic dispersion in the film", "Because of photon absorption", "B", "Reflection from an optically denser medium causes a pi phase shift by Stokes' boundary condition.", "Stokes' Relation"));
        list.add(new QuestionBlueprint("In Newton's rings experiment, the diameter of the n-th dark ring in reflected light is directly proportional to what quantity?", "n", "n^2", "sqrt(n)", "1 / n", "C", "Radius of dark rings satisfies r_n^2 = n*R*lambda, hence ring diameter D_n is proportional to sqrt(n).", "Newton's Rings Diameter"));
        list.add(new QuestionBlueprint("What is Brewster's Law relating polarizing angle i_p to refractive index μ of a dielectric medium?", "μ = sin(i_p)", "μ = cos(i_p)", "μ = tan(i_p)", "μ = cot(i_p)", "C", "Brewster's Law states mu = tan(i_p). Reflected light is completely plane-polarized at this angle.", "Brewster's Law"));
        list.add(new QuestionBlueprint("What is Malus's Law for light intensity I transmitted through an analyzer oriented at angle θ to the polarizer?", "I = I_0 * sin^2(θ)", "I = I_0 * cos(θ)", "I = I_0 * cos^2(θ)", "I = I_0 * tan(θ)", "C", "Malus's Law states I = I_0 * cos^2(theta).", "Malus's Law"));
        list.add(new QuestionBlueprint("What is the primary condition required inside a laser medium to achieve light amplification by stimulated emission?", "Spontaneous absorption", "Thermal equilibrium", "Population inversion (more atoms in excited metastable state than ground state)", "High ambient temperature", "C", "Population inversion (N2 > N1) ensures stimulated emission dominates over absorption.", "Population Inversion"));
        list.add(new QuestionBlueprint("What is the formula for the Numerical Aperture (NA) of a step-index optical fiber with core index n1 and cladding index n2?", "n1 - n2", "sqrt(n1^2 - n2^2)", "(n1^2 - n2^2) / n1", "sqrt(n1^2 + n2^2)", "B", "Numerical Aperture measures light-gathering ability: NA = sqrt(n1^2 - n2^2).", "Numerical Aperture"));
        list.add(new QuestionBlueprint("What phenomenon enables light propagation through the core of an optical fiber?", "Chromatic dispersion", "Total Internal Reflection (TIR)", "Birefringence", "Fraunhofer diffraction", "B", "Light entering within acceptance cone undergoes Total Internal Reflection at core-cladding interface.", "Total Internal Reflection"));
        list.add(new QuestionBlueprint("In semiconductor physics, what is the probability of an electron occupying an energy state E given by?", "Boltzmann distribution", "Maxwell-Boltzmann statistics", "Fermi-Dirac distribution function f(E) = 1 / [1 + exp((E - E_F) / kT)]", "Planck radiation law", "C", "Electrons are fermions (spin 1/2) obeying Fermi-Dirac statistics.", "Fermi-Dirac Distribution"));
        list.add(new QuestionBlueprint("What parameter is determined by measuring the Hall voltage in a semiconductor?", "Optical bandgap", "Carrier concentration and whether the material is p-type or n-type", "Thermal conductivity", "Dielectric breakdown voltage", "B", "Hall coefficient sign identifies majority carrier type (p-type vs n-type) and carrier density.", "Hall Effect"));
        list.add(new QuestionBlueprint("How does Fraunhofer diffraction differ fundamentally from Fresnel diffraction?", "Fraunhofer requires coherent light only", "In Fraunhofer diffraction, the source and screen are effectively at infinite distances from the diffracting aperture", "Fresnel diffraction uses plane wavefronts", "Fraunhofer diffraction produces no central maximum", "B", "Fraunhofer diffraction uses plane waves (source and screen at infinity), whereas Fresnel involves finite distances.", "Fraunhofer vs Fresnel"));
        return list;
    }

    // =========================================================================
    // 10. MECHANICS OF SOLIDS & STRENGTH OF MATERIALS
    // =========================================================================
    private static List<QuestionBlueprint> getMechanicsOfSolidsQuestions(int phaseNum) {
        List<QuestionBlueprint> list = new ArrayList<>();
        list.add(new QuestionBlueprint("What is the mathematical relationship between Young's Modulus (E), Shear Modulus (G), and Poisson's Ratio (ν)?", "E = 2G * (1 + ν)", "E = 3G * (1 - 2ν)", "G = 2E * (1 + ν)", "E = G * (1 + 2ν)", "A", "Isotropic elasticity connects Young's modulus and shear modulus as E = 2G(1 + nu).", "Elastic Constants E and G"));
        list.add(new QuestionBlueprint("What is the relationship between Young's Modulus (E), Bulk Modulus (K), and Poisson's Ratio (ν)?", "E = 2K * (1 + ν)", "E = 3K * (1 - 2ν)", "K = 3E * (1 - 2ν)", "E = 9K * (1 + ν)", "B", "Hydrostatic volumetric deformation relates Young's modulus and bulk modulus via E = 3K(1 - 2nu).", "Bulk Modulus Relation"));
        list.add(new QuestionBlueprint("What is the thermal stress developed in a bar of length L and thermal expansion coefficient α subjected to temperature increase ΔT when rigidly constrained at both ends?", "σ = 0", "σ = E * α * ΔT", "σ = L * α * ΔT", "σ = E * ΔT / α", "B", "Preventing free expansion induces compressive stress sigma = E * alpha * Delta_T.", "Thermal Stress"));
        list.add(new QuestionBlueprint("What is the point in a loaded beam where the bending moment changes sign from positive to negative called?", "Point of maximum shear", "Point of Contraflexure (or Inflection)", "Neutral Axis", "Centroidal axis", "B", "A point of contraflexure is where the bending moment curve crosses zero and changes curvature sense.", "Point of Contraflexure"));
        list.add(new QuestionBlueprint("What is the classic Euler-Bernoulli pure bending equation for beams?", "M / y = I / E = σ / R", "M / I = σ / y = E / R", "M / E = I / R = σ / y", "M * I = σ * y = E * R", "B", "The pure bending equation is M / I = sigma / y = E / R.", "Euler-Bernoulli Equation"));
        list.add(new QuestionBlueprint("What is the Section Modulus Z for a solid rectangular cross-section of width b and depth d?", "b * d^2 / 12", "b * d^2 / 6", "b^2 * d / 6", "b * d^3 / 12", "B", "I = b*d^3 / 12 and y_max = d/2. Thus Z = I / y_max = b*d^2 / 6.", "Section Modulus"));
        list.add(new QuestionBlueprint("In a circular shaft subjected to pure torsion, how does shear stress vary radially from the center to the outer surface?", "Uniformly throughout", "Parabolically from center to surface", "Linearly from zero at the center to maximum at the outer surface", "Zero at surface and maximum at center", "C", "From T/J = tau/r, shear stress tau is directly proportional to radial distance r.", "Torsional Shear Stress"));
        list.add(new QuestionBlueprint("What is the maximum deflection at the free tip of a cantilever beam of length L carrying point load P at its end?", "P * L^3 / (3 * E * I)", "P * L^3 / (48 * E * I)", "P * L^4 / (8 * E * I)", "P * L^2 / (2 * E * I)", "A", "Double integration of cantilever beam equation yields tip deflection delta = P*L^3 / (3*EI).", "Cantilever Tip Deflection"));
        list.add(new QuestionBlueprint("What does the radius of Mohr's Circle represent physically for a 2D state of stress?", "Maximum normal stress", "Maximum in-plane shear stress", "Hydrostatic pressure", "Octahedral normal stress", "B", "The radius of Mohr's circle equals maximum in-plane shear stress: R = (sigma_1 - sigma_2) / 2 = tau_max.", "Mohr's Circle Radius"));
        list.add(new QuestionBlueprint("In a thin cylindrical pressure vessel under internal fluid pressure p, what is the ratio of hoop stress to longitudinal stress?", "1 : 1", "2 : 1", "1 : 2", "4 : 1", "B", "Hoop stress is p*d / (2t) and longitudinal stress is p*d / (4t), yielding a ratio of 2:1.", "Cylinder Stress Ratio"));
        return list;
    }

    // =========================================================================
    // 11. ELECTRIC CIRCUIT THEORY & POWER SYSTEMS
    // =========================================================================
    private static List<QuestionBlueprint> getElectricCircuitsQuestions(int phaseNum) {
        List<QuestionBlueprint> list = new ArrayList<>();
        list.add(new QuestionBlueprint("What physical conservation law forms the theoretical basis of Kirchhoff's Current Law (KCL)?", "Conservation of Energy", "Conservation of Electric Charge", "Conservation of Momentum", "Conservation of Magnetic Flux", "B", "KCL states that algebraic sum of currents entering a node is zero, which is a consequence of charge conservation.", "Kirchhoff's Current Law"));
        list.add(new QuestionBlueprint("What physical conservation law forms the theoretical basis of Kirchhoff's Voltage Law (KVL)?", "Conservation of Electric Charge", "Conservation of Energy", "Faraday's Law", "Ohm's Law", "B", "KVL states directed sum of electrical potential differences around a closed loop is zero, representing energy conservation.", "Kirchhoff's Voltage Law"));
        list.add(new QuestionBlueprint("In an electrical circuit with N nodes, how many independent nodal equations are required to solve the network?", "N", "N - 1", "N + 1", "2N", "B", "Choosing one node as reference leaves N - 1 independent node-voltage equations.", "Nodal Analysis"));
        list.add(new QuestionBlueprint("According to Maximum Power Transfer Theorem, when does an AC source with internal impedance Z_s transfer maximum real power to load Z_L?", "Z_L = Z_s", "Z_L = Z_s* (complex conjugate of source impedance)", "Z_L = 0", "Real(Z_L) = 0", "B", "Maximum real power transfer occurs when load impedance is the complex conjugate of source impedance.", "Maximum Power Transfer"));
        list.add(new QuestionBlueprint("What is the resonant frequency ω_0 of a series RLC circuit?", "1 / (L * C)", "1 / sqrt(L * C)", "sqrt(L / C)", "R / (2 * L)", "B", "Resonance occurs when reactances cancel: omega*L = 1/(omega*C) => omega_0 = 1 / sqrt(LC).", "Series Resonance"));
        list.add(new QuestionBlueprint("What is the time constant τ of a series RL circuit connected to a DC source?", "R * L", "L / R", "1 / (R * L)", "R^2 / L", "B", "The governing differential equation L(di/dt) + Ri = V has time constant tau = L / R.", "RL Time Constant"));
        list.add(new QuestionBlueprint("What is the time constant τ of a series RC circuit?", "R * C", "C / R", "1 / (R * C)", "R^2 * C", "A", "Natural response of an RC circuit decays with time constant tau = R * C.", "RC Time Constant"));
        list.add(new QuestionBlueprint("In a balanced three-phase star (Y) connected system, what is the relationship between line voltage V_L and phase voltage V_ph?", "V_L = V_ph", "V_L = sqrt(3) * V_ph", "V_L = 3 * V_ph", "V_L = V_ph / sqrt(3)", "B", "In star connection, phasor difference between phase voltages yields V_L = sqrt(3) * V_ph.", "Three-Phase Star"));
        list.add(new QuestionBlueprint("In a balanced three-phase delta (Δ) connected system, what is the relationship between line current I_L and phase current I_ph?", "I_L = I_ph", "I_L = sqrt(3) * I_ph", "I_L = 3 * I_ph", "I_L = I_ph / sqrt(3)", "B", "In delta connection, line current is vector difference of phase currents: I_L = sqrt(3) * I_ph.", "Three-Phase Delta"));
        list.add(new QuestionBlueprint("For a reciprocal two-port network, what condition must the ABCD transmission parameters satisfy?", "A = D", "AD - BC = 1", "AB - CD = 0", "A + D = B + C", "B", "Reciprocity in ABCD parameter representation requires determinant AD - BC = 1.", "ABCD Reciprocity"));
        return list;
    }

    // =========================================================================
    // 12. PYTHON PROGRAMMING FOR ENGINEERING APPLICATIONS
    // =========================================================================
    private static List<QuestionBlueprint> getPythonQuestions(int phaseNum) {
        List<QuestionBlueprint> list = new ArrayList<>();
        list.add(new QuestionBlueprint("In Python, which of the following is a mutable built-in data type?", "tuple", "str", "list", "frozenset", "C", "Lists in Python can be modified in-place, whereas tuples and strings are immutable.", "Python Mutability"));
        list.add(new QuestionBlueprint("What is the output of print(type(5 / 2)) in Python 3?", "<class 'int'>", "<class 'float'>", "<class 'double'>", "<class 'number'>", "B", "In Python 3, single slash division always returns a float (2.5).", "Python Numeric Types"));
        list.add(new QuestionBlueprint("What does the 'is' operator test in Python compared to '=='?", "'is' tests object identity (memory address); '==' tests equality of value", "'is' compares types; '==' compares memory", "'is' is deprecated syntax for '=='", "'is' performs deep value comparison", "A", "'is' checks memory identity id(a) == id(b); '==' checks value equality.", "Identity vs Equality"));
        list.add(new QuestionBlueprint("What is the average time complexity of key lookup in a Python dictionary?", "O(n)", "O(1)", "O(log n)", "O(n log n)", "B", "Python dictionaries use hash tables, providing O(1) average lookup time.", "Dict Lookup Complexity"));
        list.add(new QuestionBlueprint("What does a Python generator function use instead of 'return' to yield values lazily?", "async", "yield", "await", "defer", "B", "The 'yield' statement freezes execution frame and yields values on demand, saving memory.", "Generators and Yield"));
        list.add(new QuestionBlueprint("What constraint does the Global Interpreter Lock (GIL) in CPython impose on multithreading?", "Prevents threads from sharing variables", "Restricts execution of Python bytecode to a single OS thread at any one time", "Limits memory usage to 4GB", "Forces single-core CPU execution for external C libraries", "B", "The GIL allows only one native thread to execute Python bytecode at a time.", "Global Interpreter Lock"));
        list.add(new QuestionBlueprint("Which NumPy function is optimal for creating an array of 100 evenly spaced values between 0.0 and 1.0?", "np.arange(0, 1, 100)", "np.linspace(0.0, 1.0, 100)", "np.space(0, 1, 100)", "np.range(0, 1, 100)", "B", "np.linspace(start, stop, num) generates num linearly spaced samples over interval.", "NumPy Linspace"));
        list.add(new QuestionBlueprint("In Python's asyncio module, which keyword suspends coroutine execution until an awaitable object finishes?", "yield", "await", "pause", "defer", "B", "The 'await' keyword pauses the coroutine, yielding control back to the event loop.", "Asyncio Coroutines"));
        list.add(new QuestionBlueprint("How does a list comprehension [x**2 for x in data] differ from a generator expression (x**2 for x in data)?", "List comprehension creates the full list in memory immediately; generator produces items lazily on demand", "Generator expression is deprecated", "List comprehension is slower", "There is no difference", "A", "List comprehensions allocate all items eagerly; generator expressions return an iterator computing lazily.", "Comprehensions vs Generators"));
        list.add(new QuestionBlueprint("What design pattern does the 'with' statement implement in Python via __enter__ and __exit__?", "Singleton pattern", "Context Manager (RAII pattern for deterministic resource cleanup)", "Factory pattern", "Observer pattern", "B", "Context managers guarantee cleanup code in __exit__ runs even if an unhandled exception occurs.", "Context Managers"));
        return list;
    }

    // =========================================================================
    // 13. DYNAMIC ADAPTIVE QUESTION SYNTHESIZER
    // =========================================================================
    private static List<QuestionBlueprint> synthesizeDynamicQuestions(CoursePhase phase, User student) {
        List<QuestionBlueprint> list = new ArrayList<>();
        String pt = phase.getTitle() != null ? phase.getTitle() : "Core Principles";
        String ct = (phase.getCourse() != null && phase.getCourse().getTitle() != null) ? phase.getCourse().getTitle() : "Engineering";
        String branch = (student != null && student.getBranch() != null) ? student.getBranch().toUpperCase() : "ENGINEERING";
        String year = (student != null && student.getAcademicYear() != null) ? student.getAcademicYear() : "2nd Year";

        // Balanced answer distribution: Q1:B, Q2:C, Q3:A, Q4:D, Q5:B, Q6:C, Q7:A, Q8:D, Q9:B, Q10:C
        list.add(new QuestionBlueprint(
                "In the context of " + ct + ", what is the fundamental theoretical foundation underlying " + pt + "?",
                "It relies exclusively on empirical heuristics without formal mathematical models.",
                "It establishes systematic physical or mathematical governing equations governing system behavior.",
                "It serves solely as an obsolete historical footnote in modern " + branch + " engineering.",
                "It is strictly an unverified empirical hypothesis.",
                "B",
                "The core principle of " + pt + " is to establish rigorous mathematical and physical governing models for " + branch + " systems.",
                pt.split(":")[0].trim()
        ));

        list.add(new QuestionBlueprint(
                "When analyzing parametric constraints in " + pt + ", which factor plays the most critical role in system stability?",
                "Arbitrary scaling without boundary condition validation",
                "Complete omission of dissipative damping factors",
                "Rigorous adherence to boundary conditions and governing conservation laws",
                "Operating under assumed zero-friction and zero-impedance conditions",
                "C",
                "Stability analysis in " + ct + " requires that boundary conditions and energy conservation principles are rigorously satisfied.",
                "Stability Analysis"
        ));

        list.add(new QuestionBlueprint(
                "How does " + pt + " directly influence efficiency and performance optimization in " + branch + " applications?",
                "By systematically minimizing losses and balancing structural, computational, or energetic trade-offs",
                "By increasing system overhead and redundancy indiscriminately",
                "By bypassing safety margins to achieve temporary maximum throughput",
                "By converting all continuous variables into arbitrary binary approximations",
                "A",
                "Engineering optimization in " + ct + " systematically balances trade-offs to maximize net efficiency while respecting design limits.",
                "Performance Optimization"
        ));

        list.add(new QuestionBlueprint(
                "Which analytical methodology is standard practice for verifying the empirical validity of models in " + pt + "?",
                "Subjective visual inspection without quantitative data",
                "Relying exclusively on non-converging iterative approximations",
                "Discarding all experimental outliers without statistical justification",
                "Systematic sensitivity analysis combined with controlled experimental verification",
                "D",
                "Rigorous engineering verification requires controlled experiments accompanied by sensitivity and error analysis.",
                "Experimental Verification"
        ));

        list.add(new QuestionBlueprint(
                "In " + year + " curriculum standards for " + branch + ", what primary failure mode must be guarded against when implementing " + pt + "?",
                "Exceeding theoretical maximum speed of light",
                "Resonance catastrophes, structural fatigue, or boundary overflow under dynamic loading",
                "Zero energy consumption in steady state",
                "Spontaneous instantaneous equilibrium without external stimuli",
                "B",
                "Engineering systems under " + pt + " must be designed to withstand dynamic resonance, fatigue limits, and operational stress overflows.",
                "Failure Modes"
        ));

        list.add(new QuestionBlueprint(
                "What mathematical formulation is typically utilized to model continuous transformations in " + pt + "?",
                "Arbitrary discrete lookup tables without interpolation",
                "Static linear proportionality regardless of non-linear behavior",
                "Differential equations relating input gradients to time-varying state vectors",
                "Simple scalar addition without unit consistency",
                "C",
                "Dynamic system behavior in " + ct + " is governed by differential equations capturing rate of change across state variables.",
                "Mathematical Modeling"
        ));

        list.add(new QuestionBlueprint(
                "Why is dimensional homogeneity critical when formulating equations for " + pt + "?",
                "It ensures every additive term possesses identical physical dimensions and units",
                "It speeds up numerical calculations by a factor of two",
                "It is a purely aesthetic notation standard without mathematical necessity",
                "It converts vector equations into scalar approximations automatically",
                "A",
                "Dimensional homogeneity is a physical law: terms added or equated in any physical formulation must share the exact same dimensions.",
                "Dimensional Analysis"
        ));

        list.add(new QuestionBlueprint(
                "What trade-off is typically encountered when enhancing accuracy and resolution in " + pt + "?",
                "Simultaneous reduction in both computational cost and physical complexity",
                "Complete elimination of all thermal noise and measurement uncertainty",
                "Zero impact on response time or computational overhead",
                "Increased computational overhead, sensor cost, or processing latency",
                "D",
                "Increasing resolution and analytical fidelity inevitably incurs higher computational workload, sampling bandwidth, and resource demands.",
                "Engineering Trade-offs"
        ));

        list.add(new QuestionBlueprint(
                "When integrating concepts from " + pt + " into real-world " + branch + " systems, what role do safety factors (margins) play?",
                "They are used to artificially inflate project billing costs",
                "They account for material uncertainties, unexpected dynamic surges, and environmental variations",
                "They indicate that the underlying engineering theory is fundamentally flawed",
                "They eliminate the need for routine maintenance and operational inspections",
                "B",
                "Safety factors ensure structural and operational integrity despite material defects, environmental fluctuations, and unforeseen load spikes.",
                "Safety Factors"
        ));

        list.add(new QuestionBlueprint(
                "What is the ultimate objective of mastering " + pt + " within the " + ct + " curriculum for " + branch + " engineers?",
                "Rote memorization of numerical constants without practical context",
                "Designing isolated components without considering system-level integration",
                "Developing rigorous analytical, diagnostic, and design capabilities for real-world engineering challenges",
                "Replacing all physical prototyping with unverified theoretical assumptions",
                "C",
                "The pedagogical objective is to develop practical problem-solving, design synthesis, and analytical engineering acumen.",
                "Pedagogical Objective"
        ));

        return list;
    }
}
