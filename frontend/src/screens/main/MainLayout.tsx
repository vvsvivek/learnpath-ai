import React, { useState } from 'react';
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
} from 'react-native';
import { Colors, Shadows, Radii } from '../../styles/theme';
import { api } from '../../services/api';
import HomeScreen from './HomeScreen';
import FacultyHomeScreen from './FacultyHomeScreen';
import LearningPathScreen from './LearningPathScreen';
import FacultyDashboardScreen from './FacultyDashboardScreen';
import ChatScreen from './ChatScreen';
import ProfileScreen from './ProfileScreen';

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
  facultyBranch,
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
  // Requires branch, academicYear, semester, AND valid CGPA (0.00 to 10.00)
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
  const [cgpaInput, setCgpaInput] = useState(
    currentUserData?.cgpa != null && Number(currentUserData?.cgpa) > 0
      ? Number(currentUserData.cgpa).toFixed(2)
      : ''
  );
  const [cgpaError, setCgpaError] = useState('');
  const [onboardingLoading, setOnboardingLoading] = useState(false);

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
      });
      const updated = {
        ...currentUserData,
        branch: selectedBranch,
        academicYear: selectedYear,
        semester: selectedSem,
        cgpa: cgpaResult.parsed,
      };
      setCurrentUserData(updated);
      setShowOnboarding(false);
      Alert.alert('Profile Complete!', 'Your academic details and CGPA have been saved successfully.');
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
        { id: 'Roadmap', label: 'Curriculum', icon: '🗺️' },
        { id: 'Chat', label: 'AI Tutor', icon: '🤖' },
        { id: 'Profile', label: 'Profile', icon: '👤' },
      ];

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      
      {/* Active Screen View */}
      <View style={styles.screenWrapper}>{renderContent()}</View>

      {/* Floating Modern Bottom Navigation Pill */}
      <View style={styles.navContainer}>
        <View style={styles.navBar}>
          {navItems.map((item) => {
            const isActive = activeTab === item.id;
            return (
              <TouchableOpacity
                key={item.id}
                style={[styles.navButton, isActive && styles.navButtonActive]}
                onPress={() => setActiveTab(item.id)}
                activeOpacity={0.8}
              >
                <Text style={styles.navIcon}>{item.icon}</Text>
                <Text style={[styles.navLabel, isActive && styles.navLabelActive]}>
                  {item.label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>
      </View>

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
                : 'Please configure your department and academic standing so we can tailor your study roadmaps and quizzes.'}
            </Text>

            <ScrollView showsVerticalScrollIndicator={false} style={{ maxHeight: 380 }}>
              {isOnlyCgpaMissing && !showAdvancedEdit ? (
                <View style={styles.savedCohortCard}>
                  <Text style={styles.savedCohortTitle}>✅ CURRENT ACADEMIC PROFILE</Text>
                  <Text style={styles.savedCohortText}>
                    {selectedBranch} Engineering • {selectedYear} • {selectedSem}
                  </Text>
                  <TouchableOpacity
                    style={styles.changeCohortBtn}
                    onPress={() => setShowAdvancedEdit(true)}
                  >
                    <Text style={styles.changeCohortLink}>✏️ Edit Branch, Year or Semester</Text>
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
                        onPress={() => setSelectedBranch(b)}
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
                </>
              )}

              {/* CGPA Input Field */}
              <Text style={styles.fieldLabel}>Cumulative GPA (CGPA) *</Text>
              <Text style={styles.fieldSubLabel}>Scale: 0.00 to 10.00 (e.g. 8.75, up to 2 decimal places)</Text>
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
                <Text style={styles.errorText}>⚠️ {cgpaError}</Text>
              )}
            </ScrollView>

            <TouchableOpacity
              style={styles.saveButton}
              onPress={handleSaveOnboarding}
              disabled={onboardingLoading}
            >
              {onboardingLoading ? (
                <ActivityIndicator color="#fff" />
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
  navContainer: {
    position: 'absolute',
    bottom: Platform.OS === 'ios' ? 24 : 16,
    left: 20,
    right: 20,
    alignItems: 'center',
  },
  navBar: {
    flexDirection: 'row',
    backgroundColor: Colors.surface,
    borderRadius: Radii.full,
    paddingHorizontal: 8,
    paddingVertical: 6,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.elevated,
    width: '100%',
    justifyContent: 'space-around',
  },
  navButton: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 6,
    paddingHorizontal: 6,
    borderRadius: Radii.full,
  },
  navButtonActive: {
    backgroundColor: Colors.primaryLight,
  },
  navIcon: {
    fontSize: 18,
    marginBottom: 2,
  },
  navLabel: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.textSecondary,
  },
  navLabelActive: {
    color: Colors.primaryDark,
    fontWeight: '800',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.75)',
    justifyContent: 'center',
    padding: 20,
  },
  modalCard: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.lg,
    padding: 24,
    ...Shadows.elevated,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 6,
    textAlign: 'center',
  },
  modalSubtitle: {
    fontSize: 13,
    color: Colors.textSecondary,
    marginBottom: 16,
    textAlign: 'center',
    lineHeight: 18,
  },
  fieldLabel: {
    fontSize: 14,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginTop: 12,
    marginBottom: 8,
  },
  chipRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: Radii.full,
    backgroundColor: Colors.background,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  chipActive: {
    backgroundColor: Colors.primary,
    borderColor: Colors.primary,
  },
  chipText: {
    fontSize: 13,
    fontWeight: '600',
    color: Colors.textSecondary,
  },
  chipTextActive: {
    color: '#ffffff',
    fontWeight: '700',
  },
  saveButton: {
    backgroundColor: Colors.primary,
    paddingVertical: 14,
    borderRadius: Radii.md,
    alignItems: 'center',
    marginTop: 20,
    ...Shadows.elevated,
  },
  saveButtonText: {
    color: '#ffffff',
    fontSize: 15,
    fontWeight: '700',
  },
  savedCohortCard: {
    backgroundColor: Colors.primaryLight,
    padding: 14,
    borderRadius: Radii.md,
    marginBottom: 14,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  savedCohortTitle: {
    fontSize: 11,
    fontWeight: '800',
    color: Colors.primaryDark,
    letterSpacing: 0.5,
    marginBottom: 4,
  },
  savedCohortText: {
    fontSize: 14,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  changeCohortBtn: {
    marginTop: 8,
    alignSelf: 'flex-start',
  },
  changeCohortLink: {
    fontSize: 12,
    color: Colors.primaryDark,
    fontWeight: '700',
    textDecorationLine: 'underline',
  },
  fieldSubLabel: {
    fontSize: 11,
    color: Colors.textTertiary,
    marginTop: -4,
    marginBottom: 8,
  },
  textInput: {
    backgroundColor: Colors.background,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: Radii.md,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 16,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  textInputError: {
    borderColor: Colors.danger,
    borderWidth: 1.5,
  },
  errorText: {
    fontSize: 12,
    color: Colors.danger,
    fontWeight: '600',
    marginTop: 6,
  },
});
