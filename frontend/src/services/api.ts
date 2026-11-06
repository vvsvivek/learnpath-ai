import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';

const BASE_URL = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8000';

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 30000, // 30 seconds default
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

// AI specific client with higher timeout
export const aiApiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 60000, // 60 seconds for AI generation
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

// Request interceptor to attach JWT token
const authRequestInterceptor = async (config: InternalAxiosRequestConfig) => {
  try {
    const token = await AsyncStorage.getItem('@learnpath_access_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  } catch (error) {
    console.warn('Could not read access token:', error);
  }
  return config;
};

apiClient.interceptors.request.use(authRequestInterceptor, (error) => Promise.reject(error));
aiApiClient.interceptors.request.use(authRequestInterceptor, (error) => Promise.reject(error));

// Response interceptor with Cold Start retry and 401 handling
const setupResponseInterceptor = (client: typeof apiClient) => {
  client.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
      const originalRequest = error.config as InternalAxiosRequestConfig & { _retryCount?: number };

      // Handle Render cold start retries on network error or gateway timeouts (502, 503, 504)
      if (
        originalRequest &&
        (!error.response || [502, 503, 504].includes(error.response.status))
      ) {
        originalRequest._retryCount = originalRequest._retryCount || 0;
        if (originalRequest._retryCount < 2) {
          originalRequest._retryCount += 1;
          const delayMs = originalRequest._retryCount * 2500;
          await new Promise((resolve) => setTimeout(resolve, delayMs));
          return client(originalRequest);
        }
      }

      // Handle token expiration 401
      if (error.response?.status === 401) {
        try {
          const refreshToken = await AsyncStorage.getItem('@learnpath_refresh_token');
          if (refreshToken) {
            const refreshRes = await axios.post(`${BASE_URL}/auth/refresh`, {
              refresh_token: refreshToken,
            });
            const newAccessToken = refreshRes.data.access_token;
            if (newAccessToken) {
              await AsyncStorage.setItem('@learnpath_access_token', newAccessToken);
              if (originalRequest && originalRequest.headers) {
                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                return client(originalRequest);
              }
            }
          }
        } catch (refreshErr) {
          // Token refresh failed, clean up
          await AsyncStorage.removeItem('@learnpath_access_token');
          await AsyncStorage.removeItem('@learnpath_refresh_token');
          await AsyncStorage.removeItem('@learnpath_user');
        }
      }

      return Promise.reject(error);
    }
  );
};

setupResponseInterceptor(apiClient);
setupResponseInterceptor(aiApiClient);

export default apiClient;
