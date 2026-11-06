import apiClient, { aiApiClient } from './api';
import { API_ENDPOINTS } from '../utils/constants';
import { Course, LearningPhase } from '../types';

export const learningPathService = {
  generateLearningPath: async (courseId: string): Promise<Course> => {
    const response = await aiApiClient.post<Course>(
      API_ENDPOINTS.LEARNING_PATH(courseId)
    );
    return response.data;
  },

  getLearningPath: async (courseId: string): Promise<Course> => {
    const response = await apiClient.get<Course>(
      API_ENDPOINTS.LEARNING_PATH(courseId)
    );
    return response.data;
  },

  getPhaseDetail: async (phaseId: string): Promise<LearningPhase> => {
    const response = await apiClient.get<LearningPhase>(
      API_ENDPOINTS.PHASE_DETAIL(phaseId)
    );
    return response.data;
  },

  completeTopic: async (
    topicId: string
  ): Promise<{ success: boolean; progress_percentage: number; phase_completed?: boolean }> => {
    const response = await apiClient.post(API_ENDPOINTS.TOPIC_COMPLETE(topicId));
    return response.data;
  },
};
