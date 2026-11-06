export interface WeeklyStudyData {
  day: string;
  minutes: number;
}

export interface TopicMastery {
  topic: string;
  category: string;
  score_percentage: number;
  status: 'mastered' | 'improving' | 'needs_work';
}

export interface CourseAnalytics {
  course_id: string;
  course_title: string;
  progress_percentage: number;
  completed_phases: number;
  total_phases: number;
  time_spent_minutes: number;
  average_quiz_score: number;
}

export interface QuizAnalytics {
  total_quizzes_taken: number;
  passed_quizzes: number;
  average_score_percentage: number;
  highest_score: number;
  recent_attempts: {
    quiz_id: string;
    title: string;
    score_percentage: number;
    passed: boolean;
    date: string;
  }[];
}

export interface StudentReport {
  student_id: string;
  name: string;
  branch: string;
  academic_year: number;
  semester: number;
  cgpa: number;
  total_study_minutes: number;
  weekly_study_data: WeeklyStudyData[];
  course_analytics: CourseAnalytics[];
  quiz_analytics: QuizAnalytics;
  topic_mastery: TopicMastery[];
  streak_days: number;
  recommendations: string[];
}

export interface StudentCategoryDistribution {
  high_performing: number;
  average: number;
  needs_support: number;
}

export interface FacultyStudentSummary {
  id: string;
  name: string;
  email: string;
  branch: string;
  academic_year: number;
  semester: number;
  cgpa: number;
  courses_enrolled: number;
  progress_percentage: number;
  avg_quiz_score: number;
  study_minutes: number;
  category: 'high' | 'average' | 'needs_support';
  last_active: string;
}

export interface FacultyReport {
  total_students: number;
  active_students_count: number;
  total_assigned_courses: number;
  average_class_cgpa: number;
  average_quiz_score: number;
  category_distribution: StudentCategoryDistribution;
  branch_performance: {
    branch: string;
    student_count: number;
    avg_score: number;
  }[];
  weekly_engagement: {
    week: string;
    active_count: number;
  }[];
  top_struggling_topics: {
    topic: string;
    fail_rate_percentage: number;
  }[];
}
