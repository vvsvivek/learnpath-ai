export const BRANCHES = [
  { label: 'Computer Science & Engineering (CSE)', value: 'CSE' },
  { label: 'Information Technology (IT)', value: 'IT' },
  { label: 'AI & Machine Learning (AIML)', value: 'AIML' },
  { label: 'Data Science (DS)', value: 'DS' },
  { label: 'Electronics & Communication (ECE)', value: 'ECE' },
  { label: 'Electrical & Electronics (EEE)', value: 'EEE' },
  { label: 'Mechanical Engineering (ME)', value: 'ME' },
  { label: 'Civil Engineering (CE)', value: 'CE' },
  { label: 'CS & Business Systems (CSBS)', value: 'CSBS' },
];

export const ACADEMIC_YEARS = [
  { label: '1st Year', value: 1 },
  { label: '2nd Year', value: 2 },
  { label: '3rd Year', value: 3 },
  { label: '4th Year', value: 4 },
];

export const SEMESTERS = [
  { label: 'Semester 1', value: 1 },
  { label: 'Semester 2', value: 2 },
  { label: 'Semester 3', value: 3 },
  { label: 'Semester 4', value: 4 },
  { label: 'Semester 5', value: 5 },
  { label: 'Semester 6', value: 6 },
  { label: 'Semester 7', value: 7 },
  { label: 'Semester 8', value: 8 },
];

export const LEARNING_INTERESTS = [
  'Data Structures & Algorithms',
  'Full-Stack Web Development',
  'Artificial Intelligence & Machine Learning',
  'Cloud Computing & DevOps',
  'Mobile App Development (React Native)',
  'Cybersecurity & Ethical Hacking',
  'Database Management & SQL',
  'Data Science & Analytics',
  'Internet of Things (IoT)',
  'Blockchain & Web3',
  'System Design & Architecture',
  'Computer Networks',
];

export const API_ENDPOINTS = {
  // Auth
  REQUEST_OTP: '/auth/request-otp',
  VERIFY_OTP: '/auth/verify-otp',
  REFRESH_TOKEN: '/auth/refresh',
  ONBOARDING: '/auth/onboarding',
  PROFILE: '/auth/me',

  // Courses
  COURSES: '/courses',
  COURSE_RECOMMENDATIONS: '/courses/recommendations',
  ENROLLED_COURSES: '/courses/enrolled',
  ENROLL: (id: string) => `/courses/${id}/enroll`,
  DISMISS_RECOMMENDATION: (id: string) => `/courses/recommendations/${id}/dismiss`,
  CUSTOM_COURSE: '/courses/custom',

  // Learning Paths
  LEARNING_PATH: (courseId: string) => `/courses/${courseId}/learning-path`,
  PHASE_DETAIL: (phaseId: string) => `/phases/${phaseId}`,
  TOPIC_COMPLETE: (topicId: string) => `/topics/${topicId}/complete`,

  // Quizzes
  GENERATE_QUIZ: '/quizzes/generate',
  GET_QUIZ: (id: string) => `/quizzes/${id}`,
  SUBMIT_QUIZ: (id: string) => `/quizzes/${id}/submit`,
  QUIZ_HISTORY: '/quizzes/history',

  // Chat
  CONVERSATIONS: '/chat/conversations',
  MESSAGES: (convId: string) => `/chat/conversations/${convId}/messages`,
  SEND_STUDENT_MESSAGE: '/chat/student/message',
  SEND_FACULTY_MESSAGE: '/chat/faculty/message',

  // Reports
  STUDENT_REPORT: '/reports/student',
  FACULTY_REPORT: '/reports/faculty',

  // Study Tracker
  START_SESSION: '/study/start',
  END_SESSION: (id: string) => `/study/${id}/end`,
  STUDY_SESSIONS: '/study/sessions',

  // Faculty
  FACULTY_DASHBOARD: '/faculty/dashboard',
  FACULTY_STUDENTS: '/faculty/students',
  FACULTY_STUDENT_DETAIL: (id: string) => `/faculty/students/${id}`,
  FACULTY_ASSIGN_COURSE: '/faculty/courses/assign',
  FACULTY_ASSIGNMENTS: '/faculty/assignments',
};
