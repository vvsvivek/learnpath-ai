import apiClient from './api';
import { API_ENDPOINTS } from '../utils/constants';
import { StudySession } from '../types';

export const studyService = {
  startSession: async (courseId?: string, topicId?: string): Promise<StudySession> => {
    const response = await apiClient.post<StudySession>(API_ENDPOINTS.START_SESSION, {
      course_id: courseId,
      topic_id: topicId,
      start_time: new Date().toISOString(),
    });
    return response.data;
  },

  endSession: async (
    sessionId: string,
    durationMinutes: number,
    notes?: string
  ): Promise<StudySession> => {
    const response = await apiClient.post<StudySession>(
      API_ENDPOINTS.END_SESSION(sessionId),
      {
        end_time: new Date().toISOString(),
        duration_minutes: durationMinutes,
        notes,
      }
    );
    return response.data;
  },

  getSessions: async (): Promise<StudySession[]> => {
    const response = await apiClient.get<StudySession[]>(
      API_ENDPOINTS.STUDY_SESSIONS
    );
    return response.data;
  },
};
