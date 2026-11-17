-- =================================================================
-- LEARNPATH AI — TEST SEED DATA (03_seed_data.sql)
-- =================================================================

-- 1. Insert Faculty Users
INSERT INTO users (email, username, role, branch, academic_year, semester, cgpa, is_email_verified, auth_provider)
VALUES 
('vivekvaranasi99@gmail.com', 'Prof. Vivek Varanasi', 'FACULTY', 'CSE', 'Faculty', 'N/A', 0.0, true, 'EMAIL'),
('hod.cse@siddhartha.ac.in', 'Dr. K. Srinivas', 'FACULTY', 'CSE', 'Faculty', 'N/A', 0.0, true, 'EMAIL'),
('faculty.ece@siddhartha.ac.in', 'Prof. M. Anuradha', 'FACULTY', 'ECE', 'Faculty', 'N/A', 0.0, true, 'EMAIL')
ON CONFLICT (email) DO UPDATE SET role = EXCLUDED.role, is_email_verified = true;

-- 2. Insert Eligible Students (@vrsec.ac.in)
INSERT INTO users (email, username, role, branch, academic_year, semester, cgpa, is_email_verified, auth_provider)
VALUES
('22vr1a0501@vrsec.ac.in', 'Aarav Sharma', 'STUDENT', 'CSE', '2nd Year', '1st Sem', 8.50, true, 'EMAIL'),
('22vr1a0502@vrsec.ac.in', 'Bhavya Patel', 'STUDENT', 'CSE', '2nd Year', '1st Sem', 5.80, true, 'EMAIL'),
('21vr1a0503@vrsec.ac.in', 'Chaitanya Varma', 'STUDENT', 'CSE', '3rd Year', '1st Sem', 7.20, true, 'EMAIL'),
('22vr1a0401@vrsec.ac.in', 'Divya Reddy', 'STUDENT', 'ECE', '2nd Year', '1st Sem', 6.50, true, 'EMAIL'),
('23vr1a0505@vrsec.ac.in', 'Eshwar Kumar', 'STUDENT', 'CSE', '1st Year', '1st Sem', 9.10, true, 'EMAIL')
ON CONFLICT (email) DO UPDATE SET 
    branch = EXCLUDED.branch, 
    academic_year = EXCLUDED.academic_year, 
    semester = EXCLUDED.semester, 
    cgpa = EXCLUDED.cgpa;

-- 3. Insert General Student (non-vrsec, allowed to register and learn, excluded from faculty analytics)
INSERT INTO users (email, username, role, branch, academic_year, semester, cgpa, is_email_verified, auth_provider)
VALUES
('general.learner@gmail.com', 'Rohan Gupta', 'STUDENT', 'CSE', '2nd Year', '1st Sem', 8.00, true, 'EMAIL')
ON CONFLICT (email) DO NOTHING;

-- 4. Insert Faculty Recommendation
INSERT INTO suggested_courses (faculty_name, faculty_email, title, description, target_branch, target_year, target_semester, priority, deadline)
VALUES
('Prof. Vivek Varanasi', 'vivekvaranasi99@gmail.com', 'Data Structures and Algorithms', 'Core university departmental curriculum requirement for technical placement preparation.', 'CSE', '2nd Year', '1st Sem', 'HIGH', CURRENT_DATE + INTERVAL '30 days')
ON CONFLICT DO NOTHING;
