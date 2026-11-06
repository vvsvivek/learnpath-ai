export interface QuizQuestion {
  id: string;
  question: string;
  options: string[];
  correct_index: number;
  explanation: string;
  topic_id?: string;
  difficulty?: 'easy' | 'medium' | 'hard';
}

export interface Quiz {
  id: string;
  course_id: string;
  phase_id?: string;
  title: string;
  description: string;
  questions: QuizQuestion[];
  passing_score_percentage: number;
  time_limit_minutes?: number;
  created_at: string;
}

export interface QuizSubmissionAnswer {
  question_id: string;
  selected_index: number;
}

export interface QuizSubmission {
  quiz_id: string;
  answers: QuizSubmissionAnswer[];
  duration_seconds: number;
}

export interface QuestionBreakdown {
  question_id: string;
  question_text: string;
  options: string[];
  selected_index: number;
  correct_index: number;
  is_correct: boolean;
  explanation: string;
}

export interface QuizResult {
  id: string;
  quiz_id: string;
  score_percentage: number;
  total_questions: number;
  correct_answers: number;
  passed: boolean;
  duration_seconds: number;
  completed_at: string;
  breakdown: QuestionBreakdown[];
}

export interface QuizAttemptSummary {
  id: string;
  quiz_id: string;
  course_title: string;
  phase_title?: string;
  score_percentage: number;
  passed: boolean;
  completed_at: string;
}
