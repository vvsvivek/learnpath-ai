// Academic and Department Registry Configuration
export const BRANCH_AREAS_OF_INTEREST: Record<string, string[]> = {
  CSE: [
    'Artificial Intelligence & Machine Learning',
    'Full Stack Web Development',
    'Cybersecurity & Ethical Hacking',
    'Cloud Computing & DevOps',
    'Data Science & Big Data',
    'Mobile Application Development',
  ],
  IT: [
    'Cloud Infrastructure & DevOps',
    'Enterprise Web Applications',
    'Information & Network Security',
    'Mobile Application Engineering',
    'Big Data & Data Engineering',
    'Artificial Intelligence & NLP',
  ],
  ECE: [
    'Embedded Systems & IoT',
    'VLSI Design & Chip Architecture',
    'Robotics & Industrial Automation',
    'Digital Signal Processing & Telecom',
    'Wireless & 5G Communications',
    'Sensors & Biomedical Instrumentation',
  ],
  EEE: [
    'Electric Vehicles & Powertrains',
    'Renewable Energy & Smart Grids',
    'Industrial Automation & PLC',
    'Power Systems Protection & Switchgear',
    'Control Systems & Power Electronics',
    'Embedded IoT for Energy Systems',
  ],
  MECH: [
    'Robotics & Mechatronics',
    'CAD/CAM & FEA Simulation',
    'Automobile & EV Dynamics',
    'Thermal Engineering & Aerodynamics',
    'Advanced Manufacturing & 3D Printing',
    'Computational Fluid Dynamics (CFD)',
  ],
  CIVIL: [
    'Structural Engineering & BIM',
    'Geotechnical & Foundation Engineering',
    'Environmental Engineering & Smart Cities',
    'Transportation & Highway Infrastructure',
    'Construction Project Management & GIS',
    'Water Resources & Hydraulic Modeling',
  ],
  AIML: [
    'Generative AI & LLMs',
    'Deep Learning & Computer Vision',
    'Natural Language Processing (NLP)',
    'Autonomous Systems & Reinforcement Learning',
    'MLOps & Production Model Serving',
    'Data Engineering & Big Data Analytics',
  ],
  AIDS: [
    'Data Science & Predictive Analytics',
    'Big Data Engineering with Spark/Kafka',
    'Machine Learning & Deep Learning',
    'Generative AI & Computer Vision',
    'Business Intelligence & Data Visualization',
    'Cloud Data Warehousing & MLOps',
  ],
};

export const getAreasOfInterestForBranch = (branch?: string): string[] => {
  if (!branch) return BRANCH_AREAS_OF_INTEREST.CSE;
  const b = branch.toUpperCase().trim();
  if (BRANCH_AREAS_OF_INTEREST[b]) {
    return BRANCH_AREAS_OF_INTEREST[b];
  }
  for (const key of Object.keys(BRANCH_AREAS_OF_INTEREST)) {
    if (b.includes(key) || key.includes(b)) {
      return BRANCH_AREAS_OF_INTEREST[key];
    }
  }
  return BRANCH_AREAS_OF_INTEREST.CSE;
};

export interface SpecializationCourse {
  id: number;
  title: string;
  description: string;
  difficulty: string;
  duration: string;
  category: string;
  learningObjectives?: string;
  topics?: string;
  prerequisites?: string;
}

