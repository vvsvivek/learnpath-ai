import apiClient from './api';
import { API_ENDPOINTS } from '../utils/constants';
import { OnboardingData, TokenResponse, User, UserRole } from '../types';

export const authService = {
  requestOTP: async (email: string, role: UserRole): Promise<{ message: string; success: boolean }> => {
    const response = await apiClient.post(API_ENDPOINTS.REQUEST_OTP, {
      email,
      role,
    });
    return response.data;
  },

  verifyOTP: async (email: string, otp: string, role: UserRole): Promise<TokenResponse> => {
    const response = await apiClient.post<TokenResponse>(API_ENDPOINTS.VERIFY_OTP, {
      email,
      otp,
      role,
    });
    return response.data;
  },

  refreshToken: async (refreshToken: string): Promise<TokenResponse> => {
    const response = await apiClient.post<TokenResponse>(API_ENDPOINTS.REFRESH_TOKEN, {
      refresh_token: refreshToken,
    });
    return response.data;
  },

  completeOnboarding: async (data: OnboardingData): Promise<User> => {
    const response = await apiClient.post<User>(API_ENDPOINTS.ONBOARDING, data);
    return response.data;
  },

  getProfile: async (): Promise<User> => {
    const response = await apiClient.get<User>(API_ENDPOINTS.PROFILE);
    return response.data;
  },
};
