// LearnPath AI — Unified Client API Service
import { API_BASE_URL } from '../config/apiConfig';

interface RequestOptions {
  method?: string;
  headers?: Record<string, string>;
  body?: any;
}

let storedToken: string | null = null;

export const setAuthToken = (token: string | null) => {
  storedToken = token;
};

export const getAuthToken = () => storedToken;

async function request(endpoint: string, options: RequestOptions = {}): Promise<any> {
  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    ...(options.headers || {}),
  };

  if (storedToken && !headers.Authorization) {
    headers.Authorization = `Bearer ${storedToken}`;
  }

  const config: RequestInit = {
    method: options.method || 'GET',
    headers,
  };

  if (options.body) {
    config.body = typeof options.body === 'string' ? options.body : JSON.stringify(options.body);
  }

  try {
    const res = await fetch(url, config);
    const contentType = res.headers.get('content-type');
    const isJson = contentType && contentType.includes('application/json');
    const data = isJson ? await res.json() : await res.text();

    if (!res.ok) {
      const errorMsg = (typeof data === 'object' && data?.error) || (typeof data === 'object' && data?.message) || `Request failed with status ${res.status}`;
      throw new Error(errorMsg);
    }
    return data;
  } catch (err: any) {
    console.warn(`[API ERROR] ${options.method || 'GET'} ${url} ->`, err.message);
    throw err;
  }
}

export const api = {
  // 1. Authentication
  sendOtp: async (email: string) => {
    try {
      return await request('/api/auth/send-otp', { method: 'POST', body: { email } });
    } catch {
      // Fallback to legacy endpoint
      return await request('/api/main/auth/send-otp', { method: 'POST', body: { email } });
    }
  },

  verifyOtp: async (email: string, otp: string, username?: string, password?: string) => {
    try {
      const data = await request('/api/auth/verify-otp', {
        method: 'POST',
        body: { email, otp, username, password },
      });
      if (data?.token) setAuthToken(data.token);
      return data;
    } catch {
      const data = await request('/api/main/auth', {
        method: 'POST',
        body: { email, otp, username, password },
      });
      if (data?.token) setAuthToken(data.token);
      return data;
    }
  },

  login: async (email: string, password?: string) => {
    const data = await request('/api/auth/login', {
      method: 'POST',
      body: { email, password },
    });
    if (data?.token) setAuthToken(data.token);
    return data;
  },

  googleLogin: async (idToken: string) => {
    const data = await request('/api/auth/google', {
      method: 'POST',
      body: { idToken },
    });
    if (data?.token) setAuthToken(data.token);
    return data;
  },

  getCurrentUser: async (email?: string) => {
    return await request(`/api/auth/me${email ? `?email=${encodeURIComponent(email)}` : ''}`);
  },

  // 2. Student Dashboard & Courses
  getDashboard: async (email: string) => {
    return await request(`/api/main/dashboard?email=${encodeURIComponent(email)}`);
  },

  selectCourse: async (email: string, subject: string) => {
    return await request('/api/main/course/select', {
      method: 'POST',
      body: { email, subject },
    });
  },

  generatePath: async (email: string, subject: string) => {
    return await request('/api/main/generate-path', {
      method: 'POST',
      body: { email, subject },
    });
  },

  getPhaseDetails: async (courseId?: number, subject?: string, phaseNumber: number = 1) => {
    const params = new URLSearchParams();
    if (courseId) params.append('courseId', String(courseId));
    if (subject) params.append('subject', subject);
    params.append('phaseNumber', String(phaseNumber));
    return await request(`/api/main/phase/details?${params.toString()}`);
  },

  toggleCourseDelete: async (courseId: number, isDeleted: boolean) => {
    return await request('/api/main/course/toggle-delete', {
      method: 'POST',
      body: { courseId, isDeleted },
    });
  },

  updateProfile: async (profile: any) => {
    return await request('/api/main/profile/update', {
      method: 'POST',
      body: profile,
    });
  },

  // 3. Quiz Assessments (Strictly 10 Questions & Server-side Unlocking)
  generateQuiz: async (courseId?: number, subject?: string, phaseNumber: number = 1) => {
    const params = new URLSearchParams();
    if (courseId) params.append('courseId', String(courseId));
    if (subject) params.append('subject', subject);
    params.append('phaseNumber', String(phaseNumber));
    return await request(`/api/main/quiz/generate?${params.toString()}`);
  },

  submitQuiz: async (payload: {
    email: string;
    courseId?: number;
    subject?: string;
    phaseNumber: number;
    answers: Record<number, string>;
  }) => {
    return await request('/api/main/quiz/submit', {
      method: 'POST',
      body: payload,
    });
  },

  // 4. Faculty Hub & Analytics
  getFacultyOverview: async (branch?: string) => {
    return await request(`/api/faculty/overview${branch ? `?branch=${encodeURIComponent(branch)}` : ''}`);
  },

  getFacultyStudents: async (params: { branch?: string; year?: string; semester?: string; search?: string; status?: string }) => {
    const q = new URLSearchParams();
    if (params.branch) q.append('branch', params.branch);
    if (params.year) q.append('year', params.year);
    if (params.semester) q.append('semester', params.semester);
    if (params.search) q.append('search', params.search);
    if (params.status) q.append('status', params.status);
    return await request(`/api/faculty/students?${q.toString()}`);
  },

  getStudentReport: async (studentId: number) => {
    return await request(`/api/faculty/student/${studentId}`);
  },

  recommendCourse: async (payload: {
    title: string;
    description?: string;
    branch: string;
    year: string;
    semester: string;
    facultyName: string;
    facultyEmail: string;
    priority?: string;
    deadline?: string;
  }) => {
    return await request('/api/faculty/recommend', {
      method: 'POST',
      body: payload,
    });
  },

  getFacultyRecommendations: async (facultyEmail?: string) => {
    return await request(`/api/faculty/recommendations${facultyEmail ? `?facultyEmail=${encodeURIComponent(facultyEmail)}` : ''}`);
  },

  deleteRecommendation: async (id: number) => {
    return await request(`/api/faculty/recommend/${id}`, { method: 'DELETE' });
  },

  // 5. AI Chat Hub (Student AI & Faculty AI)
  getChatSessions: async (email: string) => {
    return await request(`/v2/chat/sessions?email=${encodeURIComponent(email)}`);
  },

  createChatSession: async (email: string, contextType: 'STUDENT' | 'FACULTY' = 'STUDENT') => {
    return await request(`/v2/chat/new?email=${encodeURIComponent(email)}&contextType=${contextType}`, {
      method: 'POST',
    });
  },

  getChatHistory: async (sessionId: number) => {
    return await request(`/v2/chat/history/${sessionId}`);
  },

  sendMessage: async (email: string, sessionId: number, message: string) => {
    return await request('/v2/chat/send', {
      method: 'POST',
      body: { email, sessionId, message },
    });
  },

  updateChatStatus: async (sessionId: number, status: string) => {
    return await request(`/v2/chat/update-status/${sessionId}?status=${encodeURIComponent(status)}`, {
      method: 'POST',
    });
  },
};

export default api;
