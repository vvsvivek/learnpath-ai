import { User } from './auth';

export interface StudentProfile extends User {
  role: 'student';
  academic_year: number;
  branch: string;
  semester: number;
  cgpa: number;
  interests: string[];
  total_study_minutes?: number;
  streak_days?: number;
  completed_courses_count?: number;
  average_quiz_score?: number;
}

export interface FacultyProfile extends User {
  role: 'faculty';
  department: string;
  designation?: string;
  assigned_courses_count?: number;
  total_students_monitored?: number;
}

export interface StudySession {
  id: string;
  user_id: string;
  course_id?: string;
  topic_id?: string;
  start_time: string;
  end_time?: string;
  duration_minutes: number;
  notes?: string;
  created_at: string;
}

export interface UserSettings {
  theme: 'light' | 'dark' | 'system';
  email_notifications: boolean;
  study_reminders: boolean;
  daily_goal_minutes: number;
}
