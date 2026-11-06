import apiClient, { aiApiClient } from './api';
import { API_ENDPOINTS } from '../utils/constants';
import {
  Course,
  CourseEnrollment,
  CourseRecommendation,
  CreateCustomCoursePayload,
} from '../types';

export const courseService = {
  getRecommendations: async (): Promise<CourseRecommendation[]> => {
    const response = await apiClient.get<CourseRecommendation[]>(
      API_ENDPOINTS.COURSE_RECOMMENDATIONS
    );
    return response.data;
  },

  getEnrolledCourses: async (): Promise<CourseEnrollment[]> => {
    const response = await apiClient.get<CourseEnrollment[]>(
      API_ENDPOINTS.ENROLLED_COURSES
    );
    return response.data;
  },

  getCourseDetail: async (courseId: string): Promise<Course> => {
    const response = await apiClient.get<Course>(`${API_ENDPOINTS.COURSES}/${courseId}`);
    return response.data;
  },

  enrollInCourse: async (courseId: string): Promise<CourseEnrollment> => {
    const response = await apiClient.post<CourseEnrollment>(
      API_ENDPOINTS.ENROLL(courseId)
    );
    return response.data;
  },

  dismissRecommendation: async (recId: string): Promise<{ success: boolean }> => {
    const response = await apiClient.post<{ success: boolean }>(
      API_ENDPOINTS.DISMISS_RECOMMENDATION(recId)
    );
    return response.data;
  },

  createCustomCourse: async (payload: CreateCustomCoursePayload): Promise<Course> => {
    // Generates course with Gemini AI, uses longer timeout aiApiClient
    const response = await aiApiClient.post<Course>(
      API_ENDPOINTS.CUSTOM_COURSE,
      payload
    );
    return response.data;
  },
};
