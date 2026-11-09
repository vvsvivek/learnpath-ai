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
import { Colors, Shadows, Radii } from '../../styles/theme';
import { api } from '../../services/api';

const { width } = Dimensions.get('window');

const BRANCH_LIST = ['ALL', 'CSE', 'IT', 'ECE', 'EEE', 'MECH', 'CIVIL'];
const YEAR_LIST = ['ALL', '1st Year', '2nd Year', '3rd Year', '4th Year'];
const SEM_LIST = ['ALL', '1st Sem', '2nd Sem'];

interface FacultyDashboardProps {
  userData?: any;
}

export default function FacultyDashboardScreen({ userData }: FacultyDashboardProps) {
  const facultyName = userData?.username || userData?.email?.split('@')[0] || 'Department Faculty';
  const facultyEmail = userData?.email || 'faculty@siddhartha.ac.in';
  const facultyBranch = userData?.branch || 'CSE';

  const [selectedBranch, setSelectedBranch] = useState(facultyBranch);
  const [selectedYear, setSelectedYear] = useState('ALL');
  const [selectedSem, setSelectedSem] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'NEEDS_ATTENTION'>('ALL');

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
        api.getFacultyOverview(selectedBranch),
        api.getFacultyStudents({
          branch: selectedBranch === 'ALL' ? undefined : selectedBranch,
          year: selectedYear === 'ALL' ? undefined : selectedYear,
          semester: selectedSem === 'ALL' ? undefined : selectedSem,
          search: searchQuery.trim() || undefined,
          status: statusFilter === 'NEEDS_ATTENTION' ? 'NEEDS_ATTENTION' : undefined,
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
  }, [selectedBranch, selectedYear, selectedSem, statusFilter]);

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

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        
        {/* Faculty Portal Banner */}
        <View style={styles.bannerCard}>
          <View style={styles.bannerHeaderRow}>
            <View>
              <Text style={styles.bannerTag}>VRSEC FACULTY PORTAL</Text>
              <Text style={styles.bannerTitle}>Department Analytics Hub</Text>
            </View>
            <TouchableOpacity
              style={styles.assignNewBtn}
              onPress={() => setAssignModalVisible(true)}
            >
              <Text style={styles.assignNewBtnText}>+ Assign Course</Text>
            </TouchableOpacity>
          </View>
          <Text style={styles.bannerDesc}>
            Monitoring verified <Text style={{ fontWeight: '800' }}>@vrsec.ac.in</Text> learners. Faculty accounts are strictly excluded from student analytics.
          </Text>

          {/* Quick Metrics Cards */}
          <View style={styles.metricsGrid}>
            <View style={styles.metricBox}>
              <Text style={styles.metricVal}>{overview?.totalEligibleStudents || 0}</Text>
              <Text style={styles.metricLabel}>Eligible Students</Text>
            </View>
            <View style={styles.metricBox}>
              <Text style={styles.metricVal}>{overview?.activeLearners || 0}</Text>
              <Text style={styles.metricLabel}>Active Courses</Text>
            </View>
            <View style={styles.metricBox}>
              <Text style={styles.metricVal}>{overview?.completedCourses || 0}</Text>
              <Text style={styles.metricLabel}>Completed</Text>
            </View>
            <View style={[styles.metricBox, { borderColor: Colors.danger }]}>
              <Text style={[styles.metricVal, { color: Colors.danger }]}>{overview?.studentsNeedingSupportCount || 0}</Text>
              <Text style={styles.metricLabel}>Needs Support</Text>
            </View>
          </View>
        </View>

        {/* Slow Learners / Students Requiring Additional Academic Support */}
        {slowLearners.length > 0 && (
          <View style={styles.supportCard}>
            <View style={styles.supportHeader}>
              <Text style={styles.supportTitle}>⚠️ Students Requiring Additional Support ({slowLearners.length})</Text>
              <Text style={styles.supportTag}>Performance Intervention</Text>
            </View>
            <Text style={styles.supportDesc}>
              These @vrsec.ac.in learners have recorded repeated quiz scores &lt;= 60% or have CGPA under 6.5 benchmark:
            </Text>

            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.supportScroll}>
              {slowLearners.map((s: any, idx: number) => (
                <TouchableOpacity
                  key={idx}
                  style={styles.supportStudentCard}
                  onPress={() => handleInspectStudent(s)}
                  activeOpacity={0.8}
                >
                  <Text style={styles.supportStudentName}>{s.username}</Text>
                  <Text style={styles.supportStudentEmail}>{s.email}</Text>
                  <View style={styles.supportPills}>
                    <Text style={styles.supportBranch}>{s.branch} • {s.academicYear}</Text>
                    <Text style={styles.supportCgpa}>CGPA: {s.cgpa}</Text>
                  </View>
                  <Text style={styles.supportReason}>⚠️ {s.reason}</Text>
                  <Text style={styles.inspectLink}>Inspect Progress →</Text>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>
        )}

        {/* Filter Controls Bar */}
        <View style={styles.filterSection}>
          <Text style={styles.filterTitle}>Filter Registered @vrsec.ac.in Students</Text>
          
          {/* Branch Pill Selector */}
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.pillRow}>
            {BRANCH_LIST.map((b) => (
              <TouchableOpacity
                key={b}
                style={[styles.filterPill, selectedBranch === b && styles.filterPillActive]}
                onPress={() => setSelectedBranch(b)}
              >
                <Text style={[styles.filterPillText, selectedBranch === b && styles.filterPillTextActive]}>
                  {b}
                </Text>
              </TouchableOpacity>
            ))}
          </ScrollView>

          {/* Search Input */}
          <View style={styles.searchRow}>
            <TextInput
              style={styles.searchInput}
              placeholder="Search by student name or roll number/email..."
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

        {/* Students List */}
        <View style={styles.section}>
          <View style={styles.sectionHeaderRow}>
            <Text style={styles.sectionTitle}>
              Eligible Students List ({students.length})
            </Text>
            <TouchableOpacity
              style={[
                styles.statusToggle,
                statusFilter === 'NEEDS_ATTENTION' && styles.statusToggleActive,
              ]}
              onPress={() =>
                setStatusFilter((prev) => (prev === 'ALL' ? 'NEEDS_ATTENTION' : 'ALL'))
              }
            >
              <Text
                style={[
                  styles.statusToggleText,
                  statusFilter === 'NEEDS_ATTENTION' && styles.statusToggleTextActive,
                ]}
              >
                {statusFilter === 'NEEDS_ATTENTION' ? 'Filter: Needs Attention ✓' : 'Filter: All Students'}
              </Text>
            </TouchableOpacity>
          </View>

          {loading ? (
            <ActivityIndicator color={Colors.primary} size="small" style={{ marginVertical: 30 }} />
          ) : students.length === 0 ? (
            <Text style={styles.emptyText}>No verified @vrsec.ac.in students found matching criteria.</Text>
          ) : (
            students.map((s: any, idx: number) => {
              const needsAttn = s.performanceStatus === 'Needs Attention';
              return (
                <TouchableOpacity
                  key={idx}
                  style={[styles.studentCard, needsAttn && styles.studentCardNeedsAttn]}
                  onPress={() => handleInspectStudent(s)}
                  activeOpacity={0.88}
                >
                  <View style={styles.studentCardTop}>
                    <View style={styles.studentAvatar}>
                      <Text style={styles.studentInitial}>{s.name?.charAt(0) || 'S'}</Text>
                    </View>
                    <View style={{ flex: 1, marginLeft: 12 }}>
                      <Text style={styles.studentName}>{s.name}</Text>
                      <Text style={styles.studentEmail}>{s.email}</Text>
                    </View>
                    <View style={[styles.statusBadge, needsAttn ? styles.badgeAttn : styles.badgeGood]}>
                      <Text style={[styles.statusBadgeText, needsAttn ? styles.textAttn : styles.textGood]}>
                        {s.performanceStatus}
                      </Text>
                    </View>
                  </View>

                  <View style={styles.studentCardBottom}>
                    <Text style={styles.studentMeta}>
                      Branch: <Text style={{ fontWeight: '700', color: Colors.textPrimary }}>{s.branch}</Text> • {s.year} ({s.semester})
                    </Text>
                    <Text style={styles.studentMeta}>
                      CGPA: <Text style={{ fontWeight: '800', color: Colors.primaryDark }}>{s.cgpa || 0.0}</Text>
                    </Text>
                  </View>
                </TouchableOpacity>
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
                Broadcast an academic course to a specific student cohort. Only eligible @vrsec.ac.in students matching the selected branch, year, and semester will receive it.
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
                <Text style={styles.inputLabel}>Target Branch</Text>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ gap: 8 }}>
                  {['CSE', 'IT', 'ECE', 'EEE', 'MECH', 'CIVIL'].map((b) => (
                    <TouchableOpacity
                      key={b}
                      style={[styles.smallPill, assignBranch === b && styles.smallPillActive]}
                      onPress={() => setAssignBranch(b)}
                    >
                      <Text style={[styles.smallPillText, assignBranch === b && styles.smallPillTextActive]}>
                        {b}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </ScrollView>
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
                <Text style={styles.modalTitle}>{inspectStudent?.name || 'Student Profile'}</Text>
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
                  <Text style={styles.reportText}>Cohort: {inspectStudent?.year} • {inspectStudent?.semester}</Text>
                  <Text style={styles.reportText}>CGPA: {inspectStudent?.cgpa}</Text>
                  <Text style={styles.reportText}>Overall Average: {inspectReport?.overallAssessmentAverage || 0}%</Text>
                </View>

                {/* Enrolled Courses Progress */}
                <Text style={styles.reportSectionTitle}>Enrolled Courses ({inspectReport?.enrolledCourses?.length || 0})</Text>
                {inspectReport?.enrolledCourses?.map((c: any, i: number) => (
                  <View key={i} style={styles.reportCourseCard}>
                    <Text style={styles.reportCourseTitle}>{c.title}</Text>
                    <Text style={styles.reportCourseMeta}>Progress: {c.progress}% (Phase {c.currentPhase}/{c.totalPhases})</Text>
                  </View>
                ))}

                {/* Quiz Attempts History */}
                <Text style={styles.reportSectionTitle}>Recent Quiz Attempts ({inspectReport?.quizAttempts?.length || 0})</Text>
                {inspectReport?.quizAttempts?.slice(0, 5).map((a: any, i: number) => (
                  <View key={i} style={styles.reportAttemptCard}>
                    <Text style={styles.reportAttemptTopic}>{a.topic}</Text>
                    <Text style={[styles.reportAttemptScore, a.passed ? { color: Colors.success } : { color: Colors.danger }]}>
                      {a.score}/{a.totalQuestions} ({a.percentage}% - {a.passed ? 'PASSED' : 'LOCKED'})
                    </Text>
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
    backgroundColor: Colors.background,
  },
  scroll: {
    paddingHorizontal: 18,
    paddingTop: 16,
    paddingBottom: 110,
  },
  bannerCard: {
    backgroundColor: Colors.primaryDark,
    borderRadius: Radii.xl,
    padding: 20,
    marginBottom: 20,
    ...Shadows.medium,
  },
  bannerHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  bannerTag: {
    fontSize: 11,
    fontWeight: '800',
    color: '#93C5FD',
    letterSpacing: 0.8,
  },
  bannerTitle: {
    fontSize: 20,
    fontWeight: '900',
    color: '#FFFFFF',
  },
  assignNewBtn: {
    backgroundColor: Colors.primary,
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: Radii.md,
  },
  assignNewBtnText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 12,
  },
  bannerDesc: {
    fontSize: 12,
    color: '#CBD5E1',
    lineHeight: 17,
    marginBottom: 16,
  },
  metricsGrid: {
    flexDirection: 'row',
    gap: 8,
  },
  metricBox: {
    flex: 1,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    borderRadius: Radii.md,
    padding: 10,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.15)',
  },
  metricVal: {
    fontSize: 18,
    fontWeight: '900',
    color: '#FFFFFF',
    marginBottom: 2,
  },
  metricLabel: {
    fontSize: 10,
    color: '#E2E8F0',
    textAlign: 'center',
    fontWeight: '600',
  },
  supportCard: {
    backgroundColor: Colors.dangerLight,
    borderWidth: 1.5,
    borderColor: '#FECACA',
    borderRadius: Radii.lg,
    padding: 16,
    marginBottom: 20,
  },
  supportHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  supportTitle: {
    fontSize: 14,
    fontWeight: '800',
    color: Colors.danger,
    flex: 1,
  },
  supportTag: {
    backgroundColor: '#FEE2E2',
    color: Colors.danger,
    fontSize: 10,
    fontWeight: '800',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: Radii.full,
  },
  supportDesc: {
    fontSize: 12,
    color: '#7F1D1D',
    lineHeight: 16,
    marginBottom: 12,
  },
  supportScroll: {
    gap: 10,
  },
  supportStudentCard: {
    width: 200,
    backgroundColor: '#FFFFFF',
    borderRadius: Radii.md,
    padding: 12,
    borderWidth: 1,
    borderColor: '#FCA5A5',
  },
  supportStudentName: {
    fontSize: 14,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 2,
  },
  supportStudentEmail: {
    fontSize: 11,
    color: Colors.textSecondary,
    marginBottom: 6,
  },
  supportPills: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 6,
  },
  supportBranch: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.primaryDark,
  },
  supportCgpa: {
    fontSize: 11,
    fontWeight: '800',
    color: Colors.danger,
  },
  supportReason: {
    fontSize: 11,
    color: Colors.danger,
    lineHeight: 15,
    marginBottom: 8,
  },
  inspectLink: {
    fontSize: 12,
    fontWeight: '800',
    color: Colors.primary,
  },
  filterSection: {
    marginBottom: 16,
  },
  filterTitle: {
    fontSize: 13,
    fontWeight: '800',
    color: Colors.textSecondary,
    marginBottom: 8,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  pillRow: {
    flexDirection: 'row',
    marginBottom: 12,
  },
  filterPill: {
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: Radii.full,
    marginRight: 8,
  },
  filterPillActive: {
    backgroundColor: Colors.primaryDark,
    borderColor: Colors.primaryDark,
  },
  filterPillText: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.textSecondary,
  },
  filterPillTextActive: {
    color: '#FFFFFF',
  },
  searchRow: {
    flexDirection: 'row',
    gap: 8,
  },
  searchInput: {
    flex: 1,
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: Radii.md,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 13,
    color: Colors.textPrimary,
  },
  searchBtn: {
    backgroundColor: Colors.primary,
    paddingHorizontal: 14,
    borderRadius: Radii.md,
    justifyContent: 'center',
  },
  searchBtnText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 12,
  },
  section: {
    marginBottom: 20,
  },
  sectionHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  statusToggle: {
    backgroundColor: Colors.surfaceMuted,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: Radii.full,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  statusToggleActive: {
    backgroundColor: Colors.dangerLight,
    borderColor: Colors.danger,
  },
  statusToggleText: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.textSecondary,
  },
  statusToggleTextActive: {
    color: Colors.danger,
    fontWeight: '800',
  },
  studentCard: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.lg,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  studentCardNeedsAttn: {
    borderColor: '#FCA5A5',
    backgroundColor: '#FFFBFB',
  },
  studentCardTop: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 10,
  },
  studentAvatar: {
    width: 38,
    height: 38,
    borderRadius: Radii.full,
    backgroundColor: Colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
  },
  studentInitial: {
    fontSize: 16,
    fontWeight: '800',
    color: Colors.primaryDark,
  },
  studentName: {
    fontSize: 15,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  studentEmail: {
    fontSize: 12,
    color: Colors.textSecondary,
  },
  statusBadge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: Radii.full,
  },
  badgeGood: {
    backgroundColor: Colors.successLight,
  },
  badgeAttn: {
    backgroundColor: Colors.dangerLight,
  },
  statusBadgeText: {
    fontSize: 10,
    fontWeight: '800',
  },
  textGood: {
    color: Colors.success,
  },
  textAttn: {
    color: Colors.danger,
  },
  studentCardBottom: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    borderTopWidth: 1,
    borderColor: Colors.borderLight,
    paddingTop: 8,
  },
  studentMeta: {
    fontSize: 12,
    color: Colors.textSecondary,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.7)',
    justifyContent: 'center',
    padding: 16,
  },
  modalContent: {
    flex: 1,
    backgroundColor: Colors.surface,
    borderRadius: Radii.xl,
    padding: 20,
    maxHeight: '90%',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  modalHeaderSub: {
    fontSize: 10,
    fontWeight: '800',
    color: Colors.primary,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '900',
    color: Colors.textPrimary,
  },
  closeBtnText: {
    fontSize: 14,
    color: Colors.textSecondary,
    fontWeight: '700',
  },
  modalDesc: {
    fontSize: 12,
    color: Colors.textSecondary,
    lineHeight: 17,
    marginBottom: 16,
  },
  inputGroup: {
    marginBottom: 14,
  },
  inputLabel: {
    fontSize: 12,
    fontWeight: '800',
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
    fontSize: 14,
    color: Colors.textPrimary,
  },
  smallPill: {
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: Radii.full,
    marginRight: 6,
  },
  smallPillActive: {
    backgroundColor: Colors.primaryDark,
    borderColor: Colors.primaryDark,
  },
  smallPillText: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.textSecondary,
  },
  smallPillTextActive: {
    color: '#FFFFFF',
  },
  modalPublishBtn: {
    backgroundColor: Colors.primary,
    paddingVertical: 14,
    borderRadius: Radii.md,
    alignItems: 'center',
    marginTop: 10,
    marginBottom: 20,
  },
  modalPublishBtnText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 14,
  },
  reportSummaryBox: {
    backgroundColor: Colors.surfaceMuted,
    padding: 14,
    borderRadius: Radii.md,
    marginBottom: 16,
    gap: 4,
  },
  reportText: {
    fontSize: 13,
    color: Colors.textPrimary,
  },
  reportSectionTitle: {
    fontSize: 14,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginTop: 10,
    marginBottom: 8,
  },
  reportCourseCard: {
    backgroundColor: Colors.surface,
    padding: 10,
    borderRadius: Radii.sm,
    borderWidth: 1,
    borderColor: Colors.border,
    marginBottom: 8,
  },
  reportCourseTitle: {
    fontSize: 13,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  reportCourseMeta: {
    fontSize: 11,
    color: Colors.textSecondary,
  },
  reportAttemptCard: {
    backgroundColor: Colors.surface,
    padding: 10,
    borderRadius: Radii.sm,
    borderWidth: 1,
    borderColor: Colors.border,
    marginBottom: 8,
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  reportAttemptTopic: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.textPrimary,
    flex: 1,
  },
  reportAttemptScore: {
    fontSize: 12,
    fontWeight: '800',
  },
  emptyText: {
    fontSize: 13,
    color: Colors.textSecondary,
    fontStyle: 'italic',
    textAlign: 'center',
    marginVertical: 20,
  },
});
