import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  TextInput,
  Modal,
  ActivityIndicator,
  Alert,
  SafeAreaView,
  StatusBar,
  Dimensions,
} from 'react-native';
import { Colors, Shadows, Radii, Typography, Spacing } from '../../styles/theme';
import { api } from '../../services/api';

const { width } = Dimensions.get('window');

const YEAR_LIST = ['ALL', '1st Year', '2nd Year', '3rd Year', '4th Year'];
const SEM_LIST = ['ALL', '1st Sem', '2nd Sem'];
const CATEGORY_LIST = [
  { id: 'ALL', label: 'All Students', icon: '👥' },
  { id: 'SLOW_LEARNER', label: 'Slow Learners', icon: '⚠️' },
  { id: 'FAST_LEARNER', label: 'Fast Learners', icon: '⭐' },
  { id: 'CONSISTENT', label: 'Consistent', icon: '📘' },
];

interface FacultyDashboardProps {
  userData?: any;
}

export default function FacultyDashboardScreen({ userData }: FacultyDashboardProps) {
  const facultyName = userData?.username || userData?.email?.split('@')[0] || 'Department Faculty';
  const facultyEmail = userData?.email || 'faculty@siddhartha.ac.in';
  // Strict Department Isolation: locked to faculty's own department
  const facultyBranch = (userData?.branch || 'CSE').toUpperCase();

  const [selectedYear, setSelectedYear] = useState('ALL');
  const [selectedSem, setSelectedSem] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<'ALL' | 'SLOW_LEARNER' | 'FAST_LEARNER' | 'CONSISTENT'>('ALL');

  const [overview, setOverview] = useState<any>(null);
  const [students, setStudents] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  // Individual Student Inspection Modal
  const [inspectStudent, setInspectStudent] = useState<any>(null);
  const [inspectLoading, setInspectLoading] = useState(false);
  const [inspectReport, setInspectReport] = useState<any>(null);

  // Assign Recommended Course Modal State
  const [assignModalVisible, setAssignModalVisible] = useState(false);
  const [courseTitle, setCourseTitle] = useState('');
  const [courseDesc, setCourseDesc] = useState('');
  const [assignBranch, setAssignBranch] = useState(facultyBranch);
  const [assignYear, setAssignYear] = useState('2nd Year');
  const [assignSem, setAssignSem] = useState('1st Sem');
  const [assignPriority, setAssignPriority] = useState('HIGH');
  const [submittingAssign, setSubmittingAssign] = useState(false);

  const fetchFacultyData = async () => {
    try {
      setLoading(true);
      const [overviewData, studentsData] = await Promise.all([
        api.getFacultyOverview(facultyBranch),
        api.getFacultyStudents({
          branch: facultyBranch, // Enforce Department Isolation
          year: selectedYear === 'ALL' ? undefined : selectedYear,
          semester: selectedSem === 'ALL' ? undefined : selectedSem,
          search: searchQuery.trim() || undefined,
          category: selectedCategory === 'ALL' ? undefined : selectedCategory,
        }),
      ]);
      setOverview(overviewData);
      setStudents(studentsData || []);
    } catch (err: any) {
      console.warn('Faculty data load error:', err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFacultyData();
  }, [selectedYear, selectedSem, selectedCategory]);

  const handleInspectStudent = async (student: any) => {
    setInspectStudent(student);
    setInspectLoading(true);
    try {
      const report = await api.getStudentReport(student.id);
      setInspectReport(report);
    } catch {
      setInspectReport(null);
    } finally {
      setInspectLoading(false);
    }
  };

  const handleQuickAssignToCohort = (student: any) => {
    setAssignBranch(student.branch || facultyBranch);
    setAssignYear(student.year || '2nd Year');
    setAssignSem(student.semester || '1st Sem');
    setCourseTitle(`Remedial Foundations: ${student.branch || facultyBranch} Core`);
    setCourseDesc(`Targeted academic module to reinforce core fundamentals for ${student.year || '2nd Year'} - ${student.semester || '1st Sem'}.`);
    setAssignModalVisible(true);
  };

  const handlePublishRecommendation = async () => {
    if (!courseTitle.trim()) {
      Alert.alert('Missing Title', 'Please enter a course title to recommend.');
      return;
    }

    try {
      setSubmittingAssign(true);
      await api.recommendCourse({
        title: courseTitle.trim(),
        description: courseDesc.trim() || 'Mandatory departmental academic module recommended by Faculty.',
        branch: assignBranch,
        year: assignYear,
        semester: assignSem,
        facultyName,
        facultyEmail,
        priority: assignPriority,
      });

      Alert.alert(
        'Course Assigned Successfully',
        `"${courseTitle.trim()}" has been dispatched to all eligible @vrsec.ac.in students in ${assignBranch} (${assignYear} - ${assignSem}).`
      );
      setCourseTitle('');
      setCourseDesc('');
      setAssignModalVisible(false);
      fetchFacultyData();
    } catch (err: any) {
      Alert.alert('Publish Error', err.message || 'Failed to assign course.');
    } finally {
      setSubmittingAssign(false);
    }
  };

  const slowLearners = overview?.studentsNeedingSupport || [];
  const slowCount = overview?.slowLearnersCount != null ? overview.slowLearnersCount : slowLearners.length;
  const fastCount = overview?.fastLearnersCount != null ? overview.fastLearnersCount : 0;
  const consistentCount = overview?.consistentLearnersCount != null ? overview.consistentLearnersCount : 0;

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        
        {/* Faculty Portal Banner */}
        <View style={styles.bannerCard}>
          <View style={styles.bannerHeaderRow}>
            <View style={{ flex: 1 }}>
              <Text style={styles.bannerTag}>VRSEC FACULTY PORTAL</Text>
              <Text style={styles.bannerTitle}>Department of {facultyBranch} Engineering</Text>
            </View>
            <View style={styles.headerBtnGroup}>
              <TouchableOpacity
                style={styles.assignNewBtn}
                onPress={() => setAssignModalVisible(true)}
              >
                <Text style={styles.assignNewBtnText}>+ Assign Course</Text>
              </TouchableOpacity>
            </View>
          </View>
          
          {/* Department Isolation Guarantee */}
          <View style={styles.deptIsolationBadge}>
            <Text style={styles.deptIsolationText}>
              🔒 <Text style={{ fontWeight: '800' }}>DEPARTMENT ISOLATION ACTIVE:</Text> Access strictly restricted to verified <Text style={{ fontWeight: '800' }}>@{facultyBranch}</Text> (@vrsec.ac.in) students.
            </Text>
          </View>

          {/* Quick Metrics Cards */}
          <View style={styles.metricsGrid}>
            <View style={styles.metricBox}>
              <Text style={styles.metricVal}>{overview?.totalEligibleStudents || 0}</Text>
              <Text style={styles.metricLabel}>{facultyBranch} Students</Text>
            </View>
            <View style={[styles.metricBox, { borderColor: '#FECACA', backgroundColor: '#FFF5F5' }]}>
              <Text style={[styles.metricVal, { color: Colors.danger }]}>{slowCount}</Text>
              <Text style={styles.metricLabel}>⚠️ Slow Learners</Text>
            </View>
            <View style={[styles.metricBox, { borderColor: '#A7F3D0', backgroundColor: '#F0FDF4' }]}>
              <Text style={[styles.metricVal, { color: '#059669' }]}>{fastCount}</Text>
              <Text style={styles.metricLabel}>⭐ Fast Learners</Text>
            </View>
            <View style={styles.metricBox}>
              <Text style={styles.metricVal}>{consistentCount}</Text>
              <Text style={styles.metricLabel}>📘 Consistent</Text>
            </View>
          </View>
        </View>

        {/* =========================================================================
            STUDENT PROGRESS & COHORT MONITORING HUB
        ========================================================================= */}
        <View style={styles.progressHubCard}>
          <View style={styles.hubHeaderRow}>
            <View>
              <Text style={styles.hubHeaderTag}>COHORT PERFORMANCE MONITORING</Text>
              <Text style={styles.hubHeaderTitle}>📊 Student Progress Hub</Text>
            </View>
            <View style={styles.hubCountBadge}>
              <Text style={styles.hubCountText}>{students.length} filtered</Text>
            </View>
          </View>
          <Text style={styles.hubDesc}>
            Easily filter {facultyBranch} learners by academic performance category (Slow Learners, Fast Learners), Year, and Semester.
          </Text>

          {/* 1. Category Filter Tabs */}
          <Text style={styles.filterSubLabel}>Filter by Academic Category:</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.categoryPillsRow}>
            {CATEGORY_LIST.map((c) => {
              const isActive = selectedCategory === c.id;
              let badgeCount = overview?.totalEligibleStudents || 0;
              if (c.id === 'SLOW_LEARNER') badgeCount = slowCount;
              if (c.id === 'FAST_LEARNER') badgeCount = fastCount;
              if (c.id === 'CONSISTENT') badgeCount = consistentCount;

              return (
                <TouchableOpacity
                  key={c.id}
                  style={[styles.categoryPill, isActive && styles.categoryPillActive]}
                  onPress={() => setSelectedCategory(c.id as any)}
                  activeOpacity={0.8}
                >
                  <Text style={[styles.categoryPillText, isActive && styles.categoryPillTextActive]}>
                    {c.icon} {c.label} ({badgeCount})
                  </Text>
                </TouchableOpacity>
              );
            })}
          </ScrollView>

          {/* 2. Year-wise and Semester-wise Filters */}
          <View style={styles.yearSemRow}>
            {/* Year-wise */}
            <View style={{ flex: 1, marginRight: 6 }}>
              <Text style={styles.filterSubLabel}>Year-wise Filter:</Text>
              <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 6 }}>
                {YEAR_LIST.map((y) => (
                  <TouchableOpacity
                    key={y}
                    style={[styles.smallFilterChip, selectedYear === y && styles.smallFilterChipActive]}
                    onPress={() => setSelectedYear(y)}
                  >
                    <Text style={[styles.smallFilterChipText, selectedYear === y && styles.smallFilterChipTextActive]}>
                      {y}
                    </Text>
                  </TouchableOpacity>
                ))}
              </ScrollView>
            </View>

            {/* Semester-wise */}
            <View style={{ width: 140 }}>
              <Text style={styles.filterSubLabel}>Semester:</Text>
              <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 6 }}>
                {SEM_LIST.map((s) => (
                  <TouchableOpacity
                    key={s}
                    style={[styles.smallFilterChip, selectedSem === s && styles.smallFilterChipActive]}
                    onPress={() => setSelectedSem(s)}
                  >
                    <Text style={[styles.smallFilterChipText, selectedSem === s && styles.smallFilterChipTextActive]}>
                      {s}
                    </Text>
                  </TouchableOpacity>
                ))}
              </ScrollView>
            </View>
          </View>

          {/* 3. Search Bar */}
          <View style={styles.searchRow}>
            <TextInput
              style={styles.searchInput}
              placeholder={`Search ${facultyBranch} students by name or roll...`}
              placeholderTextColor={Colors.textTertiary}
              value={searchQuery}
              onChangeText={setSearchQuery}
              onSubmitEditing={fetchFacultyData}
            />
            <TouchableOpacity style={styles.searchBtn} onPress={fetchFacultyData}>
              <Text style={styles.searchBtnText}>Search</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Filtered Students List */}
        <View style={styles.section}>
          <View style={styles.sectionHeaderRow}>
            <Text style={styles.sectionTitle}>
              {selectedCategory === 'SLOW_LEARNER'
                ? `⚠️ Slow Learners Requiring Support (${students.length})`
                : selectedCategory === 'FAST_LEARNER'
                ? `⭐ High-Performing Fast Learners (${students.length})`
                : selectedCategory === 'CONSISTENT'
                ? `📘 Consistent Learners (${students.length})`
                : `All ${facultyBranch} Students (${students.length})`}
            </Text>
          </View>

          {loading ? (
            <ActivityIndicator color={Colors.primary} size="small" style={{ marginVertical: 30 }} />
          ) : students.length === 0 ? (
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyEmoji}>📋</Text>
              <Text style={styles.emptyTitle}>No Students Found</Text>
              <Text style={styles.emptyText}>
                No verified @vrsec.ac.in students match the selected category ({selectedCategory}), year ({selectedYear}), and semester ({selectedSem}).
              </Text>
            </View>
          ) : (
            students.map((s: any, idx: number) => {
              const isSlow = s.category === 'SLOW_LEARNER' || s.performanceStatus === 'Needs Attention';
              const isFast = s.category === 'FAST_LEARNER' || s.performanceStatus === 'High Achiever';
              const avgProg = Math.round(s.averageProgress || 0);
              const cgpaVal = s.cgpa != null ? Number(s.cgpa) : 0.0;

              return (
                <View
                  key={idx}
                  style={[
                    styles.studentProgressCard,
                    isSlow && styles.studentCardSlow,
                    isFast && styles.studentCardFast,
                  ]}
                >
                  <View style={styles.studentCardTop}>
                    <View style={[styles.studentAvatar, isSlow && styles.avatarSlow, isFast && styles.avatarFast]}>
                      <Text style={[styles.studentInitial, isSlow && { color: Colors.danger }, isFast && { color: '#059669' }]}>
                        {s.name?.charAt(0) || 'S'}
                      </Text>
                    </View>
                    <View style={{ flex: 1, marginLeft: 10 }}>
                      <Text style={styles.studentName}>{s.name || s.username}</Text>
                      <Text style={styles.studentEmail}>{s.email}</Text>
                    </View>
                    
                    {/* Performance Category Badge */}
                    <View style={[styles.catBadge, isSlow ? styles.catBadgeSlow : isFast ? styles.catBadgeFast : styles.catBadgeNorm]}>
                      <Text style={[styles.catBadgeText, isSlow ? styles.catTextSlow : isFast ? styles.catTextFast : styles.catTextNorm]}>
                        {isSlow ? '⚠️ Slow Learner' : isFast ? '⭐ Fast Learner' : '📘 Consistent'}
                      </Text>
                    </View>
                  </View>

                  {/* Cohort Meta & CGPA */}
                  <View style={styles.studentMetaRow}>
                    <Text style={styles.metaCohortPill}>
                      {s.year || s.academicYear || '2nd Year'} • {s.semester || '1st Sem'}
                    </Text>
                    <View style={[
                      styles.cgpaBadge,
                      cgpaVal < 6.5 ? styles.cgpaLow : (cgpaVal >= 8.0 ? styles.cgpaHigh : styles.cgpaMid)
                    ]}>
                      <Text style={[
                        styles.cgpaText,
                        cgpaVal < 6.5 ? styles.cgpaTextLow : (cgpaVal >= 8.0 ? styles.cgpaTextHigh : styles.cgpaTextMid)
                      ]}>
                        CGPA: {cgpaVal > 0 ? cgpaVal.toFixed(2) : 'N/A'}
                      </Text>
                    </View>
                    <Text style={styles.coursesCountText}>
                      {s.enrolledCoursesCount || 0} enrolled courses
                    </Text>
                  </View>

                  {/* Progress Bar */}
                  <View style={styles.progressWrap}>
                    <View style={styles.progressTextRow}>
                      <Text style={styles.progressLabel}>Average Course Progress</Text>
                      <Text style={styles.progressVal}>{avgProg}%</Text>
                    </View>
                    <View style={styles.progressBarBg}>
                      <View
                        style={[
                          styles.progressBarFill,
                          { width: `${Math.min(100, Math.max(0, avgProg))}%` },
                          isSlow ? { backgroundColor: Colors.danger } : isFast ? { backgroundColor: '#10B981' } : { backgroundColor: Colors.primary }
                        ]}
                      />
                    </View>
                  </View>

                  {/* Explicit Intervention Reason for Slow Learners */}
                  {isSlow && (
                    <View style={styles.remedialBox}>
                      <Text style={styles.remedialTitle}>⚠️ Academic Attention Alert:</Text>
                      <Text style={styles.remedialText}>
                        {s.supportReason || 'Student has recorded assessment scores <= 60% or low course progress.'}
                      </Text>
                    </View>
                  )}

                  {/* Card Action Buttons */}
                  <View style={styles.cardActionsRow}>
                    <TouchableOpacity
                      style={styles.inspectBtn}
                      onPress={() => handleInspectStudent(s)}
                    >
                      <Text style={styles.inspectBtnText}>Inspect Progress →</Text>
                    </TouchableOpacity>

                    {isSlow && (
                      <TouchableOpacity
                        style={styles.remedialActionBtn}
                        onPress={() => handleQuickAssignToCohort(s)}
                      >
                        <Text style={styles.remedialActionText}>+ Assign Remedial Module</Text>
                      </TouchableOpacity>
                    )}
                  </View>
                </View>
              );
            })
          )}
        </View>

      </ScrollView>

      {/* =========================================================================
          ASSIGN RECOMMENDED COURSE MODAL
      ========================================================================= */}
      <Modal visible={assignModalVisible} animationType="slide" transparent>
        <SafeAreaView style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Assign Recommended Course</Text>
              <TouchableOpacity onPress={() => setAssignModalVisible(false)}>
                <Text style={styles.closeBtnText}>✕ Close</Text>
              </TouchableOpacity>
            </View>

            <ScrollView showsVerticalScrollIndicator={false}>
              <Text style={styles.modalDesc}>
                Broadcast an academic course to a specific {facultyBranch} cohort. Only eligible @vrsec.ac.in students matching the department and semester will receive it.
              </Text>

              <View style={styles.inputGroup}>
                <Text style={styles.inputLabel}>Course Title</Text>
                <TextInput
                  style={styles.modalInput}
                  placeholder="e.g. Advanced Data Structures & Algorithms"
                  placeholderTextColor={Colors.textTertiary}
                  value={courseTitle}
                  onChangeText={setCourseTitle}
                />
              </View>

              <View style={styles.inputGroup}>
                <Text style={styles.inputLabel}>Target Department (Locked)</Text>
                <View style={[styles.smallPill, styles.smallPillActive, { alignSelf: 'flex-start' }]}>
                  <Text style={[styles.smallPillText, styles.smallPillTextActive]}>
                    🔒 {assignBranch} Engineering
                  </Text>
                </View>
              </View>

              <View style={styles.inputGroup}>
                <Text style={styles.inputLabel}>Target Academic Year</Text>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ gap: 8 }}>
                  {['1st Year', '2nd Year', '3rd Year', '4th Year'].map((y) => (
                    <TouchableOpacity
                      key={y}
                      style={[styles.smallPill, assignYear === y && styles.smallPillActive]}
                      onPress={() => setAssignYear(y)}
                    >
                      <Text style={[styles.smallPillText, assignYear === y && styles.smallPillTextActive]}>
                        {y}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </ScrollView>
              </View>

              <View style={styles.inputGroup}>
                <Text style={styles.inputLabel}>Target Semester</Text>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ gap: 8 }}>
                  {['1st Sem', '2nd Sem'].map((s) => (
                    <TouchableOpacity
                      key={s}
                      style={[styles.smallPill, assignSem === s && styles.smallPillActive]}
                      onPress={() => setAssignSem(s)}
                    >
                      <Text style={[styles.smallPillText, assignSem === s && styles.smallPillTextActive]}>
                        {s}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </ScrollView>
              </View>

              <View style={styles.inputGroup}>
                <Text style={styles.inputLabel}>Recommendation Description / Faculty Note</Text>
                <TextInput
                  style={[styles.modalInput, { height: 80, textAlignVertical: 'top' }]}
                  placeholder="e.g. Essential curriculum preparation for placements and semester exams."
                  placeholderTextColor={Colors.textTertiary}
                  value={courseDesc}
                  onChangeText={setCourseDesc}
                  multiline
                />
              </View>

              <TouchableOpacity
                style={styles.modalPublishBtn}
                onPress={handlePublishRecommendation}
                disabled={submittingAssign}
              >
                {submittingAssign ? (
                  <ActivityIndicator color="#FFFFFF" size="small" />
                ) : (
                  <Text style={styles.modalPublishBtnText}>Publish Recommendation →</Text>
                )}
              </TouchableOpacity>
            </ScrollView>

          </View>
        </SafeAreaView>
      </Modal>

      {/* =========================================================================
          INDIVIDUAL STUDENT REPORT MODAL
      ========================================================================= */}
      <Modal visible={!!inspectStudent} animationType="slide" transparent>
        <SafeAreaView style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            
            <View style={styles.modalHeader}>
              <View>
                <Text style={styles.modalHeaderSub}>INDIVIDUAL STUDENT AUDIT</Text>
                <Text style={styles.modalTitle}>{inspectStudent?.name || inspectStudent?.username || 'Student Profile'}</Text>
              </View>
              <TouchableOpacity onPress={() => setInspectStudent(null)}>
                <Text style={styles.closeBtnText}>✕ Close</Text>
              </TouchableOpacity>
            </View>

            {inspectLoading ? (
              <ActivityIndicator color={Colors.primary} size="large" style={{ marginVertical: 60 }} />
            ) : (
              <ScrollView showsVerticalScrollIndicator={false}>
                <View style={styles.reportSummaryBox}>
                  <Text style={styles.reportText}>Email: {inspectStudent?.email}</Text>
                  <Text style={styles.reportText}>Department: {inspectStudent?.branch} Engineering</Text>
                  <Text style={styles.reportText}>Cohort: {inspectStudent?.year || inspectStudent?.academicYear} • {inspectStudent?.semester}</Text>
                  <Text style={styles.reportText}>CGPA: {inspectStudent?.cgpa || 'N/A'}</Text>
                  <Text style={styles.reportText}>Overall Assessment Average: {inspectReport?.overallAssessmentAverage || 0}%</Text>
                </View>

                {/* Enrolled Courses Progress */}
                <Text style={styles.reportSectionTitle}>Enrolled Courses ({inspectReport?.enrolledCourses?.length || 0})</Text>
                {inspectReport?.enrolledCourses?.length === 0 ? (
                  <Text style={styles.emptyText}>No courses currently active.</Text>
                ) : (
                  inspectReport?.enrolledCourses?.map((c: any, i: number) => (
                    <View key={i} style={styles.reportCourseCard}>
                      <Text style={styles.reportCourseTitle}>{c.title}</Text>
                      <Text style={styles.reportCourseMeta}>Progress: {c.progress}% (Phase {c.currentPhase}/{c.totalPhases})</Text>
                    </View>
                  ))
                )}

                {/* Quiz Attempts History */}
                <Text style={styles.reportSectionTitle}>Recent Quiz Attempts ({inspectReport?.quizAttempts?.length || 0})</Text>
                {inspectReport?.quizAttempts?.length === 0 ? (
                  <Text style={styles.emptyText}>No quiz assessments taken yet.</Text>
                ) : (
                  inspectReport?.quizAttempts?.slice(0, 5).map((a: any, i: number) => (
                    <View key={i} style={styles.reportAttemptCard}>
                      <Text style={styles.reportAttemptTopic}>{a.topic}</Text>
                      <Text style={[styles.reportAttemptScore, a.passed ? { color: Colors.success } : { color: Colors.danger }]}>
                        {a.score}/{a.totalQuestions} ({a.percentage}% - {a.passed ? 'PASSED' : 'RETAKE NEEDED'})
                      </Text>
                    </View>
                  ))
                )}
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
    backgroundColor: Colors.background,
  },
  scroll: {
    paddingHorizontal: Spacing.lg,
    paddingTop: Spacing.md,
    paddingBottom: Spacing.section,
  },
  bannerCard: {
    backgroundColor: Colors.primaryDark,
    borderRadius: Radii.lg,
    padding: Spacing.md,
    marginBottom: Spacing.md,
    ...Shadows.small,
  },
  bannerHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 8,
  },
  bannerTag: {
    fontSize: Typography.tiny.fontSize,
    fontWeight: '800',
    color: '#93C5FD',
    letterSpacing: 0.5,
  },
  bannerTitle: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '800',
    color: '#FFFFFF',
    marginTop: 2,
  },
  headerBtnGroup: {
    flexDirection: 'row',
    gap: 6,
  },
  assignNewBtn: {
    backgroundColor: Colors.accent,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: Radii.sm,
  },
  assignNewBtnText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: Typography.tiny.fontSize,
  },
  deptIsolationBadge: {
    backgroundColor: 'rgba(15, 23, 42, 0.4)',
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: Radii.sm,
    marginBottom: 10,
    borderLeftWidth: 3,
    borderLeftColor: '#38BDF8',
  },
  deptIsolationText: {
    fontSize: Typography.tiny.fontSize,
    color: '#E2E8F0',
    lineHeight: 14,
  },
  metricsGrid: {
    flexDirection: 'row',
    gap: 6,
  },
  metricBox: {
    flex: 1,
    backgroundColor: Colors.surface,
    padding: 8,
    borderRadius: Radii.md,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  metricVal: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  metricLabel: {
    fontSize: 10,
    fontWeight: '600',
    color: Colors.textSecondary,
    marginTop: 2,
    textAlign: 'center',
  },
  progressHubCard: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.lg,
    padding: Spacing.md,
    marginBottom: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  hubHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  hubHeaderTag: {
    fontSize: Typography.tiny.fontSize,
    fontWeight: '800',
    color: Colors.primary,
    letterSpacing: 0.5,
  },
  hubHeaderTitle: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  hubCountBadge: {
    backgroundColor: '#EFF6FF',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: Radii.full,
    borderWidth: 1,
    borderColor: '#BFDBFE',
  },
  hubCountText: {
    fontSize: Typography.tiny.fontSize,
    fontWeight: '700',
    color: Colors.primaryDark,
  },
  hubDesc: {
    fontSize: Typography.caption.fontSize,
    color: Colors.textSecondary,
    marginBottom: Spacing.sm,
    lineHeight: 16,
  },
  filterSubLabel: {
    fontSize: Typography.tiny.fontSize,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginBottom: 4,
    marginTop: 4,
  },
  categoryPillsRow: {
    flexDirection: 'row',
    gap: 6,
    marginBottom: 8,
  },
  categoryPill: {
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: Radii.full,
  },
  categoryPillActive: {
    backgroundColor: Colors.primaryDark,
    borderColor: Colors.primaryDark,
  },
  categoryPillText: {
    fontSize: Typography.caption.fontSize,
    fontWeight: '700',
    color: Colors.textSecondary,
  },
  categoryPillTextActive: {
    color: '#FFFFFF',
  },
  yearSemRow: {
    flexDirection: 'row',
    marginBottom: 8,
  },
  smallFilterChip: {
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: Radii.sm,
  },
  smallFilterChipActive: {
    backgroundColor: Colors.primary,
    borderColor: Colors.primary,
  },
  smallFilterChipText: {
    fontSize: Typography.tiny.fontSize,
    fontWeight: '600',
    color: Colors.textSecondary,
  },
  smallFilterChipTextActive: {
    color: '#FFFFFF',
    fontWeight: '700',
  },
  searchRow: {
    flexDirection: 'row',
    gap: 6,
    marginTop: 4,
  },
  searchInput: {
    flex: 1,
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: Radii.md,
    paddingHorizontal: 10,
    paddingVertical: 7,
    fontSize: Typography.caption.fontSize,
    color: Colors.textPrimary,
  },
  searchBtn: {
    backgroundColor: Colors.primary,
    paddingHorizontal: 12,
    justifyContent: 'center',
    borderRadius: Radii.md,
  },
  searchBtnText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: Typography.caption.fontSize,
  },
  section: {
    marginBottom: Spacing.md,
  },
  sectionHeaderRow: {
    marginBottom: Spacing.sm,
  },
  sectionTitle: {
    fontSize: Typography.body.fontSize,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  emptyContainer: {
    backgroundColor: Colors.surface,
    padding: Spacing.lg,
    borderRadius: Radii.lg,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  emptyEmoji: {
    fontSize: 32,
    marginBottom: 6,
  },
  emptyTitle: {
    fontSize: Typography.body.fontSize,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 4,
  },
  emptyText: {
    fontSize: Typography.caption.fontSize,
    color: Colors.textSecondary,
    textAlign: 'center',
    lineHeight: 16,
  },
  studentProgressCard: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.md,
    padding: Spacing.md,
    marginBottom: Spacing.sm,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  studentCardSlow: {
    borderLeftWidth: 4,
    borderLeftColor: Colors.danger,
    backgroundColor: '#FFFDFD',
  },
  studentCardFast: {
    borderLeftWidth: 4,
    borderLeftColor: '#10B981',
    backgroundColor: '#FCFDFD',
  },
  studentCardTop: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 6,
  },
  studentAvatar: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#EFF6FF',
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarSlow: {
    backgroundColor: '#FEE2E2',
  },
  avatarFast: {
    backgroundColor: '#D1FAE5',
  },
  studentInitial: {
    fontSize: Typography.body.fontSize,
    fontWeight: '800',
    color: Colors.primaryDark,
  },
  studentName: {
    fontSize: Typography.bodySmall.fontSize,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  studentEmail: {
    fontSize: Typography.tiny.fontSize,
    color: Colors.textSecondary,
  },
  catBadge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: Radii.full,
  },
  catBadgeSlow: {
    backgroundColor: '#FEE2E2',
  },
  catBadgeFast: {
    backgroundColor: '#D1FAE5',
  },
  catBadgeNorm: {
    backgroundColor: '#EFF6FF',
  },
  catBadgeText: {
    fontSize: Typography.tiny.fontSize,
    fontWeight: '800',
  },
  catTextSlow: {
    color: Colors.danger,
  },
  catTextFast: {
    color: '#059669',
  },
  catTextNorm: {
    color: Colors.primaryDark,
  },
  studentMetaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginVertical: 4,
  },
  metaCohortPill: {
    fontSize: Typography.tiny.fontSize,
    color: Colors.textSecondary,
    fontWeight: '600',
  },
  cgpaBadge: {
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: Radii.sm,
  },
  cgpaLow: {
    backgroundColor: '#FEE2E2',
  },
  cgpaMid: {
    backgroundColor: '#FEF3C7',
  },
  cgpaHigh: {
    backgroundColor: '#D1FAE5',
  },
  cgpaText: {
    fontSize: Typography.tiny.fontSize,
    fontWeight: '800',
  },
  cgpaTextLow: {
    color: Colors.danger,
  },
  cgpaTextMid: {
    color: '#D97706',
  },
  cgpaTextHigh: {
    color: '#059669',
  },
  coursesCountText: {
    fontSize: Typography.tiny.fontSize,
    color: Colors.textTertiary,
    marginLeft: 'auto',
  },
  progressWrap: {
    marginTop: 4,
    marginBottom: 6,
  },
  progressTextRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 3,
  },
  progressLabel: {
    fontSize: Typography.tiny.fontSize,
    color: Colors.textSecondary,
  },
  progressVal: {
    fontSize: Typography.tiny.fontSize,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  progressBarBg: {
    height: 6,
    backgroundColor: Colors.surfaceMuted,
    borderRadius: 3,
    overflow: 'hidden',
  },
  progressBarFill: {
    height: '100%',
    borderRadius: 3,
  },
  remedialBox: {
    backgroundColor: '#FEF2F2',
    borderWidth: 1,
    borderColor: '#FECACA',
    borderRadius: Radii.sm,
    padding: 6,
    marginVertical: 4,
  },
  remedialTitle: {
    fontSize: Typography.tiny.fontSize,
    fontWeight: '800',
    color: Colors.danger,
    marginBottom: 2,
  },
  remedialText: {
    fontSize: Typography.tiny.fontSize,
    color: '#991B1B',
    lineHeight: 14,
  },
  cardActionsRow: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 8,
    marginTop: 6,
    borderTopWidth: 1,
    borderColor: Colors.borderLight,
    paddingTop: 6,
  },
  inspectBtn: {
    paddingVertical: 4,
    paddingHorizontal: 8,
  },
  inspectBtnText: {
    fontSize: Typography.caption.fontSize,
    fontWeight: '700',
    color: Colors.primary,
  },
  remedialActionBtn: {
    backgroundColor: '#FEE2E2',
    paddingVertical: 4,
    paddingHorizontal: 8,
    borderRadius: Radii.sm,
    borderWidth: 1,
    borderColor: '#FCA5A5',
  },
  remedialActionText: {
    fontSize: Typography.tiny.fontSize,
    fontWeight: '800',
    color: Colors.danger,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.7)',
    justifyContent: 'center',
    padding: Spacing.md,
  },
  modalContent: {
    flex: 1,
    backgroundColor: Colors.surface,
    borderRadius: Radii.lg,
    padding: Spacing.md,
    maxHeight: '90%',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.sm,
  },
  modalHeaderSub: {
    fontSize: Typography.tiny.fontSize,
    fontWeight: '700',
    color: Colors.primary,
  },
  modalTitle: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  closeBtnText: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.textSecondary,
    fontWeight: '700',
  },
  modalDesc: {
    fontSize: Typography.caption.fontSize,
    color: Colors.textSecondary,
    lineHeight: 15,
    marginBottom: Spacing.sm,
  },
  inputGroup: {
    marginBottom: Spacing.sm,
  },
  inputLabel: {
    fontSize: Typography.caption.fontSize,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginBottom: 4,
  },
  modalInput: {
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: Radii.md,
    paddingHorizontal: 10,
    paddingVertical: 8,
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.textPrimary,
  },
  smallPill: {
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: Radii.full,
    marginRight: 6,
  },
  smallPillActive: {
    backgroundColor: Colors.primaryDark,
    borderColor: Colors.primaryDark,
  },
  smallPillText: {
    fontSize: Typography.caption.fontSize,
    fontWeight: '600',
    color: Colors.textSecondary,
  },
  smallPillTextActive: {
    color: '#FFFFFF',
  },
  modalPublishBtn: {
    backgroundColor: Colors.primary,
    paddingVertical: 10,
    borderRadius: Radii.md,
    alignItems: 'center',
    marginTop: 8,
    marginBottom: Spacing.md,
  },
  modalPublishBtnText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: Typography.body.fontSize,
  },
  reportSummaryBox: {
    backgroundColor: Colors.surfaceMuted,
    padding: Spacing.sm,
    borderRadius: Radii.md,
    marginBottom: Spacing.sm,
    gap: 4,
  },
  reportText: {
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.textPrimary,
  },
  reportSectionTitle: {
    fontSize: Typography.body.fontSize,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginTop: 8,
    marginBottom: 6,
  },
  reportCourseCard: {
    backgroundColor: Colors.surface,
    padding: 8,
    borderRadius: Radii.sm,
    borderWidth: 1,
    borderColor: Colors.border,
    marginBottom: 6,
  },
  reportCourseTitle: {
    fontSize: Typography.bodySmall.fontSize,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  reportCourseMeta: {
    fontSize: Typography.caption.fontSize,
    color: Colors.textSecondary,
  },
  reportAttemptCard: {
    backgroundColor: Colors.surface,
    padding: 8,
    borderRadius: Radii.sm,
    borderWidth: 1,
    borderColor: Colors.border,
    marginBottom: 6,
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  reportAttemptTopic: {
    fontSize: Typography.bodySmall.fontSize,
    fontWeight: '600',
    color: Colors.textPrimary,
    flex: 1,
  },
  reportAttemptScore: {
    fontSize: Typography.caption.fontSize,
    fontWeight: '700',
  },
});
