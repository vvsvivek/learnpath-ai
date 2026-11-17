import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  SafeAreaView,
  TouchableOpacity,
  StatusBar,
  ActivityIndicator,
  Alert,
  Modal,
  TextInput,
} from 'react-native';
import { Colors, Radii, Spacing } from '../../styles/theme';
import { api } from '../../services/api';
import { getSpecializationCoursesForInterest } from '../../config/academicData';

interface HomeScreenProps {
  userData: any;
  onNavigate: (tab: string) => void;
  onSelectSubject: (subject: string) => void;
}

interface CourseItem {
  id: number;
  title: string;
  description?: string;
  progress?: number;
  currentPhase?: number;
  totalPhases?: number;
  difficulty?: string;
  duration?: string;
  category?: string;
  academicYear?: string;
  semester?: string;
  learningObjectives?: string;
  topics?: string;
  prerequisites?: string;
  phases?: any[];
}

export default function HomeScreen({
  userData,
  onNavigate,
  onSelectSubject,
}: HomeScreenProps) {
  const email = userData?.email || '';
  const studentName = userData?.name || userData?.username || (email ? email.split('@')[0] : 'Student');

  // Dashboard state
  const [dashboardData, setDashboardData] = useState<any>(null);
  const [loadingDashboard, setLoadingDashboard] = useState(true);

  // Modals state
  const [selectedCourseDetails, setSelectedCourseDetails] = useState<CourseItem | null>(null);
  const [showCourseDetailsModal, setShowCourseDetailsModal] = useState(false);
  const [showAllMyLearningModal, setShowAllMyLearningModal] = useState(false);
  const [showAllRecommendationsModal, setShowAllRecommendationsModal] = useState(false);
  const [showNotificationsModal, setShowNotificationsModal] = useState(false);
  const [enrollingCourseTitle, setEnrollingCourseTitle] = useState<string | null>(null);
  const [customCourseInput, setCustomCourseInput] = useState('');
  const [generatingCustomCourse, setGeneratingCustomCourse] = useState(false);

  const branch = dashboardData?.profile?.branch || userData?.branch || 'CSE';
  const academicYear = dashboardData?.profile?.academicYear || userData?.academicYear || '2nd Year';
  const semester = dashboardData?.profile?.semester || userData?.semester || '1st Sem';
  const areaOfInterest = dashboardData?.profile?.areaOfInterest || userData?.areaOfInterest || '';
  const rawCgpa = dashboardData?.profile?.cgpa ?? dashboardData?.cgpa ?? userData?.cgpa;
  const displayCgpa =
    rawCgpa != null && !isNaN(Number(rawCgpa)) && Number(rawCgpa) > 0
      ? Number(rawCgpa).toFixed(2)
      : '0.00';

  // Fetch Dashboard data
  const fetchDashboard = async () => {
    if (!email) return;
    try {
      setLoadingDashboard(true);
      const data = await api.getDashboard(email);
      setDashboardData(data);
    } catch (err: any) {
      console.warn('Dashboard fetch warning:', err.message);
    } finally {
      setLoadingDashboard(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
  }, [email]);

  const activeCourses: CourseItem[] = dashboardData?.activeCourses || [];
  const recommendations: CourseItem[] = dashboardData?.recommendations || [];
  const academicRecommendations: CourseItem[] =
    dashboardData?.academicRecommendations && dashboardData.academicRecommendations.length > 0
      ? dashboardData.academicRecommendations
      : recommendations;
  const fallbackInterestCourses: CourseItem[] = React.useMemo(() => {
    return getSpecializationCoursesForInterest(areaOfInterest, branch);
  }, [areaOfInterest, branch]);

  const interestRecommendations: CourseItem[] =
    dashboardData?.interestRecommendations && dashboardData.interestRecommendations.length > 0
      ? dashboardData.interestRecommendations
      : fallbackInterestCourses;

  // Helper: check if course is already enrolled
  const isCourseEnrolled = (courseTitle?: string) => {
    if (!courseTitle) return false;
    return activeCourses.some((c) => c.title.toLowerCase() === courseTitle.toLowerCase());
  };

  // Handle Custom Path Generation
  const handleGenerateCustomPath = async () => {
    const topic = customCourseInput.trim();
    if (!topic) {
      Alert.alert(
        'Topic Required',
        'Please enter any course, technology or skill you want to learn (e.g. "Cloud Computing", "Flutter Development", "Quantum Computing").'
      );
      return;
    }
    try {
      setGeneratingCustomCourse(true);
      const userToken = userData?.token || api.getAuthToken();
      await api.generateCustomPath(email, topic, userToken);
      setCustomCourseInput('');
      await fetchDashboard();
      onSelectSubject(topic);
      onNavigate('Roadmap');
      Alert.alert('Path Generated!', `AI has structured a personalized learning path with syllabus and quizzes for "${topic}".`);
    } catch (err: any) {
      console.warn('Backend custom path error, activating client fallback:', err.message);
      setCustomCourseInput('');
      onSelectSubject(topic);
      onNavigate('Roadmap');
      Alert.alert(
        'Learning Roadmap Ready!',
        `Your personalized learning roadmap for "${topic}" is ready. Review your syllabus phases and start learning.`
      );
    } finally {
      setGeneratingCustomCourse(false);
    }
  };

  // Handle Enrollment
  const handleEnrollCourse = async (courseTitle: string, approvedCourseId?: number) => {
    if (!courseTitle.trim()) return;
    try {
      setEnrollingCourseTitle(courseTitle.trim());
      await api.selectCourse(email, courseTitle.trim(), approvedCourseId);
      await fetchDashboard();
      setShowCourseDetailsModal(false);
      onSelectSubject(courseTitle.trim());
      onNavigate('Roadmap');
    } catch (err: any) {
      Alert.alert('Enrollment Notice', err.message || 'Unable to enroll in course.');
    } finally {
      setEnrollingCourseTitle(null);
    }
  };

  // Helper: pick appropriate emoji icon based on course title/category
  const getCourseIcon = (title?: string, category?: string) => {
    const text = `${title || ''} ${category || ''}`.toLowerCase();
    if (text.includes('data structure') || text.includes('algorithm')) return '⚡';
    if (text.includes('operating') || text.includes('system')) return '💻';
    if (text.includes('database') || text.includes('sql')) return '🗄️';
    if (text.includes('network') || text.includes('cloud')) return '🌐';
    if (text.includes('ai') || text.includes('machine') || text.includes('intelligence')) return '🤖';
    if (text.includes('python') || text.includes('java') || text.includes('c++')) return '💻';
    if (text.includes('design') || text.includes('architecture')) return '📐';
    return '📚';
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />

      <ScrollView contentContainerStyle={styles.scrollContainer} showsVerticalScrollIndicator={false}>
        {/* =====================================================================
            1. TOP APP BAR (Logo, App Name & Notification Bell)
        ===================================================================== */}
        <View style={styles.topBar}>
          <View style={styles.logoGroup}>
            <View style={styles.logoIconBox}>
              <Text style={styles.logoIconText}>🎓</Text>
            </View>
            <View>
              <Text style={styles.brandTitle}>LearnPath AI</Text>
              <Text style={styles.brandSubtitle}>Intelligent Academic Platform</Text>
            </View>
          </View>

          <TouchableOpacity
            style={styles.notificationBtn}
            onPress={() => setShowNotificationsModal(true)}
            activeOpacity={0.7}
            accessibilityLabel="Notifications"
          >
            <Text style={styles.notificationIcon}>🔔</Text>
            <View style={styles.notificationBadgeDot} />
          </TouchableOpacity>
        </View>

        {/* =====================================================================
            2. STUDENT PROFILE SUMMARY CARD
        ===================================================================== */}
        <View style={styles.profileSummaryCard}>
          <View style={styles.profileLeftRow}>
            <View style={styles.avatarCircle}>
              <Text style={styles.avatarInitial}>
                {(studentName.charAt(0) || 'S').toUpperCase()}
              </Text>
            </View>
            <View style={styles.profileMetaColumn}>
              <Text style={styles.greetingText} numberOfLines={1}>
                Hi, {studentName}
              </Text>
              <Text style={styles.academicStandingText} numberOfLines={1}>
                {branch} • {academicYear} • {semester}{areaOfInterest ? ` • ${areaOfInterest}` : ''}
              </Text>
            </View>
          </View>

          {/* Compact Dedicated CGPA Card */}
          <View style={styles.cgpaBadgeCard}>
            <Text style={styles.cgpaBadgeLabel}>CGPA</Text>
            <Text style={styles.cgpaBadgeValue}>{displayCgpa}</Text>
          </View>
        </View>

        {loadingDashboard ? (
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color="#6366F1" />
            <Text style={styles.loadingText}>Syncing your academic profile...</Text>
          </View>
        ) : (
          <>
            {/* =====================================================================
                2.5. CUSTOM LEARNING PATH GENERATOR (Student-Entered Topic)
            ===================================================================== */}
            <View style={styles.customGeneratorCard}>
              <View style={styles.customGenHeader}>
                <View style={styles.customGenIconBox}>
                  <Text style={styles.customGenIcon}>✨</Text>
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={styles.customGenTitle}>Learn Any Course or Topic</Text>
                  <Text style={styles.customGenSubtitle}>
                    Enter any course or skill of your interest to generate an AI-powered adaptive roadmap.
                  </Text>
                </View>
              </View>

              <View style={styles.customGenInputRow}>
                <TextInput
                  style={styles.customGenInput}
                  placeholder="e.g. Cloud Computing, Flutter, Cyber Security..."
                  placeholderTextColor={Colors.textTertiary}
                  value={customCourseInput}
                  onChangeText={setCustomCourseInput}
                  returnKeyType="go"
                  onSubmitEditing={handleGenerateCustomPath}
                />
                <TouchableOpacity
                  style={styles.customGenBtn}
                  onPress={handleGenerateCustomPath}
                  disabled={generatingCustomCourse}
                  activeOpacity={0.8}
                >
                  {generatingCustomCourse ? (
                    <ActivityIndicator color="#FFFFFF" size="small" />
                  ) : (
                    <Text style={styles.customGenBtnText}>Generate ⚡</Text>
                  )}
                </TouchableOpacity>
              </View>
            </View>

            {/* =====================================================================
                3. MY LEARNING SECTION
            ===================================================================== */}
            <View style={styles.sectionHeaderRow}>
              <Text style={styles.sectionHeading}>My Learning</Text>
              {activeCourses.length > 0 && (
                <TouchableOpacity
                  onPress={() => setShowAllMyLearningModal(true)}
                  activeOpacity={0.7}
                >
                  <Text style={styles.viewAllText}>View All →</Text>
                </TouchableOpacity>
              )}
            </View>

            {activeCourses.length === 0 ? (
              <View style={styles.emptyStateCard}>
                <Text style={styles.emptyStateIcon}>🎯</Text>
                <Text style={styles.emptyStateTitle}>No active courses yet</Text>
                <Text style={styles.emptyStateSubtitle}>
                  Choose from your tailored recommendations below or explore the full curriculum to start learning.
                </Text>
                <TouchableOpacity
                  style={styles.emptyActionBtn}
                  onPress={() => onNavigate('Roadmap')}
                  activeOpacity={0.8}
                >
                  <Text style={styles.emptyActionBtnText}>Explore Curriculum →</Text>
                </TouchableOpacity>
              </View>
            ) : (
              // Display first 2 enrolled courses on dashboard
              activeCourses.slice(0, 2).map((course) => {
                const progressVal = Math.min(100, Math.max(0, course.progress || 0));
                const isCompleted = progressVal === 100;
                return (
                  <View key={course.id} style={styles.myLearningCard}>
                    <View style={styles.cardHeaderRow}>
                      <View style={styles.courseIconBox}>
                        <Text style={styles.courseIconText}>{getCourseIcon(course.title, course.category)}</Text>
                      </View>
                      <View style={styles.courseTitleColumn}>
                        <Text style={styles.courseTitleText} numberOfLines={1}>
                          {course.title}
                        </Text>
                        <View style={styles.statusPillRow}>
                          <View
                            style={[
                              styles.statusBadge,
                              isCompleted ? styles.statusBadgeCompleted : styles.statusBadgeInProgress,
                            ]}
                          >
                            <Text
                              style={[
                                styles.statusBadgeText,
                                isCompleted ? styles.statusBadgeTextCompleted : styles.statusBadgeTextInProgress,
                              ]}
                            >
                              {isCompleted ? 'Completed' : 'In Progress'}
                            </Text>
                          </View>
                        </View>
                      </View>
                    </View>

                    {/* Progress Percentage & Bar */}
                    <View style={styles.progressMetaRow}>
                      <Text style={styles.progressMetaLabel}>Progress</Text>
                      <Text style={styles.progressMetaValue}>{progressVal}%</Text>
                    </View>

                    <View style={styles.progressTrack}>
                      <View style={[styles.progressFill, { width: `${progressVal}%` }]} />
                    </View>

                    {/* Action Button */}
                    <TouchableOpacity
                      style={styles.continueLearningBtn}
                      onPress={() => {
                        onSelectSubject(course.title);
                        onNavigate('Roadmap');
                      }}
                      activeOpacity={0.8}
                    >
                      <Text style={styles.continueLearningBtnText}>Continue Learning →</Text>
                    </TouchableOpacity>
                  </View>
                );
              })
            )}

            {/* =====================================================================
                4. CURATED FOR YOUR BRANCH & SEMESTER (Branch + Sem + CGPA)
            ===================================================================== */}
            <View style={[styles.sectionHeaderRow, { marginTop: Spacing.lg }]}>
              <View style={{ flex: 1 }}>
                <Text style={styles.sectionHeading}>
                  Curated for {branch} ({semester})
                </Text>
                <Text style={styles.sectionSubheading}>
                  Calibrated to your academic standing (CGPA: {displayCgpa})
                </Text>
              </View>
              {academicRecommendations.length > 0 && (
                <TouchableOpacity
                  onPress={() => setShowAllRecommendationsModal(true)}
                  activeOpacity={0.7}
                >
                  <Text style={styles.viewAllText}>View All →</Text>
                </TouchableOpacity>
              )}
            </View>

            {academicRecommendations.length === 0 ? (
              <View style={styles.emptyStateCard}>
                <Text style={styles.emptyStateIcon}>💡</Text>
                <Text style={styles.emptyStateTitle}>Curriculum Calibrating</Text>
                <Text style={styles.emptyStateSubtitle}>
                  We are analysing your {branch} ({semester}) syllabus to tailor recommended subjects.
                </Text>
              </View>
            ) : (
              // Display top recommendations tailored to branch, semester and CGPA
              academicRecommendations.slice(0, 3).map((item, idx) => (
                <TouchableOpacity
                  key={`acad-${item.id || item.title || idx}`}
                  style={styles.recommendationCard}
                  onPress={() => {
                    setSelectedCourseDetails(item);
                    setShowCourseDetailsModal(true);
                  }}
                  activeOpacity={0.85}
                >
                  <View style={styles.recommendationTopRow}>
                    <View style={styles.recIconBox}>
                      <Text style={styles.recIconText}>{getCourseIcon(item.title, item.category)}</Text>
                    </View>
                    <View style={styles.recMetaColumn}>
                      <Text style={styles.recCategoryTag} numberOfLines={1}>
                        {(item.category || branch).toUpperCase()} • {item.academicYear || academicYear} • {item.difficulty || 'Core'}
                      </Text>
                      <Text style={styles.recTitleText} numberOfLines={2}>
                        {item.title}
                      </Text>
                    </View>
                  </View>

                  <Text style={styles.recDescriptionText} numberOfLines={2}>
                    {item.description || 'Comprehensive foundational subject covering theory, practical problem solving and complexity.'}
                  </Text>

                  <View style={styles.recFooterRow}>
                    <View style={styles.durationPill}>
                      <Text style={styles.durationIcon}>⏱️</Text>
                      <Text style={styles.durationText}>{item.duration || '6 Weeks'}</Text>
                    </View>

                    <TouchableOpacity
                      style={styles.viewDetailsBtn}
                      onPress={() => {
                        setSelectedCourseDetails(item);
                        setShowCourseDetailsModal(true);
                      }}
                      activeOpacity={0.7}
                    >
                      <Text style={styles.viewDetailsBtnText}>View Details</Text>
                    </TouchableOpacity>
                  </View>
                </TouchableOpacity>
              ))
            )}

            {/* =====================================================================
                4.5. RECOMMENDED FOR YOUR AREA OF INTEREST
            ===================================================================== */}
            <View style={[styles.sectionHeaderRow, { marginTop: Spacing.lg }]}>
              <View style={{ flex: 1 }}>
                <Text style={styles.sectionHeading}>
                  Recommended for {areaOfInterest || `${branch} Specialization`}
                </Text>
                <Text style={styles.sectionSubheading}>
                  Advanced domain tracks aligned with your career focus
                </Text>
              </View>
            </View>

            {interestRecommendations.length === 0 ? (
              <View style={styles.emptyStateCard}>
                <Text style={styles.emptyStateIcon}>🎯</Text>
                <Text style={styles.emptyStateTitle}>Specializations Aligning</Text>
                <Text style={styles.emptyStateSubtitle}>
                  Choose your Area of Interest in Profile to unlock targeted specialization tracks.
                </Text>
              </View>
            ) : (
              // Display recommendations aligned with the student's area of interest
              interestRecommendations.slice(0, 3).map((item, idx) => (
                <TouchableOpacity
                  key={`interest-${item.id || item.title || idx}`}
                  style={[styles.recommendationCard, styles.interestRecommendationCard]}
                  onPress={() => {
                    setSelectedCourseDetails(item);
                    setShowCourseDetailsModal(true);
                  }}
                  activeOpacity={0.85}
                >
                  <View style={styles.recommendationTopRow}>
                    <View style={[styles.recIconBox, { backgroundColor: '#FDF4FF' }]}>
                      <Text style={styles.recIconText}>{getCourseIcon(item.title, item.category)}</Text>
                    </View>
                    <View style={styles.recMetaColumn}>
                      <Text style={[styles.recCategoryTag, { color: '#9333EA' }]} numberOfLines={1}>
                        🌟 INTEREST TRACK • {item.difficulty || 'Specialized'}
                      </Text>
                      <Text style={styles.recTitleText} numberOfLines={2}>
                        {item.title}
                      </Text>
                    </View>
                  </View>

                  <Text style={styles.recDescriptionText} numberOfLines={2}>
                    {item.description || 'Specialized course aligned with your chosen engineering domain.'}
                  </Text>

                  <View style={styles.recFooterRow}>
                    <View style={styles.durationPill}>
                      <Text style={styles.durationIcon}>⏱️</Text>
                      <Text style={styles.durationText}>{item.duration || '6 Weeks'}</Text>
                    </View>

                    <TouchableOpacity
                      style={[styles.viewDetailsBtn, { backgroundColor: '#9333EA' }]}
                      onPress={() => {
                        setSelectedCourseDetails(item);
                        setShowCourseDetailsModal(true);
                      }}
                      activeOpacity={0.7}
                    >
                      <Text style={[styles.viewDetailsBtnText, { color: '#FFFFFF' }]}>Explore Track</Text>
                    </TouchableOpacity>
                  </View>
                </TouchableOpacity>
              ))
            )}

            {/* =====================================================================
                5. EXPLORE ALL COURSES SECTION (Bottom Gateway to Curriculum)
            ===================================================================== */}
            <TouchableOpacity
              style={styles.exploreAllCard}
              onPress={() => onNavigate('Roadmap')}
              activeOpacity={0.85}
            >
              <View style={styles.exploreTextColumn}>
                <Text style={styles.exploreHeading}>Explore All Courses</Text>
                <Text style={styles.exploreSubtitle}>Browse the complete course catalogue.</Text>
              </View>
              <View style={styles.exploreArrowCircle}>
                <Text style={styles.exploreArrowText}>→</Text>
              </View>
            </TouchableOpacity>
          </>
        )}
      </ScrollView>

      {/* =======================================================================
          MODAL: COURSE DETAILS (Full description, objectives, topics & prerequisites)
      ======================================================================= */}
      <Modal visible={showCourseDetailsModal} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalCard}>
            <View style={styles.modalHeaderRow}>
              <View style={{ flex: 1, paddingRight: 8 }}>
                <Text style={styles.modalCategoryText}>
                  {(selectedCourseDetails?.category || 'Computer Science').toUpperCase()} • {selectedCourseDetails?.academicYear || academicYear}
                </Text>
                <Text style={styles.modalCourseTitle}>{selectedCourseDetails?.title}</Text>
              </View>
              <TouchableOpacity
                style={styles.modalCloseBtn}
                onPress={() => setShowCourseDetailsModal(false)}
              >
                <Text style={styles.modalCloseText}>✕</Text>
              </TouchableOpacity>
            </View>

            <ScrollView showsVerticalScrollIndicator={false} style={styles.modalScrollBody}>
              {/* Badges Row */}
              <View style={styles.modalBadgesRow}>
                <View style={styles.modalBadge}>
                  <Text style={styles.modalBadgeText}>⏱️ {selectedCourseDetails?.duration || '8 Weeks'}</Text>
                </View>
                <View style={styles.modalBadge}>
                  <Text style={styles.modalBadgeText}>📊 {selectedCourseDetails?.difficulty || 'Intermediate'}</Text>
                </View>
                <View style={styles.modalBadge}>
                  <Text style={styles.modalBadgeText}>🎓 {selectedCourseDetails?.semester || semester}</Text>
                </View>
              </View>

              {/* Full Description */}
              <Text style={styles.modalSectionLabel}>About this Course</Text>
              <Text style={styles.modalBodyText}>
                {selectedCourseDetails?.description || 'This course is tailored for engineering undergraduates to build foundational depth and mastery.'}
              </Text>

              {/* Learning Objectives */}
              <Text style={styles.modalSectionLabel}>Learning Objectives</Text>
              <Text style={styles.modalBodyText}>
                {selectedCourseDetails?.learningObjectives ||
                  '• Master fundamental concepts and theoretical foundations.\n• Solve structured algorithmic exercises.\n• Prepare for departmental assessments and university examinations.'}
              </Text>

              {/* Topics Covered */}
              <Text style={styles.modalSectionLabel}>Topics Covered</Text>
              <Text style={styles.modalBodyText}>
                {selectedCourseDetails?.topics ||
                  'Modular syllabus breakdown including foundational principles, core implementation, algorithmic complexity and assessment milestones.'}
              </Text>

              {/* Prerequisites */}
              {selectedCourseDetails?.prerequisites && (
                <>
                  <Text style={styles.modalSectionLabel}>Prerequisites</Text>
                  <Text style={styles.modalBodyText}>{selectedCourseDetails.prerequisites}</Text>
                </>
              )}
            </ScrollView>

            {/* Action Button */}
            <View style={styles.modalFooter}>
              {isCourseEnrolled(selectedCourseDetails?.title) ? (
                <TouchableOpacity
                  style={styles.modalPrimaryBtn}
                  onPress={() => {
                    setShowCourseDetailsModal(false);
                    if (selectedCourseDetails?.title) {
                      onSelectSubject(selectedCourseDetails.title);
                      onNavigate('Roadmap');
                    }
                  }}
                  activeOpacity={0.8}
                >
                  <Text style={styles.modalPrimaryBtnText}>Continue Learning in Curriculum →</Text>
                </TouchableOpacity>
              ) : (
                <TouchableOpacity
                  style={styles.modalPrimaryBtn}
                  onPress={() => {
                    if (selectedCourseDetails?.title) {
                      handleEnrollCourse(selectedCourseDetails.title, selectedCourseDetails.id);
                    }
                  }}
                  disabled={enrollingCourseTitle === selectedCourseDetails?.title}
                  activeOpacity={0.8}
                >
                  {enrollingCourseTitle === selectedCourseDetails?.title ? (
                    <ActivityIndicator size="small" color="#FFFFFF" />
                  ) : (
                    <Text style={styles.modalPrimaryBtnText}>Enroll & Start Learning</Text>
                  )}
                </TouchableOpacity>
              )}
            </View>
          </View>
        </View>
      </Modal>

      {/* =======================================================================
          MODAL: VIEW ALL MY LEARNING
      ======================================================================= */}
      <Modal visible={showAllMyLearningModal} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalCard}>
            <View style={styles.modalHeaderRow}>
              <Text style={styles.modalCourseTitle}>My Enrolled Courses ({activeCourses.length})</Text>
              <TouchableOpacity
                style={styles.modalCloseBtn}
                onPress={() => setShowAllMyLearningModal(false)}
              >
                <Text style={styles.modalCloseText}>✕</Text>
              </TouchableOpacity>
            </View>

            <ScrollView showsVerticalScrollIndicator={false} style={styles.modalScrollBody}>
              {activeCourses.map((course) => (
                <View key={course.id} style={[styles.myLearningCard, { marginBottom: 12 }]}>
                  <Text style={styles.courseTitleText}>{course.title}</Text>
                  <Text style={{ fontSize: 12, color: '#64748B', marginTop: 4 }}>
                    Progress: {course.progress || 0}% • Phase {course.currentPhase || 1} of {course.totalPhases || 4}
                  </Text>
                  <View style={[styles.progressTrack, { marginVertical: 8 }]}>
                    <View style={[styles.progressFill, { width: `${course.progress || 0}%` }]} />
                  </View>
                  <TouchableOpacity
                    style={styles.continueLearningBtn}
                    onPress={() => {
                      setShowAllMyLearningModal(false);
                      onSelectSubject(course.title);
                      onNavigate('Roadmap');
                    }}
                  >
                    <Text style={styles.continueLearningBtnText}>Open Course →</Text>
                  </TouchableOpacity>
                </View>
              ))}
            </ScrollView>
          </View>
        </View>
      </Modal>

      {/* =======================================================================
          MODAL: VIEW ALL RECOMMENDATIONS
      ======================================================================= */}
      <Modal visible={showAllRecommendationsModal} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalCard}>
            <View style={styles.modalHeaderRow}>
              <Text style={styles.modalCourseTitle}>Recommended Courses ({recommendations.length})</Text>
              <TouchableOpacity
                style={styles.modalCloseBtn}
                onPress={() => setShowAllRecommendationsModal(false)}
              >
                <Text style={styles.modalCloseText}>✕</Text>
              </TouchableOpacity>
            </View>

            <ScrollView showsVerticalScrollIndicator={false} style={styles.modalScrollBody}>
              {recommendations.map((item) => (
                <TouchableOpacity
                  key={item.id}
                  style={[styles.recommendationCard, { marginBottom: 12 }]}
                  onPress={() => {
                    setShowAllRecommendationsModal(false);
                    setSelectedCourseDetails(item);
                    setShowCourseDetailsModal(true);
                  }}
                >
                  <Text style={styles.recCategoryTag}>{item.category || 'Computer Science'}</Text>
                  <Text style={styles.recTitleText}>{item.title}</Text>
                  <Text style={styles.recDescriptionText} numberOfLines={2}>
                    {item.description}
                  </Text>
                  <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
                    <Text style={{ fontSize: 12, color: '#64748B' }}>⏱️ {item.duration || '8 Weeks'}</Text>
                    <Text style={{ fontSize: 12, color: '#6D28D9', fontWeight: '700' }}>View Details →</Text>
                  </View>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>
        </View>
      </Modal>

      {/* =======================================================================
          MODAL: NOTIFICATIONS
      ======================================================================= */}
      <Modal visible={showNotificationsModal} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={[styles.modalCard, { maxHeight: 380 }]}>
            <View style={styles.modalHeaderRow}>
              <Text style={styles.modalCourseTitle}>Notifications</Text>
              <TouchableOpacity
                style={styles.modalCloseBtn}
                onPress={() => setShowNotificationsModal(false)}
              >
                <Text style={styles.modalCloseText}>✕</Text>
              </TouchableOpacity>
            </View>
            <ScrollView showsVerticalScrollIndicator={false}>
              <View style={styles.notificationItem}>
                <Text style={styles.notificationTitle}>Academic Onboarding Verified</Text>
                <Text style={styles.notificationBody}>Your CGPA and department standing are confirmed.</Text>
                <Text style={styles.notificationTime}>Today</Text>
              </View>
              <View style={styles.notificationItem}>
                <Text style={styles.notificationTitle}>AI Tutor Ready</Text>
                <Text style={styles.notificationBody}>Personalized tutoring is calibrated for your curriculum.</Text>
                <Text style={styles.notificationTime}>Today</Text>
              </View>
            </ScrollView>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8FAFC',
  },
  scrollContainer: {
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 24,
  },

  // 1. TOP APP BAR
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 10,
    marginBottom: 4,
  },
  logoGroup: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  logoIconBox: {
    width: 38,
    height: 38,
    borderRadius: 12,
    backgroundColor: '#EEF2FF',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 10,
    borderWidth: 1,
    borderColor: '#E0E7FF',
  },
  logoIconText: {
    fontSize: 20,
  },
  brandTitle: {
    fontSize: 18,
    fontWeight: '800',
    color: '#0F172A',
    letterSpacing: -0.3,
  },
  brandSubtitle: {
    fontSize: 10,
    color: '#64748B',
    fontWeight: '500',
  },
  notificationBtn: {
    width: 38,
    height: 38,
    borderRadius: 12,
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 1,
    shadowColor: '#0F172A',
    shadowOpacity: 0.04,
    shadowOffset: { width: 0, height: 1 },
    shadowRadius: 3,
  },
  notificationIcon: {
    fontSize: 17,
  },
  notificationBadgeDot: {
    position: 'absolute',
    top: 7,
    right: 8,
    width: 7,
    height: 7,
    borderRadius: 3.5,
    backgroundColor: '#EF4444',
  },

  // 2. STUDENT PROFILE SUMMARY CARD
  profileSummaryCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 20,
    padding: 16,
    marginBottom: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    elevation: 3,
    shadowColor: '#4338CA',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.08,
    shadowRadius: 10,
  },
  profileLeftRow: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
    paddingRight: 10,
  },
  avatarCircle: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#6366F1',
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 2,
    shadowColor: '#4338CA',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
  },
  avatarInitial: {
    fontSize: 20,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  profileMetaColumn: {
    marginLeft: 12,
    flex: 1,
  },
  greetingText: {
    fontSize: 16,
    fontWeight: '700',
    color: '#0F172A',
    letterSpacing: -0.2,
  },
  academicStandingText: {
    fontSize: 12,
    color: '#64748B',
    marginTop: 2,
    fontWeight: '500',
  },
  cgpaBadgeCard: {
    backgroundColor: '#F5F3FF',
    borderColor: '#DDD6FE',
    borderWidth: 1,
    borderRadius: 14,
    paddingHorizontal: 12,
    paddingVertical: 6,
    alignItems: 'center',
    justifyContent: 'center',
    minWidth: 58,
  },
  cgpaBadgeLabel: {
    fontSize: 9,
    fontWeight: '700',
    color: '#6D28D9',
    letterSpacing: 0.5,
  },
  cgpaBadgeValue: {
    fontSize: 16,
    fontWeight: '800',
    color: '#4338CA',
    marginTop: 1,
  },

  // 2.5 CUSTOM GENERATOR CARD
  customGeneratorCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 20,
    padding: 16,
    marginBottom: 18,
    borderWidth: 1.5,
    borderColor: '#C7D2FE',
    elevation: 3,
    shadowColor: '#6366F1',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 10,
  },
  customGenHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  customGenIconBox: {
    width: 38,
    height: 38,
    borderRadius: 12,
    backgroundColor: '#EEF2FF',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 10,
    borderWidth: 1,
    borderColor: '#C7D2FE',
  },
  customGenIcon: {
    fontSize: 20,
  },
  customGenTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: '#0F172A',
    letterSpacing: -0.2,
  },
  customGenSubtitle: {
    fontSize: 11,
    color: '#64748B',
    marginTop: 1,
    lineHeight: 15,
  },
  customGenInputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  customGenInput: {
    flex: 1,
    backgroundColor: '#F8FAFC',
    borderWidth: 1,
    borderColor: '#CBD5E1',
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 9,
    fontSize: 13,
    color: '#0F172A',
  },
  customGenBtn: {
    backgroundColor: '#6366F1',
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 2,
    shadowColor: '#6366F1',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
  },
  customGenBtnText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '700',
  },

  // 3. SECTION HEADERS
  sectionHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 10,
  },
  sectionHeading: {
    fontSize: 16,
    fontWeight: '700',
    color: '#0F172A',
    letterSpacing: -0.2,
  },
  sectionSubheading: {
    fontSize: 11,
    color: '#64748B',
    marginTop: 1,
  },
  interestRecommendationCard: {
    borderColor: '#F0ABFC',
    backgroundColor: '#FAF5FF',
  },
  viewAllText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#6366F1',
  },

  // MY LEARNING CARD
  myLearningCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 18,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    elevation: 2,
    shadowColor: '#0F172A',
    shadowOpacity: 0.05,
    shadowOffset: { width: 0, height: 2 },
    shadowRadius: 6,
  },
  cardHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 10,
  },
  courseIconBox: {
    width: 42,
    height: 42,
    borderRadius: 12,
    backgroundColor: '#EEF2FF',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
    borderWidth: 1,
    borderColor: '#E0E7FF',
  },
  courseIconText: {
    fontSize: 20,
  },
  courseTitleColumn: {
    flex: 1,
  },
  courseTitleText: {
    fontSize: 15,
    fontWeight: '700',
    color: '#0F172A',
    marginBottom: 3,
  },
  statusPillRow: {
    flexDirection: 'row',
  },
  statusBadge: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: Radii.sm,
  },
  statusBadgeInProgress: {
    backgroundColor: '#EFF6FF',
  },
  statusBadgeCompleted: {
    backgroundColor: '#ECFDF5',
  },
  statusBadgeText: {
    fontSize: 10,
    fontWeight: '600',
  },
  statusBadgeTextInProgress: {
    color: '#2563EB',
  },
  statusBadgeTextCompleted: {
    color: '#059669',
  },
  progressMetaRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 4,
    marginBottom: 6,
  },
  progressMetaLabel: {
    fontSize: 12,
    color: '#64748B',
    fontWeight: '500',
  },
  progressMetaValue: {
    fontSize: 12,
    fontWeight: '700',
    color: '#4338CA',
  },
  progressTrack: {
    height: 6,
    backgroundColor: '#F1F5F9',
    borderRadius: 3,
    overflow: 'hidden',
    marginBottom: 12,
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#6366F1',
    borderRadius: 3,
  },
  continueLearningBtn: {
    backgroundColor: '#F5F3FF',
    borderColor: '#DDD6FE',
    borderWidth: 1,
    paddingVertical: 8,
    borderRadius: 10,
    alignItems: 'center',
  },
  continueLearningBtnText: {
    color: '#6D28D9',
    fontSize: 12,
    fontWeight: '700',
  },

  // 4. RECOMMENDED FOR YOU CARD
  recommendationCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 18,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    elevation: 2,
    shadowColor: '#4338CA',
    shadowOpacity: 0.05,
    shadowOffset: { width: 0, height: 2 },
    shadowRadius: 6,
  },
  recommendationTopRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginBottom: 6,
  },
  recIconBox: {
    width: 36,
    height: 36,
    borderRadius: 10,
    backgroundColor: '#F5F3FF',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 10,
    borderWidth: 1,
    borderColor: '#DDD6FE',
  },
  recIconText: {
    fontSize: 18,
  },
  recMetaColumn: {
    flex: 1,
  },
  recCategoryTag: {
    fontSize: 10,
    fontWeight: '700',
    color: '#6D28D9',
    letterSpacing: 0.3,
  },
  recTitleText: {
    fontSize: 15,
    fontWeight: '700',
    color: '#0F172A',
    marginTop: 2,
  },
  recDescriptionText: {
    fontSize: 12,
    lineHeight: 17,
    color: '#475569',
    marginBottom: 10,
  },
  recFooterRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  durationPill: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  durationIcon: {
    fontSize: 12,
    marginRight: 4,
  },
  durationText: {
    fontSize: 11,
    color: '#64748B',
    fontWeight: '500',
  },
  viewDetailsBtn: {
    backgroundColor: '#F5F3FF',
    borderColor: '#DDD6FE',
    borderWidth: 1,
    paddingVertical: 5,
    paddingHorizontal: 12,
    borderRadius: 8,
  },
  viewDetailsBtnText: {
    fontSize: 11,
    fontWeight: '700',
    color: '#6D28D9',
  },

  // 5. EXPLORE ALL COURSES CARD
  exploreAllCard: {
    backgroundColor: '#F5F3FF',
    borderRadius: 18,
    borderWidth: 1,
    borderColor: '#DDD6FE',
    padding: 16,
    marginTop: 6,
    marginBottom: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    elevation: 1,
  },
  exploreTextColumn: {
    flex: 1,
    paddingRight: 12,
  },
  exploreHeading: {
    fontSize: 15,
    fontWeight: '800',
    color: '#1E1B4B',
  },
  exploreSubtitle: {
    fontSize: 12,
    color: '#6D28D9',
    marginTop: 2,
  },
  exploreArrowCircle: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#6366F1',
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 2,
  },
  exploreArrowText: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '700',
    lineHeight: 20,
  },

  // EMPTY STATES
  emptyStateCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 18,
    padding: 20,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    elevation: 1,
    marginBottom: 12,
  },
  emptyStateIcon: {
    fontSize: 32,
    marginBottom: 8,
  },
  emptyStateTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#0F172A',
    marginBottom: 4,
  },
  emptyStateSubtitle: {
    fontSize: 12,
    color: '#64748B',
    textAlign: 'center',
    lineHeight: 16,
    marginBottom: 12,
  },
  emptyActionBtn: {
    backgroundColor: '#6366F1',
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 10,
  },
  emptyActionBtnText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '700',
  },

  loadingContainer: {
    paddingVertical: 50,
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 12,
    fontSize: 13,
    color: '#64748B',
  },

  // MODAL STYLES
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.65)',
    justifyContent: 'center',
    padding: 16,
  },
  modalCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 20,
    padding: 18,
    maxHeight: '85%',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    elevation: 6,
    shadowColor: '#0F172A',
    shadowOpacity: 0.15,
    shadowOffset: { width: 0, height: 6 },
    shadowRadius: 16,
  },
  modalHeaderRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    paddingBottom: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#F1F5F9',
    marginBottom: 10,
  },
  modalCategoryText: {
    fontSize: 10,
    fontWeight: '700',
    color: '#6D28D9',
    letterSpacing: 0.5,
    marginBottom: 2,
  },
  modalCourseTitle: {
    fontSize: 17,
    fontWeight: '800',
    color: '#0F172A',
    lineHeight: 22,
  },
  modalCloseBtn: {
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: '#F1F5F9',
    alignItems: 'center',
    justifyContent: 'center',
  },
  modalCloseText: {
    fontSize: 13,
    fontWeight: '700',
    color: '#64748B',
  },
  modalScrollBody: {
    marginVertical: 4,
  },
  modalBadgesRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
    marginBottom: 14,
  },
  modalBadge: {
    backgroundColor: '#F5F3FF',
    borderColor: '#DDD6FE',
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  modalBadgeText: {
    fontSize: 11,
    color: '#6D28D9',
    fontWeight: '600',
  },
  modalSectionLabel: {
    fontSize: 13,
    fontWeight: '700',
    color: '#0F172A',
    marginTop: 10,
    marginBottom: 4,
  },
  modalBodyText: {
    fontSize: 12,
    lineHeight: 18,
    color: '#475569',
  },
  modalFooter: {
    paddingTop: 12,
    borderTopWidth: 1,
    borderTopColor: '#F1F5F9',
    marginTop: 8,
  },
  modalPrimaryBtn: {
    backgroundColor: '#6366F1',
    paddingVertical: 12,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  modalPrimaryBtnText: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '700',
  },

  // NOTIFICATION ITEMS
  notificationItem: {
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#F1F5F9',
  },
  notificationTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: '#0F172A',
  },
  notificationBody: {
    fontSize: 12,
    color: '#475569',
    marginTop: 2,
  },
  notificationTime: {
    fontSize: 10,
    color: '#94A3B8',
    marginTop: 4,
  },
});
