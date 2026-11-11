package com.vrsec.backend.service;

import com.vrsec.backend.model.User;

import java.util.*;

public class SubjectCurriculumRegistry {

    /**
     * Checks whether a subject matches one of the 12 syllabus-verified core engineering subjects
     * with validated university syllabi (VR20 / autonomous syllabus).
     * Courses returning false use AICTE model curriculum or domain-synthesized blueprints.
     */
    public static boolean isSyllabusVerified(String rawSubject) {
        if (rawSubject == null) return false;
        String s = rawSubject.trim().toLowerCase();
        return (s.contains("data struct") || s.contains("dsa") || (s.contains("algorithm") && !s.contains("machine") && !s.contains("design & analysis")))
                || (s.contains("math") || s.contains("calculus") || s.contains("algebra") || s.contains("differential equation"))
                || (s.contains("digital") || s.contains("logic design") || s.contains("verilog") || (s.contains("electronic") && !s.contains("power")))
                || (s.contains("physic") || s.contains("optics") || s.contains("laser") || s.contains("semiconductor"))
                || (s.contains("solid") || (s.contains("mechanic") && !s.contains("fluid") && !s.contains("quantum")) || s.contains("strength of material"))
                || (s.contains("fluid") || s.contains("hydraulic"))
                || (s.contains("operating system") || s.contains("os &") || s.contains("kernel"))
                || (s.contains("database") || s.contains("dbms") || s.contains("sql"))
                || (s.contains("network") && !s.contains("neural"))
                || (s.contains("environment") || s.contains("ecology") || s.contains("pollution"))
                || (s.contains("thermodynamic") || s.contains("heat transfer"))
                || (s.contains("python"));
    }

