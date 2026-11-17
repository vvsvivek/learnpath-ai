import React, { useState, useEffect, useContext } from 'react';
import {
  StyleSheet,
  Text,
  View,
  SafeAreaView,
  TouchableOpacity,
  StatusBar,
  Platform,
  Modal,
  ScrollView,
  Alert,
  ActivityIndicator,
  TextInput,
  Keyboard,
} from 'react-native';
import { SafeAreaInsetsContext } from 'react-native-safe-area-context';
import { Colors, Shadows, Radii, Spacing } from '../../styles/theme';
import { api } from '../../services/api';
import HomeScreen from './HomeScreen';
import FacultyHomeScreen from './FacultyHomeScreen';
import LearningPathScreen from './LearningPathScreen';
import FacultyDashboardScreen from './FacultyDashboardScreen';
import ChatScreen from './ChatScreen';
import ProfileScreen from './ProfileScreen';
import { getAreasOfInterestForBranch } from '../../config/academicData';

interface MainLayoutProps {
  userData: any;
  onLogout: () => void;
  onNavigateSettings: () => void;
  facultyBranch?: string;
}

export default function MainLayout({
  userData,
  onLogout,
  onNavigateSettings,
  facultyBranch: _facultyBranch,
}: MainLayoutProps) {
  const [currentUserData, setCurrentUserData] = useState(userData);
  const email = (currentUserData?.email || '').toLowerCase();
  const rawRole = (currentUserData?.role || 'STUDENT').toUpperCase();

  // STRICT BACKEND-ALIGNED ROLE CHECK:
  const isFaculty =
    rawRole === 'FACULTY' ||
    email.endsWith('@siddhartha.ac.in') ||
    email === 'vivekvaranasi99@gmail.com';

  // Academic profile completion check:
  const hasCohortDetails = Boolean(
    currentUserData?.branch && currentUserData?.academicYear && currentUserData?.semester
  );
  const hasCgpa =
    currentUserData?.cgpa != null &&
    !isNaN(Number(currentUserData?.cgpa)) &&
    Number(currentUserData?.cgpa) > 0;

  const isAcademicProfileIncomplete = !isFaculty && (!hasCohortDetails || !hasCgpa);
  const isOnlyCgpaMissing = !isFaculty && hasCohortDetails && !hasCgpa;

  const [showOnboarding, setShowOnboarding] = useState(isAcademicProfileIncomplete);
  const [showAdvancedEdit, setShowAdvancedEdit] = useState(!hasCohortDetails);

  const [selectedBranch, setSelectedBranch] = useState(currentUserData?.branch || 'CSE');
  const [selectedYear, setSelectedYear] = useState(currentUserData?.academicYear || '2nd Year');
  const [selectedSem, setSelectedSem] = useState(currentUserData?.semester || '1st Sem');
  const [selectedInterest, setSelectedInterest] = useState(
    currentUserData?.areaOfInterest || getAreasOfInterestForBranch(currentUserData?.branch || 'CSE')[0]
  );
  const [cgpaInput, setCgpaInput] = useState(
    currentUserData?.cgpa != null && Number(currentUserData?.cgpa) > 0
      ? Number(currentUserData.cgpa).toFixed(2)
      : ''
  );
  const [cgpaError, setCgpaError] = useState('');
  const [onboardingLoading, setOnboardingLoading] = useState(false);

  // Keyboard visibility state to prevent navigation bar from covering inputs or keyboard
  const [isKeyboardVisible, setKeyboardVisible] = useState(false);
  useEffect(() => {
    const showSub = Keyboard.addListener(
      Platform.OS === 'ios' ? 'keyboardWillShow' : 'keyboardDidShow',
      () => setKeyboardVisible(true)
    );
    const hideSub = Keyboard.addListener(
      Platform.OS === 'ios' ? 'keyboardWillHide' : 'keyboardDidHide',
      () => setKeyboardVisible(false)
    );
    return () => {
      showSub.remove();
      hideSub.remove();
    };
  }, []);

  const validateCgpa = (val: string): { valid: boolean; error?: string; parsed?: number } => {
    const trimmed = val.trim();
    if (!trimmed) {
      return { valid: false, error: 'CGPA is mandatory. Please enter a value between 0.00 and 10.00.' };
    }
    const num = Number(trimmed);
    if (isNaN(num) || !isFinite(num)) {
      return { valid: false, error: 'Invalid CGPA. Please enter a numerical value between 0.00 and 10.00.' };
    }
    if (num < 0 || num > 10) {
      return { valid: false, error: 'CGPA must be between 0.00 and 10.00.' };
    }
    const parts = trimmed.split('.');
    if (parts.length > 2 || (parts.length === 2 && parts[1].length > 2)) {
      return { valid: false, error: 'CGPA allows a maximum of 2 decimal places only.' };
    }
    return { valid: true, parsed: Math.round(num * 100) / 100 };
  };

  const handleSaveOnboarding = async () => {
    const cgpaResult = validateCgpa(cgpaInput);
    if (!cgpaResult.valid) {
      setCgpaError(cgpaResult.error || 'Please enter a valid CGPA.');
      Alert.alert('Validation Error', cgpaResult.error || 'Please enter a valid CGPA between 0.00 and 10.00.');
      return;
    }
    setCgpaError('');

    try {
      setOnboardingLoading(true);
      await api.updateProfile({
        email: currentUserData.email,
        branch: selectedBranch,
        academicYear: selectedYear,
        semester: selectedSem,
        cgpa: cgpaResult.parsed,
        areaOfInterest: selectedInterest,
      });
      const updated = {
        ...currentUserData,
        branch: selectedBranch,
        academicYear: selectedYear,
        semester: selectedSem,
        cgpa: cgpaResult.parsed,
        areaOfInterest: selectedInterest,
      };
      setCurrentUserData(updated);
      setShowOnboarding(false);
      Alert.alert('Profile Complete!', 'Your academic details, area of interest, and CGPA have been saved successfully.');
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to update academic profile.');
    } finally {
      setOnboardingLoading(false);
    }
  };

  const [activeTab, setActiveTab] = useState<string>('Home');
  const [activeSubject, setActiveSubject] = useState('Data Structures & Algorithmic Complexity');

  const renderContent = () => {
    if (isFaculty) {
      switch (activeTab) {
        case 'Courses':
          return <FacultyDashboardScreen userData={currentUserData} />;
        case 'Chat':
          return <ChatScreen userData={currentUserData} activeSubject="Faculty Institutional AI Hub" />;
        case 'Profile':
          return (
            <ProfileScreen
              userData={currentUserData}
              onLogout={onLogout}
              onNavigateSettings={onNavigateSettings}
              onEditAcademicProfile={() => {
                setSelectedBranch(currentUserData?.branch || 'CSE');
                setSelectedYear(currentUserData?.academicYear || '2nd Year');
                setSelectedSem(currentUserData?.semester || '1st Sem');
                setSelectedInterest(
                  currentUserData?.areaOfInterest || getAreasOfInterestForBranch(currentUserData?.branch || 'CSE')[0]
                );
                setCgpaInput(
                  currentUserData?.cgpa != null && Number(currentUserData?.cgpa) > 0
                    ? Number(currentUserData.cgpa).toFixed(2)
                    : ''
                );
                setCgpaError('');
                setShowAdvancedEdit(true);
                setShowOnboarding(true);
              }}
            />
          );
        case 'Home':
        default:
          return <FacultyHomeScreen userData={currentUserData} onNavigateTab={setActiveTab} />;
      }
    } else {
      switch (activeTab) {
        case 'Roadmap':
          return (
            <LearningPathScreen
              userData={currentUserData}
              activeSubject={activeSubject}
              onSelectSubject={setActiveSubject}
            />
          );
        case 'Chat':
          return <ChatScreen userData={currentUserData} activeSubject={activeSubject} />;
        case 'Profile':
          return (
            <ProfileScreen
              userData={currentUserData}
              onLogout={onLogout}
              onNavigateSettings={onNavigateSettings}
              onEditAcademicProfile={() => {
                setSelectedBranch(currentUserData?.branch || 'CSE');
                setSelectedYear(currentUserData?.academicYear || '2nd Year');
                setSelectedSem(currentUserData?.semester || '1st Sem');
                setSelectedInterest(
                  currentUserData?.areaOfInterest || getAreasOfInterestForBranch(currentUserData?.branch || 'CSE')[0]
                );
                setCgpaInput(
                  currentUserData?.cgpa != null && Number(currentUserData?.cgpa) > 0
                    ? Number(currentUserData.cgpa).toFixed(2)
                    : ''
                );
                setCgpaError('');
                setShowAdvancedEdit(true);
                setShowOnboarding(true);
              }}
            />
          );
        case 'Home':
        default:
          return (
            <HomeScreen
              userData={currentUserData}
              onNavigate={setActiveTab}
              onSelectSubject={setActiveSubject}
            />
          );
      }
    }
  };

  const navItems = isFaculty
    ? [
        { id: 'Home', label: 'Overview', icon: '🏛️' },
        { id: 'Courses', label: 'Audit & Assign', icon: '📋' },
        { id: 'Chat', label: 'Faculty AI', icon: '🤖' },
        { id: 'Profile', label: 'Profile', icon: '👤' },
      ]
    : [
        { id: 'Home', label: 'Dashboard', icon: '🏠' },
        { id: 'Roadmap', label: 'Curriculum', icon: '📖' },
        { id: 'Chat', label: 'AI Tutor', icon: '🤖' },
        { id: 'Profile', label: 'Profile', icon: '👤' },
      ];

  const insets = useContext(SafeAreaInsetsContext);
  // Calculate safe bottom padding above Android system navigation / iOS home indicator
  const bottomPadding = Math.max(insets?.bottom ?? 0, Platform.OS === 'ios' ? 14 : 10);
  const safeTopPadding = Platform.OS === 'android'
    ? Math.max(insets?.top ?? 0, StatusBar.currentHeight || 0, 36)
    : 0;

  return (
    <SafeAreaView style={[styles.container, { paddingTop: safeTopPadding }]}>
      <StatusBar barStyle="dark-content" />

      {/* Screen Container (Stops cleanly above floating navigation bar) */}
      <View style={styles.screenWrapper}>{renderContent()}</View>

      {/* Floating, Rounded-Pill Bottom Navigation Bar */}
      {!isKeyboardVisible && (
        <View style={[styles.navContainer, { paddingBottom: bottomPadding }]}>
          <View style={styles.navBar}>
            {navItems.map((item) => {
              const isActive = activeTab === item.id;
              return (
                <TouchableOpacity
                  key={item.id}
                  testID={`bottom-nav-${item.id}`}
                  style={[styles.navButton, isActive && styles.navButtonActive]}
                  onPress={() => setActiveTab(item.id)}
                  activeOpacity={0.7}
                  accessibilityRole="tab"
                  accessibilityState={{ selected: isActive }}
                  accessibilityLabel={item.label}
                >
                  <Text style={[styles.navIcon, isActive && styles.navIconActive]}>
                    {item.icon}
                  </Text>
                  <Text
                    style={[styles.navLabel, isActive && styles.navLabelActive]}
                    numberOfLines={1}
                    ellipsizeMode="tail"
                  >
                    {item.label}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </View>
        </View>
      )}

      {/* Academic Onboarding & Profile Completion Modal */}
      <Modal visible={showOnboarding} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>
              {isOnlyCgpaMissing && !showAdvancedEdit
                ? 'Complete Profile: Enter CGPA'
                : 'Complete Academic Profile'}
            </Text>
            <Text style={styles.modalSubtitle}>
              {isOnlyCgpaMissing && !showAdvancedEdit
                ? 'Your department details are saved. Please provide your current CGPA so we can calibrate your course recommendations.'
                : 'Configure your department standing so we can tailor your study roadmaps and quizzes.'}
            </Text>

            <ScrollView showsVerticalScrollIndicator={false} style={{ maxHeight: 340 }}>
              {isOnlyCgpaMissing && !showAdvancedEdit ? (
                <View style={styles.savedCohortCard}>
                  <Text style={styles.savedCohortTitle}>CURRENT ACADEMIC PROFILE</Text>
                  <Text style={styles.savedCohortText}>
                    {selectedBranch} Engineering • {selectedYear} • {selectedSem}
                  </Text>
                  <TouchableOpacity
                    style={styles.changeCohortBtn}
                    onPress={() => setShowAdvancedEdit(true)}
                  >
                    <Text style={styles.changeCohortLink}>Edit Branch, Year or Semester</Text>
                  </TouchableOpacity>
                </View>
              ) : (
                <>
                  <Text style={styles.fieldLabel}>Department / Branch</Text>
                  <View style={styles.chipRow}>
                    {['CSE', 'IT', 'ECE', 'EEE', 'MECH', 'CIVIL', 'AIML', 'AIDS'].map((b) => (
                      <TouchableOpacity
                        key={b}
                        style={[styles.chip, selectedBranch === b && styles.chipActive]}
                        onPress={() => {
                          setSelectedBranch(b);
                          const interests = getAreasOfInterestForBranch(b);
                          if (!interests.includes(selectedInterest)) {
                            setSelectedInterest(interests[0]);
                          }
                        }}
                      >
                        <Text style={[styles.chipText, selectedBranch === b && styles.chipTextActive]}>{b}</Text>
                      </TouchableOpacity>
                    ))}
                  </View>

                  <Text style={styles.fieldLabel}>Academic Year</Text>
                  <View style={styles.chipRow}>
                    {['1st Year', '2nd Year', '3rd Year', '4th Year'].map((y) => (
                      <TouchableOpacity
                        key={y}
                        style={[styles.chip, selectedYear === y && styles.chipActive]}
                        onPress={() => setSelectedYear(y)}
                      >
                        <Text style={[styles.chipText, selectedYear === y && styles.chipTextActive]}>{y}</Text>
                      </TouchableOpacity>
                    ))}
                  </View>

                  <Text style={styles.fieldLabel}>Semester</Text>
                  <View style={styles.chipRow}>
                    {['1st Sem', '2nd Sem'].map((s) => (
                      <TouchableOpacity
                        key={s}
                        style={[styles.chip, selectedSem === s && styles.chipActive]}
                        onPress={() => setSelectedSem(s)}
                      >
                        <Text style={[styles.chipText, selectedSem === s && styles.chipTextActive]}>{s}</Text>
                      </TouchableOpacity>
                    ))}
                  </View>

                  <Text style={styles.fieldLabel}>Area of Interest ({selectedBranch}) *</Text>
                  <Text style={styles.fieldSubLabel}>Select your primary focus area for tailored course suggestions</Text>
                  <View style={styles.interestChipGrid}>
                    {getAreasOfInterestForBranch(selectedBranch).map((interest) => (
                      <TouchableOpacity
                        key={interest}
                        style={[styles.interestChip, selectedInterest === interest && styles.interestChipActive]}
                        onPress={() => setSelectedInterest(interest)}
                      >
                        <Text style={[styles.interestChipText, selectedInterest === interest && styles.interestChipTextActive]}>
                          {selectedInterest === interest ? '✓ ' : ''}{interest}
                        </Text>
                      </TouchableOpacity>
                    ))}
                  </View>
                </>
              )}

              {/* CGPA Input Field */}
              <Text style={styles.fieldLabel}>Cumulative GPA (CGPA) *</Text>
              <Text style={styles.fieldSubLabel}>Scale: 0.00 to 10.00 (e.g. 8.75, up to 2 decimals)</Text>
              <TextInput
                style={[styles.textInput, !!cgpaError && styles.textInputError]}
                value={cgpaInput}
                onChangeText={(text) => {
                  setCgpaInput(text);
                  if (cgpaError) setCgpaError('');
                }}
                placeholder="e.g. 8.50"
                placeholderTextColor={Colors.textTertiary}
                keyboardType="decimal-pad"
                maxLength={5}
                returnKeyType="done"
              />
              {!!cgpaError && (
                <Text style={styles.errorText}>{cgpaError}</Text>
              )}
            </ScrollView>

            <TouchableOpacity
              style={styles.saveButton}
              onPress={handleSaveOnboarding}
              disabled={onboardingLoading}
            >
              {onboardingLoading ? (
                <ActivityIndicator color="#fff" size="small" />
              ) : (
                <Text style={styles.saveButtonText}>Confirm & Start Learning</Text>
              )}
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  screenWrapper: {
    flex: 1,
  },
  // Floating, Rounded-Pill Bottom Navigation Bar
  navContainer: {
    backgroundColor: Colors.background,
    paddingHorizontal: 16,
    paddingTop: 4,
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
  },
  navBar: {
    flexDirection: 'row',
    width: '100%',
    height: 70,
    backgroundColor: Colors.surface,
    borderRadius: 35,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingHorizontal: 8,
    alignItems: 'center',
    justifyContent: 'space-between',
    // Android subtle elevation & shadow
    elevation: 8,
    // iOS shadow
    shadowColor: Colors.shadowColor || '#0F172A',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.10,
    shadowRadius: 12,
  },
  navButton: {
    flex: 1,
    height: 56,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 4,
    paddingHorizontal: 2,
    marginHorizontal: 2,
    borderRadius: 28,
  },
  navButtonActive: {
    backgroundColor: '#EDE9FE',
  },
  navIcon: {
    fontSize: 22,
    lineHeight: 26,
    marginBottom: 2,
    textAlign: 'center',
    opacity: 0.85,
  },
  navIconActive: {
    opacity: 1,
    transform: [{ scale: 1.05 }],
  },
  navLabel: {
    fontSize: 11,
    lineHeight: 14,
    fontWeight: '500',
    color: '#0F172A',
    textAlign: 'center',
    letterSpacing: -0.1,
  },
  navLabelActive: {
    color: '#6D28D9',
    fontWeight: '700',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.65)',
    justifyContent: 'center',
    padding: Spacing.lg,
  },
  modalCard: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.lg,
    padding: Spacing.lg,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.elevated,
  },
  modalTitle: {
    fontSize: 17,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginBottom: 4,
    textAlign: 'center',
  },
  modalSubtitle: {
    fontSize: 12,
    color: Colors.textSecondary,
    marginBottom: Spacing.md,
    textAlign: 'center',
    lineHeight: 16,
  },
  fieldLabel: {
    fontSize: 12,
    fontWeight: '600',
    color: Colors.textPrimary,
    marginTop: Spacing.sm,
    marginBottom: Spacing.xs,
  },
  chipRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
    marginBottom: Spacing.xs,
  },
  chip: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: Radii.sm,
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  chipActive: {
    backgroundColor: Colors.primary,
    borderColor: Colors.primary,
  },
  chipText: {
    fontSize: 11,
    fontWeight: '500',
    color: Colors.textSecondary,
  },
  chipTextActive: {
    color: '#ffffff',
    fontWeight: '600',
  },
  interestChipGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
    marginBottom: Spacing.sm,
  },
  interestChip: {
    paddingHorizontal: 9,
    paddingVertical: 5,
    borderRadius: Radii.sm,
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  interestChipActive: {
    backgroundColor: '#EEF2FF',
    borderColor: Colors.primary,
  },
  interestChipText: {
    fontSize: 11,
    fontWeight: '500',
    color: Colors.textSecondary,
  },
  interestChipTextActive: {
    color: Colors.primary,
    fontWeight: '700',
  },
  saveButton: {
    backgroundColor: Colors.primary,
    paddingVertical: 11,
    borderRadius: Radii.md,
    alignItems: 'center',
    marginTop: Spacing.md,
  },
  saveButtonText: {
    color: '#ffffff',
    fontSize: 13,
    fontWeight: '600',
  },
  savedCohortCard: {
    backgroundColor: Colors.primaryLight,
    padding: Spacing.md,
    borderRadius: Radii.md,
    marginBottom: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  savedCohortTitle: {
    fontSize: 10,
    fontWeight: '700',
    color: Colors.primaryDark,
    letterSpacing: 0.5,
    marginBottom: 2,
  },
  savedCohortText: {
    fontSize: 13,
    fontWeight: '600',
    color: Colors.textPrimary,
  },
  changeCohortBtn: {
    marginTop: 6,
    alignSelf: 'flex-start',
  },
  changeCohortLink: {
    fontSize: 11,
    color: Colors.primary,
    fontWeight: '600',
  },
  fieldSubLabel: {
    fontSize: 10,
    color: Colors.textTertiary,
    marginBottom: Spacing.xs,
  },
  textInput: {
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: Radii.md,
    paddingHorizontal: 12,
    paddingVertical: 8,
    fontSize: 14,
    fontWeight: '600',
    color: Colors.textPrimary,
  },
  textInputError: {
    borderColor: Colors.danger,
    borderWidth: 1,
  },
  errorText: {
    fontSize: 11,
    color: Colors.danger,
    fontWeight: '500',
    marginTop: 4,
  },
});