export const SPECIALIZATION_COURSE_REGISTRY: Record<string, SpecializationCourse[]> = {
  // AI & ML / Data Science / NLP
  'Artificial Intelligence & Machine Learning': [
    {
      id: 901,
      title: 'Deep Learning & Neural Network Architectures',
      description: 'Backpropagation calculus, CNNs, RNNs, LSTMs, and PyTorch model implementation for modern cognitive AI.',
      difficulty: 'Advanced',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Master neural activation functions and gradients\n• Train convolutional architectures for computer vision\n• Deploy deep models using PyTorch & ONNX',
      topics: 'Perceptrons, Backpropagation, CNNs, RNNs, PyTorch, Model Optimization',
      prerequisites: 'Linear Algebra, Python Programming',
    },
    {
      id: 902,
      title: 'Generative AI, LLMs & Prompt Engineering',
      description: 'Transformer decoder architectures, LoRA fine-tuning, RAG pipelines, and vector databases for production generative apps.',
      difficulty: 'Specialized',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Build production RAG applications with vector indices\n• Fine-tune foundation models with PEFT/LoRA\n• Master prompt security and guardrails',
      topics: 'Transformers, Self-Attention, Vector DBs, LangChain, RAG Pipelines, LoRA',
      prerequisites: 'Python, Basic Machine Learning',
    },
    {
      id: 903,
      title: 'Machine Learning Fundamentals & Scikit-Learn',
      description: 'Supervised and unsupervised learning, regression, decision trees, ensemble methods, and validation metrics.',
      difficulty: 'Intermediate',
      duration: '5 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Implement linear, logistic, and tree ensembles\n• Optimize hyperparameters using cross-validation\n• Evaluate model performance via ROC-AUC and F1-score',
      topics: 'Regression, Classification, K-Means Clustering, Random Forests, Scikit-Learn',
      prerequisites: 'Python, Statistics Basics',
    },
  ],

  'Full Stack Web Development': [
    {
      id: 904,
      title: 'Full-Stack Microservices & Cloud Architectures',
      description: 'RESTful API contracts, microservices choreography, containerization, and distributed database state management.',
      difficulty: 'Advanced',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Design scalable backend services with Express/FastAPI/Spring\n• Containerize multi-tier systems with Docker\n• Implement event-driven messaging with Kafka/RabbitMQ',
      topics: 'REST APIs, Microservices, Docker, Event Queues, Distributed Caching',
      prerequisites: 'JavaScript/TypeScript, Data Structures',
    },
    {
      id: 905,
      title: 'Web Technologies & Interactive UI Development',
      description: 'Modern component-driven architecture, state management, asynchronous data flows, and performance auditing.',
      difficulty: 'Intermediate',
      duration: '5 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Master component lifecycles and modern reactive patterns\n• Implement accessible, high-performance UI components\n• Integrate secure client authentication and caching',
      topics: 'React, TypeScript, CSS Grid, State Stores, Web Performance',
      prerequisites: 'HTML, CSS, JavaScript Basics',
    },
    {
      id: 906,
      title: 'Database Management Systems & High-Performance SQL',
      description: 'Relational schema design, normalization, ACID transactions, B-Tree indexing, and query execution plans.',
      difficulty: 'Intermediate',
      duration: '5 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Author performant relational queries and CTEs\n• Design normalized schemas with constraint integrity\n• Profile and eliminate database execution bottlenecks',
      topics: 'PostgreSQL, Normalization, ACID, Query Optimization, Indexing',
      prerequisites: 'Basic Programming',
    },
  ],

  'Cybersecurity & Ethical Hacking': [
    {
      id: 907,
      title: 'Information & Cyber Security Engineering',
      description: 'Applied cryptography, network penetration testing, common vulnerabilities (OWASP Top 10), and secure system coding.',
      difficulty: 'Specialized',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Audit web applications for security vulnerabilities\n• Implement public key cryptography and secure hashing\n• Configure firewall rules and intrusion detection',
      topics: 'Cryptography, Kali Linux, Penetration Testing, OWASP Top 10, Network Security',
      prerequisites: 'Computer Networks, Linux CLI',
    },
    {
      id: 908,
      title: 'Computer Networks & Protocol Architectures',
      description: 'OSI and TCP/IP stack internals, packet routing protocols, transport-layer congestion control, and network socket programming.',
      difficulty: 'Advanced',
      duration: '5 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Analyze network traffic using Wireshark\n• Program TCP/UDP client-server sockets\n• Configure IP subnetting and routing tables',
      topics: 'TCP/IP, Routing Protocols, DNS, HTTP/3, TLS Handshake, Socket API',
      prerequisites: 'Operating Systems Basics',
    },
    {
      id: 909,
      title: 'Ethical Hacking & Network Defense Systems',
      description: 'Vulnerability assessment, ethical exploit reproduction, reconnaissance techniques, and automated threat defense.',
      difficulty: 'Specialized',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Conduct ethical reconnaissance and port scanning\n• Implement defensive security controls and SIEM alerts\n• Formulate incidence response playbooks',
      topics: 'Nmap, Metasploit, SIEM, Endpoint Security, Incident Handling',
      prerequisites: 'Computer Networks, Linux',
    },
  ],

  'Cloud Computing & DevOps': [
    {
      id: 910,
      title: 'Cloud Infrastructure & DevOps Automation',
      description: 'Docker containerization, Kubernetes container orchestration, continuous integration pipelines, and infrastructure-as-code.',
      difficulty: 'Advanced',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Orchestrate containerized workloads using Kubernetes\n• Author CI/CD pipelines with GitHub Actions / Jenkins\n• Provision declarative cloud resources using Terraform',
      topics: 'Docker, Kubernetes, CI/CD, Terraform, AWS Services, Monitoring',
      prerequisites: 'Linux Administration, Git',
    },
    {
      id: 911,
      title: 'Distributed Systems & Cloud Computing Architectures',
      description: 'Consensus protocols (Raft, Paxos), cloud storage primitives, partitioned replication, and CAP theorem trade-offs.',
      difficulty: 'Specialized',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Evaluate consistency models in distributed data stores\n• Implement fault-tolerant consensus algorithms\n• Design globally distributed, low-latency microservices',
      topics: 'Raft, CAP Theorem, Sharding, Distributed Cache, S3 Architecture',
      prerequisites: 'Operating Systems, Networking',
    },
    {
      id: 912,
      title: 'Enterprise Cloud Architecture & Data Engineering',
      description: 'Distributed data pipelines, event streaming with Kafka, cloud data lakes, and serverless compute paradigms.',
      difficulty: 'Specialized',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Construct fault-tolerant streaming pipelines\n• Deploy serverless functions and event bus handlers\n• Manage cloud data lakehouse storage and queries',
      topics: 'Apache Kafka, Serverless Lambda, Cloud Data Lakes, Snowflake, IAM',
      prerequisites: 'Cloud Basics, Python / Java',
    },
  ],

  'Data Science & Big Data': [
    {
      id: 913,
      title: 'Data Engineering Pipelines & Distributed Computing',
      description: 'Massive data ingestion with Apache Kafka, batch/streaming computation with Apache Spark, and resilient data architectures.',
      difficulty: 'Advanced',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Build distributed analytics pipelines in Apache Spark\n• Stream event logs with guaranteed delivery semantics\n• Architect scalable data schemas for analytics',
      topics: 'Apache Spark, PySpark, Kafka, Data Warehousing, Parquet/Delta',
      prerequisites: 'Python, SQL',
    },
    {
      id: 914,
      title: 'Probability & Statistics for Machine Learning',
      description: 'Bayesian probability theory, probability distributions, hypothesis testing, ANOVA, and maximum likelihood estimation.',
      difficulty: 'Intermediate',
      duration: '5 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Formulate and test statistical hypotheses\n• Compute conditional probabilities and Bayesian priors\n• Apply sampling techniques and confidence intervals',
      topics: 'Probability Distributions, Bayes Theorem, Hypothesis Testing, Central Limit Theorem',
      prerequisites: 'Engineering Mathematics',
    },
    {
      id: 915,
      title: 'Business Intelligence & Data Visualization Mastery',
      description: 'Data storytelling, interactive dashboard authoring, dimensional modeling, and predictive KPI modeling.',
      difficulty: 'Intermediate',
      duration: '4 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Transform raw business records into executive KPI dashboards\n• Model star and snowflake relational schemas\n• Implement predictive trend forecasting models',
      topics: 'Tableau, PowerBI, Dimensional Modeling, DAX, KPI Metrics',
      prerequisites: 'SQL Basics',
    },
  ],

  'Mobile Application Development': [
    {
      id: 916,
      title: 'Cross-Platform Mobile App Development',
      description: 'React Native and Flutter architecture, native device modules, UI responsiveness, and offline state persistence.',
      difficulty: 'Intermediate',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Build native-feel Android & iOS applications from one codebase\n• Integrate camera, geolocation, and hardware sensors\n• Manage offline state and asynchronous data synchronization',
      topics: 'React Native, Flutter, Device APIs, Async Storage, Navigation',
      prerequisites: 'JavaScript or Dart Basics',
    },
    {
      id: 917,
      title: 'Mobile UI/UX Architecture & Gesture Animation',
      description: 'Fluid mobile navigation patterns, reanimated gestures, 60fps performance profiling, and design token systems.',
      difficulty: 'Advanced',
      duration: '5 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Implement physics-based drag and swipe interactions\n• Profile memory leaks and render bottlenecks in mobile apps\n• Build consistent theme systems supporting dark/light mode',
      topics: 'Reanimated, Gesture Handler, Performance Profiling, Theme Tokens',
      prerequisites: 'React Native Basics',
    },
    {
      id: 918,
      title: 'Mobile Cloud Services & Offline Synchronization',
      description: 'Push notification architectures, SQLite local caching, OAuth token refresh flows, and background sync workers.',
      difficulty: 'Advanced',
      duration: '5 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Implement background task schedulers and synchronization\n• Configure Firebase Cloud Messaging push channels\n• Design zero-latency offline-first database caches',
      topics: 'SQLite, FCM Push Notifications, OAuth2, WorkManager, Offline Cache',
      prerequisites: 'Mobile Development Basics',
    },
  ],

  // Core ECE
  'Embedded Systems & IoT': [
    {
      id: 919,
      title: 'Embedded Real-Time Operating Systems (RTOS)',
      description: 'FreeRTOS kernel primitives, preemptive task scheduling, mutexes, semaphores, and interrupt service routines on ARM Cortex-M.',
      difficulty: 'Specialized',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Write concurrent embedded firmware using FreeRTOS\n• Prevent priority inversions and deadlocks in microcontroller code\n• Interface high-speed sensors via DMA, SPI, and I2C',
      topics: 'FreeRTOS, Task Schedulers, Semaphores, ISR, DMA, ARM Cortex-M',
      prerequisites: 'C Programming, Microcontrollers',
    },
    {
      id: 920,
      title: 'Microprocessors, Microcontrollers & Interfacing',
      description: 'ARM and 8086 processor architectures, memory mapping, interrupt vectors, and peripheral communication buses.',
      difficulty: 'Advanced',
      duration: '5 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Program bare-metal register-level device drivers\n• Interface LCD, ADC, DAC, and keypad peripherals\n• Optimize memory footprint in resource-constrained MCUs',
      topics: 'ARM Architecture, Memory Interfacing, Timers, Interrupts, GPIO',
      prerequisites: 'Digital Logic Design',
    },
    {
      id: 921,
      title: 'Internet of Things (IoT) Protocols & Sensor Networks',
      description: 'Low-power mesh networking, MQTT, CoAP, Zigbee, LoRaWAN, and cloud telemetry ingestion architectures.',
      difficulty: 'Intermediate',
      duration: '5 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Connect edge nodes to cloud brokers via secure MQTT\n• Implement battery-efficient deep-sleep power states\n• Analyze sensor telemetry using time-series graphs',
      topics: 'MQTT, ESP32, LoRaWAN, Sensor Telemetry, Cloud IoT Core',
      prerequisites: 'Embedded C, Basic Networking',
    },
  ],

  'VLSI Design & Chip Architecture': [
    {
      id: 922,
      title: 'VLSI Design & CMOS Circuit Implementation',
      description: 'CMOS inverter dynamics, stick diagrams, propagation delay modeling, parasitic extraction, and Verilog digital synthesis.',
      difficulty: 'Advanced',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Design CMOS standard cell layouts following DRC/LVS rules\n• Synthesize RTL descriptions into gate-level netlists\n• Analyze static timing paths and setup/hold constraints',
      topics: 'CMOS Physics, Stick Diagrams, Static Timing Analysis, Verilog Synthesis',
      prerequisites: 'Digital Logic, Semiconductor Physics',
    },
    {
      id: 923,
      title: 'Digital Logic Design & Verilog Modeling',
      description: 'Combinational logic optimization, sequential state machines, testbenches, and FPGA synthesis workflows.',
      difficulty: 'Intermediate',
      duration: '5 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Model complex Moore and Mealy state machines in Verilog\n• Write behavioral testbenches with self-checking assertions\n• Deploy digital hardware architectures onto Xilinx FPGAs',
      topics: 'Verilog HDL, Finite State Machines, FPGA Implementation, Testbenches',
      prerequisites: 'Basic Electronics',
    },
    {
      id: 924,
      title: 'Electronic Devices & Circuit Theory',
      description: 'PN junction diodes, BJT and MOSFET small-signal amplifiers, biasing stability, and frequency response analysis.',
      difficulty: 'Intermediate',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Analyze BJT and MOSFET amplifier configurations\n• Calculate high/low cutoff frequencies using Bode plots\n• Simulate analog circuits with SPICE tools',
      topics: 'MOSFET Modeling, Small Signal Amplifiers, Biasing, Frequency Response',
      prerequisites: 'Basic Electrical Circuit Theory',
    },
  ],

  // Core EEE
  'Electric Vehicles & Powertrains': [
    {
      id: 925,
      title: 'Electric Vehicle Powertrains & Renewable Energy',
      description: 'EV traction motor controllers, regenerative braking systems, battery management systems (BMS), and solar/wind charging integration.',
      difficulty: 'Specialized',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Model BLDC and PMSM traction motor drives\n• Design battery cell balancing and state-of-charge (SoC) algorithms\n• Evaluate fast DC charging and grid-to-vehicle (V2G) topologies',
      topics: 'EV Motor Controllers, Battery Management (BMS), Power Inverters, Regenerative Braking',
      prerequisites: 'Power Electronics, Electrical Machines',
    },
    {
      id: 926,
      title: 'Power Electronics & Solid-State Switching',
      description: 'SCR, MOSFET, IGBT characteristics, phase-controlled rectifiers, DC-DC buck/boost converters, and PWM inverters.',
      difficulty: 'Advanced',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Design high-efficiency switched-mode DC-DC converters\n• Implement sinusoidal PWM switching for 3-phase inverters\n• Mitigate harmonic distortion in industrial power converters',
      topics: 'Buck-Boost Converters, Inverters, IGBT Switching, Harmonic Mitigation',
      prerequisites: 'Circuit Theory, Analog Electronics',
    },
    {
      id: 927,
      title: 'Renewable Energy Systems & Smart Grid Technology',
      description: 'Solar PV grid integration, MPPT algorithms, wind turbine generators, microgrids, and smart meters.',
      difficulty: 'Intermediate',
      duration: '5 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Implement perturb-and-observe MPPT for solar arrays\n• Analyze microgrid islanding and synchronization\n• Study smart meter data communication architectures',
      topics: 'Solar PV, Wind Energy, MPPT, Microgrid Control, Smart Inverters',
      prerequisites: 'Power Systems Basics',
    },
  ],

  // Core MECH
  'Robotics & Mechatronics': [
    {
      id: 928,
      title: 'Robotics & Industrial Automation Systems',
      description: 'Forward and inverse kinematics, Denavit-Hartenberg (DH) parameters, trajectory planning, and PLC integration in manufacturing.',
      difficulty: 'Specialized',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Compute kinematic transforms for multi-axis robotic arms\n• Plan smooth point-to-point and spline trajectories\n• Program industrial PLCs using ladder logic diagrams',
      topics: 'DH Parameters, Inverse Kinematics, Trajectory Generation, PLC Ladder Logic',
      prerequisites: 'Mechanics of Machinery, Linear Algebra',
    },
    {
      id: 929,
      title: 'CAD/CAM & Finite Element Modeling (FEA)',
      description: '3D parametric solid modeling, computer-aided manufacturing NC part codes, 1D/2D structural stress analysis, and mesh convergence.',
      difficulty: 'Advanced',
      duration: '5 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Create parametric mechanical assemblies in modern CAD\n• Generate CNC G-code and M-code toolpaths for machining\n• Perform static structural FEA stress and deformation studies',
      topics: 'Solid Modeling, G-Code CNC, FEA Mesh Generation, Von Mises Stress Analysis',
      prerequisites: 'Strength of Materials',
    },
    {
      id: 930,
      title: 'Automobile Engineering & Electric Vehicle Dynamics',
      description: 'Chassis mechanics, steering and suspension geometry, aerodynamic drag reduction, and thermal cooling for EV battery packs.',
      difficulty: 'Specialized',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Model vehicle suspension dynamics and cornering stability\n• Design aerodynamic body profiles to reduce drag coefficient\n• Calculate battery thermal cooling requirements under peak loads',
      topics: 'Vehicle Dynamics, Suspension Systems, Aerodynamic Drag, EV Thermal Management',
      prerequisites: 'Thermodynamics, Kinematics',
    },
  ],

  // Core CIVIL
  'Structural Engineering & BIM': [
    {
      id: 931,
      title: 'Design of Reinforced Concrete Structures (RCC)',
      description: 'Limit state design of singly and doubly reinforced beams, one-way and two-way slabs, short/slender columns, and isolated footings.',
      difficulty: 'Advanced',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Apply IS 456 limit state specifications for flexure and shear\n• Design earthquake-resistant ductile structural frames\n• Formulate complete reinforcement detailing schedules',
      topics: 'Limit State Design, RCC Beams, Slabs, Column Axial-Bending, Footings',
      prerequisites: 'Structural Analysis, Strength of Materials',
    },
    {
      id: 932,
      title: 'Building Information Modeling (BIM) & Computational Design',
      description: 'Multi-disciplinary 3D building modeling, clash detection, 4D construction schedule linking, and automated quantity takeoffs.',
      difficulty: 'Specialized',
      duration: '5 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Model architectural and structural elements in BIM software\n• Perform automated clash analysis across MEP systems\n• Generate precise material quantities and cost estimations',
      topics: 'Autodesk Revit, BIM 360, Clash Detection, 4D Scheduling, Quantity Takeoffs',
      prerequisites: 'Building Construction Basics',
    },
    {
      id: 933,
      title: 'Structural Analysis & Energy Methods',
      description: 'Indeterminate structure analysis, slope deflection method, moment distribution method, and matrix stiffness approaches.',
      difficulty: 'Intermediate',
      duration: '6 Weeks',
      category: 'Specialization AI',
      learningObjectives: '• Solve continuous beams and rigid portal frames\n• Construct shear and moment influence line diagrams\n• Formulate global stiffness matrices for structural frames',
      topics: 'Slope Deflection, Moment Distribution, Matrix Stiffness Method, Influence Lines',
      prerequisites: 'Mechanics of Materials',
    },
  ],
};

