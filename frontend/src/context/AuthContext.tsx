import React, { createContext, useContext, useEffect, useState } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { OnboardingData, User, UserRole } from '../types';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  token: string | null;
  role: UserRole | null;
  loginWithTokens: (accessToken: string, refreshToken: string, userData: User) => Promise<void>;
  logout: () => Promise<void>;
  updateUser: (updatedUser: Partial<User>) => Promise<void>;
  completeOnboarding: (data: OnboardingData) => Promise<void>;
  refreshAuthToken: () => Promise<boolean>;
}

const ACCESS_TOKEN_KEY = '@learnpath_access_token';
const REFRESH_TOKEN_KEY = '@learnpath_refresh_token';
const USER_KEY = '@learnpath_user';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const storedToken = await AsyncStorage.getItem(ACCESS_TOKEN_KEY);
        const storedUser = await AsyncStorage.getItem(USER_KEY);

        if (storedToken && storedUser) {
          setToken(storedToken);
          setUser(JSON.parse(storedUser));
        }
      } catch (error) {
        console.warn('Failed to restore session:', error);
      } finally {
        setIsLoading(false);
      }
    };

    initializeAuth();
  }, []);

  const loginWithTokens = async (
    accessToken: string,
    refreshToken: string,
    userData: User
  ) => {
    try {
      await AsyncStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
      await AsyncStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
      await AsyncStorage.setItem(USER_KEY, JSON.stringify(userData));

      setToken(accessToken);
      setUser(userData);
    } catch (error) {
      console.error('Error saving authentication credentials:', error);
      throw error;
    }
  };

  const logout = async () => {
    try {
      await AsyncStorage.removeItem(ACCESS_TOKEN_KEY);
      await AsyncStorage.removeItem(REFRESH_TOKEN_KEY);
      await AsyncStorage.removeItem(USER_KEY);
    } catch (error) {
      console.warn('Error clearing stored session:', error);
    } finally {
      setToken(null);
      setUser(null);
    }
  };

  const updateUser = async (updatedFields: Partial<User>) => {
    if (!user) return;
    const mergedUser = { ...user, ...updatedFields };
    setUser(mergedUser);
    try {
      await AsyncStorage.setItem(USER_KEY, JSON.stringify(mergedUser));
    } catch (error) {
      console.warn('Failed to persist user updates:', error);
    }
  };

  const completeOnboarding = async (data: OnboardingData) => {
    if (!user) return;
    const updated: User = {
      ...user,
      name: data.name,
      academic_year: data.academic_year,
      branch: data.branch,
      semester: data.semester,
      cgpa: data.cgpa,
      interests: data.interests,
      is_onboarded: true,
    };
    await updateUser(updated);
  };

  const refreshAuthToken = async (): Promise<boolean> => {
    try {
      const storedRefreshToken = await AsyncStorage.getItem(REFRESH_TOKEN_KEY);
      if (!storedRefreshToken) {
        await logout();
        return false;
      }
      return true;
    } catch (error) {
      await logout();
      return false;
    }
  };

  const isAuthenticated = !!token && !!user;
  const role = user?.role || null;

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated,
        isLoading,
        token,
        role,
        loginWithTokens,
        logout,
        updateUser,
        completeOnboarding,
        refreshAuthToken,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuthContext = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuthContext must be used within an AuthProvider');
  }
  return context;
};
