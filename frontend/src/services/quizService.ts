import apiClient, { aiApiClient } from './api';
import { API_ENDPOINTS } from '../utils/constants';
import { Quiz, QuizAttemptSummary, QuizResult, QuizSubmission } from '../types';

export const quizService = {
  generateQuiz: async (courseId: string, phaseId?: string): Promise<Quiz> => {
    const response = await aiApiClient.post<Quiz>(API_ENDPOINTS.GENERATE_QUIZ, {
      course_id: courseId,
      phase_id: phaseId,
    });
    return response.data;
  },

  getQuiz: async (quizId: string): Promise<Quiz> => {
    const response = await apiClient.get<Quiz>(API_ENDPOINTS.GET_QUIZ(quizId));
    return response.data;
  },

  submitQuiz: async (submission: QuizSubmission): Promise<QuizResult> => {
    const response = await apiClient.post<QuizResult>(
      API_ENDPOINTS.SUBMIT_QUIZ(submission.quiz_id),
      submission
    );
    return response.data;
  },

  getQuizHistory: async (): Promise<QuizAttemptSummary[]> => {
    const response = await apiClient.get<QuizAttemptSummary[]>(
      API_ENDPOINTS.QUIZ_HISTORY
    );
    return response.data;
  },
};
