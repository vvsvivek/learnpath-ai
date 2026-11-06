-- ============================================================================
-- Seed Data for AI Personalized Learning Path Generator
-- Optional development & demo data for B.Tech students & Siddhartha faculty
-- ============================================================================

-- 1. Insert Demo Courses
INSERT INTO courses (id, title, description, category, difficulty, is_custom)
VALUES 
  (
    'a1111111-1111-1111-1111-111111111111',
    'Full-Stack Machine Learning Systems',
    'End-to-end machine learning engineering from exploratory data analysis to model deployment, FastAPI serving, and continuous monitoring.',
    'Artificial Intelligence',
    'Intermediate',
    FALSE
  ),
  (
    'b2222222-2222-2222-2222-222222222222',
    'Advanced Data Structures & Algorithmic Problem Solving',
    'Comprehensive mastery of dynamic programming, graph algorithms, trees, greedy strategies, and competitive programming patterns.',
    'Computer Science',
    'Advanced',
    FALSE
  ),
  (
    'c3333333-3333-3333-3333-333333333333',
    'Cloud-Native Microservices & Docker Architecture',
    'Building highly scalable distributed architectures using Docker, Kubernetes, gRPC, and asynchronous message queues.',
    'Cloud Computing',
    'Intermediate',
    FALSE
  )
ON CONFLICT (id) DO NOTHING;

-- 2. Insert 4-Phase Learning Path for 'Full-Stack Machine Learning Systems'
INSERT INTO learning_phases (id, course_id, phase_number, title, description, estimated_hours)
VALUES
  (
    'p1111111-1111-1111-1111-111111111111',
    'a1111111-1111-1111-1111-111111111111',
    1,
    'Phase 1: Foundations & Exploratory Data Science',
    'Master vector computing with NumPy, vectorized operations with Pandas, and data pipeline cleanliness.',
    8.0
  ),
  (
    'p2222222-2222-2222-2222-222222222222',
    'a1111111-1111-1111-1111-111111111111',
    2,
    'Phase 2: Supervised & Unsupervised Modeling',
    'Dive deep into regression, decision trees, ensemble methods (Random Forest, XGBoost), and validation metrics.',
    12.0
  ),
  (
    'p3333333-3333-3333-3333-333333333333',
    'a1111111-1111-1111-1111-111111111111',
    3,
    'Phase 3: Deep Learning & Neural Representations',
    'Construct multi-layer perceptrons, convolutional layers, and transformers using PyTorch.',
    15.0
  ),
  (
    'p4444444-4444-4444-4444-444444444444',
    'a1111111-1111-1111-1111-111111111111',
    4,
    'Phase 4: Production Serving & MLOps Pipelines',
    'Containerize PyTorch inference engines in Docker, expose REST/gRPC endpoints with FastAPI, and track model drift.',
    10.0
  )
ON CONFLICT (course_id, phase_number) DO NOTHING;

-- 3. Insert Topics for Phase 1
INSERT INTO phase_topics (id, phase_id, topic_number, title, description)
VALUES
  (
    't1111111-1111-1111-1111-111111111111',
    'p1111111-1111-1111-1111-111111111111',
    1,
    'Vectorized Tensor Operations with NumPy',
    'Array slicing, broadcasting semantics, and performance benchmarking against vanilla Python loops.'
  ),
  (
    't2222222-2222-2222-2222-222222222222',
    'p1111111-1111-1111-1111-111111111111',
    2,
    'Feature Engineering & Data Cleansing Pipelines',
    'Handling missing values, outlier detection with IQR/Z-score, categorical encodings, and scaling.'
  ),
  (
    't3333333-3333-3333-3333-333333333333',
    'p1111111-1111-1111-1111-111111111111',
    3,
    'Exploratory Visual Analytics with Seaborn',
    'Heatmaps, correlation matrices, pair plots, and distribution analysis for hypothesis testing.'
  )
ON CONFLICT (id) DO NOTHING;

-- 4. Insert Resources for Topics
INSERT INTO resources (topic_id, title, url, resource_type, estimated_minutes)
VALUES
  (
    't1111111-1111-1111-1111-111111111111',
    'NumPy Visual Guide to Broadcasting',
    'https://numpy.org/doc/stable/user/basics.broadcasting.html',
    'documentation',
    25
  ),
  (
    't2222222-2222-2222-2222-222222222222',
    'Scikit-Learn Preprocessing & Imputation Guide',
    'https://scikit-learn.org/stable/modules/preprocessing.html',
    'article',
    30
  )
ON CONFLICT (id) DO NOTHING;

-- 5. Insert Quiz for Phase 1
INSERT INTO quizzes (id, phase_id, title, description, time_limit_minutes)
VALUES
  (
    'q1111111-1111-1111-1111-111111111111',
    'p1111111-1111-1111-1111-111111111111',
    'Phase 1 Diagnostic Assessment: NumPy & Feature Engineering',
    'Evaluate your understanding of array broadcasting, missing value imputation, and feature scaling.',
    15
  )
ON CONFLICT (phase_id) DO NOTHING;

-- 6. Insert Quiz Questions
INSERT INTO quiz_questions (quiz_id, question_text, options, correct_option, explanation)
VALUES
  (
    'q1111111-1111-1111-1111-111111111111',
    'Under which condition are two NumPy arrays compatible for broadcasting along a specific dimension?',
    '["Both arrays must have identical memory addresses", "The trailing dimensions must either be equal, or one of them must be 1", "The sum of their elements must be even", "Both arrays must use float64 data type"]'::jsonb,
    1,
    'NumPy broadcasting rules dictate that two dimensions are compatible when they are equal, or one of them is 1.'
  ),
  (
    'q1111111-1111-1111-1111-111111111111',
    'Why is Standard Scaler (Z-score normalization) preferred over Min-Max Scaler when outliers are present?',
    '["Standard Scaler ignores float numbers", "Standard Scaler forces all values strictly between 0 and 1", "Standard Scaler centers the data to zero mean and unit variance, avoiding severe compression of inliers", "Min-Max Scaler deletes outlier rows automatically"]'::jsonb,
    2,
    'Min-Max scaling binds data between 0 and 1, which squashes valid inliers into a tiny sub-range if an extreme outlier is present.'
  ),
  (
    'q1111111-1111-1111-1111-111111111111',
    'What data structure does Pandas DataFrame use internally for vectorized column-wise computation?',
    '["Doubly linked lists", "Contiguous NumPy 1D ndarrays / Arrow arrays", "Hash maps mapping each cell to an integer pointer", "Binary search trees"]'::jsonb,
    1,
    'Pandas Series and DataFrame columns are backed by contiguous 1D NumPy arrays or Apache Arrow buffers, providing SIMD hardware vectorization.'
  )
ON CONFLICT (id) DO NOTHING;