/**
 * Returns specialized courses tailored to a student's chosen Area of Interest.
 * Provides resilient fallbacks across all engineering disciplines.
 */
export const getSpecializationCoursesForInterest = (
  areaOfInterest?: string,
  branch?: string
): SpecializationCourse[] => {
  if (areaOfInterest && SPECIALIZATION_COURSE_REGISTRY[areaOfInterest]) {
    return SPECIALIZATION_COURSE_REGISTRY[areaOfInterest];
  }

  // Keyword fuzzy matching
  if (areaOfInterest) {
    const norm = areaOfInterest.toLowerCase();
    for (const [key, courses] of Object.entries(SPECIALIZATION_COURSE_REGISTRY)) {
      const keyNorm = key.toLowerCase();
      if (norm.includes(keyNorm) || keyNorm.includes(norm)) {
        return courses;
      }
    }

    // Individual token matching
    const tokens = norm.split(/[\s&/,]+/);
    for (const [key, courses] of Object.entries(SPECIALIZATION_COURSE_REGISTRY)) {
      const keyNorm = key.toLowerCase();
      for (const t of tokens) {
        if (t.length > 3 && keyNorm.includes(t)) {
          return courses;
        }
      }
    }
  }

  // Fallback by branch
  const b = (branch || 'CSE').toUpperCase().trim();
  if (b.includes('CIVIL')) return SPECIALIZATION_COURSE_REGISTRY['Structural Engineering & BIM'];
  if (b.includes('MECH')) return SPECIALIZATION_COURSE_REGISTRY['Robotics & Mechatronics'];
  if (b.includes('EEE')) return SPECIALIZATION_COURSE_REGISTRY['Electric Vehicles & Powertrains'];
  if (b.includes('ECE')) return SPECIALIZATION_COURSE_REGISTRY['Embedded Systems & IoT'];
  if (b.includes('AIML') || b.includes('AIDS')) return SPECIALIZATION_COURSE_REGISTRY['Artificial Intelligence & Machine Learning'];
  if (b.includes('IT')) return SPECIALIZATION_COURSE_REGISTRY['Cloud Computing & DevOps'];
  return SPECIALIZATION_COURSE_REGISTRY['Artificial Intelligence & Machine Learning'];
};
