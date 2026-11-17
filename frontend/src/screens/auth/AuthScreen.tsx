import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  SafeAreaView,
  TextInput,
  TouchableOpacity,
  StatusBar,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Colors, Shadows, Radii, Spacing, Typography } from '../../styles/theme';
import { WalkingStudentIllustration, GoogleLogoIcon } from '../../components/Illustrations';
import { api } from '../../services/api';
import { getAreasOfInterestForBranch } from '../../config/academicData';

interface AuthScreenProps {
  initialRole?: 'STUDENT' | 'FACULTY';
  initialMode?: 'SIGN_IN' | 'REGISTER';
  onLoginComplete: (userData: any, branch?: string) => void;
  onBack: () => void;
}

type AuthMode = 'SIGN_IN' | 'REGISTER';
type Step = 'EMAIL_INPUT' | 'OTP_VERIFY';

export default function AuthScreen({
  initialRole = 'STUDENT',
  initialMode = 'SIGN_IN',
  onLoginComplete,
  onBack,
}: AuthScreenProps) {
  const [role, setRole] = useState<'STUDENT' | 'FACULTY'>(initialRole);
  const [authMode, setAuthMode] = useState<AuthMode>(initialMode);
  const [step, setStep] = useState<Step>('EMAIL_INPUT');

  // Form Fields
  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [loading, setLoading] = useState(false);

  const insets = useSafeAreaInsets();
  const safeTopPadding = Platform.OS === 'android'
    ? Math.max(insets.top, StatusBar.currentHeight || 0, 36) + 12
    : Math.max(insets.top, 12);

  // Student Onboarding Fields during Registration
  const [studentBranch, setStudentBranch] = useState('CSE');
  const [studentYear, setStudentYear] = useState('2nd Year');
  const [studentSemester, setStudentSemester] = useState('1st Sem');
  const [studentCgpa, setStudentCgpa] = useState('8.50');
  const [studentInterest, setStudentInterest] = useState('Artificial Intelligence & Machine Learning');

  // Faculty Onboarding Fields during Registration
  const [facultyBranchSelection, setFacultyBranchSelection] = useState('CSE');

  // OTP Timer State
  const [timerSeconds, setTimerSeconds] = useState(60);
  const [canResend, setCanResend] = useState(false);

  useEffect(() => {
    let interval: any = null;
    if (step === 'OTP_VERIFY' && timerSeconds > 0) {
      interval = setInterval(() => {
        setTimerSeconds((prev) => prev - 1);
      }, 1000);
    } else if (timerSeconds === 0) {
      setCanResend(true);
    }
    return () => clearInterval(interval);
  }, [step, timerSeconds]);

  const handleSendOtp = async () => {
    const cleanEmail = email.trim().toLowerCase();
    if (!cleanEmail || !cleanEmail.includes('@') || !cleanEmail.includes('.')) {
      Alert.alert('Invalid Email', 'Please enter a valid academic or personal email address.');
      return;
    }

    if (role === 'FACULTY' && authMode === 'REGISTER') {
      const isSiddhartha = cleanEmail.endsWith('@siddhartha.ac.in');
      const isAllowlisted = cleanEmail === 'vivekvaranasi99@gmail.com';
      if (!isSiddhartha && !isAllowlisted) {
        Alert.alert(
          'Unauthorized Faculty Email',
          'Faculty registration is restricted to @siddhartha.ac.in institutional emails or authorized faculty addresses (e.g. vivekvaranasi99@gmail.com). Please switch to the Student Portal to register with this email.'
        );
        return;
      }
    }

    if (authMode === 'REGISTER' && !fullName.trim()) {
      Alert.alert('Name Required', 'Please enter your full name for registration.');
      return;
    }

    try {
      setLoading(true);
      const res = await api.sendOtp(cleanEmail, role, authMode === 'SIGN_IN' ? 'LOGIN' : 'REGISTER');
      if (res && res.dispatched === false) {
        throw new Error(res.message || 'Email delivery service failed to dispatch verification code. Please try again shortly.');
      }
      setStep('OTP_VERIFY');
      setTimerSeconds(60);
      setCanResend(false);
      Alert.alert('Verification Code Sent', `A 6-digit verification code has been dispatched to ${cleanEmail}. Check your inbox.`);
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to send verification code. Please verify your connection.');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async () => {
    const cleanEmail = email.trim().toLowerCase();
    const cleanOtp = otpCode.trim();

    if (!cleanOtp || cleanOtp.length < 4) {
      Alert.alert('Missing Code', 'Please enter the 6-digit verification code sent to your email.');
      return;
    }

    try {
      setLoading(true);
      const res = await api.verifyOtp(
        cleanEmail,
        cleanOtp,
        fullName.trim(),
        undefined,
        role,
        authMode === 'SIGN_IN' ? 'LOGIN' : 'REGISTER'
      );
      if (res && res.user) {
        if (role === 'STUDENT' && authMode === 'REGISTER') {
          try {
            const parsedCgpa = studentCgpa ? parseFloat(studentCgpa) : 8.0;
            const updatedProfile = await api.updateProfile({
              email: cleanEmail,
              branch: studentBranch,
              academicYear: studentYear,
              semester: studentSemester,
              cgpa: isNaN(parsedCgpa) ? 8.0 : parsedCgpa,
              areaOfInterest: studentInterest,
            });
            Alert.alert('Welcome!', 'Registration completed! Your learning roadmaps and recommendations have been tailored to your profile.');
            onLoginComplete({ ...(updatedProfile || res.user), token: res.token, branch: studentBranch, academicYear: studentYear, semester: studentSemester, cgpa: parsedCgpa, areaOfInterest: studentInterest }, studentBranch);
            return;
          } catch (syncErr) {
            console.warn('Profile sync fallback:', syncErr);
          }
        } else if (role === 'FACULTY' && authMode === 'REGISTER') {
          try {
            const updatedProfile = await api.updateProfile({
              email: cleanEmail,
              branch: facultyBranchSelection,
            });
            Alert.alert('Welcome Professor!', `Registration completed! Your account is assigned to the Department of ${facultyBranchSelection}.`);
            onLoginComplete({ ...(updatedProfile || res.user), token: res.token, branch: facultyBranchSelection }, facultyBranchSelection);
            return;
          } catch (syncErr) {
            console.warn('Faculty branch sync fallback:', syncErr);
          }
        }
        Alert.alert('Welcome!', `Logged in successfully as ${res.user.role}.`);
        onLoginComplete({ ...res.user, token: res.token }, res.user.branch || 'CSE');
      } else {
        throw new Error('Authentication response did not contain user credentials.');
      }
    } catch (err: any) {
      Alert.alert('Verification Failed', err.message || 'Invalid or expired OTP code.');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSignIn = async () => {
    Alert.alert(
      'Google Authentication',
      'Please select your university or personal Google account.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Continue with Google',
          onPress: async () => {
            if (!email.trim()) {
              Alert.alert('Account Required', 'Please enter your Google account email in the input box first.');
              return;
            }
            try {
              setLoading(true);
              const clean = email.trim().toLowerCase();
              const res = await api.verifyOtp(clean, '000000', fullName.trim());
              onLoginComplete(res.user, res.user.branch || 'CSE');
            } catch {
              handleSendOtp();
            } finally {
              setLoading(false);
            }
          },
        },
      ]
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={{ flex: 1 }}
      >
        <ScrollView
          contentContainerStyle={[styles.scroll, { paddingTop: safeTopPadding }]}
          showsVerticalScrollIndicator={false}
        >
          {/* Top Bar with Back Button */}
          <View style={styles.topBar}>
            <TouchableOpacity
              onPress={onBack}
              style={styles.backButton}
              hitSlop={{ top: 20, bottom: 20, left: 20, right: 20 }}
              activeOpacity={0.7}
              accessibilityRole="button"
              accessibilityLabel="Back to welcome screen"
            >
              <Text style={styles.backButtonText}>← Back</Text>
            </TouchableOpacity>
            <Text style={styles.topBarTitle}>LearnPath AI</Text>
            <View style={{ width: 84 }} />
          </View>

          {/* Compact Educational Vector Illustration */}
          <View style={styles.illustrationWrap}>
            <WalkingStudentIllustration width={160} height={105} />
          </View>

          {/* Auth Card */}
          <View style={styles.authCard}>

            {/* Portal Role Selector (Student vs Faculty) */}
            <View style={styles.roleSelector}>
              <TouchableOpacity
                style={[styles.roleTab, role === 'STUDENT' && styles.activeStudentRoleTab]}
                onPress={() => { setRole('STUDENT'); setStep('EMAIL_INPUT'); }}
                activeOpacity={0.7}
              >
                <Text style={[styles.roleTabText, role === 'STUDENT' && styles.activeStudentRoleText]}>
                  🎓 Student Portal
                </Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.roleTab, role === 'FACULTY' && styles.activeFacultyRoleTab]}
                onPress={() => { setRole('FACULTY'); setStep('EMAIL_INPUT'); }}
                activeOpacity={0.7}
              >
                <Text style={[styles.roleTabText, role === 'FACULTY' && styles.activeFacultyRoleText]}>
                  🏛️ Faculty Portal
                </Text>
              </TouchableOpacity>
            </View>
            
            {/* Mode Switcher Tabs (Sign In vs Register) */}
            <View style={styles.tabContainer}>
              <TouchableOpacity
                style={[styles.tab, authMode === 'SIGN_IN' && styles.activeTab]}
                onPress={() => { setAuthMode('SIGN_IN'); setStep('EMAIL_INPUT'); }}
              >
                <Text style={[styles.tabText, authMode === 'SIGN_IN' && styles.activeTabText]}>
                  {role === 'STUDENT' ? 'Student Sign In' : 'Faculty Sign In'}
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.tab, authMode === 'REGISTER' && styles.activeTab]}
                onPress={() => { setAuthMode('REGISTER'); setStep('EMAIL_INPUT'); }}
              >
                <Text style={[styles.tabText, authMode === 'REGISTER' && styles.activeTabText]}>
                  {role === 'STUDENT' ? 'Student Register' : 'Faculty Register'}
                </Text>
              </TouchableOpacity>
            </View>

            {/* Portal Role Notice Banner */}
            {role === 'STUDENT' ? (
              <View style={styles.studentNotice}>
                <Text style={styles.studentNoticeText}>
                  🎓 <Text style={{ fontWeight: '700' }}>Student Portal:</Text> Open to all students. Any valid email accepted.
                </Text>
              </View>
            ) : (
              <View style={styles.facultyNotice}>
                <Text style={styles.facultyNoticeText}>
                  🏛️ <Text style={{ fontWeight: '700' }}>Faculty Portal:</Text> Restricted to verified @siddhartha.ac.in emails.
                </Text>
              </View>
            )}

            {step === 'EMAIL_INPUT' ? (
              // Step 1: Email & Optional Name
              <View style={styles.formContainer}>
                {authMode === 'REGISTER' && (
                  <View style={styles.inputGroup}>
                    <Text style={styles.inputLabel}>Full Name</Text>
                    <TextInput
                      style={styles.input}
                      placeholder="e.g. Vivek Varanasi"
                      placeholderTextColor={Colors.textTertiary}
                      value={fullName}
                      onChangeText={setFullName}
                      autoCapitalize="words"
                    />
                  </View>
                )}

                <View style={styles.inputGroup}>
                  <Text style={styles.inputLabel}>
                    {role === 'STUDENT' ? 'Student Email Address' : 'Faculty Email Address'}
                  </Text>
                  <TextInput
                    style={styles.input}
                    placeholder={
                      role === 'STUDENT'
                        ? 'student@gmail.com or @vrsec.ac.in'
                        : 'professor@siddhartha.ac.in'
                    }
                    placeholderTextColor={Colors.textTertiary}
                    value={email}
                    onChangeText={setEmail}
                    keyboardType="email-address"
                    autoCapitalize="none"
                    autoCorrect={false}
                  />
                  <Text style={styles.inputHint}>
                    {role === 'STUDENT'
                      ? 'Students may register with any personal or institutional email.'
                      : 'Faculty require an authorized institutional address.'}
                  </Text>
                </View>

                {role === 'FACULTY' && authMode === 'REGISTER' && (
                  <View style={styles.studentProfileSection}>
                    <Text style={styles.sectionDividerText}>🏛️ FACULTY DEPARTMENT SELECTION</Text>
                    <Text style={styles.inputLabel}>Select Your Department / Branch</Text>
                    <Text style={styles.fieldSubHint}>
                      Your faculty account will be isolated to monitor and evaluate students of this department
                    </Text>
                    <View style={styles.chipRow}>
                      {['CSE', 'IT', 'ECE', 'EEE', 'MECH', 'CIVIL', 'AIML', 'AIDS'].map((b) => (
                        <TouchableOpacity
                          key={b}
                          style={[styles.smallChip, facultyBranchSelection === b && styles.smallChipActive]}
                          onPress={() => setFacultyBranchSelection(b)}
                        >
                          <Text style={[styles.smallChipText, facultyBranchSelection === b && styles.smallChipTextActive]}>
                            {facultyBranchSelection === b ? '✓ ' : ''}{b}
                          </Text>
                        </TouchableOpacity>
                      ))}
                    </View>
                  </View>
                )}

                {role === 'STUDENT' && authMode === 'REGISTER' && (
                  <View style={styles.studentProfileSection}>
                    <Text style={styles.sectionDividerText}>🎓 ACADEMIC PROFILE SETUP</Text>

                    {/* Department / Branch */}
                    <Text style={styles.inputLabel}>Department / Branch</Text>
                    <View style={styles.chipRow}>
                      {['CSE', 'IT', 'ECE', 'EEE', 'MECH', 'CIVIL', 'AIML', 'AIDS'].map((b) => (
                        <TouchableOpacity
                          key={b}
                          style={[styles.smallChip, studentBranch === b && styles.smallChipActive]}
                          onPress={() => {
                            setStudentBranch(b);
                            const interests = getAreasOfInterestForBranch(b);
                            if (!interests.includes(studentInterest)) {
                              setStudentInterest(interests[0]);
                            }
                          }}
                        >
                          <Text style={[styles.smallChipText, studentBranch === b && styles.smallChipTextActive]}>
                            {b}
                          </Text>
                        </TouchableOpacity>
                      ))}
                    </View>

                    {/* Semester */}
                    <Text style={[styles.inputLabel, { marginTop: 10 }]}>Current Semester</Text>
                    <View style={styles.chipRow}>
                      {['1st Sem', '2nd Sem', '3rd Sem', '4th Sem', '5th Sem', '6th Sem', '7th Sem', '8th Sem'].map((s) => (
                        <TouchableOpacity
                          key={s}
                          style={[styles.smallChip, studentSemester === s && styles.smallChipActive]}
                          onPress={() => setStudentSemester(s)}
                        >
                          <Text style={[styles.smallChipText, studentSemester === s && styles.smallChipTextActive]}>
                            {s}
                          </Text>
                        </TouchableOpacity>
                      ))}
                    </View>

                    {/* CGPA */}
                    <View style={[styles.inputGroup, { marginTop: 10 }]}>
                      <Text style={styles.inputLabel}>Cumulative GPA (CGPA)</Text>
                      <TextInput
                        style={styles.input}
                        placeholder="e.g. 8.50 (Scale 0.00 to 10.00)"
                        placeholderTextColor={Colors.textTertiary}
                        value={studentCgpa}
                        onChangeText={setStudentCgpa}
                        keyboardType="decimal-pad"
                        maxLength={5}
                      />
                    </View>

                    {/* Dynamic Area of Interest */}
                    <Text style={[styles.inputLabel, { marginTop: 10 }]}>
                      Area of Interest ({studentBranch})
                    </Text>
                    <Text style={styles.fieldSubHint}>
                      Select your primary technical passion to customize recommendations
                    </Text>
                    <View style={styles.interestChipGrid}>
                      {getAreasOfInterestForBranch(studentBranch).map((interest) => (
                        <TouchableOpacity
                          key={interest}
                          style={[
                            styles.interestChip,
                            studentInterest === interest && styles.interestChipActive,
                          ]}
                          onPress={() => setStudentInterest(interest)}
                        >
                          <Text
                            style={[
                              styles.interestChipText,
                              studentInterest === interest && styles.interestChipTextActive,
                            ]}
                          >
                            {studentInterest === interest ? '✓ ' : ''}{interest}
                          </Text>
                        </TouchableOpacity>
                      ))}
                    </View>
                  </View>
                )}

                {/* Submit Email Button */}
                <TouchableOpacity
                  style={styles.primaryButton}
                  onPress={handleSendOtp}
                  disabled={loading}
                  activeOpacity={0.7}
                >
                  {loading ? (
                    <ActivityIndicator color="#FFFFFF" size="small" />
                  ) : (
                    <Text style={styles.primaryButtonText}>Continue with Email OTP →</Text>
                  )}
                </TouchableOpacity>

                {/* Divider */}
                <View style={styles.dividerRow}>
                  <View style={styles.dividerLine} />
                  <Text style={styles.dividerText}>OR</Text>
                  <View style={styles.dividerLine} />
                </View>

                {/* Google Sign In Button */}
                <TouchableOpacity
                  style={styles.googleButton}
                  onPress={handleGoogleSignIn}
                  disabled={loading}
                  activeOpacity={0.7}
                >
                  <GoogleLogoIcon size={16} />
                  <Text style={styles.googleButtonText}>Continue with Google</Text>
                </TouchableOpacity>
              </View>
            ) : (
              // Step 2: OTP Verification
              <View style={styles.formContainer}>
                <View style={styles.otpHeader}>
                  <Text style={styles.otpTitle}>Verify Your Email</Text>
                  <Text style={styles.otpSubtitle}>
                    Enter the 6-digit code sent to{'\n'}
                    <Text style={{ fontWeight: '700', color: Colors.textPrimary }}>{email}</Text>
                  </Text>
                </View>

                <View style={styles.inputGroup}>
                  <TextInput
                    style={styles.otpInput}
                    placeholder="• • • • • •"
                    placeholderTextColor={Colors.textTertiary}
                    value={otpCode}
                    onChangeText={setOtpCode}
                    keyboardType="number-pad"
                    maxLength={6}
                    autoFocus
                  />
                </View>

                <TouchableOpacity
                  style={styles.primaryButton}
                  onPress={handleVerifyOtp}
                  disabled={loading}
                  activeOpacity={0.7}
                >
                  {loading ? (
                    <ActivityIndicator color="#FFFFFF" size="small" />
                  ) : (
                    <Text style={styles.primaryButtonText}>Verify &amp; Continue →</Text>
                  )}
                </TouchableOpacity>

                {/* Resend OTP Row */}
                <View style={styles.resendRow}>
                  {canResend ? (
                    <TouchableOpacity onPress={handleSendOtp}>
                      <Text style={styles.resendActiveText}>Resend Verification Code</Text>
                    </TouchableOpacity>
                  ) : (
                    <Text style={styles.resendWaitText}>
                      Resend code in <Text style={{ fontWeight: '700' }}>{timerSeconds}s</Text>
                    </Text>
                  )}
                </View>

                <TouchableOpacity
                  style={styles.changeEmailButton}
                  onPress={() => setStep('EMAIL_INPUT')}
                >
                  <Text style={styles.changeEmailText}>← Edit Email Address</Text>
                </TouchableOpacity>
              </View>
            )}

          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  scroll: {
    paddingHorizontal: Spacing.md,
    paddingBottom: Spacing.lg,
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 10,
    marginBottom: 8,
  },
  backButton: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: Radii.md,
    backgroundColor: Colors.surface,
    borderWidth: 1.5,
    borderColor: Colors.border,
    minHeight: 44,
    minWidth: 84,
    justifyContent: 'center',
    alignItems: 'center',
    ...Shadows.small,
  },
  backButtonText: {
    fontSize: 14,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  topBarTitle: {
    fontSize: 17,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  illustrationWrap: {
    alignItems: 'center',
    marginVertical: 4,
  },
  authCard: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.lg,
    padding: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  roleSelector: {
    flexDirection: 'row',
    backgroundColor: Colors.surfaceMuted,
    borderRadius: Radii.md,
    padding: 3,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  roleTab: {
    flex: 1,
    paddingVertical: 6,
    alignItems: 'center',
    borderRadius: Radii.sm,
  },
  activeStudentRoleTab: {
    backgroundColor: Colors.primary,
  },
  activeFacultyRoleTab: {
    backgroundColor: Colors.primaryDark,
  },
  roleTabText: {
    fontSize: 11,
    fontWeight: '600',
    color: Colors.textSecondary,
  },
  activeStudentRoleText: {
    color: '#FFFFFF',
    fontWeight: '700',
  },
  activeFacultyRoleText: {
    color: '#FFFFFF',
    fontWeight: '700',
  },
  tabContainer: {
    flexDirection: 'row',
    backgroundColor: Colors.surfaceMuted,
    borderRadius: Radii.sm,
    padding: 2,
    marginBottom: 10,
  },
  tab: {
    flex: 1,
    paddingVertical: 5,
    alignItems: 'center',
    borderRadius: Radii.xs,
  },
  activeTab: {
    backgroundColor: Colors.surface,
    ...Shadows.small,
  },
  tabText: {
    fontSize: 11,
    fontWeight: '500',
    color: Colors.textSecondary,
  },
  activeTabText: {
    color: Colors.primaryDark,
    fontWeight: '700',
  },
  facultyNotice: {
    backgroundColor: '#FEF3C7',
    paddingHorizontal: 8,
    paddingVertical: 6,
    borderRadius: Radii.sm,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: '#FDE68A',
  },
  facultyNoticeText: {
    fontSize: 10,
    color: '#92400E',
    lineHeight: 14,
  },
  studentNotice: {
    backgroundColor: Colors.primaryLight,
    paddingHorizontal: 8,
    paddingVertical: 6,
    borderRadius: Radii.sm,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  studentNoticeText: {
    fontSize: 10,
    color: Colors.primaryDark,
    lineHeight: 14,
  },
  formContainer: {
    width: '100%',
  },
  inputGroup: {
    marginBottom: 10,
  },
  inputLabel: {
    fontSize: 11,
    fontWeight: '600',
    color: Colors.textPrimary,
    marginBottom: 4,
  },
  input: {
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: Radii.sm,
    paddingHorizontal: 12,
    paddingVertical: 8,
    fontSize: 13,
    color: Colors.textPrimary,
  },
  inputHint: {
    fontSize: 10,
    color: Colors.textTertiary,
    marginTop: 2,
  },
  primaryButton: {
    backgroundColor: Colors.primary,
    paddingVertical: 10,
    borderRadius: Radii.sm,
    alignItems: 'center',
    marginTop: 4,
  },
  primaryButtonText: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '700',
  },
  dividerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 10,
  },
  dividerLine: {
    flex: 1,
    height: 1,
    backgroundColor: Colors.border,
  },
  dividerText: {
    marginHorizontal: 8,
    fontSize: 10,
    fontWeight: '600',
    color: Colors.textTertiary,
  },
  googleButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingVertical: 9,
    borderRadius: Radii.sm,
    gap: 8,
  },
  googleButtonText: {
    fontSize: 12,
    fontWeight: '600',
    color: Colors.textPrimary,
  },
  otpHeader: {
    alignItems: 'center',
    marginBottom: 14,
  },
  otpTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginBottom: 4,
  },
  otpSubtitle: {
    fontSize: 11,
    color: Colors.textSecondary,
    textAlign: 'center',
    lineHeight: 15,
  },
  otpInput: {
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1.5,
    borderColor: Colors.primary,
    borderRadius: Radii.md,
    paddingVertical: 10,
    fontSize: 22,
    fontWeight: '700',
    color: Colors.primaryDark,
    textAlign: 'center',
    letterSpacing: 6,
  },
  resendRow: {
    alignItems: 'center',
    marginTop: 10,
  },
  resendActiveText: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.primary,
  },
  resendWaitText: {
    fontSize: 11,
    color: Colors.textSecondary,
  },
  changeEmailButton: {
    alignItems: 'center',
    marginTop: 8,
  },
  changeEmailText: {
    fontSize: 11,
    color: Colors.textSecondary,
    fontWeight: '500',
  },
  studentProfileSection: {
    backgroundColor: '#F8FAFC',
    borderRadius: Radii.sm,
    padding: 10,
    marginTop: 6,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  sectionDividerText: {
    fontSize: 10,
    fontWeight: '700',
    color: Colors.primaryDark,
    letterSpacing: 0.5,
    marginBottom: 8,
  },
  chipRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
    marginTop: 4,
  },
  smallChip: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: Radii.sm,
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  smallChipActive: {
    backgroundColor: Colors.primary,
    borderColor: Colors.primary,
  },
  smallChipText: {
    fontSize: 10,
    fontWeight: '600',
    color: Colors.textSecondary,
  },
  smallChipTextActive: {
    color: '#FFFFFF',
    fontWeight: '700',
  },
  fieldSubHint: {
    fontSize: 9,
    color: Colors.textTertiary,
    marginBottom: 6,
    marginTop: 1,
  },
  interestChipGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
    marginTop: 2,
  },
  interestChip: {
    paddingHorizontal: 8,
    paddingVertical: 5,
    borderRadius: Radii.sm,
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  interestChipActive: {
    backgroundColor: '#EEF2FF',
    borderColor: Colors.primary,
  },
  interestChipText: {
    fontSize: 10,
    fontWeight: '500',
    color: Colors.textSecondary,
  },
  interestChipTextActive: {
    color: Colors.primary,
    fontWeight: '700',
  },
});
