import React, { useState, useEffect, useMemo } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  SafeAreaView,
  TouchableOpacity,
  TextInput,
  StatusBar,
  ActivityIndicator,
  Alert,
  Dimensions,
  Modal,
} from 'react-native';
import { Colors, Shadows, Radii } from '../../styles/theme';
import { api } from '../../services/api';

const { width } = Dimensions.get('window');

interface HomeScreenProps {
  userData: any;
  onNavigate: (tab: string) => void;
  onSelectSubject: (subject: string) => void;
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

export default function HomeScreen({
  userData,
  onNavigate,
  onSelectSubject,
}: HomeScreenProps) {
  const email = userData?.email || '';
  const username = userData?.username || userData?.name || 'Student';

  // Dashboard & Profile state
  const [dashboardData, setDashboardData] = useState<any>(null);
  const [loadingDashboard, setLoadingDashboard] = useState(true);

  // Course Catalogue state
  const [catalogue, setCatalogue] = useState<ApprovedCourseItem[]>([]);
  const [loadingCatalogue, setLoadingCatalogue] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [catalogueScope, setCatalogueScope] = useState<'eligible' | 'all'>('eligible');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');

  // Enrolling state
  const [enrollingCourseTitle, setEnrollingCourseTitle] = useState<string | null>(null);

  // Request a Course Modal state
  const [showRequestModal, setShowRequestModal] = useState(false);
  const [requestTitle, setRequestTitle] = useState('');
  const [requestReason, setRequestReason] = useState('');
  const [submittingRequest, setSubmittingRequest] = useState(false);

  // My Requests Modal state
  const [showMyRequestsModal, setShowMyRequestsModal] = useState(false);
  const [myRequests, setMyRequests] = useState<CourseRequestItem[]>([]);
  const [loadingMyRequests, setLoadingMyRequests] = useState(false);

  const branch = dashboardData?.profile?.branch || userData?.branch || 'CSE';
  const academicYear = dashboardData?.profile?.academicYear || userData?.academicYear || '2nd Year';
  const semester = dashboardData?.profile?.semester || userData?.semester || '1st Sem';
  const rawCgpa = dashboardData?.profile?.cgpa ?? dashboardData?.cgpa ?? userData?.cgpa;
  const displayCgpa =
    rawCgpa != null && !isNaN(Number(rawCgpa)) && Number(rawCgpa) > 0
      ? Number(rawCgpa).toFixed(2)
      : 'Not Set';

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

  // Fetch Approved Catalogue
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

  // Fetch My Course Requests
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
    fetchDashboard();
  }, [email]);

  useEffect(() => {
    fetchCatalogue();
  }, [email, branch, academicYear, semester, catalogueScope]);

  // Handle Enrollment
  const handleEnrollCourse = async (courseTitle: string, approvedCourseId?: number) => {
    if (!courseTitle.trim()) return;
    try {
      setEnrollingCourseTitle(courseTitle.trim());
      await api.selectCourse(email, courseTitle.trim(), approvedCourseId);
      // Refresh dashboard to show enrolled course
      await fetchDashboard();
      onSelectSubject(courseTitle.trim());
      onNavigate('Roadmap');
    } catch (err: any) {
      Alert.alert('Course Selection Error', err.message || 'Unable to enroll in course.');
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
      const res = await api.requestCourse(email, trimmedTitle, requestReason.trim());
      Alert.alert(
        'Request Submitted',
        'Your course request has been submitted for faculty review with status PENDING. Learning paths are only generated once approved.'
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

  const activeCourses = dashboardData?.activeCourses || [];
  const recommendations = dashboardData?.recommendations || [];
  const weakTopics = dashboardData?.weakTopics || [];

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

  // Helper to check if student is already enrolled in a catalogue course
  const isAlreadyEnrolled = (courseTitle: string) => {
    return activeCourses.some(
      (ac: any) => ac.title?.trim().toLowerCase() === courseTitle.trim().toLowerCase()
    );
  };

  const getEnrolledCourseInfo = (courseTitle: string) => {
    return activeCourses.find(
      (ac: any) => ac.title?.trim().toLowerCase() === courseTitle.trim().toLowerCase()
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        
        {/* Top Header Card */}
        <View style={styles.headerCard}>
          <View style={styles.headerTopRow}>
            <View>
              <Text style={styles.headerGreeting}>Welcome back 👋</Text>
              <Text style={styles.headerName}>{username}</Text>
            </View>
            <View style={styles.avatarBadge}>
              <Text style={styles.avatarInitial}>
                {username.charAt(0).toUpperCase()}
              </Text>
            </View>
          </View>

          {/* Academic Profile Badges */}
          <View style={styles.badgeRow}>
            <View style={styles.pillBadge}>
              <Text style={styles.pillText}>🎓 {branch}</Text>
            </View>
            <View style={styles.pillBadge}>
              <Text style={styles.pillText}>📚 {academicYear} • {semester}</Text>
            </View>
            <View style={[styles.pillBadge, { backgroundColor: '#FEF3C7' }]}>
              <Text style={[styles.pillText, { color: '#B45309' }]}>⭐ CGPA: {displayCgpa}</Text>
            </View>
          </View>
        </View>

        {/* Adaptive Remedial Booster Banner (if weak topics exist) */}
        {weakTopics.length > 0 && (
          <View style={styles.remedialCard}>
            <View style={styles.remedialHeader}>
              <Text style={styles.remedialTitle}>💡 Adaptive Learning Focus</Text>
              <Text style={styles.remedialTag}>Needs Attention</Text>
            </View>
            <Text style={styles.remedialText}>
              Quiz analytics indicate you struggled with <Text style={{ fontWeight: '800' }}>{weakTopics[0].conceptTag}</Text> in {weakTopics[0].courseTitle}.
            </Text>
            <TouchableOpacity
              style={styles.remedialBtn}
              onPress={() => onNavigate('Chat')}
            >
              <Text style={styles.remedialBtnText}>Ask AI Tutor to Explain {weakTopics[0].conceptTag} →</Text>
            </TouchableOpacity>
          </View>
        )}

        {/* PRIORITY 1: APPROVED COURSE CATALOGUE */}
        <View style={styles.catalogueSection}>
          <View style={styles.sectionHeaderRow}>
            <View style={{ flex: 1 }}>
              <View style={styles.catalogueTitleRow}>
                <Text style={styles.sectionTitle}>Course Catalogue</Text>
                <View style={styles.approvedBadge}>
                  <Text style={styles.approvedBadgeText}>APPROVED</Text>
                </View>
              </View>
              <Text style={styles.catalogueSubtitle}>
                Select an approved engineering course. Verified subjects are aligned with university autonomous regulations; synthesized tracks follow AICTE model guidelines.
              </Text>
            </View>
            <TouchableOpacity
              style={styles.requestCourseBtn}
              onPress={() => {
                setRequestTitle(searchQuery.trim());
                setShowRequestModal(true);
              }}
            >
              <Text style={styles.requestCourseBtnText}>+ Request Course</Text>
            </TouchableOpacity>
          </View>

          {/* Search Bar */}
          <View style={styles.searchContainer}>
            <Text style={styles.searchIcon}>🔍</Text>
            <TextInput
              style={styles.searchInput}
              placeholder="Search approved courses by title, code, branch..."
              placeholderTextColor={Colors.textTertiary}
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
            >
              <Text style={[styles.scopeTabText, catalogueScope === 'eligible' && styles.scopeTabTextActive]}>
                🎯 Eligible for Me ({branch})
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.scopeTab, catalogueScope === 'all' && styles.scopeTabActive]}
              onPress={() => setCatalogueScope('all')}
            >
              <Text style={[styles.scopeTabText, catalogueScope === 'all' && styles.scopeTabTextActive]}>
                🌐 All Approved Courses
              </Text>
            </TouchableOpacity>
          </View>

          {/* Category Filter Pills */}
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.categoryPillsRow}>
            {['ALL', 'CORE', 'FOUNDATION', 'ELECTIVE'].map((cat) => (
              <TouchableOpacity
                key={cat}
                style={[styles.categoryPill, selectedCategory === cat && styles.categoryPillActive]}
                onPress={() => setSelectedCategory(cat)}
              >
                <Text style={[styles.categoryPillText, selectedCategory === cat && styles.categoryPillTextActive]}>
                  {cat === 'ALL' ? 'All Types' : cat}
                </Text>
              </TouchableOpacity>
            ))}
            <TouchableOpacity
              style={styles.myRequestsPill}
              onPress={() => {
                fetchMyRequests();
                setShowMyRequestsModal(true);
              }}
            >
              <Text style={styles.myRequestsPillText}>📋 My Requests</Text>
            </TouchableOpacity>
          </ScrollView>

          {/* Catalogue List */}
          {loadingCatalogue ? (
            <View style={styles.loaderContainer}>
              <ActivityIndicator color={Colors.primary} size="small" />
              <Text style={styles.loaderText}>Loading approved courses...</Text>
            </View>
          ) : filteredCatalogue.length === 0 ? (
            <View style={styles.emptyCatalogueBox}>
              <Text style={styles.emptyIcon}>🔍</Text>
              <Text style={styles.emptyTitle}>
                {searchQuery ? `No approved courses match "${searchQuery}"` : 'No courses found'}
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
              >
                <Text style={styles.emptyRequestBtnText}>
                  + Request "{searchQuery || 'New Course'}" for Faculty Review
                </Text>
              </TouchableOpacity>
            </View>
          ) : (
            filteredCatalogue.map((c) => {
              const enrolled = isAlreadyEnrolled(c.title);
              const enrolledInfo = enrolled ? getEnrolledCourseInfo(c.title) : null;
              const isEnrolling = enrollingCourseTitle === c.title;

              return (
                <View key={c.id} style={styles.catalogueCard}>
                  {/* Card Top Row: Code, Category, Eligibility */}
                  <View style={styles.cardHeaderRow}>
                    <View style={styles.codeBadge}>
                      <Text style={styles.codeBadgeText}>{c.code || 'ENG'}</Text>
                    </View>
                    <View
                      style={[
                        styles.catBadge,
                        c.category?.toUpperCase() === 'FOUNDATION'
                          ? styles.catBadgeFoundation
                          : c.category?.toUpperCase() === 'ELECTIVE'
                          ? styles.catBadgeElective
                          : styles.catBadgeCore,
                      ]}
                    >
                      <Text
                        style={[
                          styles.catBadgeText,
                          c.category?.toUpperCase() === 'FOUNDATION'
                            ? styles.catBadgeTextFoundation
                            : c.category?.toUpperCase() === 'ELECTIVE'
                            ? styles.catBadgeTextElective
                            : styles.catBadgeTextCore,
                        ]}
                      >
                        {c.category?.toUpperCase() || 'CORE'}
                      </Text>
                    </View>
                    {c.branch?.toUpperCase() === 'ALL' || c.branch?.toUpperCase() === 'SHARED' ? (
                      <View style={styles.sharedBadge}>
                        <Text style={styles.sharedBadgeText}>🌐 All Branches</Text>
                      </View>
                    ) : (
                      <View style={styles.branchBadge}>
                        <Text style={styles.branchBadgeText}>🎯 {c.branch}</Text>
                      </View>
                    )}

                    {c.isSyllabusVerified ? (
                      <View style={styles.syllabusVerifiedBadge}>
                        <Text style={styles.syllabusVerifiedBadgeText}>📜 Syllabus Aligned</Text>
                      </View>
                    ) : (
                      <View style={styles.domainSynthesizedBadge}>
                        <Text style={styles.domainSynthesizedBadgeText}>🤖 AICTE Synthesized</Text>
                      </View>
                    )}
                  </View>

                  {/* Title & Description */}
                  <Text style={styles.courseTitle}>{c.title}</Text>
                  {!!c.description && <Text style={styles.courseDescription}>{c.description}</Text>}

                  {/* Meta Details */}
                  <View style={styles.courseMetaRow}>
                    <Text style={styles.metaItemText}>
                      ⚡ <Text style={{ fontWeight: '700' }}>{c.difficulty || 'Intermediate'}</Text>
                    </Text>
                    <Text style={styles.metaDot}>•</Text>
                    <Text style={styles.metaItemText}>
                      ⏱️ <Text style={{ fontWeight: '700' }}>{c.duration || '6 Weeks'}</Text>
                    </Text>
                    <Text style={styles.metaDot}>•</Text>
                    <Text style={styles.metaItemText}>
                      📚 {c.academicYear} ({c.semester})
                    </Text>
                  </View>

                  {/* Action Button */}
                  {enrolled ? (
                    <TouchableOpacity
                      style={styles.continueButton}
                      onPress={() => {
                        onSelectSubject(c.title);
                        onNavigate('Roadmap');
                      }}
                    >
                      <Text style={styles.continueButtonText}>
                        Continue Roadmap (Phase {enrolledInfo?.currentPhase || 1} of {enrolledInfo?.totalPhases || 6}) →
                      </Text>
                    </TouchableOpacity>
                  ) : (
                    <TouchableOpacity
                      style={styles.enrollButton}
                      onPress={() => handleEnrollCourse(c.title, c.id)}
                      disabled={isEnrolling}
                    >
                      {isEnrolling ? (
                        <ActivityIndicator color="#FFFFFF" size="small" />
                      ) : (
                        <Text style={styles.enrollButtonText}>Enroll & Generate Roadmap →</Text>
                      )}
                    </TouchableOpacity>
                  )}
                </View>
              );
            })
          )}
        </View>

        {/* Recommended Courses Section (Preserved & Enhanced) */}
        {recommendations.length > 0 && (
          <View style={styles.section}>
            <View style={styles.sectionTitleRow}>
              <Text style={styles.sectionTitle}>Recommended for You</Text>
              <Text style={styles.sectionBadge}>Personalized</Text>
            </View>
            <Text style={styles.sectionSub}>
              Dynamically curated based on your branch ({branch}), semester, and quiz performance:
            </Text>

            {recommendations.map((rec: any, idx: number) => {
              const isFaculty = rec.isFacultyAssigned;
              const isEnrolling = enrollingCourseTitle === rec.title;
              return (
                <View
                  key={idx}
                  style={[styles.recommendationCard, isFaculty && styles.facultyAssignedCard]}
                >
                  <View style={styles.recHeaderRow}>
                    <Text style={styles.recTitle}>{rec.title}</Text>
                    {isFaculty ? (
                      <View style={styles.facultyBadge}>
                        <Text style={styles.facultyBadgeText}>🏛️ FACULTY ASSIGNED</Text>
                      </View>
                    ) : (
                      <View style={styles.aiBadge}>
                        <Text style={styles.aiBadgeText}>AI CURATED</Text>
                      </View>
                    )}
                  </View>

                  {!!rec.reason && (
                    <View style={styles.recReasonBox}>
                      <Text style={styles.recReasonText}>💡 {rec.reason}</Text>
                    </View>
                  )}

                  {!!rec.description && <Text style={styles.recDesc}>{rec.description}</Text>}

                  <View style={styles.recMetaRow}>
                    <Text style={styles.metaLabel}>
                      Difficulty: <Text style={{ fontWeight: '700', color: Colors.textPrimary }}>{rec.difficulty || 'Intermediate'}</Text>
                    </Text>
                    <Text style={styles.metaLabel}>
                      Duration: <Text style={{ fontWeight: '700', color: Colors.textPrimary }}>{rec.duration || '6 Weeks'}</Text>
                    </Text>
                  </View>

                  <TouchableOpacity
                    style={[styles.enrollButton, isFaculty && styles.facultyEnrollButton]}
                    onPress={() => handleEnrollCourse(rec.title)}
                    disabled={isEnrolling}
                  >
                    {isEnrolling ? (
                      <ActivityIndicator color="#FFFFFF" size="small" />
                    ) : (
                      <Text style={styles.enrollButtonText}>Start Learning Path →</Text>
                    )}
                  </TouchableOpacity>
                </View>
              );
            })}
          </View>
        )}

        {/* My Enrolled Courses Section */}
        <View style={styles.section}>
          <View style={styles.sectionTitleRow}>
            <Text style={styles.sectionTitle}>My Learning ({activeCourses.length})</Text>
            <TouchableOpacity onPress={() => onNavigate('Roadmap')}>
              <Text style={styles.viewAllText}>View All Roadmaps →</Text>
            </TouchableOpacity>
          </View>

          {loadingDashboard ? (
            <ActivityIndicator color={Colors.primary} size="small" style={{ marginVertical: 20 }} />
          ) : activeCourses.length === 0 ? (
            <View style={styles.emptyCard}>
              <Text style={styles.emptyIcon}>📖</Text>
              <Text style={styles.emptyTitle}>No courses enrolled yet</Text>
              <Text style={styles.emptySubtitle}>
                Select an approved course from the catalogue above to begin your structured learning path.
              </Text>
            </View>
          ) : (
            activeCourses.map((c: any, idx: number) => {
              const progressPct = c.progress !== undefined ? c.progress : 0;
              return (
                <TouchableOpacity
                  key={idx}
                  style={styles.courseItemCard}
                  onPress={() => {
                    onSelectSubject(c.title);
                    onNavigate('Roadmap');
                  }}
                  activeOpacity={0.88}
                >
                  <View style={styles.courseItemHeader}>
                    <Text style={styles.courseItemTitle}>{c.title}</Text>
                    <Text style={styles.progressPctText}>{progressPct}%</Text>
                  </View>

                  {/* Progress Bar */}
                  <View style={styles.progressBarTrack}>
                    <View style={[styles.progressBarFill, { width: `${Math.min(progressPct, 100)}%` }]} />
                  </View>

                  <View style={styles.courseItemFooter}>
                    <Text style={styles.phaseStatusText}>
                      Current Phase: {c.currentPhase || 1} of {c.totalPhases || 6}
                    </Text>
                    <Text style={styles.continueLink}>Continue Learning →</Text>
                  </View>
                </TouchableOpacity>
              );
            })
          )}
        </View>

      </ScrollView>

      {/* REQUEST A COURSE MODAL */}
      <Modal
        visible={showRequestModal}
        transparent={true}
        animationType="slide"
        onRequestClose={() => setShowRequestModal(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeaderRow}>
              <View>
                <Text style={styles.modalTitle}>Request a New Course</Text>
                <Text style={styles.modalSubtitle}>Submit to department faculty for curriculum review</Text>
              </View>
              <TouchableOpacity onPress={() => setShowRequestModal(false)} style={styles.modalCloseBtn}>
                <Text style={styles.modalCloseText}>✕</Text>
              </TouchableOpacity>
            </View>

            {/* Information Banner */}
            <View style={styles.modalInfoBanner}>
              <Text style={styles.modalInfoText}>
                ℹ️ Submitted requests are saved as <Text style={{ fontWeight: '800' }}>PENDING</Text>. Learning paths are generated only after faculty approval.
              </Text>
            </View>

            {/* Form Fields */}
            <Text style={styles.inputLabel}>Course Title *</Text>
            <TextInput
              style={styles.modalInput}
              placeholder="e.g. Advanced Operating Systems, Cloud Computing"
              placeholderTextColor={Colors.textTertiary}
              value={requestTitle}
              onChangeText={setRequestTitle}
            />

            <Text style={styles.inputLabel}>Academic Justification / Reason</Text>
            <TextInput
              style={[styles.modalInput, styles.modalTextArea]}
              placeholder="Why would you like this course added? (e.g., Gate preparation, specialization, departmental elective)"
              placeholderTextColor={Colors.textTertiary}
              value={requestReason}
              onChangeText={setRequestReason}
              multiline
              numberOfLines={3}
            />

            {/* Context Pills */}
            <View style={styles.modalContextRow}>
              <Text style={styles.modalContextText}>Student: {username}</Text>
              <Text style={styles.modalContextDot}>•</Text>
              <Text style={styles.modalContextText}>{branch} • {academicYear}</Text>
            </View>

            {/* Modal Actions */}
            <View style={styles.modalActionsRow}>
              <TouchableOpacity
                style={styles.modalCancelBtn}
                onPress={() => setShowRequestModal(false)}
                disabled={submittingRequest}
              >
                <Text style={styles.modalCancelText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.modalSubmitBtn}
                onPress={handleSubmitCourseRequest}
                disabled={submittingRequest}
              >
                {submittingRequest ? (
                  <ActivityIndicator color="#FFFFFF" size="small" />
                ) : (
                  <Text style={styles.modalSubmitText}>Submit Request</Text>
                )}
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* MY COURSE REQUESTS MODAL */}
      <Modal
        visible={showMyRequestsModal}
        transparent={true}
        animationType="slide"
        onRequestClose={() => setShowMyRequestsModal(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeaderRow}>
              <View>
                <Text style={styles.modalTitle}>My Course Requests</Text>
                <Text style={styles.modalSubtitle}>Track the status of your requested subjects</Text>
              </View>
              <TouchableOpacity onPress={() => setShowMyRequestsModal(false)} style={styles.modalCloseBtn}>
                <Text style={styles.modalCloseText}>✕</Text>
              </TouchableOpacity>
            </View>

            {loadingMyRequests ? (
              <ActivityIndicator color={Colors.primary} size="small" style={{ marginVertical: 30 }} />
            ) : myRequests.length === 0 ? (
              <View style={styles.emptyRequestsBox}>
                <Text style={styles.emptyRequestsIcon}>📋</Text>
                <Text style={styles.emptyRequestsTitle}>No requests submitted yet</Text>
                <Text style={styles.emptyRequestsSubtitle}>
                  If you need a subject not found in the approved catalogue, you can submit a request anytime.
                </Text>
              </View>
            ) : (
              <ScrollView style={{ maxHeight: 360 }} showsVerticalScrollIndicator={false}>
                {myRequests.map((req) => (
                  <View key={req.id} style={styles.requestItemCard}>
                    <View style={styles.requestItemHeader}>
                      <Text style={styles.requestItemTitle}>{req.suggestedTitle}</Text>
                      <View
                        style={[
                          styles.statusBadge,
                          req.status === 'APPROVED'
                            ? styles.statusBadgeApproved
                            : req.status === 'REJECTED'
                            ? styles.statusBadgeRejected
                            : styles.statusBadgePending,
                        ]}
                      >
                        <Text
                          style={[
                            styles.statusBadgeText,
                            req.status === 'APPROVED'
                              ? styles.statusBadgeTextApproved
                              : req.status === 'REJECTED'
                              ? styles.statusBadgeTextRejected
                              : styles.statusBadgeTextPending,
                          ]}
                        >
                          {req.status}
                        </Text>
                      </View>
                    </View>
                    {!!req.reason && <Text style={styles.requestItemReason}>"{req.reason}"</Text>}
                    <Text style={styles.requestItemFooter}>
                      {req.status === 'PENDING'
                        ? '⏳ Under review by department faculty'
                        : req.status === 'APPROVED'
                        ? '✅ Approved! Available in catalogue.'
                        : '❌ Declined by department faculty.'}
                    </Text>
                    {req.status === 'APPROVED' && (
                      <TouchableOpacity
                        style={styles.requestEnrollBtn}
                        onPress={() => {
                          setShowMyRequestsModal(false);
                          handleEnrollCourse(req.suggestedTitle);
                        }}
                      >
                        <Text style={styles.requestEnrollBtnText}>Enroll Now →</Text>
                      </TouchableOpacity>
                    )}
                  </View>
                ))}
              </ScrollView>
            )}

            <TouchableOpacity
              style={styles.modalCloseFullBtn}
              onPress={() => setShowMyRequestsModal(false)}
            >
              <Text style={styles.modalCloseFullText}>Close</Text>
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
  scroll: {
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 110,
  },
  headerCard: {
    backgroundColor: Colors.primaryDark,
    borderRadius: Radii.xl,
    padding: 22,
    marginBottom: 20,
    ...Shadows.medium,
  },
  headerTopRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  headerGreeting: {
    fontSize: 13,
    color: '#93C5FD',
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  headerName: {
    fontSize: 24,
    fontWeight: '900',
    color: '#FFFFFF',
    letterSpacing: -0.3,
  },
  avatarBadge: {
    width: 48,
    height: 48,
    borderRadius: Radii.full,
    backgroundColor: Colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarInitial: {
    color: '#FFFFFF',
    fontSize: 20,
    fontWeight: '900',
  },
  badgeRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  pillBadge: {
    backgroundColor: 'rgba(255, 255, 255, 0.15)',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: Radii.full,
  },
  pillText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '700',
  },
  remedialCard: {
    backgroundColor: Colors.dangerLight,
    borderWidth: 1,
    borderColor: '#FECACA',
    borderRadius: Radii.lg,
    padding: 16,
    marginBottom: 20,
  },
  remedialHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  remedialTitle: {
    fontSize: 14,
    fontWeight: '800',
    color: Colors.danger,
  },
  remedialTag: {
    backgroundColor: '#FEE2E2',
    color: Colors.danger,
    fontSize: 10,
    fontWeight: '800',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: Radii.full,
  },
  remedialText: {
    fontSize: 13,
    color: '#7F1D1D',
    lineHeight: 18,
    marginBottom: 10,
  },
  remedialBtn: {
    alignSelf: 'flex-start',
  },
  remedialBtnText: {
    fontSize: 13,
    fontWeight: '800',
    color: Colors.danger,
  },
  // Catalogue Section
  catalogueSection: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.xl,
    padding: 20,
    marginBottom: 24,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  sectionHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 14,
    gap: 10,
  },
  catalogueTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 4,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '900',
    color: Colors.textPrimary,
  },
  approvedBadge: {
    backgroundColor: '#DCFCE7',
    paddingHorizontal: 7,
    paddingVertical: 2,
    borderRadius: Radii.full,
  },
  approvedBadgeText: {
    fontSize: 9,
    fontWeight: '900',
    color: '#15803D',
    letterSpacing: 0.5,
  },
  syllabusVerifiedBadge: {
    backgroundColor: '#FEF3C7',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: Radii.sm,
    borderWidth: 1,
    borderColor: '#FDE68A',
  },
  syllabusVerifiedBadgeText: {
    fontSize: 10,
    fontWeight: '700',
    color: '#92400E',
  },
  domainSynthesizedBadge: {
    backgroundColor: '#F3F4F6',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: Radii.sm,
    borderWidth: 1,
    borderColor: '#E5E7EB',
  },
  domainSynthesizedBadgeText: {
    fontSize: 10,
    fontWeight: '600',
    color: '#4B5563',
  },
  catalogueSubtitle: {
    fontSize: 12,
    color: Colors.textSecondary,
    lineHeight: 16,
  },
  requestCourseBtn: {
    backgroundColor: '#F1F5F9',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: Radii.md,
    borderWidth: 1,
    borderColor: '#CBD5E1',
  },
  requestCourseBtnText: {
    fontSize: 12,
    fontWeight: '800',
    color: Colors.primaryDark,
  },
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: Radii.md,
    paddingHorizontal: 12,
    marginBottom: 12,
  },
  searchIcon: {
    fontSize: 14,
    marginRight: 8,
  },
  searchInput: {
    flex: 1,
    paddingVertical: 10,
    fontSize: 13,
    color: Colors.textPrimary,
  },
  clearSearchBtn: {
    padding: 6,
  },
  clearSearchText: {
    color: Colors.textTertiary,
    fontSize: 13,
    fontWeight: '700',
  },
  scopeTabsRow: {
    flexDirection: 'row',
    backgroundColor: Colors.surfaceMuted,
    borderRadius: Radii.md,
    padding: 3,
    marginBottom: 12,
  },
  scopeTab: {
    flex: 1,
    paddingVertical: 8,
    alignItems: 'center',
    borderRadius: Radii.sm,
  },
  scopeTabActive: {
    backgroundColor: Colors.surface,
    ...Shadows.small,
  },
  scopeTabText: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.textSecondary,
  },
  scopeTabTextActive: {
    color: Colors.primary,
    fontWeight: '800',
  },
  categoryPillsRow: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 16,
  },
  categoryPill: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: Radii.full,
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  categoryPillActive: {
    backgroundColor: Colors.primaryLight,
    borderColor: Colors.primary,
  },
  categoryPillText: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.textSecondary,
  },
  categoryPillTextActive: {
    color: Colors.primaryDark,
    fontWeight: '800',
  },
  myRequestsPill: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: Radii.full,
    backgroundColor: '#FEF3C7',
    borderWidth: 1,
    borderColor: '#FDE68A',
  },
  myRequestsPillText: {
    fontSize: 11,
    fontWeight: '800',
    color: '#B45309',
  },
  loaderContainer: {
    paddingVertical: 24,
    alignItems: 'center',
  },
  loaderText: {
    marginTop: 8,
    fontSize: 12,
    color: Colors.textSecondary,
  },
  emptyCatalogueBox: {
    backgroundColor: Colors.surfaceMuted,
    borderRadius: Radii.lg,
    padding: 22,
    alignItems: 'center',
    marginVertical: 8,
  },
  emptyIcon: {
    fontSize: 28,
    marginBottom: 6,
  },
  emptyTitle: {
    fontSize: 15,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 4,
    textAlign: 'center',
  },
  emptySubtitle: {
    fontSize: 12,
    color: Colors.textSecondary,
    textAlign: 'center',
    lineHeight: 16,
    marginBottom: 14,
  },
  emptyRequestBtn: {
    backgroundColor: Colors.primary,
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: Radii.md,
  },
  emptyRequestBtnText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 12,
  },
  // Catalogue Card
  catalogueCard: {
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: Radii.lg,
    padding: 16,
    marginBottom: 12,
    ...Shadows.small,
  },
  cardHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 8,
  },
  codeBadge: {
    backgroundColor: '#F1F5F9',
    paddingHorizontal: 7,
    paddingVertical: 3,
    borderRadius: Radii.sm,
  },
  codeBadgeText: {
    fontSize: 11,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  catBadge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: Radii.sm,
  },
  catBadgeText: {
    fontSize: 10,
    fontWeight: '800',
  },
  catBadgeCore: {
    backgroundColor: '#EFF6FF',
  },
  catBadgeTextCore: {
    color: Colors.primary,
    fontSize: 10,
    fontWeight: '800',
  },
  catBadgeFoundation: {
    backgroundColor: '#ECFDF5',
  },
  catBadgeTextFoundation: {
    color: '#059669',
    fontSize: 10,
    fontWeight: '800',
  },
  catBadgeElective: {
    backgroundColor: '#F3E8FF',
  },
  catBadgeTextElective: {
    color: '#7C3AED',
    fontSize: 10,
    fontWeight: '800',
  },
  sharedBadge: {
    backgroundColor: '#FEF3C7',
    paddingHorizontal: 7,
    paddingVertical: 3,
    borderRadius: Radii.sm,
  },
  sharedBadgeText: {
    color: '#B45309',
    fontSize: 10,
    fontWeight: '800',
  },
  branchBadge: {
    backgroundColor: '#F1F5F9',
    paddingHorizontal: 7,
    paddingVertical: 3,
    borderRadius: Radii.sm,
  },
  branchBadgeText: {
    color: Colors.textSecondary,
    fontSize: 10,
    fontWeight: '700',
  },
  courseTitle: {
    fontSize: 15,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 4,
  },
  courseDescription: {
    fontSize: 12,
    color: Colors.textSecondary,
    lineHeight: 16,
    marginBottom: 10,
  },
  courseMetaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
    gap: 6,
  },
  metaItemText: {
    fontSize: 11,
    color: Colors.textSecondary,
  },
  metaDot: {
    fontSize: 11,
    color: Colors.border,
  },
  enrollButton: {
    backgroundColor: Colors.primary,
    paddingVertical: 10,
    borderRadius: Radii.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  enrollButtonText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 13,
  },
  continueButton: {
    backgroundColor: '#F8FAFC',
    borderWidth: 1.5,
    borderColor: Colors.primary,
    paddingVertical: 10,
    borderRadius: Radii.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  continueButtonText: {
    color: Colors.primaryDark,
    fontWeight: '800',
    fontSize: 13,
  },
  // Section Styles
  section: {
    marginBottom: 24,
  },
  sectionTitleRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  sectionBadge: {
    fontSize: 11,
    fontWeight: '800',
    color: Colors.primary,
    backgroundColor: Colors.primaryLight,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: Radii.full,
  },
  sectionSub: {
    fontSize: 13,
    color: Colors.textSecondary,
    lineHeight: 18,
    marginBottom: 16,
  },
  viewAllText: {
    fontSize: 13,
    color: Colors.primary,
    fontWeight: '700',
  },
  recommendationCard: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.lg,
    padding: 18,
    marginBottom: 14,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  facultyAssignedCard: {
    borderColor: Colors.primary,
    borderWidth: 1.5,
    backgroundColor: Colors.primarySubtle,
  },
  recHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 8,
  },
  recTitle: {
    flex: 1,
    fontSize: 16,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginRight: 8,
  },
  facultyBadge: {
    backgroundColor: '#FEF3C7',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: Radii.sm,
  },
  facultyBadgeText: {
    color: '#B45309',
    fontSize: 10,
    fontWeight: '900',
  },
  aiBadge: {
    backgroundColor: Colors.primaryLight,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: Radii.sm,
  },
  aiBadgeText: {
    color: Colors.primaryDark,
    fontSize: 10,
    fontWeight: '800',
  },
  recReasonBox: {
    backgroundColor: '#EFF6FF',
    borderRadius: Radii.sm,
    paddingHorizontal: 10,
    paddingVertical: 6,
    marginBottom: 10,
    borderLeftWidth: 3,
    borderLeftColor: Colors.primary,
  },
  recReasonText: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.primaryDark,
    lineHeight: 16,
  },
  recDesc: {
    fontSize: 13,
    color: Colors.textSecondary,
    lineHeight: 18,
    marginBottom: 12,
  },
  recMetaRow: {
    flexDirection: 'row',
    gap: 16,
    marginBottom: 14,
  },
  metaLabel: {
    fontSize: 12,
    color: Colors.textSecondary,
  },
  facultyEnrollButton: {
    backgroundColor: Colors.primaryDark,
  },
  courseItemCard: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.lg,
    padding: 18,
    marginBottom: 14,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  courseItemHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  courseItemTitle: {
    fontSize: 15,
    fontWeight: '800',
    color: Colors.textPrimary,
    flex: 1,
    marginRight: 10,
  },
  progressPctText: {
    fontSize: 15,
    fontWeight: '900',
    color: Colors.primary,
  },
  progressBarTrack: {
    height: 6,
    backgroundColor: Colors.surfaceMuted,
    borderRadius: Radii.full,
    overflow: 'hidden',
    marginBottom: 12,
  },
  progressBarFill: {
    height: '100%',
    backgroundColor: Colors.primary,
    borderRadius: Radii.full,
  },
  courseItemFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  phaseStatusText: {
    fontSize: 12,
    color: Colors.textSecondary,
    fontWeight: '600',
  },
  continueLink: {
    fontSize: 13,
    color: Colors.primary,
    fontWeight: '800',
  },
  emptyCard: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.lg,
    padding: 28,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  // Modal Styles
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.55)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  modalContent: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.xl,
    padding: 22,
    width: '100%',
    maxWidth: 480,
    ...Shadows.elevated,
  },
  modalHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 14,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '900',
    color: Colors.textPrimary,
  },
  modalSubtitle: {
    fontSize: 12,
    color: Colors.textSecondary,
    marginTop: 2,
  },
  modalCloseBtn: {
    padding: 4,
  },
  modalCloseText: {
    fontSize: 16,
    color: Colors.textTertiary,
    fontWeight: '700',
  },
  modalInfoBanner: {
    backgroundColor: '#EFF6FF',
    borderLeftWidth: 3,
    borderLeftColor: Colors.primary,
    padding: 10,
    borderRadius: Radii.sm,
    marginBottom: 16,
  },
  modalInfoText: {
    fontSize: 12,
    color: Colors.primaryDark,
    lineHeight: 16,
  },
  inputLabel: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginBottom: 6,
  },
  modalInput: {
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: Radii.md,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 13,
    color: Colors.textPrimary,
    marginBottom: 14,
  },
  modalTextArea: {
    height: 72,
    textAlignVertical: 'top',
  },
  modalContextRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 18,
  },
  modalContextText: {
    fontSize: 11,
    color: Colors.textSecondary,
    fontWeight: '600',
  },
  modalContextDot: {
    fontSize: 11,
    color: Colors.border,
  },
  modalActionsRow: {
    flexDirection: 'row',
    gap: 10,
  },
  modalCancelBtn: {
    flex: 1,
    backgroundColor: Colors.surfaceMuted,
    paddingVertical: 12,
    borderRadius: Radii.md,
    alignItems: 'center',
  },
  modalCancelText: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.textSecondary,
  },
  modalSubmitBtn: {
    flex: 2,
    backgroundColor: Colors.primary,
    paddingVertical: 12,
    borderRadius: Radii.md,
    alignItems: 'center',
  },
  modalSubmitText: {
    fontSize: 13,
    fontWeight: '800',
    color: '#FFFFFF',
  },
  modalCloseFullBtn: {
    backgroundColor: Colors.surfaceMuted,
    paddingVertical: 12,
    borderRadius: Radii.md,
    alignItems: 'center',
    marginTop: 16,
  },
  modalCloseFullText: {
    fontSize: 13,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  // My Requests List
  emptyRequestsBox: {
    paddingVertical: 28,
    alignItems: 'center',
  },
  emptyRequestsIcon: {
    fontSize: 28,
    marginBottom: 6,
  },
  emptyRequestsTitle: {
    fontSize: 14,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 4,
  },
  emptyRequestsSubtitle: {
    fontSize: 12,
    color: Colors.textSecondary,
    textAlign: 'center',
    lineHeight: 16,
  },
  requestItemCard: {
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: Radii.md,
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
    fontWeight: '800',
    color: Colors.textPrimary,
    flex: 1,
    marginRight: 8,
  },
  statusBadge: {
    paddingHorizontal: 7,
    paddingVertical: 2,
    borderRadius: Radii.sm,
  },
  statusBadgeText: {
    fontSize: 9,
    fontWeight: '900',
  },
  statusBadgePending: {
    backgroundColor: '#FEF3C7',
  },
  statusBadgeTextPending: {
    color: '#B45309',
    fontSize: 9,
    fontWeight: '900',
  },
  statusBadgeApproved: {
    backgroundColor: '#ECFDF5',
  },
  statusBadgeTextApproved: {
    color: '#059669',
    fontSize: 9,
    fontWeight: '900',
  },
  statusBadgeRejected: {
    backgroundColor: '#FEE2E2',
  },
  statusBadgeTextRejected: {
    color: '#DC2626',
    fontSize: 9,
    fontWeight: '900',
  },
  requestItemReason: {
    fontSize: 11,
    color: Colors.textSecondary,
    fontStyle: 'italic',
    marginBottom: 4,
  },
  requestItemFooter: {
    fontSize: 11,
    color: Colors.textTertiary,
    fontWeight: '600',
  },
  requestEnrollBtn: {
    marginTop: 8,
    backgroundColor: Colors.primary,
    paddingVertical: 6,
    borderRadius: Radii.sm,
    alignItems: 'center',
  },
  requestEnrollBtnText: {
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '800',
  },
});
