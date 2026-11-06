import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Pressable,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import Animated from 'react-native-reanimated';
import { Mail, GraduationCap, School, Sparkles, ArrowRight } from 'lucide-react-native';
import { useTheme } from '../../src/hooks/useTheme';
import { Button, Input, ServerWakeup } from '../../src/components/ui';
import { validateEmail, isFacultyEmail, isStudentEmail, getDetectedRole } from '../../src/utils/validators';
import { authService } from '../../src/services/authService';
import { UserRole } from '../../src/types';
import { useFadeIn, useSlideUp } from '../../src/animations/microInteractions';

export default function LoginScreen() {
  const router = useRouter();
  const { theme, isDark } = useTheme();

  const [role, setRole] = useState<UserRole>('student');
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showServerWakeup, setShowServerWakeup] = useState(false);

  const headerAnim = useFadeIn(100, 400);
  const formAnim = useSlideUp(250, 30, 400);

  const handleRoleSelect = (selectedRole: UserRole) => {
    setRole(selectedRole);
    setError('');
  };

  const handleSendOTP = async () => {
    setError('');
    const cleanEmail = email.trim().toLowerCase();

    if (!cleanEmail) {
      setError('Please enter your email address');
      return;
    }

    if (!validateEmail(cleanEmail)) {
      setError('Please enter a valid email address');
      return;
    }

    const detectedRole = getDetectedRole(cleanEmail);
    if (!detectedRole) {
      setError('Access restricted: Students must use @vrsec.ac.in, Faculty must use @siddhartha.ac.in.');
      return;
    }

    // Automatically enforce and assign the detected role
    const effectiveRole = detectedRole;
    if (role !== effectiveRole) {
      setRole(effectiveRole);
    }

    setIsLoading(true);

    const timer = setTimeout(() => {
      setShowServerWakeup(true);
    }, 4000);

    try {
      await authService.requestOTP(cleanEmail, effectiveRole);
      clearTimeout(timer);
      setShowServerWakeup(false);
      setIsLoading(false);

      router.push({
        pathname: '/(auth)/otp-verify',
        params: { email: cleanEmail, role: effectiveRole },
      });
    } catch (err: any) {
      clearTimeout(timer);
      setShowServerWakeup(false);
      setIsLoading(false);

      const message =
        err?.response?.data?.detail ||
        err?.message ||
        'Failed to send OTP. Please verify your connection.';

      if (err?.code === 'ERR_NETWORK' || !err?.response) {
        Alert.alert(
          'Notice',
          'Could not connect to backend server. Would you like to proceed with demo verification?',
          [
            { text: 'Cancel', style: 'cancel' },
            {
              text: 'Continue (Demo OTP: 123456)',
              onPress: () => {
                router.push({
                  pathname: '/(auth)/otp-verify',
                  params: { email: cleanEmail, role },
                });
              },
            },
          ]
        );
      } else {
        setError(message);
      }
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={[styles.container, { backgroundColor: theme.colors.background }]}
    >
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
      >
        <View
          style={[
            styles.decorCircleTop,
            { backgroundColor: isDark ? 'rgba(74, 144, 217, 0.08)' : 'rgba(74, 144, 217, 0.12)' },
          ]}
        />
        <View
          style={[
            styles.decorCircleBottom,
            { backgroundColor: isDark ? 'rgba(30, 58, 95, 0.15)' : 'rgba(235, 244, 252, 0.8)' },
          ]}
        />

        <Animated.View style={[styles.header, headerAnim]}>
          <View
            style={[
              styles.logoBadge,
              { backgroundColor: theme.colors.primaryLight },
            ]}
          >
            <Sparkles size={32} color={theme.colors.primary} />
          </View>
          <Text style={[styles.appName, { color: theme.colors.textPrimary }]}>
            LearnPath AI
          </Text>
          <Text style={[styles.appTagline, { color: theme.colors.textSecondary }]}>
            Adaptive Learning Powered by Artificial Intelligence
          </Text>
        </Animated.View>

        <Animated.View
          style={[
            styles.formCard,
            {
              backgroundColor: theme.colors.surface,
              borderColor: theme.colors.borderLight,
            },
            formAnim,
          ]}
        >
          <View style={styles.roleToggleContainer}>
            <Text style={[styles.sectionTitle, { color: theme.colors.textSecondary }]}>
              SELECT YOUR ROLE
            </Text>
            <View
              style={[
                styles.roleToggleGroup,
                {
                  backgroundColor: isDark
                    ? theme.colors.surfaceSubtle
                    : theme.colors.inputBackground,
                },
              ]}
            >
              <Pressable
                onPress={() => handleRoleSelect('student')}
                style={[
                  styles.roleOption,
                  role === 'student' && {
                    backgroundColor: theme.colors.surfaceElevated,
                    shadowColor: '#000',
                    shadowOffset: { width: 0, height: 2 },
                    shadowOpacity: 0.08,
                    shadowRadius: 4,
                    elevation: 2,
                  },
                ]}
              >
                <GraduationCap
                  size={18}
                  color={
                    role === 'student'
                      ? theme.colors.primary
                      : theme.colors.textTertiary
                  }
                />
                <Text
                  style={[
                    styles.roleText,
                    {
                      color:
                        role === 'student'
                          ? theme.colors.primary
                          : theme.colors.textSecondary,
                      fontWeight: role === 'student' ? '700' : '500',
                    },
                  ]}
                >
                  Student
                </Text>
              </Pressable>

              <Pressable
                onPress={() => handleRoleSelect('faculty')}
                style={[
                  styles.roleOption,
                  role === 'faculty' && {
                    backgroundColor: theme.colors.surfaceElevated,
                    shadowColor: '#000',
                    shadowOffset: { width: 0, height: 2 },
                    shadowOpacity: 0.08,
                    shadowRadius: 4,
                    elevation: 2,
                  },
                ]}
              >
                <School
                  size={18}
                  color={
                    role === 'faculty'
                      ? theme.colors.primary
                      : theme.colors.textTertiary
                  }
                />
                <Text
                  style={[
                    styles.roleText,
                    {
                      color:
                        role === 'faculty'
                          ? theme.colors.primary
                          : theme.colors.textSecondary,
                      fontWeight: role === 'faculty' ? '700' : '500',
                    },
                  ]}
                >
                  Faculty
                </Text>
              </Pressable>
            </View>
          </View>

          <Input
            label={role === 'faculty' ? 'Faculty Institutional Email' : 'Student Institutional Email'}
            placeholder={
              role === 'faculty'
                ? 'faculty@siddhartha.ac.in'
                : 'student@vrsec.ac.in'
            }
            value={email}
            onChangeText={(text) => {
              setEmail(text);
              if (error) setError('');
              const detected = getDetectedRole(text.trim());
              if (detected && detected !== role) {
                setRole(detected);
              }
            }}
            keyboardType="email-address"
            autoCapitalize="none"
            autoCorrect={false}
            error={error}
            hint={
              role === 'faculty'
                ? 'Restricted to @siddhartha.ac.in faculty accounts'
                : 'Restricted to @vrsec.ac.in student accounts'
            }
            leftIcon={
              <Mail
                size={20}
                color={error ? theme.colors.error : theme.colors.textTertiary}
              />
            }
          />

          <Button
            title="Send Verification Code"
            onPress={handleSendOTP}
            loading={isLoading}
            variant="primary"
            size="lg"
            rightIcon={<ArrowRight size={18} color="#FFFFFF" />}
            style={styles.submitButton}
          />
        </Animated.View>

        <Text style={[styles.footerText, { color: theme.colors.textTertiary }]}>
          Velagapudi Ramakrishna Siddhartha Engineering College
        </Text>
      </ScrollView>

      <ServerWakeup
        visible={showServerWakeup}
        onRetry={handleSendOTP}
      />
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    paddingHorizontal: 24,
    paddingTop: 64,
    paddingBottom: 32,
    justifyContent: 'center',
  },
  decorCircleTop: {
    position: 'absolute',
    top: -40,
    right: -40,
    width: 220,
    height: 220,
    borderRadius: 110,
  },
  decorCircleBottom: {
    position: 'absolute',
    bottom: -60,
    left: -60,
    width: 260,
    height: 260,
    borderRadius: 130,
  },
  header: {
    alignItems: 'center',
    marginBottom: 32,
  },
  logoBadge: {
    width: 64,
    height: 64,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
  },
  appName: {
    fontSize: 28,
    fontWeight: '800',
    letterSpacing: -0.6,
    marginBottom: 6,
  },
  appTagline: {
    fontSize: 14,
    textAlign: 'center',
    maxWidth: 280,
  },
  formCard: {
    borderRadius: 20,
    borderWidth: 1,
    padding: 24,
    shadowColor: '#1E3A5F',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.08,
    shadowRadius: 16,
    elevation: 4,
    marginBottom: 24,
  },
  roleToggleContainer: {
    marginBottom: 20,
  },
  sectionTitle: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.8,
    marginBottom: 10,
  },
  roleToggleGroup: {
    flexDirection: 'row',
    borderRadius: 12,
    padding: 4,
  },
  roleOption: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 10,
    borderRadius: 10,
    gap: 8,
  },
  roleText: {
    fontSize: 14,
  },
  submitButton: {
    marginTop: 8,
  },
  footerText: {
    textAlign: 'center',
    fontSize: 12,
    marginTop: 12,
  },
});
