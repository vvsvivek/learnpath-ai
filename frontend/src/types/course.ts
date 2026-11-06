export type CourseDifficulty = 'Beginner' | 'Intermediate' | 'Advanced';

export interface Resource {
  id: string;
  title: string;
  url: string;
  type: 'video' | 'article' | 'documentation' | 'book' | 'interactive';
  estimated_minutes?: number;
}

export interface PhaseTopic {
  id: string;
  phase_id: string;
  title: string;
  description: string;
  order: number;
  is_completed: boolean;
  estimated_hours: number;
  resources: Resource[];
  key_takeaways?: string[];
}

export interface LearningPhase {
  id: string;
  course_id: string;
  phase_number: number;
  title: string;
  description: string;
  objectives: string[];
  is_locked: boolean;
  is_completed: boolean;
  progress_percentage: number;
  topics: PhaseTopic[];
  quiz_id?: string;
  quiz_completed?: boolean;
  quiz_score?: number;
}

export interface Course {
  id: string;
  title: string;
  description: string;
  category: string;
  difficulty: CourseDifficulty;
  estimated_hours: number;
  is_custom: boolean;
  created_by?: string;
  created_at: string;
  phases?: LearningPhase[];
  thumbnail_url?: string;
}

export interface CourseRecommendation {
  id: string;
  course: Course;
  match_score: number;
  reason: string;
  source: 'ai' | 'faculty';
  recommended_by?: string;
  faculty_name?: string;
  is_dismissed?: boolean;
}

export interface CourseEnrollment {
  id: string;
  user_id: string;
  course_id: string;
  course: Course;
  enrolled_at: string;
  progress_percentage: number;
  current_phase: number;
  is_completed: boolean;
  last_accessed_at?: string;
}

export interface CreateCustomCoursePayload {
  title: string;
  description: string;
  target_role?: string;
  difficulty?: CourseDifficulty;
  preferred_hours?: number;
}

export interface AssignCoursePayload {
  course_title: string;
  description: string;
  target_year: number;
  target_branch: string;
  target_semester: number;
  instructions?: string;
  deadline?: string;
}
