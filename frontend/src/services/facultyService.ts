import apiClient from './api';
import { API_ENDPOINTS } from '../utils/constants';
import {
  AssignCoursePayload,
  FacultyReport,
  FacultyStudentSummary,
  StudentReport,
} from '../types';

export const facultyService = {
  getDashboard: async (): Promise<FacultyReport> => {
    const response = await apiClient.get<FacultyReport>(
      API_ENDPOINTS.FACULTY_DASHBOARD
    );
    return response.data;
  },

  getStudents: async (
    year?: number,
    branch?: string,
    semester?: number
  ): Promise<FacultyStudentSummary[]> => {
    const params: Record<string, any> = {};
    if (year) params.year = year;
    if (branch) params.branch = branch;
    if (semester) params.semester = semester;

    const response = await apiClient.get<FacultyStudentSummary[]>(
      API_ENDPOINTS.FACULTY_STUDENTS,
      { params }
    );
    return response.data;
  },

  getStudentDetail: async (studentId: string): Promise<StudentReport> => {
    const response = await apiClient.get<StudentReport>(
      API_ENDPOINTS.FACULTY_STUDENT_DETAIL(studentId)
    );
    return response.data;
  },

  assignCourse: async (
    payload: AssignCoursePayload
  ): Promise<{ success: boolean; assignment_id: string; message: string }> => {
    const response = await apiClient.post(
      API_ENDPOINTS.FACULTY_ASSIGN_COURSE,
      payload
    );
    return response.data;
  },

  getAssignments: async (): Promise<any[]> => {
    const response = await apiClient.get(API_ENDPOINTS.FACULTY_ASSIGNMENTS);
    return response.data;
  },
};
