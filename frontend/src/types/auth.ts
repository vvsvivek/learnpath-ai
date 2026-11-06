export type UserRole = 'student' | 'faculty';

export interface User {
  id: string;
  email: string;
  role: UserRole;
  name?: string;
  is_onboarded: boolean;
  avatar_url?: string;
  academic_year?: number;
  branch?: string;
  semester?: number;
  cgpa?: number;
  interests?: string[];
  department?: string;
  designation?: string;
  completed_courses_count?: number;
  streak_days?: number;
  quiz_accuracy?: number;
  created_at?: string;
}

export interface LoginRequest {
  email: string;
  role: UserRole;
}

export interface OTPRequest {
  email: string;
  role: UserRole;
}

export interface OTPVerify {
  email: string;
  otp: string;
  role: UserRole;
}

export interface TokenResponse {
  access_token: string;
  refresh_token: string;
  token_type: string;
  user: User;
}

export interface OnboardingData {
  name: string;
  academic_year: number;
  branch: string;
  semester: number;
  cgpa: number;
  interests: string[];
}
