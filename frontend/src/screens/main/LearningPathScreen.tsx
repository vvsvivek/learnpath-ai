import React, { useState, useEffect, useMemo } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  SafeAreaView,
  TouchableOpacity,
  TextInput,
  ActivityIndicator,
  Modal,
  Alert,
  StatusBar,
  Dimensions,
} from 'react-native';
import { Colors, Shadows, Radii, Spacing } from '../../styles/theme';
import { api } from '../../services/api';

const { width } = Dimensions.get('window');

interface LearningPathScreenProps {
  userData: any;
  activeSubject: string;
  onSelectSubject: (subject: string) => void;
  onCompletePhase?: (subject: string, phaseId: number) => void;
}

interface ApprovedCourseItem {
  id: number;
  code: string;
  title: string;
  description: string;
  branch: string;
  academicYear: string;
  semester: string;
  difficulty: string;
  duration: string;
  category: string;
  isEligible?: boolean;
  isSyllabusVerified?: boolean;
  curriculumSource?: string;
}

interface CourseRequestItem {
  id: number;
  suggestedTitle: string;
  reason?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt?: string;
  reviewedAt?: string;
  reviewedBy?: string;
}

export default function LearningPathScreen({
  userData,
  activeSubject,
  onSelectSubject,
  onCompletePhase,
}: LearningPathScreenProps) {
  const email = userData?.email || '';
  const branch = userData?.branch || 'CSE';
  const academicYear = userData?.academicYear || '2nd Year';
  const semester = userData?.semester || '1st Sem';

  // Top View Toggle: 'curriculum' (My Syllabus Roadmap) vs 'catalogue' (Complete Course Catalogue)
  const [activeView, setActiveView] = useState<'curriculum' | 'catalogue'>('curriculum');

  // Enrolled Courses & Phases State
  const [courseList, setCourseList] = useState<any[]>([]);
  const [selectedCourse, setSelectedCourse] = useState<any>(null);
  const [loadingCourses, setLoadingCourses] = useState(true);

  // Active Phase Reading Material Viewer State
  const [activePhaseIndex, setActivePhaseIndex] = useState(0);

  // 10-Question Quiz Assessment Modal State
  const [quizModalVisible, setQuizModalVisible] = useState(false);
  const [quizLoading, setQuizLoading] = useState(false);
  const [quizQuestions, setQuizQuestions] = useState<any[]>([]);
  const [currentQuestionIdx, setCurrentQuestionIdx] = useState(0);
  const [selectedAnswers, setSelectedAnswers] = useState<Record<number, string>>({});

  // Post-Quiz Submission Result State
  const [quizSubmitting, setQuizSubmitting] = useState(false);
  const [quizResult, setQuizResult] = useState<any>(null);
  const [showResultModal, setShowResultModal] = useState(false);

  // Course Catalogue State (97 Courses)
  const [catalogue, setCatalogue] = useState<ApprovedCourseItem[]>([]);
  const [loadingCatalogue, setLoadingCatalogue] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [catalogueScope, setCatalogueScope] = useState<'eligible' | 'all'>('eligible');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [enrollingCourseTitle, setEnrollingCourseTitle] = useState<string | null>(null);

  // Request a Course Modal State
  const [showRequestModal, setShowRequestModal] = useState(false);
  const [requestTitle, setRequestTitle] = useState('');
  const [requestReason, setRequestReason] = useState('');
  const [submittingRequest, setSubmittingRequest] = useState(false);

  // My Requests Modal State
  const [showMyRequestsModal, setShowMyRequestsModal] = useState(false);
  const [myRequests, setMyRequests] = useState<CourseRequestItem[]>([]);
  const [loadingMyRequests, setLoadingMyRequests] = useState(false);

  // Fetch student enrolled courses and phases
  const fetchCoursesAndPhases = async () => {
    if (!email) return;
    try {
      setLoadingCourses(true);
      const dash = await api.getDashboard(email);
      const courses = dash?.activeCourses || [];
      setCourseList(courses);

      if (courses.length > 0) {
        const match = courses.find((c: any) => c.title.toLowerCase() === activeSubject.toLowerCase()) || courses[0];
        setSelectedCourse(match);
        onSelectSubject(match.title);
      } else {
        setSelectedCourse(null);
      }
    } catch (err: any) {
      console.warn('Failed to load courses:', err.message);
    } finally {
      setLoadingCourses(false);
    }
  };

  // Fetch approved course catalogue
  const fetchCatalogue = async () => {
    try {
      setLoadingCatalogue(true);
      const data = await api.getCatalogue({
        email,
        branch,
        year: academicYear,
        semester,
        scope: catalogueScope,
      });
      if (Array.isArray(data)) {
        setCatalogue(data);
      }
    } catch (err: any) {
      console.warn('Catalogue fetch error:', err.message);
    } finally {
      setLoadingCatalogue(false);
    }
  };

  // Fetch student's course requests
  const fetchMyRequests = async () => {
    if (!email) return;
    try {
      setLoadingMyRequests(true);
      const data = await api.getCourseRequests(email);
      if (Array.isArray(data)) {
        setMyRequests(data);
      }
    } catch (err: any) {
      console.warn('Course requests fetch warning:', err.message);
    } finally {
      setLoadingMyRequests(false);
    }
  };

  useEffect(() => {
    fetchCoursesAndPhases();
  }, [email, activeSubject]);

  useEffect(() => {
    fetchCatalogue();
  }, [email, branch, academicYear, semester, catalogueScope]);

  // Handle Enrollment from Catalogue
  const handleEnrollCourse = async (courseTitle: string, approvedCourseId?: number) => {
    if (!courseTitle.trim()) return;
    try {
      setEnrollingCourseTitle(courseTitle.trim());
      await api.selectCourse(email, courseTitle.trim(), approvedCourseId);
      await fetchCoursesAndPhases();
      onSelectSubject(courseTitle.trim());
      setActiveView('curriculum');
      Alert.alert(
        'Enrollment Successful! 🎉',
        `You have enrolled in "${courseTitle.trim()}". Your syllabus roadmap and study phases are now active in My Curriculum.`
      );
    } catch (err: any) {
      Alert.alert('Enrollment Error', err.message || 'Unable to enroll in course.');
    } finally {
      setEnrollingCourseTitle(null);
    }
  };

  // Handle Submit Course Request
  const handleSubmitCourseRequest = async () => {
    const trimmedTitle = requestTitle.trim();
    if (!trimmedTitle || trimmedTitle.length < 3) {
      Alert.alert('Validation Error', 'Please enter a valid course title (at least 3 characters).');
      return;
    }

    try {
      setSubmittingRequest(true);
      await api.requestCourse(email, trimmedTitle, requestReason.trim());
      Alert.alert(
        'Request Submitted',
        'Your course request has been submitted for faculty review with status PENDING. Verified learning paths are generated once approved.'
      );
      setRequestTitle('');
      setRequestReason('');
      setShowRequestModal(false);
      fetchMyRequests();
    } catch (err: any) {
      Alert.alert('Request Failed', err.message || 'Could not submit course request.');
    } finally {
      setSubmittingRequest(false);
    }
  };

  // Check if course is already enrolled
  const isAlreadyEnrolled = (title: string) => {
    return courseList.some(
      (c) => c.title?.trim().toLowerCase() === title.trim().toLowerCase()
    );
  };

  // Filtered catalogue courses
  const filteredCatalogue = useMemo(() => {
    return catalogue.filter((c) => {
      // Category filter
      if (selectedCategory !== 'ALL' && c.category?.toUpperCase() !== selectedCategory) {
        return false;
      }
      // Search filter
      if (searchQuery.trim()) {
        const q = searchQuery.trim().toLowerCase();
        const matchesTitle = c.title?.toLowerCase().includes(q);
        const matchesCode = c.code?.toLowerCase().includes(q);
        const matchesDesc = c.description?.toLowerCase().includes(q);
        const matchesBranch = c.branch?.toLowerCase().includes(q);
        return matchesTitle || matchesCode || matchesDesc || matchesBranch;
      }
      return true;
    });
  }, [catalogue, selectedCategory, searchQuery]);

  // Start 10-Question Quiz for a Phase
  const handleLaunchQuiz = async (phaseNumber: number) => {
    if (!selectedCourse) return;
    try {
      setQuizLoading(true);
      setQuizResult(null);
      setSelectedAnswers({});
      setCurrentQuestionIdx(0);
      setQuizModalVisible(true);

      const res = await api.generateQuiz(selectedCourse.id, selectedCourse.title, phaseNumber);
      if (res && Array.isArray(res.questions) && res.questions.length > 0) {
        setQuizQuestions(res.questions);
      } else {
        Alert.alert('Quiz Generation', 'Unable to load 10 questions for this phase.');
        setQuizModalVisible(false);
      }
    } catch (err: any) {
      Alert.alert('Assessment Error', err.message || 'Failed to start phase quiz.');
      setQuizModalVisible(false);
    } finally {
      setQuizLoading(false);
    }
  };

  // Submit Completed Quiz
  const handleSubmitQuiz = async () => {
    if (!selectedCourse || quizQuestions.length === 0) return;

    const unansweredCount = quizQuestions.filter((q) => !selectedAnswers[q.id]).length;
    if (unansweredCount > 0) {
      Alert.alert(
        'Incomplete Assessment',
        `You have ${unansweredCount} unanswered questions. All 10 questions must be answered before evaluation.`
      );
      return;
    }

    try {
      setQuizSubmitting(true);
      const phaseNum = activePhaseIndex + 1;

      const submissionPayload = {
        email,
        courseId: selectedCourse?.id,
        subject: selectedCourse?.title,
        phaseNumber: phaseNum,
        answers: selectedAnswers,
      };

      const result = await api.submitQuiz(submissionPayload);
      setQuizResult(result);
      setQuizModalVisible(false);
      setShowResultModal(true);

      if (result.passed && onCompletePhase) {
        onCompletePhase(selectedCourse.title, phaseNum);
      }

      await fetchCoursesAndPhases();
    } catch (err: any) {
      Alert.alert('Submission Error', err.message || 'Error evaluating quiz answers.');
    } finally {
      setQuizSubmitting(false);
    }
  };

  const phases = selectedCourse?.phases || [];
  const activePhase = phases[activePhaseIndex] || null;
  const courseProgress = selectedCourse?.progress !== undefined ? selectedCourse.progress : 0;

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />

      {/* Top Segmented View Switcher: Curriculum vs Catalogue */}
      <View style={styles.viewSwitcherContainer}>
        <TouchableOpacity
          style={[styles.viewSwitcherTab, activeView === 'curriculum' && styles.viewSwitcherTabActive]}
          onPress={() => setActiveView('curriculum')}
          activeOpacity={0.8}
        >
          <Text
            style={[
              styles.viewSwitcherTabText,
              activeView === 'curriculum' && styles.viewSwitcherTabTextActive,
            ]}
          >
            📖 My Curriculum {courseList.length > 0 ? `(${courseList.length})` : ''}
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.viewSwitcherTab, activeView === 'catalogue' && styles.viewSwitcherTabActive]}
          onPress={() => setActiveView('catalogue')}
          activeOpacity={0.8}
        >
          <Text
            style={[
              styles.viewSwitcherTabText,
              activeView === 'catalogue' && styles.viewSwitcherTabTextActive,
            ]}
          >
            📚 Course Catalogue
          </Text>
        </TouchableOpacity>
      </View>

      {/* =========================================================================
          VIEW 1: MY CURRICULUM (SYLLABUS ROADMAP & PHASES)
      ========================================================================= */}
      {activeView === 'curriculum' && (
        <>
          {/* Horizontal Enrolled Course Switcher Ribbon */}
          {courseList.length > 0 && (
            <View style={styles.ribbonContainer}>
              <ScrollView
                horizontal
                showsHorizontalScrollIndicator={false}
                contentContainerStyle={styles.ribbonScroll}
              >
                {courseList.map((course: any) => {
                  const isSelected = selectedCourse?.id === course.id;
                  return (
                    <TouchableOpacity
                      key={course.id}
                      style={[styles.ribbonPill, isSelected && styles.ribbonPillActive]}
                      onPress={() => {
                        setSelectedCourse(course);
                        onSelectSubject(course.title);
                        setActivePhaseIndex(0);
                      }}
                      activeOpacity={0.7}
                    >
                      <Text
                        style={[styles.ribbonPillText, isSelected && styles.ribbonPillTextActive]}
                        numberOfLines={1}
                      >
                        {course.title}
                      </Text>
                    </TouchableOpacity>
                  );
                })}
              </ScrollView>
            </View>
          )}

          <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
            {loadingCourses ? (
              <ActivityIndicator color={Colors.primary} size="small" style={{ marginVertical: 40 }} />
            ) : !selectedCourse ? (
              <View style={styles.emptyContainer}>
                <View style={styles.emptyIconCircle}>
                  <Text style={{ fontSize: 32 }}>📚</Text>
                </View>
                <Text style={styles.emptyTitle}>No Courses Enrolled Yet</Text>
                <Text style={styles.emptySubtitle}>
                  Explore the verified university catalogue to enroll in your semester courses and generate your autonomous syllabus roadmap.
                </Text>
                <TouchableOpacity
                  style={styles.exploreCatalogueBtn}
                  onPress={() => setActiveView('catalogue')}
                  activeOpacity={0.8}
                >
                  <Text style={styles.exploreCatalogueBtnText}>Explore Course Catalogue →</Text>
                </TouchableOpacity>
              </View>
            ) : (
              <>
                {/* Compact Subject Progress Banner */}
                <View style={styles.bannerCard}>
                  <View style={styles.bannerHeader}>
                    <Text style={styles.bannerSubjectTag} numberOfLines={1}>
                      SYLLABUS ROADMAP
                    </Text>
                    <Text style={styles.bannerProgressText}>{courseProgress}% Completed</Text>
                  </View>

                  <Text style={styles.bannerTitle} numberOfLines={2}>
                    {selectedCourse.title}
                  </Text>

                  <View style={styles.progressTrack}>
                    <View style={[styles.progressFill, { width: `${courseProgress}%` }]} />
                  </View>
                </View>

                {/* Stepper: Horizontal Syllabus Phases */}
                <View style={styles.stepperContainer}>
                  <Text style={styles.stepperTitle}>
                    Syllabus Phases ({phases.length})
                  </Text>
                  <ScrollView
                    horizontal
                    showsHorizontalScrollIndicator={false}
                    contentContainerStyle={styles.stepperScroll}
                  >
                    {phases.map((phase: any, idx: number) => {
                      const isCurrent = idx === activePhaseIndex;
                      const isUnlocked = phase.isUnlocked || idx === 0;
                      const isCompleted = phase.isCompleted;

                      return (
                        <TouchableOpacity
                          key={phase.id || idx}
                          style={[
                            styles.phaseStepCard,
                            isCurrent && styles.phaseStepCardSelected,
                            !isUnlocked && styles.phaseStepCardLocked,
                          ]}
                          onPress={() => isUnlocked && setActivePhaseIndex(idx)}
                          disabled={!isUnlocked}
                          activeOpacity={0.7}
                        >
                          <View style={[styles.stepBadge, isCompleted && styles.stepBadgeCompleted]}>
                            <Text style={[styles.stepBadgeText, isCompleted && styles.stepBadgeTextCompleted]}>
                              {isCompleted ? '✓' : idx + 1}
                            </Text>
                          </View>
                          <Text
                            style={[styles.stepTitleText, isCurrent && styles.stepTitleTextSelected]}
                            numberOfLines={2}
                          >
                            {phase.title}
                          </Text>
                          <Text style={styles.stepStatusLabel}>
                            {isCompleted ? 'Passed' : isUnlocked ? 'Unlocked' : 'Locked 🔒'}
                          </Text>
                        </TouchableOpacity>
                      );
                    })}
                  </ScrollView>
                </View>

                {/* Active Phase Study Material Card */}
                {activePhase && (
                  <View style={styles.studyCard}>
                    <View style={styles.studyHeader}>
                      <View style={styles.phasePill}>
                        <Text style={styles.phasePillText}>PHASE {activePhase.phaseNumber || activePhaseIndex + 1}</Text>
                      </View>
                      <Text style={styles.durationBadge}>⏱️ {activePhase.duration || '2-3 Hours'}</Text>
                    </View>

                    <Text style={styles.activePhaseTitle}>{activePhase.title}</Text>

                    <View style={styles.objectiveBox}>
                      <Text style={styles.objectiveTitle}>🎯 Learning Objectives</Text>
                      <Text style={styles.objectiveText}>
                        {activePhase.objectives || 'Master foundational paradigms and solve practical challenges.'}
                      </Text>
                    </View>

                    {/* Detailed Study Content */}
                    <View style={styles.contentBox}>
                      <Text style={styles.contentBoxTitle}>📖 Study Material &amp; Concepts</Text>
                      <Text style={styles.studyMaterialText}>
                        {activePhase.studyMaterial || 'Comprehensive curriculum materials for this phase.'}
                      </Text>
                    </View>

                    {/* Phase Action Buttons */}
                    <View style={styles.actionRow}>
                      <TouchableOpacity
                        style={[
                          styles.takeQuizBtn,
                          (!activePhase.isUnlocked && activePhaseIndex !== 0) && styles.takeQuizBtnDisabled,
                        ]}
                        onPress={() => handleLaunchQuiz(activePhase.phaseNumber || activePhaseIndex + 1)}
                        disabled={!activePhase.isUnlocked && activePhaseIndex !== 0}
                        activeOpacity={0.7}
                      >
                        <Text style={styles.takeQuizBtnText}>
                          📝 Take Phase {activePhase.phaseNumber || activePhaseIndex + 1} Quiz (10 Questions) →
                        </Text>
                      </TouchableOpacity>
                    </View>
                  </View>
                )}
              </>
            )}
          </ScrollView>
        </>
      )}

      {/* =========================================================================
          VIEW 2: COURSE CATALOGUE (97 APPROVED COURSES, SEARCH & FILTERS)
      ========================================================================= */}
      {activeView === 'catalogue' && (
        <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
          {/* Section Header & Request Actions */}
          <View style={styles.catalogueHeaderCard}>
            <View style={styles.catalogueTitleRow}>
              <Text style={styles.catalogueTitle}>Course Catalogue</Text>
              <View style={styles.approvedBadge}>
                <Text style={styles.approvedBadgeText}>APPROVED</Text>
              </View>
            </View>
            <Text style={styles.catalogueSubtitle}>
              Select an approved engineering course. Verified subjects are aligned with university autonomous regulations; synthesized tracks follow AICTE model guidelines.
            </Text>

            <View style={styles.catalogueActionButtonsRow}>
              <TouchableOpacity
                style={styles.requestCourseBtn}
                onPress={() => {
                  setRequestTitle(searchQuery.trim());
                  setShowRequestModal(true);
                }}
                activeOpacity={0.8}
              >
                <Text style={styles.requestCourseBtnText}>+ Request Course</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.myRequestsBtn}
                onPress={() => {
                  fetchMyRequests();
                  setShowMyRequestsModal(true);
                }}
                activeOpacity={0.8}
              >
                <Text style={styles.myRequestsBtnText}>
                  📋 My Requests {myRequests.length > 0 ? `(${myRequests.length})` : ''}
                </Text>
              </TouchableOpacity>
            </View>
          </View>

          {/* Search Bar */}
          <View style={styles.searchContainer}>
            <Text style={styles.searchIcon}>🔍</Text>
            <TextInput
              style={styles.searchInput}
              placeholder="Search approved courses by title, code, branch..."
              placeholderTextColor="#94A3B8"
              value={searchQuery}
              onChangeText={setSearchQuery}
            />
            {searchQuery.length > 0 && (
              <TouchableOpacity onPress={() => setSearchQuery('')} style={styles.clearSearchBtn}>
                <Text style={styles.clearSearchText}>✕</Text>
              </TouchableOpacity>
            )}
          </View>

          {/* Scope Filters (Eligible for Me vs All Approved) */}
          <View style={styles.scopeTabsRow}>
            <TouchableOpacity
              style={[styles.scopeTab, catalogueScope === 'eligible' && styles.scopeTabActive]}
              onPress={() => setCatalogueScope('eligible')}
              activeOpacity={0.8}
            >
              <Text style={[styles.scopeTabText, catalogueScope === 'eligible' && styles.scopeTabTextActive]}>
                🎯 Eligible for Me ({branch})
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.scopeTab, catalogueScope === 'all' && styles.scopeTabActive]}
              onPress={() => setCatalogueScope('all')}
              activeOpacity={0.8}
            >
              <Text style={[styles.scopeTabText, catalogueScope === 'all' && styles.scopeTabTextActive]}>
                🌐 All Approved Courses
              </Text>
            </TouchableOpacity>
          </View>

          {/* Category Filter Pills */}
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.categoryPillsRow}>
            {['ALL', 'CORE', 'FOUNDATION', 'ELECTIVE', 'SPECIALIZATION', 'LAB'].map((cat) => (
              <TouchableOpacity
                key={cat}
                style={[styles.categoryPill, selectedCategory === cat && styles.categoryPillActive]}
                onPress={() => setSelectedCategory(cat)}
                activeOpacity={0.7}
              >
                <Text style={[styles.categoryPillText, selectedCategory === cat && styles.categoryPillTextActive]}>
                  {cat === 'ALL' ? 'All Types' : cat}
                </Text>
              </TouchableOpacity>
            ))}
          </ScrollView>

          {/* Catalogue Course List */}
          {loadingCatalogue ? (
            <View style={styles.loaderContainer}>
              <ActivityIndicator color={Colors.primary} size="small" />
              <Text style={styles.loaderText}>Loading approved courses...</Text>
            </View>
          ) : filteredCatalogue.length === 0 ? (
            <View style={styles.emptyCatalogueBox}>
              <Text style={styles.emptyIcon}>🔍</Text>
              <Text style={styles.emptyTitle}>
                {searchQuery ? `No courses match "${searchQuery}"` : 'No courses found'}
              </Text>
              <Text style={styles.emptySubtitle}>
                LearnPath AI strictly validates approved engineering curricula. Arbitrary course titles are not permitted.
              </Text>
              <TouchableOpacity
                style={styles.emptyRequestBtn}
                onPress={() => {
                  setRequestTitle(searchQuery.trim());
                  setShowRequestModal(true);
                }}
                activeOpacity={0.8}
              >
                <Text style={styles.emptyRequestBtnText}>+ Request Course for Faculty Review</Text>
              </TouchableOpacity>
            </View>
          ) : (
            <View style={styles.coursesListContainer}>
              {filteredCatalogue.map((course) => {
                const enrolled = isAlreadyEnrolled(course.title);
                const isEnrolling = enrollingCourseTitle === course.title;

                return (
                  <View key={course.id} style={styles.catalogueCard}>
                    {/* Top Row: Code, Category, Syllabus Badge */}
                    <View style={styles.cardHeaderRow}>
                      <View style={styles.codeBadge}>
                        <Text style={styles.codeBadgeText}>{course.code || 'ENG'}</Text>
                      </View>
                      <View style={styles.categoryBadge}>
                        <Text style={styles.categoryBadgeText}>{course.category || 'CORE'}</Text>
                      </View>
                      {course.isSyllabusVerified ? (
                        <View style={styles.verifiedBadge}>
                          <Text style={styles.verifiedBadgeText}>✓ Autonomous Verified</Text>
                        </View>
                      ) : (
                        <View style={styles.synthesizedBadge}>
                          <Text style={styles.synthesizedBadgeText}>⚡ Synthesized Track</Text>
                        </View>
                      )}
                    </View>

                    {/* Course Title & Description */}
                    <Text style={styles.courseCardTitle}>{course.title}</Text>
                    <Text style={styles.courseCardDesc} numberOfLines={3}>
                      {course.description || 'Comprehensive curriculum aligned with AICTE engineering model regulations.'}
                    </Text>

                    {/* Metadata Row */}
                    <View style={styles.cardMetaRow}>
                      <Text style={styles.cardMetaItem}>🎓 {course.branch || branch}</Text>
                      <Text style={styles.cardMetaDot}>•</Text>
                      <Text style={styles.cardMetaItem}>📚 {course.academicYear || academicYear} • {course.semester || semester}</Text>
                      <Text style={styles.cardMetaDot}>•</Text>
                      <Text style={styles.cardMetaItem}>⏱️ {course.duration || '40-60 Hours'}</Text>
                    </View>

                    {/* Action Button */}
                    <View style={styles.cardActionRow}>
                      {enrolled ? (
                        <TouchableOpacity
                          style={styles.enrolledActionBtn}
                          onPress={() => {
                            const match = courseList.find((c) => c.title.toLowerCase() === course.title.toLowerCase());
                            if (match) setSelectedCourse(match);
                            onSelectSubject(course.title);
                            setActiveView('curriculum');
                          }}
                          activeOpacity={0.8}
                        >
                          <Text style={styles.enrolledActionText}>✓ Enrolled (Open in Curriculum) →</Text>
                        </TouchableOpacity>
                      ) : (
                        <TouchableOpacity
                          style={styles.enrollActionBtn}
                          onPress={() => handleEnrollCourse(course.title, course.id)}
                          disabled={isEnrolling}
                          activeOpacity={0.8}
                        >
                          {isEnrolling ? (
                            <ActivityIndicator color="#FFFFFF" size="small" />
                          ) : (
                            <Text style={styles.enrollActionText}>Enroll &amp; Start Learning →</Text>
                          )}
                        </TouchableOpacity>
                      )}
                    </View>
                  </View>
                );
              })}
            </View>
          )}
        </ScrollView>
      )}

      {/* =========================================================================
          10-QUESTION QUIZ MODAL
      ========================================================================= */}
      <Modal visible={quizModalVisible} animationType="slide" transparent>
        <SafeAreaView style={styles.modalOverlay}>
          <View style={styles.quizModalContent}>
            {/* Modal Header */}
            <View style={styles.quizHeader}>
              <View>
                <Text style={styles.quizHeaderSub}>ASSESSMENT IN PROGRESS</Text>
                <Text style={styles.quizHeaderTitle}>
                  Question {currentQuestionIdx + 1} of {quizQuestions.length}
                </Text>
              </View>
              <TouchableOpacity
                onPress={() => {
                  Alert.alert('Exit Quiz', 'Are you sure you want to exit? Your answers will not be saved.', [
                    { text: 'Cancel', style: 'cancel' },
                    { text: 'Exit', style: 'destructive', onPress: () => setQuizModalVisible(false) },
                  ]);
                }}
              >
                <Text style={styles.closeBtnText}>✕ Close</Text>
              </TouchableOpacity>
            </View>

            {/* Quiz Question Progress Indicator */}
            <View style={styles.quizProgressTrack}>
              <View
                style={[
                  styles.quizProgressFill,
                  { width: `${((currentQuestionIdx + 1) / Math.max(quizQuestions.length, 1)) * 100}%` },
                ]}
              />
            </View>

            {quizLoading ? (
              <ActivityIndicator color={Colors.primary} size="small" style={{ marginVertical: 40 }} />
            ) : quizQuestions.length === 0 ? (
              <Text style={styles.emptyText}>No questions generated for this phase.</Text>
            ) : (
              <ScrollView style={{ flex: 1 }} showsVerticalScrollIndicator={false}>
                {(() => {
                  const q = quizQuestions[currentQuestionIdx];
                  if (!q) return null;
                  const currentAnswer = selectedAnswers[q.id];
                  const letters = ['A', 'B', 'C', 'D'];

                  return (
                    <View style={styles.questionCard}>
                      <Text style={styles.questionText}>{q.questionText}</Text>

                      {Array.isArray(q.options) &&
                        q.options.map((optText: string, oIdx: number) => {
                          const letter = letters[oIdx] || 'A';
                          const isSelected = currentAnswer === letter;
                          return (
                            <TouchableOpacity
                              key={oIdx}
                              style={[styles.optionCard, isSelected && styles.optionCardSelected]}
                              onPress={() => setSelectedAnswers((prev) => ({ ...prev, [q.id]: letter }))}
                              activeOpacity={0.7}
                            >
                              <View style={[styles.optionLetterBadge, isSelected && styles.optionLetterBadgeSelected]}>
                                <Text style={[styles.optionLetterText, isSelected && styles.optionLetterTextSelected]}>
                                  {letter}
                                </Text>
                              </View>
                              <Text style={[styles.optionContentText, isSelected && styles.optionContentTextSelected]}>
                                {optText}
                              </Text>
                            </TouchableOpacity>
                          );
                        })}
                    </View>
                  );
                })()}
              </ScrollView>
            )}

            {/* Quiz Navigation Buttons */}
            <View style={styles.quizNavRow}>
              <TouchableOpacity
                style={[styles.navBtn, currentQuestionIdx === 0 && styles.navBtnDisabled]}
                onPress={() => setCurrentQuestionIdx((prev) => Math.max(0, prev - 1))}
                disabled={currentQuestionIdx === 0}
              >
                <Text style={styles.navBtnText}>← Previous</Text>
              </TouchableOpacity>

              {currentQuestionIdx < quizQuestions.length - 1 ? (
                <TouchableOpacity
                  style={[styles.navBtn, styles.navBtnPrimary]}
                  onPress={() => setCurrentQuestionIdx((prev) => Math.min(quizQuestions.length - 1, prev + 1))}
                >
                  <Text style={styles.navBtnPrimaryText}>Next →</Text>
                </TouchableOpacity>
              ) : (
                <TouchableOpacity
                  style={[styles.navBtn, styles.submitBtn]}
                  onPress={handleSubmitQuiz}
                  disabled={quizSubmitting}
                >
                  {quizSubmitting ? (
                    <ActivityIndicator color="#FFFFFF" size="small" />
                  ) : (
                    <Text style={styles.submitBtnText}>Submit Assessment ✓</Text>
                  )}
                </TouchableOpacity>
              )}
            </View>
          </View>
        </SafeAreaView>
      </Modal>

      {/* =========================================================================
          POST-QUIZ RESULT MODAL (FEEDBACK & PASS/FAIL)
      ========================================================================= */}
      <Modal visible={showResultModal} animationType="fade" transparent>
        <SafeAreaView style={styles.modalOverlay}>
          <View style={styles.resultCard}>
            <View style={[styles.resultIconCircle, quizResult?.passed ? styles.iconPassed : styles.iconFailed]}>
              <Text style={{ fontSize: 28 }}>{quizResult?.passed ? '🎉' : '⚠️'}</Text>
            </View>

            <Text style={styles.resultTitle}>
              {quizResult?.passed ? 'Phase Assessment Passed!' : 'Assessment Incomplete'}
            </Text>

            <View style={styles.scoreRow}>
              <Text style={styles.scoreBigText}>
                {quizResult?.score} / {quizResult?.totalQuestions || 10}
              </Text>
              <Text style={[styles.pctBadge, quizResult?.passed ? styles.pctPassed : styles.pctFailed]}>
                {quizResult?.percentage}%
              </Text>
            </View>

            <Text style={styles.resultRuleExplanation}>
              {quizResult?.passed
                ? 'Score strictly > 60% achieved (7/10 or higher). The next learning phase has been unlocked automatically!'
                : 'Progression rule: A score strictly greater than 60% (at least 7/10) is required to unlock the next phase. Review the concepts below and retake the quiz.'}
            </Text>

            {/* Incorrect Concepts Review */}
            {quizResult?.review && (
              <ScrollView style={styles.reviewScroll} showsVerticalScrollIndicator={false}>
                <Text style={styles.reviewHeader}>Question Analysis &amp; Explanations:</Text>
                {quizResult.review.map((item: any, rIdx: number) => (
                  <View key={rIdx} style={styles.reviewItem}>
                    <Text style={styles.reviewQuestionText}>
                      Q{rIdx + 1}: {item.questionText}
                    </Text>
                    <Text style={item.isCorrect ? styles.correctReview : styles.wrongReview}>
                      Your Answer: {item.selectedOption || 'None'} {item.isCorrect ? '✓' : `✗ (Correct: ${item.correctOption})`}
                    </Text>
                    <Text style={styles.explanationText}>💡 {item.explanation}</Text>
                  </View>
                ))}
              </ScrollView>
            )}

            {/* Action Buttons */}
            <TouchableOpacity
              style={styles.resultDoneBtn}
              onPress={() => setShowResultModal(false)}
            >
              <Text style={styles.resultDoneBtnText}>
                {quizResult?.passed ? 'Continue Learning →' : 'Return to Study Material'}
              </Text>
            </TouchableOpacity>
          </View>
        </SafeAreaView>
      </Modal>

      {/* =========================================================================
          REQUEST A COURSE MODAL
      ========================================================================= */}
      <Modal visible={showRequestModal} animationType="slide" transparent>
        <SafeAreaView style={styles.modalOverlay}>
          <View style={styles.requestModalContent}>
            <View style={styles.modalHeaderRow}>
              <View>
                <Text style={styles.modalHeaderTitle}>Request a New Course</Text>
                <Text style={styles.modalHeaderSubtitle}>Submit a proposal for faculty syllabus verification.</Text>
              </View>
              <TouchableOpacity onPress={() => setShowRequestModal(false)}>
                <Text style={styles.closeBtnText}>✕ Close</Text>
              </TouchableOpacity>
            </View>

            <View style={styles.modalInfoBanner}>
              <Text style={styles.modalInfoText}>
                Faculty review ensures course content aligns with autonomous academic regulations and AICTE guidelines before activation.
              </Text>
            </View>

            <Text style={styles.inputLabel}>Proposed Course Title *</Text>
            <TextInput
              style={styles.modalInput}
              placeholder="e.g. Distributed Cloud Computing"
              placeholderTextColor="#94A3B8"
              value={requestTitle}
              onChangeText={setRequestTitle}
            />

            <Text style={styles.inputLabel}>Reason / Learning Motivation</Text>
            <TextInput
              style={[styles.modalInput, styles.modalTextArea]}
              placeholder="Explain why this subject is valuable for your academic or career track..."
              placeholderTextColor="#94A3B8"
              multiline
              numberOfLines={3}
              value={requestReason}
              onChangeText={setRequestReason}
            />

            <View style={styles.modalActionsRow}>
              <TouchableOpacity
                style={styles.modalCancelBtn}
                onPress={() => setShowRequestModal(false)}
              >
                <Text style={styles.modalCancelBtnText}>Cancel</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.modalSubmitBtn}
                onPress={handleSubmitCourseRequest}
                disabled={submittingRequest}
              >
                {submittingRequest ? (
                  <ActivityIndicator color="#FFFFFF" size="small" />
                ) : (
                  <Text style={styles.modalSubmitBtnText}>Submit Request ✓</Text>
                )}
              </TouchableOpacity>
            </View>
          </View>
        </SafeAreaView>
      </Modal>

      {/* =========================================================================
          MY REQUESTS MODAL
      ========================================================================= */}
      <Modal visible={showMyRequestsModal} animationType="slide" transparent>
        <SafeAreaView style={styles.modalOverlay}>
          <View style={styles.requestModalContent}>
            <View style={styles.modalHeaderRow}>
              <View>
                <Text style={styles.modalHeaderTitle}>My Course Requests</Text>
                <Text style={styles.modalHeaderSubtitle}>Track your submissions and review status.</Text>
              </View>
              <TouchableOpacity onPress={() => setShowMyRequestsModal(false)}>
                <Text style={styles.closeBtnText}>✕ Close</Text>
              </TouchableOpacity>
            </View>

            {loadingMyRequests ? (
              <ActivityIndicator color={Colors.primary} size="small" style={{ marginVertical: 30 }} />
            ) : myRequests.length === 0 ? (
              <View style={styles.emptyRequestsBox}>
                <Text style={{ fontSize: 32, marginBottom: 8 }}>📋</Text>
                <Text style={styles.emptyRequestsTitle}>No Requests Submitted</Text>
                <Text style={styles.emptyRequestsSubtitle}>
                  You haven't requested any custom courses yet. If a subject is missing from the catalogue, tap "+ Request Course".
                </Text>
              </View>
            ) : (
              <ScrollView style={{ maxHeight: 380 }} showsVerticalScrollIndicator={false}>
                {myRequests.map((req) => (
                  <View key={req.id} style={styles.requestItemCard}>
                    <View style={styles.requestItemHeader}>
                      <Text style={styles.requestItemTitle}>{req.suggestedTitle}</Text>
                      <View
                        style={[
                          styles.statusBadge,
                          req.status === 'APPROVED'
                            ? styles.statusApproved
                            : req.status === 'REJECTED'
                            ? styles.statusRejected
                            : styles.statusPending,
                        ]}
                      >
                        <Text
                          style={[
                            styles.statusBadgeText,
                            req.status === 'APPROVED'
                              ? styles.statusApprovedText
                              : req.status === 'REJECTED'
                              ? styles.statusRejectedText
                              : styles.statusPendingText,
                          ]}
                        >
                          {req.status}
                        </Text>
                      </View>
                    </View>
                    {req.reason ? <Text style={styles.requestItemReason}>"{req.reason}"</Text> : null}
                    <Text style={styles.requestItemDate}>
                      Submitted: {req.createdAt ? new Date(req.createdAt).toLocaleDateString() : 'Recently'}
                    </Text>
                    {req.status === 'APPROVED' && (
                      <TouchableOpacity
                        style={styles.requestEnrollBtn}
                        onPress={() => {
                          setShowMyRequestsModal(false);
                          handleEnrollCourse(req.suggestedTitle);
                        }}
                      >
                        <Text style={styles.requestEnrollBtnText}>Enroll &amp; Start Learning →</Text>
                      </TouchableOpacity>
                    )}
                  </View>
                ))}
              </ScrollView>
            )}
          </View>
        </SafeAreaView>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8FAFC',
  },

  // VIEW SWITCHER TABS
  viewSwitcherContainer: {
    flexDirection: 'row',
    backgroundColor: '#EEF2F6',
    borderRadius: 14,
    padding: 4,
    marginHorizontal: 16,
    marginTop: 8,
    marginBottom: 8,
  },
  viewSwitcherTab: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  viewSwitcherTabActive: {
    backgroundColor: '#6D28D9',
    ...Shadows.small,
  },
  viewSwitcherTabText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#64748B',
  },
  viewSwitcherTabTextActive: {
    color: '#FFFFFF',
    fontWeight: '700',
  },

  // RIBBON CONTAINER
  ribbonContainer: {
    backgroundColor: '#FFFFFF',
    borderBottomWidth: 1,
    borderColor: '#E2E8F0',
    paddingVertical: 8,
  },
  ribbonScroll: {
    paddingHorizontal: 16,
    gap: 8,
  },
  ribbonPill: {
    backgroundColor: '#F1F5F9',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: Radii.full,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    maxWidth: 220,
  },
  ribbonPillActive: {
    backgroundColor: '#6D28D9',
    borderColor: '#6D28D9',
  },
  ribbonPillText: {
    fontSize: 11,
    fontWeight: '600',
    color: '#475569',
  },
  ribbonPillTextActive: {
    color: '#FFFFFF',
    fontWeight: '700',
  },

  scroll: {
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: 110, // Generous clearance for floating bottom navigation bar
  },

  // PROGRESS BANNER
  bannerCard: {
    backgroundColor: '#4338CA',
    borderRadius: 16,
    padding: 16,
    marginBottom: 16,
    ...Shadows.small,
  },
  bannerHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  bannerSubjectTag: {
    fontSize: 10,
    fontWeight: '700',
    color: '#C7D2FE',
    letterSpacing: 0.5,
  },
  bannerProgressText: {
    fontSize: 11,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  bannerTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#FFFFFF',
    marginBottom: 10,
    lineHeight: 22,
  },
  progressTrack: {
    height: 6,
    backgroundColor: 'rgba(255, 255, 255, 0.25)',
    borderRadius: Radii.full,
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#A5B4FC',
    borderRadius: Radii.full,
  },

  // STEPPER
  stepperContainer: {
    marginBottom: 16,
  },
  stepperTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#0F172A',
    marginBottom: 10,
  },
  stepperScroll: {
    gap: 10,
  },
  phaseStepCard: {
    width: 120,
    backgroundColor: '#FFFFFF',
    borderRadius: 14,
    padding: 12,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    ...Shadows.small,
  },
  phaseStepCardSelected: {
    borderColor: '#6D28D9',
    borderWidth: 1.5,
    backgroundColor: '#F5F3FF',
  },
  phaseStepCardLocked: {
    opacity: 0.6,
    backgroundColor: '#F8FAFC',
  },
  stepBadge: {
    width: 24,
    height: 24,
    borderRadius: Radii.full,
    backgroundColor: '#EDE9FE',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 6,
  },
  stepBadgeCompleted: {
    backgroundColor: '#DCFCE7',
  },
  stepBadgeText: {
    fontSize: 11,
    fontWeight: '700',
    color: '#6D28D9',
  },
  stepBadgeTextCompleted: {
    color: '#059669',
  },
  stepTitleText: {
    fontSize: 11,
    fontWeight: '600',
    color: '#0F172A',
    lineHeight: 15,
    marginBottom: 4,
    minHeight: 30,
  },
  stepTitleTextSelected: {
    color: '#6D28D9',
    fontWeight: '700',
  },
  stepStatusLabel: {
    fontSize: 9,
    fontWeight: '600',
    color: '#64748B',
  },

  // STUDY CARD
  studyCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    ...Shadows.small,
    marginBottom: 16,
  },
  studyHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  phasePill: {
    backgroundColor: '#EDE9FE',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  phasePillText: {
    color: '#6D28D9',
    fontSize: 10,
    fontWeight: '700',
  },
  durationBadge: {
    fontSize: 11,
    color: '#64748B',
    fontWeight: '500',
  },
  activePhaseTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#0F172A',
    marginBottom: 12,
    lineHeight: 22,
  },
  objectiveBox: {
    backgroundColor: '#F5F3FF',
    padding: 10,
    borderRadius: 10,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#DDD6FE',
  },
  objectiveTitle: {
    fontSize: 11,
    fontWeight: '700',
    color: '#6D28D9',
    marginBottom: 3,
  },
  objectiveText: {
    fontSize: 12,
    color: '#1E293B',
    lineHeight: 16,
  },
  contentBox: {
    marginBottom: 16,
  },
  contentBoxTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: '#0F172A',
    marginBottom: 6,
  },
  studyMaterialText: {
    fontSize: 12,
    color: '#334155',
    lineHeight: 18,
  },
  actionRow: {
    marginTop: 6,
  },
  takeQuizBtn: {
    backgroundColor: '#6366F1',
    paddingVertical: 12,
    borderRadius: 12,
    alignItems: 'center',
  },
  takeQuizBtnDisabled: {
    backgroundColor: '#CBD5E1',
  },
  takeQuizBtnText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 13,
  },

  // EMPTY CURRICULUM STATE
  emptyContainer: {
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    borderRadius: 18,
    padding: 28,
    marginTop: 20,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    ...Shadows.small,
  },
  emptyIconCircle: {
    width: 64,
    height: 64,
    borderRadius: Radii.full,
    backgroundColor: '#EDE9FE',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 14,
  },
  emptyTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#0F172A',
    marginBottom: 6,
    textAlign: 'center',
  },
  emptySubtitle: {
    fontSize: 12,
    color: '#64748B',
    textAlign: 'center',
    lineHeight: 18,
    marginBottom: 18,
  },
  exploreCatalogueBtn: {
    backgroundColor: '#6D28D9',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 12,
  },
  exploreCatalogueBtnText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 13,
  },

  // CATALOGUE HEADER CARD
  catalogueHeaderCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    ...Shadows.small,
  },
  catalogueTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 6,
  },
  catalogueTitle: {
    fontSize: 17,
    fontWeight: '800',
    color: '#0F172A',
  },
  approvedBadge: {
    backgroundColor: '#DCFCE7',
    paddingHorizontal: 7,
    paddingVertical: 2,
    borderRadius: 6,
  },
  approvedBadgeText: {
    color: '#059669',
    fontSize: 10,
    fontWeight: '800',
  },
  catalogueSubtitle: {
    fontSize: 12,
    color: '#64748B',
    lineHeight: 16,
    marginBottom: 12,
  },
  catalogueActionButtonsRow: {
    flexDirection: 'row',
    gap: 8,
  },
  requestCourseBtn: {
    flex: 1,
    backgroundColor: '#EDE9FE',
    paddingVertical: 9,
    borderRadius: 10,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#DDD6FE',
  },
  requestCourseBtnText: {
    color: '#6D28D9',
    fontSize: 12,
    fontWeight: '700',
  },
  myRequestsBtn: {
    flex: 1,
    backgroundColor: '#F1F5F9',
    paddingVertical: 9,
    borderRadius: 10,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  myRequestsBtnText: {
    color: '#334155',
    fontSize: 12,
    fontWeight: '600',
  },

  // SEARCH BAR
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    paddingHorizontal: 12,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    marginBottom: 10,
    height: 44,
  },
  searchIcon: {
    fontSize: 14,
    marginRight: 8,
  },
  searchInput: {
    flex: 1,
    fontSize: 13,
    color: '#0F172A',
    paddingVertical: 0,
  },
  clearSearchBtn: {
    padding: 4,
  },
  clearSearchText: {
    fontSize: 14,
    color: '#94A3B8',
    fontWeight: '700',
  },

  // SCOPE TABS
  scopeTabsRow: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 10,
  },
  scopeTab: {
    flex: 1,
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    paddingVertical: 8,
    borderRadius: 10,
    alignItems: 'center',
  },
  scopeTabActive: {
    backgroundColor: '#EDE9FE',
    borderColor: '#6D28D9',
  },
  scopeTabText: {
    fontSize: 11,
    fontWeight: '600',
    color: '#64748B',
  },
  scopeTabTextActive: {
    color: '#6D28D9',
    fontWeight: '700',
  },

  // CATEGORY PILLS
  categoryPillsRow: {
    flexDirection: 'row',
    gap: 6,
    marginBottom: 14,
    paddingVertical: 2,
  },
  categoryPill: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: Radii.full,
  },
  categoryPillActive: {
    backgroundColor: '#6D28D9',
    borderColor: '#6D28D9',
  },
  categoryPillText: {
    fontSize: 11,
    fontWeight: '600',
    color: '#64748B',
  },
  categoryPillTextActive: {
    color: '#FFFFFF',
    fontWeight: '700',
  },

  // LOADER & EMPTY CATALOGUE
  loaderContainer: {
    alignItems: 'center',
    paddingVertical: 40,
    gap: 8,
  },
  loaderText: {
    fontSize: 12,
    color: '#64748B',
  },
  emptyCatalogueBox: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 24,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  emptyIcon: {
    fontSize: 28,
    marginBottom: 8,
  },
  emptyRequestBtn: {
    backgroundColor: '#6366F1',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 10,
    marginTop: 12,
  },
  emptyRequestBtnText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '700',
  },

  // CATALOGUE COURSE CARDS
  coursesListContainer: {
    gap: 12,
  },
  catalogueCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    ...Shadows.small,
  },
  cardHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 8,
    flexWrap: 'wrap',
  },
  codeBadge: {
    backgroundColor: '#F1F5F9',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 6,
  },
  codeBadgeText: {
    fontSize: 10,
    fontWeight: '700',
    color: '#475569',
  },
  categoryBadge: {
    backgroundColor: '#EDE9FE',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 6,
  },
  categoryBadgeText: {
    fontSize: 10,
    fontWeight: '700',
    color: '#6D28D9',
  },
  verifiedBadge: {
    backgroundColor: '#DCFCE7',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 6,
  },
  verifiedBadgeText: {
    fontSize: 10,
    fontWeight: '700',
    color: '#059669',
  },
  synthesizedBadge: {
    backgroundColor: '#FEF3C7',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 6,
  },
  synthesizedBadgeText: {
    fontSize: 10,
    fontWeight: '700',
    color: '#B45309',
  },
  courseCardTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: '#0F172A',
    marginBottom: 6,
    lineHeight: 20,
  },
  courseCardDesc: {
    fontSize: 12,
    color: '#64748B',
    lineHeight: 17,
    marginBottom: 10,
  },
  cardMetaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 12,
    flexWrap: 'wrap',
  },
  cardMetaItem: {
    fontSize: 11,
    color: '#64748B',
    fontWeight: '500',
  },
  cardMetaDot: {
    fontSize: 11,
    color: '#CBD5E1',
  },
  cardActionRow: {
    borderTopWidth: 1,
    borderTopColor: '#F1F5F9',
    paddingTop: 10,
  },
  enrolledActionBtn: {
    backgroundColor: '#F1F5F9',
    paddingVertical: 8,
    borderRadius: 10,
    alignItems: 'center',
  },
  enrolledActionText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#059669',
  },
  enrollActionBtn: {
    backgroundColor: '#6366F1',
    paddingVertical: 10,
    borderRadius: 10,
    alignItems: 'center',
  },
  enrollActionText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#FFFFFF',
  },

  // MODAL OVERLAYS & SHEETS
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.6)',
    justifyContent: 'center',
    padding: 16,
  },
  quizModalContent: {
    flex: 1,
    backgroundColor: '#FFFFFF',
    borderRadius: 20,
    padding: 16,
    maxHeight: '92%',
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  quizHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  quizHeaderSub: {
    fontSize: 9,
    fontWeight: '700',
    color: '#6366F1',
  },
  quizHeaderTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#0F172A',
  },
  closeBtnText: {
    fontSize: 12,
    color: '#64748B',
    fontWeight: '600',
  },
  quizProgressTrack: {
    height: 4,
    backgroundColor: '#F1F5F9',
    borderRadius: Radii.full,
    marginBottom: 14,
  },
  quizProgressFill: {
    height: '100%',
    backgroundColor: '#6366F1',
    borderRadius: Radii.full,
  },
  questionCard: {
    marginBottom: 14,
  },
  questionText: {
    fontSize: 13,
    fontWeight: '600',
    color: '#0F172A',
    lineHeight: 18,
    marginBottom: 14,
  },
  optionCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F8FAFC',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    borderRadius: 12,
    padding: 10,
    marginBottom: 8,
  },
  optionCardSelected: {
    borderColor: '#6D28D9',
    backgroundColor: '#F5F3FF',
  },
  optionLetterBadge: {
    width: 24,
    height: 24,
    borderRadius: Radii.full,
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 10,
  },
  optionLetterBadgeSelected: {
    backgroundColor: '#6D28D9',
    borderColor: '#6D28D9',
  },
  optionLetterText: {
    fontSize: 11,
    fontWeight: '700',
    color: '#0F172A',
  },
  optionLetterTextSelected: {
    color: '#FFFFFF',
  },
  optionContentText: {
    flex: 1,
    fontSize: 12,
    color: '#0F172A',
    lineHeight: 16,
  },
  optionContentTextSelected: {
    fontWeight: '600',
    color: '#6D28D9',
  },
  quizNavRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingTop: 10,
    borderTopWidth: 1,
    borderColor: '#E2E8F0',
    gap: 8,
  },
  navBtn: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 10,
    alignItems: 'center',
    backgroundColor: '#F1F5F9',
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  navBtnDisabled: {
    opacity: 0.4,
  },
  navBtnText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#64748B',
  },
  navBtnPrimary: {
    backgroundColor: '#6366F1',
    borderColor: '#6366F1',
  },
  navBtnPrimaryText: {
    color: '#FFFFFF',
    fontWeight: '600',
    fontSize: 12,
  },
  submitBtn: {
    backgroundColor: '#059669',
    borderColor: '#059669',
  },
  submitBtnText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 12,
  },

  // RESULT MODAL
  resultCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 20,
    padding: 20,
    alignItems: 'center',
    maxHeight: '85%',
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  resultIconCircle: {
    width: 56,
    height: 56,
    borderRadius: Radii.full,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 10,
  },
  iconPassed: {
    backgroundColor: '#DCFCE7',
  },
  iconFailed: {
    backgroundColor: '#FEE2E2',
  },
  resultTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#0F172A',
    marginBottom: 6,
    textAlign: 'center',
  },
  scoreRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 10,
  },
  scoreBigText: {
    fontSize: 22,
    fontWeight: '700',
    color: '#0F172A',
  },
  pctBadge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: Radii.full,
    fontSize: 12,
    fontWeight: '700',
  },
  pctPassed: {
    backgroundColor: '#DCFCE7',
    color: '#059669',
  },
  pctFailed: {
    backgroundColor: '#FEE2E2',
    color: '#DC2626',
  },
  resultRuleExplanation: {
    fontSize: 11,
    color: '#64748B',
    textAlign: 'center',
    lineHeight: 15,
    marginBottom: 12,
  },
  reviewScroll: {
    width: '100%',
    maxHeight: 160,
    marginBottom: 12,
  },
  reviewHeader: {
    fontSize: 11,
    fontWeight: '700',
    color: '#0F172A',
    marginBottom: 6,
  },
  reviewItem: {
    backgroundColor: '#F8FAFC',
    padding: 8,
    borderRadius: 8,
    marginBottom: 6,
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  reviewQuestionText: {
    fontSize: 11,
    fontWeight: '600',
    color: '#0F172A',
    marginBottom: 2,
  },
  correctReview: {
    fontSize: 10,
    fontWeight: '700',
    color: '#059669',
  },
  wrongReview: {
    fontSize: 10,
    fontWeight: '700',
    color: '#DC2626',
  },
  explanationText: {
    fontSize: 10,
    color: '#64748B',
    marginTop: 2,
  },
  resultDoneBtn: {
    width: '100%',
    backgroundColor: '#6366F1',
    paddingVertical: 12,
    borderRadius: 12,
    alignItems: 'center',
  },
  resultDoneBtnText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 13,
  },

  // REQUEST / MY REQUESTS MODAL
  requestModalContent: {
    backgroundColor: '#FFFFFF',
    borderRadius: 20,
    padding: 20,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    ...Shadows.small,
  },
  modalHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 14,
  },
  modalHeaderTitle: {
    fontSize: 16,
    fontWeight: '800',
    color: '#0F172A',
  },
  modalHeaderSubtitle: {
    fontSize: 11,
    color: '#64748B',
    marginTop: 2,
  },
  modalInfoBanner: {
    backgroundColor: '#F5F3FF',
    borderLeftWidth: 3,
    borderLeftColor: '#6D28D9',
    padding: 10,
    borderRadius: 8,
    marginBottom: 14,
  },
  modalInfoText: {
    fontSize: 11,
    color: '#6D28D9',
    lineHeight: 15,
  },
  inputLabel: {
    fontSize: 12,
    fontWeight: '700',
    color: '#0F172A',
    marginBottom: 6,
  },
  modalInput: {
    backgroundColor: '#F8FAFC',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 13,
    color: '#0F172A',
    marginBottom: 14,
  },
  modalTextArea: {
    height: 72,
    textAlignVertical: 'top',
  },
  modalActionsRow: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 4,
  },
  modalCancelBtn: {
    flex: 1,
    backgroundColor: '#F1F5F9',
    paddingVertical: 12,
    borderRadius: 12,
    alignItems: 'center',
  },
  modalCancelBtnText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#64748B',
  },
  modalSubmitBtn: {
    flex: 2,
    backgroundColor: '#6366F1',
    paddingVertical: 12,
    borderRadius: 12,
    alignItems: 'center',
  },
  modalSubmitBtnText: {
    fontSize: 12,
    fontWeight: '700',
    color: '#FFFFFF',
  },

  // MY REQUESTS ITEMS
  emptyRequestsBox: {
    alignItems: 'center',
    paddingVertical: 24,
  },
  emptyRequestsTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: '#0F172A',
    marginBottom: 4,
  },
  emptyRequestsSubtitle: {
    fontSize: 12,
    color: '#64748B',
    textAlign: 'center',
    lineHeight: 16,
  },
  requestItemCard: {
    backgroundColor: '#F8FAFC',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    padding: 12,
    marginBottom: 10,
  },
  requestItemHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  requestItemTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: '#0F172A',
    flex: 1,
    marginRight: 8,
  },
  statusBadge: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 6,
  },
  statusBadgeText: {
    fontSize: 10,
    fontWeight: '800',
  },
  statusPending: {
    backgroundColor: '#FEF3C7',
  },
  statusPendingText: {
    color: '#B45309',
  },
  statusApproved: {
    backgroundColor: '#DCFCE7',
  },
  statusApprovedText: {
    color: '#059669',
  },
  statusRejected: {
    backgroundColor: '#FEE2E2',
  },
  statusRejectedText: {
    color: '#DC2626',
  },
  requestItemReason: {
    fontSize: 11,
    color: '#64748B',
    fontStyle: 'italic',
    marginBottom: 4,
  },
  requestItemDate: {
    fontSize: 10,
    color: '#94A3B8',
  },
  requestEnrollBtn: {
    backgroundColor: '#6366F1',
    paddingVertical: 8,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 8,
  },
  requestEnrollBtnText: {
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '700',
  },
  emptyText: {
    fontSize: 12,
    color: '#64748B',
    textAlign: 'center',
    marginVertical: 16,
  },
});