    public static List<Map<String, Object>> getCurriculumForSubject(String rawSubject, User student) {
        String s = rawSubject != null ? rawSubject.trim().toLowerCase() : "engineering";
        String branch = student != null && student.getBranch() != null ? student.getBranch().toUpperCase() : "ENGINEERING";
        String year = student != null && student.getAcademicYear() != null ? student.getAcademicYear() : "2nd Year";

        List<Map<String, Object>> phases = new ArrayList<>();

        if (s.contains("data struct") || s.contains("dsa") || (s.contains("algorithm") && !s.contains("machine") && !s.contains("design & analysis"))) {
            phases.add(phase("Phase 1: Algorithmic Complexity, Big-O Notation & Asymptotic Analysis",
                    "Master asymptotic growth rates (Big-O, Omega, Theta), best/average/worst-case analysis, recurrence relations, and Master Theorem.",
                    "Detailed Analysis:\n- Asymptotic notation and mathematical bounds\n- Time and space complexity trade-offs\n- Master Theorem for divide-and-conquer recurrences\n- Amortized complexity analysis\n\nPractical Applications:\n- Evaluating nested loops\n- Recursive Fibonacci complexity\n- Profiling memory allocation overhead.",
                    "4-5 Hours"));
            phases.add(phase("Phase 2: Contiguous Arrays, Strings & Two-Pointer Manipulation Algorithms",
                    "Understand contiguous memory layout, row/column major addressing, prefix sums, sliding window, and two-pointer traversal techniques.",
                    "Detailed Analysis:\n- Cache locality and row-major vs column-major matrix layout\n- Amortized O(1) resizing of dynamic vectors\n- Sliding window pattern for maximum subarray\n- Prefix sums and two-pointer palindrome detection\n\nPractical Applications:\n- In-place string reversal\n- Trapping rain water problem\n- Subarray sum equals K.",
                    "4-5 Hours"));
            phases.add(phase("Phase 3: Singly, Doubly & Circular Linked Lists with Memory Pointers",
                    "Master dynamic node allocation, pointer manipulation, node insertion/deletion, Floyd's cycle detection, and LRU cache structures.",
                    "Detailed Analysis:\n- Heap memory allocation vs stack frame layout\n- Pointer manipulation without memory leaks\n- Floyd's tortoise and hare cycle finding algorithm\n- Doubly linked list with sentinel head and tail nodes\n\nPractical Applications:\n- Inverting singly linked lists\n- Implementing O(1) LRU Cache\n- Merging two sorted linked lists.",
                    "4-5 Hours"));
            phases.add(phase("Phase 4: Stacks, Expression Parsing & Queue Buffers (Circular, Deque)",
                    "Implement LIFO/FIFO linear data structures, infix-to-postfix translation, parenthesis balancing, and monotonic stack algorithms.",
                    "Detailed Analysis:\n- Stack frames and call stack mechanics\n- Shunting-yard algorithm for infix expression evaluation\n- Circular queue modulo arithmetic implementation\n- Monotonic stacks for next greater element\n\nPractical Applications:\n- Syntax bracket matching\n- Postfix expression evaluator\n- Circular ring buffer for network packets.",
                    "4-5 Hours"));
            phases.add(phase("Phase 5: Hierarchical Binary Trees, BST & Self-Balancing AVL Rotations",
                    "Master recursive tree traversals (Inorder, Preorder, Postorder, Level-order), BST search/insert/delete, and AVL height balancing.",
                    "Detailed Analysis:\n- Tree properties: depth, height, balance factor\n- BST search and deletion via in-order successor\n- AVL rotations: Left-Left, Right-Right, Left-Right, Right-Left\n- Level-order traversal using BFS queue\n\nPractical Applications:\n- Validating Binary Search Trees\n- Lowest Common Ancestor (LCA)\n- Diameter of binary tree.",
                    "5-6 Hours"));
            phases.add(phase("Phase 6: Priority Queues, Binary Heaps & Prefix Trees (Tries)",
                    "Implement min/max binary heaps, heapify algorithm (O(n) build-heap), priority queues, and trie structures for prefix searching.",
                    "Detailed Analysis:\n- Complete binary tree array representation (2i + 1, 2i + 2)\n- Heapify and heap sort mechanics\n- Prefix retrieval using 26-way trie nodes\n- Top-K elements using Min-Heap\n\nPractical Applications:\n- Median in a running data stream\n- Search engine autocomplete engine\n- Huffman coding tree compression.",
                    "5-6 Hours"));
            phases.add(phase("Phase 7: Graph Representations, Breadth-First & Depth-First Search",
                    "Master adjacency list vs matrix representations, BFS traversal, DFS traversal, cycle detection, and topological sorting.",
                    "Detailed Analysis:\n- Graph density and storage trade-offs\n- Breadth-First Search for unweighted shortest paths\n- Depth-First Search with recursion stack and visited sets\n- Kahn's algorithm for Topological Sort in Directed Acyclic Graphs (DAG)\n\nPractical Applications:\n- Course schedule prerequisites\n- Connected components in networks\n- Bipartite graph verification.",
                    "5-6 Hours"));
            phases.add(phase("Phase 8: Shortest Paths (Dijkstra), Minimum Spanning Trees & Dynamic Programming",
                    "Implement Dijkstra's shortest path, Kruskal's/Prim's MST, Bellman-Ford algorithm, and dynamic programming memoization vs tabulation.",
                    "Detailed Analysis:\n- Greedy paradigm: Kruskal's MST using Disjoint Set Union (DSU)\n- Dijkstra's algorithm with priority queue\n- Overlapping subproblems and optimal substructure in DP\n- 0/1 Knapsack: 2D tabulation vs space-optimized 1D array\n\nPractical Applications:\n- GPS routing shortest path network\n- Minimum network cabling deployment\n- Longest Common Subsequence (LCS).",
                    "6 Hours"));
        } else if (s.contains("math") || s.contains("calculus") || s.contains("algebra") || s.contains("differential equation")) {
            phases.add(phase("Phase 1: Differential Calculus, Rolle's Theorem & Lagrange Mean Value Theorems",
                    "Master continuity, differentiability, Rolle's theorem, Lagrange and Cauchy mean value theorems, Taylor series, and L'Hôpital's rule.",
                    "Mathematical Foundations:\n- Conditions for differentiability on open intervals\n- Geometric interpretation of Rolle's and Lagrange's theorems\n- Proof of Cauchy's Mean Value Theorem\n- Indeterminate limits (0/0, inf/inf) via L'Hôpital's derivatives\n\nSolved Examples:\n- Verifying Lagrange's Theorem for f(x) = x^3 - x\n- Expanding sin(x) and e^x via Taylor series\n- Curvature and radius of curvature derivations.",
                    "4-5 Hours"));
            phases.add(phase("Phase 2: Partial Derivatives, Euler's Theorem on Homogeneous Functions & Jacobians",
                    "Master partial derivatives of higher orders, Euler's theorem for homogeneous functions, total derivatives, and Jacobian matrices.",
                    "Mathematical Foundations:\n- First and second order partial derivatives (Clairaut's theorem)\n- Euler's theorem on homogeneous functions of degree n: x(df/dx) + y(df/dy) = nf\n- Jacobians of transformations J = d(u,v)/d(x,y)\n- Maxima and minima of functions of two variables using discriminant (rt - s^2)\n\nSolved Examples:\n- Verifying Euler's theorem for f(x,y) = (x^2 + y^2)/(x + y)\n- Coordinate transform Jacobian from Cartesian to polar coordinates.",
                    "4-5 Hours"));
            phases.add(phase("Phase 3: Multiple Integrals: Double & Triple Integrals with Polar/Spherical Coordinates",
                    "Evaluate double integrals over general regions, change of variables using Jacobians, triple integrals, and volume calculations.",
                    "Mathematical Foundations:\n- Double integrals over rectangular and non-rectangular regions\n- Change of order of integration\n- Double integrals in polar coordinates: dx dy = r dr d(theta)\n- Triple integrals in cylindrical and spherical coordinate systems\n\nSolved Examples:\n- Area enclosed by cardioids and parabolas\n- Volume of sphere and paraboloid using triple integrals.",
                    "5 Hours"));
            phases.add(phase("Phase 4: Vector Calculus: Gradient, Divergence, Curl & Integral Theorems",
                    "Understand scalar and vector fields, directional derivatives, gradient, divergence, curl, and Green's, Stokes', and Gauss divergence theorems.",
                    "Mathematical Foundations:\n- Gradient of scalar field and directional derivative along vector\n- Divergence of vector field: del . V (solenoidal fields)\n- Curl of vector field: del x V (irrotational fields and scalar potentials)\n- Green's Theorem in a plane connecting line and surface integrals\n- Stokes' Theorem and Gauss Divergence Theorem\n\nSolved Examples:\n- Finding work done by conservative force field\n- Verifying Gauss divergence theorem for cubic and spherical volumes.",
                    "5-6 Hours"));
            phases.add(phase("Phase 5: Ordinary Differential Equations of First & Higher Order with Constant Coefficients",
                    "Solve exact differential equations, integrating factors, linear higher order ODEs, method of variation of parameters, and Cauchy-Euler equations.",
                    "Mathematical Foundations:\n- Exact differential equations: M dx + N dy = 0 where dM/dy = dN/dx\n- Integrating factors for non-exact equations\n- Linear ODEs: Complementary Function (CF) and Particular Integral (PI)\n- Method of variation of parameters for non-standard forcing functions\n\nSolved Examples:\n- Solving (D^2 + 4)y = sin(2x)\n- Cauchy-Euler homogeneous linear equations: x^2 y'' + xy' + y = x.",
                    "5 Hours"));
            phases.add(phase("Phase 6: Laplace Transforms, Inverse Transforms & Boundary Value Applications",
                    "Master Laplace transform properties, transforms of derivatives and integrals, unit step functions, convolution theorem, and solving ODEs.",
                    "Mathematical Foundations:\n- Definition and linearity of Laplace transform\n- Shifting theorems (First and Second shifting)\n- Laplace transform of derivatives: L{f'(t)} = sF(s) - f(0)\n- Convolution Theorem: L{f * g} = F(s)G(s)\n- Inverse Laplace transform using partial fractions\n\nSolved Examples:\n- Solving y'' + 4y = 0 with y(0) = 1, y'(0) = 0 via Laplace transforms\n- Transform of unit step Heaviside function.",
                    "5 Hours"));
        } else if (s.contains("digital") || s.contains("logic design") || s.contains("verilog") || (s.contains("electronic") && !s.contains("power"))) {
            phases.add(phase("Phase 1: Number Systems, Complements & Binary Codes",
                    "Understand binary, octal, decimal, hexadecimal radix conversions, signed binary representations, 1's and 2's complements, and binary codes.",
                    "Engineering Analysis:\n- Positional radix number systems: binary (radix-2), hexadecimal (radix-16)\n- Signed magnitude, 1's complement, and 2's complement arithmetic\n- Binary overflow conditions in signed arithmetic\n- BCD (8421), Excess-3, and Gray code (unit distance property)\n\nPractical Circuits:\n- Binary to Gray code converter design\n- 8-bit complement arithmetic in microprocessors.",
                    "4-5 Hours"));
            phases.add(phase("Phase 2: Boolean Algebra Postulates, De Morgan's Laws & K-Map Minimization",
                    "Master Boolean algebra theorems, duality, canonical SOP and POS forms, minterms, and logic simplification using 2, 3, and 4-variable K-Maps.",
                    "Engineering Analysis:\n- Boolean postulates, Huntington postulates, and consensus theorem\n- De Morgan's theorems: (A + B)' = A'B' and (AB)' = A' + B'\n- Standard and canonical SOP/POS expressions\n- Karnaugh Map (K-Map) grouping rules: pairs, quads, octets\n- Don't-care condition optimization in K-maps\n\nPractical Circuits:\n- Simplifying 4-variable logic equations to minimal gate count.",
                    "4-5 Hours"));
            phases.add(phase("Phase 3: Logic Gates, Universal Gate Synthesis & Propagation Characteristics",
                    "Analyze AND, OR, NOT, NAND, NOR, XOR, XNOR gates, implement universal logic using NAND and NOR, and evaluate propagation delay.",
                    "Engineering Analysis:\n- Truth tables and electrical characteristics of basic logic gates\n- NAND and NOR as universal gates (synthesizing AND, OR, NOT from NAND)\n- XOR and XNOR gates and parity generation\n- Fan-in, fan-out, noise margin, power dissipation, and propagation delay\n\nPractical Circuits:\n- Active-low vs active-high enable circuits\n- Interfacing TTL and CMOS logic families.",
                    "4-5 Hours"));
            phases.add(phase("Phase 4: Combinational Logic Circuits: Adders, Subtractors, Decoders & Multiplexers",
                    "Design half and full adders, ripple carry adders, subtractors, magnitude comparators, decoders, encoders, and multiplexer logic trees.",
                    "Engineering Analysis:\n- Half adder (S = A XOR B, C = AB) and Full adder using basic gates\n- Look-ahead carry adder for eliminating carry propagation delay\n- 3-to-8 decoder with active low enables\n- 4:1 and 8:1 Multiplexers as universal combinational logic function generators\n\nPractical Circuits:\n- Designing 4-bit parallel adder/subtractor with XOR control\n- Implementing Boolean functions using single 8:1 MUX.",
                    "5 Hours"));
            phases.add(phase("Phase 5: Sequential Circuits: Latches, Flip-Flops & Timing Analysis",
                    "Master SR, D, JK, and T latches and flip-flops, master-slave configurations, race-around conditions, and excitation tables.",
                    "Engineering Analysis:\n- Difference between level-triggered latches and edge-triggered flip-flops\n- SR latch using NAND/NOR cross-coupled gates\n- JK flip-flop and race-around condition when J=1, K=1 and clock pulse is high\n- Master-Slave JK flip-flop and edge-triggered D flip-flop\n- Setup time, hold time, and clock-to-Q propagation delay\n\nPractical Circuits:\n- Converting JK flip-flop to D and T flip-flops\n- Debounce switch circuit using SR latch.",
                    "5 Hours"));
            phases.add(phase("Phase 6: Synchronous Counters, Shift Registers & Finite State Machine Design",
                    "Design synchronous and asynchronous counters, ring/Johnson counters, shift registers (SISO, SIPO, PIPO), and Mealy/Moore state machines.",
                    "Engineering Analysis:\n- Asynchronous (ripple) counters and cumulative propagation delay\n- Synchronous counter design using excitation tables and K-maps\n- Shift register architectures: Serial-In Serial-Out, Parallel-In Parallel-Out\n- Ring counter vs Johnson (twisted ring) counter\n- Mealy machine (output depends on input and state) vs Moore machine\n\nPractical Circuits:\n- Modulo-10 (BCD) synchronous decade counter\n- Sequence detector for recognizing binary bitstream 1011.",
                    "5-6 Hours"));
        } else if (s.contains("physic") || s.contains("optics") || s.contains("laser") || s.contains("semiconductor")) {
            phases.add(phase("Phase 1: Wave Optics: Interference of Light & Thin Film Phenomena",
                    "Understand wave superposition, coherence conditions, division of wavefront and division of amplitude, Newton's rings, and thin films.",
                    "Physical Principles:\n- Coherent sources, spatial and temporal coherence\n- Interference in thin films: path difference delta = 2*mu*t*cos(r) + lambda/2 (Stokes' treatment)\n- Reflected vs transmitted light complementary fringe systems\n- Newton's rings experiment: circular fringes of equal thickness\n- Proof: diameter of bright rings proportional to sqrt(2n - 1), dark rings to sqrt(n)\n\nLaboratory Applications:\n- Testing optical flatness of glass plates\n- Determination of wavelength of sodium light\n- Antireflection optical coatings.",
                    "4-5 Hours"));
            phases.add(phase("Phase 2: Diffraction of Light: Fresnel, Fraunhofer & Optical Gratings",
                    "Distinguish Fresnel and Fraunhofer diffraction, single-slit intensity distribution, double-slit patterns, and plane diffraction gratings.",
                    "Physical Principles:\n- Huygens-Fresnel principle and half-period zones\n- Fraunhofer diffraction at single slit: intensity I = I0*(sin(alpha)/alpha)^2\n- Minima condition: a*sin(theta) = m*lambda\n- Plane transmission diffraction grating: grating equation (a + b)*sin(theta) = n*lambda\n- Dispersive power and resolving power of diffraction grating: R = lambda/dlambda = n*N\n\nLaboratory Applications:\n- Measuring spectral lines of mercury vapor lamp\n- Optical spectrometer calibration.",
                    "4-5 Hours"));
            phases.add(phase("Phase 3: Polarization of Light, Double Refraction & Optical Devices",
                    "Master plane, circular, and elliptical polarization, Brewster's law, Malus's law, double refraction in calcite, and retardation plates.",
                    "Physical Principles:\n- Transverse electromagnetic nature of light waves\n- Polarization by reflection: Brewster's law mu = tan(i_p)\n- Malus's law of transmitted intensity: I = I0*cos^2(theta)\n- Birefringence (double refraction) in uniaxial crystals: O-ray (ordinary) and E-ray (extraordinary)\n- Nicol prism construction using Canada balsam cement\n- Quarter-wave plate (path diff lambda/4) and Half-wave plate (path diff lambda/2)\n\nLaboratory Applications:\n- Laurent's half-shade polarimeter for optical activity of sugar solutions.",
                    "5 Hours"));
            phases.add(phase("Phase 4: Lasers & Quantum Mechanics: Stimulated Emission & Resonators",
                    "Understand Einstein coefficients, spontaneous vs stimulated emission, population inversion, optical pumping, and He-Ne/diode lasers.",
                    "Physical Principles:\n- Thermal equilibrium and Boltzmann distribution\n- Einstein's A and B coefficients and their ratio: A21/B21 proportional to nu^3\n- Metastable states, optical pumping, and achieving population inversion\n- Optical resonant cavity: longitudinal and transverse cavity modes\n- Ruby laser (three-level system) and Helium-Neon laser (four-level system: 632.8 nm red output)\n- Semiconductor GaAs injection laser diode\n\nEngineering Applications:\n- Laser cutting, barcode scanning, and optical disk storage.",
                    "5 Hours"));
            phases.add(phase("Phase 5: Fiber Optics & Waveguide Light Propagation",
                    "Master total internal reflection in optical fibers, acceptance angle, numerical aperture derivation, fiber types, and signal losses.",
                    "Physical Principles:\n- Critical angle and condition for total internal reflection: n1 > n2\n- Derivation of Acceptance Angle: theta_0 = arcsin(sqrt(n1^2 - n2^2))\n- Numerical Aperture (NA) = sqrt(n1^2 - n2^2)\n- Step-index vs Graded-index (GRIN) optical fibers\n- Single-mode vs Multimode fiber propagation\n- Signal attenuation mechanisms: Rayleigh scattering, absorption, bending losses\n\nEngineering Applications:\n- High-speed fiber optic telecommunication networks\n- Endoscopic medical imaging.",
                    "5 Hours"));
            phases.add(phase("Phase 6: Semiconductor Physics & Solid-State Band Theory",
                    "Analyze energy band formation in solids, Kronig-Penney model, direct/indirect bandgaps, intrinsic/extrinsic carriers, and Hall effect.",
                    "Physical Principles:\n- Formation of energy bands: valence band, conduction band, and forbidden gap Eg\n- Kronig-Penney model and E-k dispersion relations\n- Direct bandgap (GaAs) vs Indirect bandgap (Si) semiconductors\n- Fermi-Dirac distribution function f(E) = 1 / (1 + exp((E - Ef)/kT))\n- Intrinsic carrier concentration ni and mass-action law n*p = ni^2\n- Hall effect: Hall coefficient Rh = 1 / (n*e), Hall voltage, and carrier type determination\n\nEngineering Applications:\n- Hall probe magnetic flux sensors\n- Photovoltaic solar cell efficiency.",
                    "5 Hours"));
        } else if (s.contains("solid") || (s.contains("mechanic") && !s.contains("fluid") && !s.contains("quantum")) || s.contains("strength of material")) {
            phases.add(phase("Phase 1: Simple Stress, Strain, Hooke's Law & Elastic Constants",
                    "Understand normal and shear stress, tensile strain, Hooke's law, Poisson's ratio, relationship between elastic moduli, and thermal stresses.",
                    "Engineering Analysis:\n- Tensile, compressive, and shear stress definitions\n- Stress-strain diagram for ductile (mild steel) and brittle materials\n- Hooke's law and Young's modulus E = sigma / epsilon\n- Poisson's ratio nu = lateral strain / longitudinal strain\n- Relationships between E, Shear Modulus G, Bulk Modulus K: E = 2G(1+nu) = 3K(1-2nu)\n- Thermal stresses in constrained bars: sigma = E * alpha * delta_T\n\nSolved Problems:\n- Elongation of uniformly tapering and stepped circular bars\n- Compound bar under axial tensile load.",
                    "4-5 Hours"));
            phases.add(phase("Phase 2: Shear Force & Bending Moment Diagrams in Beams",
                    "Calculate support reactions and plot Shear Force Diagrams (SFD) and Bending Moment Diagrams (BMD) for cantilever, simply supported, and overhanging beams.",
                    "Engineering Analysis:\n- Types of beams: cantilever, simply supported, overhanging, fixed\n- Sign conventions for shear force and bending moment\n- Relationship between load intensity w, shear force V, and bending moment M: dV/dx = -w, dM/dx = V\n- Points of maximum bending moment where V = 0\n- Point of contraflexure (inflection) where bending moment changes sign\n\nSolved Problems:\n- Simply supported beam with uniformly distributed load (UDL) and concentrated point loads\n- Overhanging beam with point of contraflexure calculation.",
                    "5 Hours"));
            phases.add(phase("Phase 3: Flexural & Transverse Shear Stresses in Beams",
                    "Derive the pure bending equation, neutral axis, section modulus, and transverse shear stress distribution across beam sections.",
                    "Engineering Analysis:\n- Assumptions in simple theory of bending (Euler-Bernoulli beam theory)\n- Derivation: M/I = sigma/y = E/R\n- Neutral axis passing through centroid of cross-section\n- Section modulus Z = I / y_max and flexural strength\n- Transverse shear stress formula: tau = (V * A * y_bar) / (I * b)\n- Parabolic shear stress distribution across rectangular and I-sections\n\nSolved Problems:\n- Maximum bending stress in symmetric I-beam\n- Ratio of maximum shear stress to average shear stress for rectangular section (tau_max = 1.5 * tau_avg).",
                    "5 Hours"));
            phases.add(phase("Phase 4: Torsion of Circular Shafts, Polar Modulus & Torsional Rigidity",
                    "Derive the torsional equation, polar moment of inertia, power transmission by solid/hollow shafts, and shear stresses in torsion.",
                    "Engineering Analysis:\n- Pure torsion assumptions and angular twist theta\n- Torsion equation derivation: T/J = tau/r = G*theta/L\n- Polar moment of inertia J = pi*D^4 / 32 (solid) and pi*(D^4 - d^4)/32 (hollow)\n- Polar section modulus Zp = J / R\n- Power transmitted by rotating shaft: P = 2*pi*N*T / 60\n- Comparison of weight and strength between solid and hollow shafts of same material\n\nSolved Problems:\n- Designing shaft diameter for 150 kW motor at 300 RPM under allowable shear stress.",
                    "5 Hours"));
            phases.add(phase("Phase 5: Deflection of Beams & Differential Equations of Elastic Curve",
                    "Master the differential equation of beam deflection, double integration method, Macaulay's method, and moment-area theorems.",
                    "Engineering Analysis:\n- Governing differential equation: EI * (d^2 y / dx^2) = M(x)\n- Boundary conditions for simply supported (zero deflection at supports) and cantilever beams\n- Macaulay's bracket notation method for discontinuous point loads and partial UDLs\n- Mohr's Moment-Area Theorems: slope and deflection from M/EI diagram\n\nSolved Problems:\n- Maximum center deflection of simply supported beam with center load P: delta = P*L^3 / (48*EI)\n- Cantilever tip deflection under uniform load w: delta = w*L^4 / (8*EI).",
                    "5-6 Hours"));
            phases.add(phase("Phase 6: Complex Stresses, Principal Planes, Mohr's Circle & Thin Cylinders",
                    "Analyze stresses on inclined planes, calculate principal stresses and maximum shear stress, construct 2D Mohr's Circle, and evaluate thin cylinders.",
                    "Engineering Analysis:\n- General 2D state of stress: sigma_x, sigma_y, tau_xy\n- Stresses on inclined plane theta: normal stress sigma_theta and shear stress tau_theta\n- Principal planes (shear stress is zero) and principal stresses sigma_1, sigma_2\n- Maximum shear stress: tau_max = (sigma_1 - sigma_2) / 2\n- Graphical Mohr's Circle construction and coordinate interpretation\n- Thin cylindrical pressure vessels: circumferential (hoop) stress sigma_h = P*d/(2t), longitudinal stress sigma_l = P*d/(4t)\n\nSolved Problems:\n- Finding principal stresses and maximum shear from given triaxial stress tensor\n- Wall thickness design for compressed air storage tank.",
                    "5-6 Hours"));
        } else if (s.contains("fluid") || s.contains("hydraulic")) {
            phases.add(phase("Phase 1: Fluid Properties, Hydrostatic Pressure & Manometry",
                    "Master fluid density, specific gravity, dynamic/kinematic viscosity, surface tension, capillarity, Pascal's law, and pressure measurement via manometers.",
                    "Fluid Principles:\n- Newton's law of viscosity: tau = mu * (du/dy)\n- Absolute pressure, gauge pressure, and atmospheric pressure relations: P_abs = P_atm + P_gauge\n- Hydrostatic paradox and Pascal's principle\n- Differential U-tube and inverted U-tube manometers\n- Hydrostatic force on submerged planar and curved surfaces\n- Center of pressure derivation: h_cp = h_bar + (I_G * sin^2 theta) / (A * h_bar)\n\nSolved Problems:\n- Sizing hydraulic press piston diameters\n- Total hydrostatic thrust and center of pressure on vertical dam face.",
                    "4-5 Hours"));
            phases.add(phase("Phase 2: Fluid Kinematics: Velocity Fields, Streamlines & Continuity",
                    "Understand Eulerian vs Lagrangian flow descriptions, steady/unsteady flow, uniform/non-uniform flow, rotational flow, and continuity equations.",
                    "Fluid Principles:\n- Streamlines, pathlines, and streaklines definitions\n- Velocity vector V = u*i + v*j + w*k and local/convective acceleration\n- Derivation of 3D differential continuity equation: d(rho)/dt + del . (rho * V) = 0\n- Incompressible continuity equation: du/dx + dv/dy + dw/dz = 0\n- Velocity potential function phi and stream function psi (Cauchy-Riemann condition for 2D irrotational flow)\n\nSolved Problems:\n- Flow rate in converging conduit\n- Checking irrotationality of velocity vector V = (2x)i - (2y)j.",
                    "4-5 Hours"));
            phases.add(phase("Phase 3: Fluid Dynamics, Euler's Equation & Bernoulli's Principle",
                    "Derive Euler's equation of motion along a streamline, Bernoulli's energy theorem, and hydraulic flow measuring devices.",
                    "Fluid Principles:\n- Forces on a moving fluid element: pressure, gravity, viscosity\n- Euler's differential equation of motion along streamline: dp/rho + V dV + g dz = 0\n- Bernoulli's theorem: P/(rho*g) + V^2/(2g) + z = Constant (total head)\n- Energy correction factor and momentum correction factor\n- Flow measurement: Venturi meter (discharge coefficient Cd), Orifice meter, and Pitot tube\n- Torricelli's law of efflux through an orifice\n\nSolved Problems:\n- Measuring oil pipeline flow rate using Venturi meter\n- Airspeed calculation from Pitot-static tube pressure differential.",
                    "5 Hours"));
            phases.add(phase("Phase 4: Laminar Flow, Turbulent Pipe Flow & Darcy-Weisbach Losses",
                    "Analyze Reynolds experiment, Hagen-Poiseuille equation for viscous laminar flow, turbulent shear, and major/minor head losses in pipelines.",
                    "Fluid Principles:\n- Reynolds number Re = rho*V*D / mu (laminar flow for Re < 2000, turbulent for Re > 4000)\n- Hagen-Poiseuille laminar flow equation through circular pipe: Delta_P = 32*mu*V*L / D^2\n- Parabolic velocity distribution with u_max = 2 * u_avg\n- Darcy-Weisbach equation for major head loss: h_f = f * L * V^2 / (2 * g * D)\n- Friction factor f = 64 / Re for laminar flow; Moody diagram for turbulent flow\n- Minor losses: sudden expansion, contraction, valves, and bends\n\nSolved Problems:\n- Pumping power required to transport heavy crude oil through 500m pipeline.",
                    "5 Hours"));
            phases.add(phase("Phase 5: Boundary Layer Theory, Flow Separation & Drag/Lift Forces",
                    "Master Prandtl boundary layer equations, displacement and momentum thicknesses, boundary layer separation, and aerodynamic forces.",
                    "Fluid Principles:\n- Prandtl's boundary layer hypothesis over flat plate\n- Boundary layer thickness delta, displacement thickness delta*, and momentum thickness theta\n- Von Karman momentum integral equation\n- Adverse pressure gradient (dp/dx > 0) and onset of boundary layer separation\n- Friction drag vs form (pressure) drag; bluff bodies vs streamlined bodies\n- Terminal velocity and Stokes' law: F_d = 6*pi*mu*r*V for spherical particles\n\nSolved Problems:\n- Total skin friction drag on ship hull model in towing tank.",
                    "5 Hours"));
            phases.add(phase("Phase 6: Hydraulic Turbines & Centrifugal/Reciprocating Pumps",
                    "Evaluate impulse and reaction hydraulic turbines (Pelton, Francis, Kaplan), pump characteristics, cavitation, and Net Positive Suction Head.",
                    "Fluid Principles:\n- Euler's turbomachine turbine equation and velocity triangles\n- Pelton wheel impulse turbine: bucket velocity, jet ratio, hydraulic efficiency\n- Francis and Kaplan reaction turbines: draft tube function and pressure recovery\n- Specific speed N_s = N * sqrt(P) / H^(5/4) for turbine selection\n- Centrifugal pumps: impeller design, manometric head, and priming requirement\n- Cavitation phenomenon and Net Positive Suction Head (NPSH)\n\nSolved Problems:\n- Power output and hydraulic efficiency of a 2-jet Pelton turbine.",
                    "5-6 Hours"));
        } else if (s.contains("operating system") || s.contains("os &") || s.contains("kernel")) {
            phases.add(phase("Phase 1: OS Kernel Architectures, System Calls & Process Management",
                    "Understand monolithic vs microkernel architectures, Dual-mode CPU operation, process states, PCB, and fork/exec system calls.",
                    "Systems Engineering:\n- Kernel architectures: monolithic, microkernel, layered, and hybrid designs\n- User mode vs Kernel mode (privilege ring 0) and interrupt service routines (ISR)\n- Process Control Block (PCB): CPU registers, program counter, PID, and open file descriptors\n- Process life cycle states: New, Ready, Running, Waiting, Terminated\n- Unix process creation mechanics: fork(), execvp(), and waitpid()\n\nPractical Labs:\n- C implementation of multi-process process tree with zombie/orphan handling.",
                    "4-5 Hours"));
            phases.add(phase("Phase 2: CPU Scheduling Algorithms & Threading Models",
                    "Master preemptive vs non-preemptive scheduling, FCFS, SJF, Priority, Round Robin, Multi-Level Feedback Queues, and thread synchronization.",
                    "Systems Engineering:\n- CPU-I/O burst cycle and scheduling criteria: turnaround time, waiting time, response time\n- Algorithms: FCFS, Shortest Job First (SJF / SRTF), Priority scheduling with aging\n- Round Robin (RR) time quantum tuning and context switch overhead\n- Multi-Level Feedback Queue (MLFQ) priority boosting\n- User-level threads vs Kernel-level threads (1:1, N:1, M:N mapping models)\n\nPractical Labs:\n- Implementing an event-driven CPU scheduler simulation in C++.",
                    "5 Hours"));
            phases.add(phase("Phase 3: Process Synchronization, Critical Section & Deadlock Prevention",
                    "Solve critical section problems, master Peterson's algorithm, mutex locks, counting semaphores, monitor abstractions, and Banker's algorithm.",
                    "Systems Engineering:\n- Race conditions and mutual exclusion, progress, and bounded waiting requirements\n- Hardware atomic primitives: Test-and-Set and Compare-and-Swap (CAS)\n- Dijkstra's Semaphores (Wait/P and Signal/V operations) and binary mutexes\n- Classic synchronization challenges: Producer-Consumer, Dining Philosophers, Readers-Writers\n- Coffman's four deadlock conditions: Mutual Exclusion, Hold & Wait, No Preemption, Circular Wait\n- Deadlock avoidance using Dijkstra's Banker's Algorithm (Safety and Resource-Request algorithms)\n\nPractical Labs:\n- Posix pthread semaphore solution for multi-threaded thread pool.",
                    "5-6 Hours"));
            phases.add(phase("Phase 4: Main Memory Management, Paging & Segmentation",
                    "Understand logical vs physical address spaces, MMU hardware translation, contiguous allocation, paging, TLB, and segmentation.",
                    "Systems Engineering:\n- Dynamic relocatable memory address translation via Base and Limit registers\n- Internal vs external fragmentation, compaction, and Best-fit/First-fit allocation\n- Paging architecture: page table entries, frame allocation, and offset bitmasks\n- Translation Lookaside Buffer (TLB) hit ratios and Effective Memory Access Time (EMAT)\n- Multi-level and inverted page tables for 64-bit address spaces\n\nPractical Labs:\n- Simulating a hardware MMU with 2-level page table translation.",
                    "5 Hours"));
            phases.add(phase("Phase 5: Virtual Memory, Demand Paging & Page Replacement Algorithms",
                    "Master demand paging, page fault handling, working set model, thrashing, and page replacement strategies (FIFO, LRU, Optimal, Clock).",
                    "Systems Engineering:\n- Demand paging mechanics and page fault trap handling sequence\n- Page replacement policies: FIFO (and Belady's Anomaly), Optimal (OPT / MIN), LRU\n- Clock (Second-Chance) algorithm and aging registers\n- Thrashing phenomenon and CPU utilization collapse\n- Working-Set Model and Page Fault Frequency (PFF) control\n\nPractical Labs:\n- Comparative benchmarking of LRU vs Clock page replacement policies.",
                    "5 Hours"));
            phases.add(phase("Phase 6: File System Implementation, Storage I/O & OS Security",
                    "Master inode data structures, disk allocation (contiguous, linked, indexed), RAID configurations, and access control matrices.",
                    "Systems Engineering:\n- File system layout: superblocks, inode tables, and directory dentries\n- Allocation strategies: Contiguous, Linked list, and Multi-level Indexed Allocation (ext4/UFS)\n- Free-space management: bit vectors and linked free lists\n- Disk arm scheduling: FCFS, SSTF, SCAN, C-SCAN, and LOOK\n- RAID levels: RAID 0 (striping), RAID 1 (mirroring), RAID 5 (distributed parity)\n- Principle of least privilege, access control lists (ACL), and capabilities\n\nPractical Labs:\n- Writing a custom user-space file system (FUSE) in C.",
                    "5 Hours"));
        } else if (s.contains("database") || s.contains("dbms") || s.contains("sql")) {
            phases.add(phase("Phase 1: Relational Data Model, ER Modeling & Relational Algebra",
                    "Master three-schema architecture, entity-relationship diagrams (ER), mapping ER to relational tables, and formal relational algebra.",
                    "Database Engineering:\n- Database system architecture: internal, conceptual, and external schemas\n- Relational model: relations, tuples, attributes, domains, primary keys, and foreign keys\n- Entity-Relationship (ER) and Extended ER (EER) modeling: cardinalities, weak entities\n- Relational Algebra operations: Selection (sigma), Projection (pi), Cartesian Product, Set Union, Difference\n- Join operations: Theta join, Equijoin, Natural join, and Outer joins\n\nPractical Applications:\n- Converting enterprise university schema from ER diagram to DDL tables.",
                    "4-5 Hours"));
            phases.add(phase("Phase 2: Advanced SQL Querying, Subqueries & Complex Joins",
                    "Master DDL, DML, complex SELECT queries, correlated subqueries, aggregate grouping, window functions, and database views.",
                    "Database Engineering:\n- SQL Data Definition: CREATE, ALTER, DROP, and integrity constraints (CHECK, UNIQUE, NOT NULL, REFERENCES)\n- Aggregate queries: GROUP BY, HAVING, and statistical window functions (ROW_NUMBER, DENSE_RANK)\n- Correlated vs non-correlated subqueries and EXISTS/IN predicates\n- Multi-table joins: INNER, LEFT OUTER, RIGHT OUTER, FULL OUTER, and CROSS joins\n- Materialized views, user-defined functions (UDF), and stored procedures\n\nPractical Applications:\n- Formulating multi-join enterprise revenue reporting queries.",
                    "5 Hours"));
            phases.add(phase("Phase 3: Database Normalization & Functional Dependency Theory",
                    "Understand functional dependencies, Armstrong's axioms, closure sets, canonical cover, and normalization (1NF, 2NF, 3NF, BCNF).",
                    "Database Engineering:\n- Functional dependencies (FDs) X -> Y and Armstrong's Inference Axioms\n- Attribute closure calculation (X+) and finding minimal canonical cover of FDs\n- Lossless-join decomposition theorem and dependency preservation property\n- Normal forms: 1NF (atomic attributes), 2NF (elimination of partial dependencies), 3NF (elimination of transitive dependencies)\n- Boyce-Codd Normal Form (BCNF): every determinant must be a superkey\n\nPractical Applications:\n- Decomposing an unnormalized hospital management database to BCNF.",
                    "5 Hours"));
            phases.add(phase("Phase 4: Transaction Processing, ACID Properties & Concurrency Schedules",
                    "Master transaction states, ACID guarantees, serializability, conflict serializability, precedence graphs, and recoverability.",
                    "Database Engineering:\n- Transaction states: Active, Partially Committed, Failed, Aborted, Committed\n- ACID Properties: Atomicity (WAL logging), Consistency, Isolation, Durability\n- Concurrent transaction execution schedules: serial vs non-serial schedules\n- Conflict serializability: identifying conflicting read/write operations\n- Precedence (serialization) graph cycle detection algorithm\n- View serializability and blind writes\n- Cascading rollbacks and recoverable vs cascadeless schedules\n\nPractical Applications:\n- Testing transaction schedules for conflict serializability.",
                    "5 Hours"));
            phases.add(phase("Phase 5: Concurrency Control Protocols & Deadlock Handling in DBMS",
                    "Master lock-based protocols (2PL, Strict 2PL), timestamp ordering protocols, validation protocols, and deadlock management.",
                    "Database Engineering:\n- Shared (S) and Exclusive (X) lock compatibility matrices\n- Two-Phase Locking (2PL) protocol: growing phase and shrinking phase\n- Strict 2PL and Rigorous 2PL for preventing dirty reads and cascading rollbacks\n- Lock conversion and hierarchical Granular Locking (Intention locks: IS, IX, SIX)\n- Timestamp-based concurrency ordering (Thomas Write Rule)\n- Wait-Die and Wound-Wait deadlock prevention schemes\n\nPractical Applications:\n- Configuring transaction isolation levels (Read Committed vs Serializable) in PostgreSQL.",
                    "5 Hours"));
            phases.add(phase("Phase 6: Storage Architectures, Indexing B/B+ Trees & Query Optimization",
                    "Master file organization, dense vs sparse indexes, B-Tree and B+ Tree mechanics, hashing, and cost-based query optimization.",
                    "Database Engineering:\n- Physical storage hierarchy, block storage, and slotted page architecture\n- Primary, secondary, clustering, and composite indexing structures\n- B-Tree and B+ Tree structures: node capacity, insertion splitting, and deletion merging\n- Hash indexing, static vs extendible hashing, and directory doubling\n- Query optimization pipeline: SQL parsing, relational algebra equivalence trees, and cost estimation\n\nPractical Applications:\n- Profiling PostgreSQL EXPLAIN ANALYZE execution plans and indexing strategy.",
                    "5-6 Hours"));
        } else if (s.contains("network") && !s.contains("neural")) {
            phases.add(phase("Phase 1: Computer Network Architecture, OSI & TCP/IP Reference Models",
                    "Understand network topologies, circuit switching vs packet switching, delay/jitter/loss metrics, and layered protocol architectures.",
                    "Network Architecture:\n- Network topologies: mesh, star, bus, ring, and hybrid topologies\n- Circuit switching vs Packet switching (datagram vs virtual circuit)\n- Network delay components: transmission, propagation, queuing, and processing delays\n- 7-Layer OSI model vs 4-Layer TCP/IP model: encapsulation and decapsulation\n- Physical layer transmission media: twisted pair, coaxial, optical fiber, and wireless\n\nPractical Labs:\n- Packet sniffing with Wireshark to inspect layered protocol headers.",
                    "4-5 Hours"));
            phases.add(phase("Phase 2: Data Link Layer: Framing, Error Detection & Sliding Window MAC",
                    "Master framing techniques, CRC polynomial error detection, Hamming code, Stop-and-Wait, Go-Back-N, Selective Repeat, and Ethernet MAC.",
                    "Network Architecture:\n- Framing: character count, byte stuffing, and bit stuffing (flag byte 01111110)\n- Error detection: parity check, checksum, and Cyclic Redundancy Check (CRC polynomial division)\n- Sliding window protocols: Stop-and-Wait ARQ, Go-Back-N ARQ (window size 2^n - 1), and Selective Repeat ARQ\n- Media Access Control (MAC): pure ALOHA, slotted ALOHA throughput derivation\n- CSMA/CD (Carrier Sense Multiple Access with Collision Detection) and binary exponential backoff\n- Ethernet IEEE 802.3 frame structure and MAC address resolution\n\nPractical Labs:\n- Implementing Go-Back-N ARQ sliding window simulator in Python.",
                    "5 Hours"));
            phases.add(phase("Phase 3: Network Layer: IPv4/IPv6 Addressing, Subnetting & CIDR",
                    "Master classful vs classless IP addressing (CIDR), variable length subnet masks (VLSM), NAT, ICMP, and ARP.",
                    "Network Architecture:\n- IPv4 datagram header format (TTL, fragmentation flags, checksum)\n- Classful addressing (Class A, B, C, D) limitations and CIDR notation (/24, /28)\n- Subnetting design: calculating network address, broadcast address, and host ranges\n- Address Resolution Protocol (ARP) cache and Reverse ARP\n- Network Address Translation (NAT) and Port Address Translation (PAT)\n- Internet Control Message Protocol (ICMP) ping and traceroute mechanics\n- IPv6 128-bit address architecture and transition mechanisms (dual-stack, tunneling)\n\nPractical Labs:\n- Designing enterprise subnet allocation across 4 department branch offices.",
                    "5 Hours"));
            phases.add(phase("Phase 4: Routing Algorithms: Distance Vector, Link State & BGP",
                    "Understand intra-domain and inter-domain routing, Bellman-Ford algorithm, Dijkstra link-state routing, OSPF, and BGP.",
                    "Network Architecture:\n- Static vs dynamic routing paradigms\n- Distance Vector Routing: Bellman-Ford algorithm, count-to-infinity problem, and split horizon/poison reverse\n- Link State Routing: Dijkstra's shortest path algorithm and Open Shortest Path First (OSPF)\n- Interior Gateway Protocols (IGP) vs Exterior Gateway Protocols (EGP)\n- Border Gateway Protocol (BGP): autonomous systems, path vector routing, and peering\n\nPractical Labs:\n- Configuring multi-router OSPF network in Cisco Packet Tracer / GNS3.",
                    "5-6 Hours"));
            phases.add(phase("Phase 5: Transport Layer: TCP Reliability, Flow Control & Congestion Management",
                    "Master UDP datagrams, TCP segment format, 3-way handshake, sliding window flow control, and Tahoe/Reno congestion avoidance.",
                    "Network Architecture:\n- Multiplexing and demultiplexing via port numbers (well-known ports 0-1023)\n- User Datagram Protocol (UDP): lightweight connectionless transmission and checksum\n- Transmission Control Protocol (TCP): byte-stream orientation, sequence numbers, and ACKs\n- TCP connection establishment (3-way SYN/ACK handshake) and 4-way termination (FIN/ACK)\n- Flow control via TCP Receive Window (rwnd) and silly window syndrome\n- Congestion control mechanics: Slow Start, Congestion Avoidance, Fast Retransmit, and Fast Recovery\n\nPractical Labs:\n- Building a multi-client socket chat server using TCP sockets in C.",
                    "5 Hours"));
            phases.add(phase("Phase 6: Application Layer Protocols, DNS, HTTP & Network Security",
                    "Master Domain Name System (DNS), HTTP/1.1 vs HTTP/2/3, TLS/SSL handshake, asymmetric cryptography, and firewalls.",
                    "Network Architecture:\n- DNS hierarchical namespace, root servers, iterative vs recursive queries, and resource records\n- Hypertext Transfer Protocol: HTTP/1.1 persistent connections, HTTP/2 multiplexing, HTTP/3 QUIC\n- Email architecture: SMTP, POP3, and IMAP protocols\n- Network security: symmetric (AES) vs asymmetric (RSA) cryptography\n- Transport Layer Security (TLS/SSL) cryptographic handshake and digital certificates (PKI)\n- Firewalls: packet filtering, stateful inspection, and DMZ network topology\n\nPractical Labs:\n- Capturing and inspecting TLS 1.3 cryptographic handshake in Wireshark.",
                    "5 Hours"));
        } else if (s.contains("environment") || s.contains("ecology") || s.contains("pollution")) {
            phases.add(phase("Phase 1: Ecosystem Dynamics, Biogeochemical Cycles & Biodiversity",
                    "Understand ecological hierarchy, energy flow, 10% trophic efficiency rule, carbon/nitrogen cycles, and biodiversity conservation.",
                    "Environmental Science:\n- Ecosystem structure: abiotic and biotic components, food chains, food webs, and ecological pyramids\n- Energy flow thermodynamics in ecosystems: Lindeman's 10% efficiency rule\n- Biogeochemical cycles: Carbon cycle, Nitrogen cycle (fixation, nitrification, denitrification), and Phosphorus cycle\n- Ecological succession: primary vs secondary succession, pioneer and climax communities\n- Biodiversity: genetic, species, and ecosystem diversity; biodiversity hotspots in India\n- In-situ (national parks, wildlife sanctuaries) vs Ex-situ (zoological parks, seed banks) conservation\n\nPractical Case Studies:\n- Impact of habitat fragmentation on Western Ghats biodiversity hotspot.",
                    "4-5 Hours"));
            phases.add(phase("Phase 2: Natural Energy Resources & Renewable Energy Systems",
                    "Evaluate renewable vs non-renewable energy resources, solar photovoltaic, wind energy, biomass, geothermal, and mineral conservation.",
                    "Environmental Science:\n- Depletion of fossil fuels: coal, crude oil, and natural gas environmental impacts\n- Solar energy: photovoltaic conversion principle, solar thermal collectors, and solar cell efficiency\n- Wind energy: Betz limit, wind turbine aerodynamics, and site assessment\n- Biomass energy: anaerobic digestion, biogas generation, and biofuel production\n- Geothermal and ocean energy (tidal and wave power)\n- Sustainable resource management and circular economy frameworks\n\nPractical Case Studies:\n- Techno-economic feasibility of grid-connected 1 MW solar PV plant.",
                    "4-5 Hours"));
            phases.add(phase("Phase 3: Air Pollution, Atmospheric Chemistry & Emissions Control",
                    "Analyze primary and secondary pollutants, photochemical smog, acid rain, greenhouse effect, and industrial air pollution control devices.",
                    "Environmental Science:\n- Criteria air pollutants: PM2.5, PM10, SOx, NOx, CO, and volatile organic compounds (VOCs)\n- Atmospheric dispersion: lapse rates, temperature inversion, and plume behavior (looping, coning, fanning)\n- Photochemical smog chemistry: ozone formation via NOx and hydrocarbons\n- Acid rain mechanisms and ecological damage to aquatic ecosystems and monuments\n- Control technologies: gravitational settling chambers, cyclone separators, fabric filters, and electrostatic precipitators (ESP)\n- National Ambient Air Quality Standards (NAAQS) and Air Quality Index (AQI)\n\nPractical Case Studies:\n- Sizing an electrostatic precipitator for thermal power plant flue gas.",
                    "5 Hours"));
            phases.add(phase("Phase 4: Water Quality, Wastewater Treatment & Effluent Guidelines",
                    "Master physical/chemical water quality parameters, BOD, COD, DO sag curve, activated sludge process, and advanced tertiary treatment.",
                    "Environmental Science:\n- Water quality parameters: turbidity, pH, total dissolved solids (TDS), hardness, and heavy metals\n- Biochemical Oxygen Demand (BOD5 at 20 deg C) and Chemical Oxygen Demand (COD) testing protocols\n- Dissolved Oxygen (DO) sag curve and Streeter-Phelps oxygen balance equation in rivers\n- Wastewater treatment train: Primary (screening, grit chamber, primary sedimentation)\n- Secondary biological treatment: Activated Sludge Process (ASP), trickling filters, and sequencing batch reactors (SBR)\n- Tertiary treatment: coagulation-flocculation, chlorination, and reverse osmosis (RO) membrane filtration\n\nPractical Case Studies:\n- Design calculation of municipal sewage treatment plant (STP) aeration tank volume.",
                    "5 Hours"));
            phases.add(phase("Phase 5: Municipal Solid Waste & Hazardous Waste Management",
                    "Master solid waste generation, 3R principle, sanitary landfill design, composting, incineration, e-waste, and hazardous waste handling.",
                    "Environmental Science:\n- Solid waste classification: municipal, industrial, biomedical, electronic, and hazardous wastes\n- Integrated Solid Waste Management (ISWM) and the 3R Hierarchy (Reduce, Reuse, Recycle)\n- Waste processing: mechanical sorting, aerobic windrow composting, and vermicomposting\n- Thermal conversion technologies: incineration with energy recovery and pyrolysis/gasification\n- Engineered sanitary landfills: bottom liner systems (geomembrane/clay), leachate collection, and landfill gas extraction\n- Biomedical waste color-coding categories and E-waste management rules\n\nPractical Case Studies:\n- Designing landfill liner and leachate collection system for a tier-2 city.",
                    "5 Hours"));
            phases.add(phase("Phase 6: Climate Change, Environmental Impact Assessment (EIA) & Environmental Law",
                    "Evaluate global warming mechanisms, IPCC carbon budgets, carbon credits, Environmental Impact Assessment steps, and environmental legislation.",
                    "Environmental Science:\n- Global warming potential (GWP) of greenhouse gases (CO2, CH4, N2O, fluorinated gases)\n- IPCC climate scenarios, sea-level rise, and ocean acidification\n- Carbon footprint calculation, Kyoto Protocol, and Paris Climate Agreement goals\n- Environmental Impact Assessment (EIA) notification process: Screening, Scoping, Baseline data, Impact Prediction, Public Hearing, and EMP\n- Indian Environmental Legislation: Water Act (1974), Air Act (1981), Environment Protection Act (1986)\n- Central and State Pollution Control Board (CPCB/SPCB) enforcement mechanisms\n\nPractical Case Studies:\n- Comprehensive EIA baseline report for an upcoming greenfield highway project.",
                    "5-6 Hours"));
        } else if (s.contains("thermodynamic") || s.contains("heat transfer")) {
            phases.add(phase("Phase 1: Fundamental Thermodynamic Laws, Energy Conservation & Pure Substances",
                    "Master macroscopic vs microscopic perspectives, thermodynamic equilibrium, Zeroth and First Laws, enthalpy, and P-V-T surfaces of pure substances.",
                    "Thermal Engineering:\n- Thermodynamic systems: closed, open (control volume), and isolated systems\n- Properties: intensive vs extensive; state, path, and quasi-static processes\n- Zeroth Law of Thermodynamics and temperature measurement scales\n- First Law of Thermodynamics for closed system undergoing a process: Q - W = Delta_U\n- Steady Flow Energy Equation (SFEE) applied to nozzles, diffusers, turbines, and compressors\n- Phase change of pure substances (water): saturation curve, critical point, dryness fraction x, and steam tables\n\nSolved Problems:\n- SFEE calculation of steam turbine power output.",
                    "5 Hours"));
            phases.add(phase("Phase 2: Second Law of Thermodynamics, Heat Engines, Carnot Cycle & Entropy",
                    "Analyze heat engines, heat pumps, refrigerators, Kelvin-Planck and Clausius statements, Carnot theorem, and Clausius inequality.",
                    "Thermal Engineering:\n- Thermal energy reservoirs, heat engine thermal efficiency eta = W_net / Q_H\n- Refrigerator COP_R = Q_L / W_in and Heat Pump COP_HP = 1 + COP_R\n- Kelvin-Planck and Clausius statements of Second Law and proof of their equivalence\n- Reversibility criteria and Carnot cycle efficiency: eta_carnot = 1 - T_L / T_H\n- Clausius inequality: cyclic integral of dQ/T <= 0\n- Definition of entropy S, principle of increase of entropy, and T-s / h-s Mollier diagrams\n\nSolved Problems:\n- Entropy generation calculation for irreversible heat exchange.",
                    "5 Hours"));
            phases.add(phase("Phase 3: Gas Power Cycles & Steam Rankine Vapor Cycles",
                    "Analyze air standard cycles (Otto, Diesel, Dual) and vapor power Rankine cycles with reheat and regeneration.",
                    "Thermal Engineering:\n- Air standard assumptions for internal combustion engines\n- Otto cycle (constant volume heat addition): thermal efficiency derivation in terms of compression ratio r\n- Diesel cycle (constant pressure heat addition) and cutoff ratio\n- Dual combustion cycle comparison at same peak pressure and heat input\n- Simple Rankine vapor cycle: boiler, turbine, condenser, pump processes\n- Rankine cycle efficiency improvements: Superheat, Reheat Rankine cycle, and Regenerative feed heating\n\nSolved Problems:\n- Thermal efficiency and specific steam consumption of a 500 MW reheat thermal power station.",
                    "5-6 Hours"));
            phases.add(phase("Phase 4: Steady-State Heat Conduction & Extended Surfaces (Fins)",
                    "Master Fourier's law of thermal conduction, general conduction equation in Cartesian/cylindrical coordinates, thermal resistance networks, and fins.",
                    "Thermal Engineering:\n- Fourier's law of heat conduction: q = -k * A * (dT/dx)\n- Derivation of general 3D heat conduction equation with internal heat generation\n- 1D steady conduction through composite flat walls and cylindrical pipe insulation\n- Critical radius of insulation: r_cr = k / h for cylinders, 2k / h for spheres\n- Heat transfer through extended surfaces (fins): governing differential equation\n- Fin efficiency and fin effectiveness for rectangular and pin fins\n\nSolved Problems:\n- Sizing economic insulation thickness for a high-pressure steam pipe.",
                    "5 Hours"));
            phases.add(phase("Phase 5: Convective Heat Transfer & Boundary Layer Hydrodynamics",
                    "Understand Newton's law of cooling, forced vs natural convection, boundary layers, and dimensionless numbers (Nusselt, Prandtl, Reynolds, Grashof).",
                    "Thermal Engineering:\n- Newton's law of cooling: q = h * A * (T_s - T_inf)\n- Hydrodynamic and thermal boundary layers over flat plates and inside tubes\n- Dimensionless numbers: Reynolds (Re), Prandtl (Pr), Nusselt (Nu), and Grashof (Gr)\n- Forced convection empirical correlations for laminar and turbulent flow over flat plates and cylinders\n- Free (natural) convection over vertical and horizontal plates (Rayleigh number Ra = Gr * Pr)\n\nSolved Problems:\n- Calculating convection heat loss from an uninsulated boiler wall.",
                    "5 Hours"));
            phases.add(phase("Phase 6: Radiation Heat Transfer & Heat Exchanger Design",
                    "Master thermal radiation laws, blackbody emission, shape factor algebra, radiation shields, and LMTD/NTU heat exchanger design.",
                    "Thermal Engineering:\n- Electromagnetic radiation spectrum and blackbody definition\n- Stefan-Boltzmann law (E_b = sigma * T^4), Planck's law, and Wien's displacement law\n- Surface properties: emissivity, absorptivity, reflectivity, transmissivity, and Kirchhoff's law\n- Radiation view factor (shape factor) reciprocity and summation relations\n- Radiation exchange between diffuse gray surfaces with radiation shields\n- Heat exchangers: parallel flow, counter flow, shell-and-tube, and cross flow\n- Logarithmic Mean Temperature Difference (LMTD) method and Effectiveness-NTU method\n\nSolved Problems:\n- Sizing surface area of a counter-flow shell-and-tube water-to-water heat exchanger.",
                    "5-6 Hours"));
        } else if (s.contains("python")) {
            phases.add(phase("Phase 1: Python Core Foundations & Data Types", "Master variables, dynamic typing, primitive data types (int, float, str, bool), memory references, and operators.", "Detailed syntax, memory model, references vs values, dynamic typing, and standard I/O.", "3-4 Hours"));
            phases.add(phase("Phase 2: Control Structures & Functional Programming", "Master if-elif-else branching, for/while loops, break/continue, functions, scope, and *args/**kwargs.", "Control flow, lambda functions, first-class functions, recursion, and namespace resolution.", "4 Hours"));
            phases.add(phase("Phase 3: Data Structures (Lists, Tuples, Sets, Dicts)", "In-depth hashing, indexing, slicing, comprehensions, key-value lookup complexity, and immutability.", "Hash tables, dictionary collisions, set operations, list slicing, and memory overhead.", "4 Hours"));
            phases.add(phase("Phase 4: Object-Oriented Python & Design Principles", "Classes, objects, constructors (__init__), inheritance, polymorphism, encapsulation, and magic methods.", "Class definitions, self keyword, method resolution order (MRO), dunder methods, and dataclasses.", "4 Hours"));
            phases.add(phase("Phase 5: File I/O, Exception Handling & Context Managers", "Try-except-finally blocks, custom exceptions, reading/writing files using 'with' statement, and JSON parsing.", "Exception hierarchy, custom error classes, resource management with 'with', and json serialization.", "4 Hours"));
            phases.add(phase("Phase 6: Advanced Python: Iterators, Generators & Decorators", "Higher-order functions, function decorators, lazy generator evaluation with yield, and memory efficiency.", "Iterables, __iter__ and __next__, generator expressions, memory efficiency, and closure decorators.", "5 Hours"));
            phases.add(phase("Phase 7: Practical Libraries: NumPy, Pandas & REST APIs", "Vectorized array manipulation, data frames, HTTP requests handling, and modular packaging.", "NumPy ndarrays, vectorization, Pandas Series/DataFrames, and HTTP client requests.", "5 Hours"));
            phases.add(phase("Phase 8: Production Projects & Concurrency", "Multithreading, asyncio event loops, testing with pytest, and enterprise application structuring.", "GIL constraints, multiprocessing vs threading, asyncio event loop, and pytest unit testing.", "5 Hours"));
        } else {
            // DYNAMIC GENERAL ENGINEERING CURRICULUM SYNTHESIS
            // Adapts phase titles and study material to the actual academic domain, NOT generic computing!
            String capitalized = rawSubject != null ? rawSubject.trim() : "Engineering Domain";
            boolean isComputing = branch.contains("CSE") || branch.contains("IT") || branch.contains("AI") || s.contains("software") || s.contains("program") || s.contains("data");
            boolean isCivilOrMech = branch.contains("CIVIL") || branch.contains("MECH") || s.contains("survey") || s.contains("concrete") || s.contains("structure") || s.contains("machine") || s.contains("material");
            boolean isElectrical = branch.contains("EEE") || branch.contains("ECE") || s.contains("circuit") || s.contains("signal") || s.contains("power") || s.contains("communication");

            if (isCivilOrMech) {
                phases.add(phase("Phase 1: Foundations, Physical Principles & Governing Laws of " + capitalized,
                        "Establish the theoretical axioms, physical governing laws, standard material units, and structural definitions of " + capitalized + ".",
                        "Physical Principles:\n- Static and dynamic force balances, equilibrium equations, and physical units\n- Standard material properties, stress-strain behavior, and boundary constraints\n- Historical evolution and fundamental analytical models.",
                        "4 Hours"));
                phases.add(phase("Phase 2: Mathematical Formulations & Analytical Derivations in " + capitalized,
                        "Derive core mathematical formulations, differential equations, and analytical load-bearing solutions in " + capitalized + ".",
                        "Analytical Formulations:\n- Differential equations of equilibrium and deformation compatibility\n- Closed-form analytical solutions and boundary value formulations\n- Step-by-step solved engineering problems.",
                        "4-5 Hours"));
                phases.add(phase("Phase 3: Laboratory Techniques, Empirical Testing & Experimental Measurement",
                        "Master physical measurement tools, experimental testing apparatus, and empirical verification methodologies for " + capitalized + ".",
                        "Laboratory Techniques:\n- Standard experimental apparatus, instrumentation, and sensor calibration\n- Material testing protocols adhering to ASTM/BIS standards\n- Empirical error analysis and data logging.",
                        "4-5 Hours"));
                phases.add(phase("Phase 4: Structural System Modeling & Multi-Variable Stress Analysis",
                        "Evaluate behavior under complex combined loading, multi-variable constraints, and operational safety criteria in " + capitalized + ".",
                        "System Modeling:\n- Complex combined loading (axial, shear, bending, torsion)\n- Safety factor margins, failure criteria (Tresca, Von Mises), and fatigue limits\n- Numerical modeling and computational simulations.",
                        "5 Hours"));
                phases.add(phase("Phase 5: Design Codes, Optimization & Failure Prevention Strategies",
                        "Apply national design codes, material optimization, safety factors, and failure prevention strategies in " + capitalized + ".",
                        "Design Codes & Standards:\n- Regulatory safety standards and limit state design parameters\n- Material efficiency, lifecycle durability, and environmental degradation mitigation\n- Comparative structural trade-off matrices.",
                        "5 Hours"));
                phases.add(phase("Phase 6: Engineering Capstone Project & Real-World Infrastructure Case Studies",
                        "Synthesize learned concepts into an end-to-end practical engineering design project adhering to industry standards.",
                        "Capstone Project:\n- Full engineering design specification and architectural validation\n- Structural drawing interpretation, cost estimation, and technical documentation\n- Failure analysis of historical case studies.",
                        "5-6 Hours"));
            } else if (isElectrical) {
                phases.add(phase("Phase 1: Electromagnetic Principles & Foundational Equations of " + capitalized,
                        "Establish electromagnetic laws, physical circuit axioms, and mathematical frameworks of " + capitalized + ".",
                        "Electromagnetic Foundations:\n- Field equations, potential distributions, and circuit law derivations\n- Electrical parameters, passive/active component models, and unit standards\n- Foundational theorems and baseline analytical frameworks.",
                        "4 Hours"));
                phases.add(phase("Phase 2: Signal Analysis, Transform Methods & Network Formulations",
                        "Apply mathematical transforms (Fourier, Laplace, Z), frequency-domain analysis, and state-space equations in " + capitalized + ".",
                        "Analytical Formulations:\n- Continuous and discrete frequency response and Bode plots\n- Transient and steady-state differential equations\n- Network theorems and multi-terminal parameter matrices.",
                        "4-5 Hours"));
                phases.add(phase("Phase 3: Hardware Simulation, Prototyping & Laboratory Instrumentation",
                        "Master oscilloscopes, spectrum analyzers, SPICE simulation tools, and experimental circuit testing in " + capitalized + ".",
                        "Laboratory Techniques:\n- Test bench instrumentation, signal generators, and digital logic analyzers\n- SPICE circuit modeling and schematic capture\n- Signal-to-noise ratio (SNR) optimization and shielding.",
                        "4-5 Hours"));
                phases.add(phase("Phase 4: Component Integration, Amplification & Feedback Systems",
                        "Design integrated sub-systems, feedback loops, stability margins, and power efficiency stages in " + capitalized + ".",
                        "System Integration:\n- Closed-loop feedback stability (Nyquist, Routh-Hurwitz criteria)\n- Active amplification stages and filter design\n- Thermal dissipation, heat sinking, and power integrity.",
                        "5 Hours"));
                phases.add(phase("Phase 5: Digital Control, Embedded Firmware & Telemetry",
                        "Implement microcontroller interfacing, digital control algorithms, and telemetric data conversion in " + capitalized + ".",
                        "Control & Embedded Systems:\n- ADC/DAC conversion resolution, sampling theorem, and aliasing\n- PWM generation, microcontroller register configuration, and interrupts\n- Communication buses (I2C, SPI, CAN).",
                        "5 Hours"));
                phases.add(phase("Phase 6: Comprehensive Capstone System Design & Hardware Case Studies",
                        "Synthesize learned principles into an end-to-end operational electrical/electronic engineering project.",
                        "Capstone Project:\n- Full system design specification, PCB layout guidelines, and bill of materials\n- Verification metrics, safety compliance, and power efficiency benchmarking\n- Diagnostic troubleshooting and technical reporting.",
                        "5-6 Hours"));
            } else {
                phases.add(phase("Phase 1: Foundations, Principles & Core Terminology of " + capitalized,
                        "Establish a rigorous academic foundation, core terminology, theoretical axioms, and fundamental frameworks of " + capitalized + " for " + branch + " (" + year + ").",
                        "Comprehensive Foundations:\n- Historical evolution, terminology, and core axioms of " + capitalized + "\n- Standard notation, units, and structural definitions\n- Essential theoretical principles and baseline analytical models\n\nEngineering Context:\n- Real-world applications in " + branch + " engineering.",
                        "4 Hours"));
                phases.add(phase("Phase 2: Theoretical Models & Analytical Formulations of " + capitalized,
                        "Derive core mathematical and physical governing equations, analytical models, and fundamental behavior patterns in " + capitalized + ".",
                        "Theoretical Analysis:\n- Governing physical equations and mathematical formulations\n- Analytical parameter derivation and boundary conditions\n- Systematic derivations and step-by-step solved engineering problems.",
                        "4-5 Hours"));
                phases.add(phase("Phase 3: Applied Methodologies & Practical Techniques in " + capitalized,
                        "Apply core domain methodologies, standard tools, measurement techniques, and practical execution algorithms in " + capitalized + ".",
                        "Methodologies & Techniques:\n- Standard laboratory and computational tools\n- Practical problem-solving methodologies\n- Implementation algorithms and step-by-step engineering workflows.",
                        "4-5 Hours"));
                phases.add(phase("Phase 4: Intermediate Systems & Problem Solving in " + capitalized,
                        "Solve multi-step, multi-variable engineering challenges, model inter-system interactions, and analyze behavior under varied constraints in " + capitalized + ".",
                        "System-Level Modeling:\n- Multi-variable system equations and parameter sensitivity\n- Error analysis, constraint satisfaction, and failure mode identification\n- Structured engineering case studies.",
                        "5 Hours"));
                phases.add(phase("Phase 5: Advanced Optimization, Design Patterns & Trade-offs in " + capitalized,
                        "Evaluate architectural design trade-offs, performance efficiency, cost-benefit trade-offs, and optimization strategies in " + capitalized + ".",
                        "Advanced Synthesis:\n- Performance optimization algorithms and trade-off matrices\n- Industry standards, safety factors, and failure prevention\n- Comparative analysis of state-of-the-art designs.",
                        "5 Hours"));
                phases.add(phase("Phase 6: Capstone Engineering Project & Real-World Case Studies in " + capitalized,
                        "Synthesize learned concepts into an end-to-end practical capstone engineering project adhering to industry standards and best practices.",
                        "Capstone Synthesis:\n- Comprehensive problem specification, design validation, and implementation\n- Verification metrics, automated testing, and performance profiling\n- Comprehensive technical documentation.",
                        "5-6 Hours"));
            }
        }

        return phases;
    }

    private static Map<String, Object> phase(String title, String objectives, String studyMaterial, String duration) {
        Map<String, Object> m = new HashMap<>();
        m.put("title", title);
        m.put("objectives", objectives);
        m.put("studyMaterial", studyMaterial);
        m.put("duration", duration);
        return m;
    }
}
