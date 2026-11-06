import apiClient from './api';
import { API_ENDPOINTS } from '../utils/constants';
import { FacultyReport, StudentReport } from '../types';

export const reportService = {
  getStudentReport: async (): Promise<StudentReport> => {
    const response = await apiClient.get<StudentReport>(
      API_ENDPOINTS.STUDENT_REPORT
    );
    return response.data;
  },

  getFacultyReport: async (): Promise<FacultyReport> => {
    const response = await apiClient.get<FacultyReport>(
      API_ENDPOINTS.FACULTY_REPORT
    );
    return response.data;
  },
};
