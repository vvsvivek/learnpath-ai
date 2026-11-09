import React, { useState, useEffect } from 'react';
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
} from 'react-native';
import { Colors, Shadows, Radii } from '../../styles/theme';
import { api } from '../../services/api';

const { width } = Dimensions.get('window');

interface HomeScreenProps {
  userData: any;
  onNavigate: (tab: string) => void;
  onSelectSubject: (subject: string) => void;
}

export default function HomeScreen({
  userData,
  onNavigate,
  onSelectSubject,
}: HomeScreenProps) {
  const email = userData?.email || '';
  const username = userData?.username || userData?.name || 'Student';
  const branch = userData?.branch || 'CSE';
  const academicYear = userData?.academicYear || '2nd Year';
  const semester = userData?.semester || '1st Sem';
  const cgpa = userData?.cgpa !== undefined ? userData?.cgpa : 8.5;

  const [dashboardData, setDashboardData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [customSubject, setCustomSubject] = useState('');
  const [submittingCourse, setSubmittingCourse] = useState(false);

  const fetchDashboard = async () => {
    if (!email) return;
    try {
      setLoading(true);
      const data = await api.getDashboard(email);
      setDashboardData(data);
    } catch (err: any) {
      console.warn('Dashboard fetch warning:', err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
  }, [email]);

  const handleEnrollCourse = async (courseTitle: string) => {
    if (!courseTitle.trim()) return;
    try {
      setSubmittingCourse(true);
      await api.selectCourse(email, courseTitle.trim());
      onSelectSubject(courseTitle.trim());
      onNavigate('Roadmap');
    } catch (err: any) {
      Alert.alert('Course Selection', err.message || 'Opening learning path.');
      onSelectSubject(courseTitle.trim());
      onNavigate('Roadmap');
    } finally {
      setSubmittingCourse(false);
    }
  };

  const handleCustomSubjectSubmit = () => {
    if (!customSubject.trim()) {
      Alert.alert('Subject Required', 'Please enter a subject, language, or technical skill you want to learn.');
      return;
    }
    handleEnrollCourse(customSubject.trim());
    setCustomSubject('');
  };

  const activeCourses = dashboardData?.activeCourses || [];
  const recommendations = dashboardData?.recommendations || [];
  const weakTopics = dashboardData?.weakTopics || [];

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
              <Text style={[styles.pillText, { color: '#B45309' }]}>⭐ CGPA: {cgpa}</Text>
            </View>
          </View>
        </View>

        {/* Explore Custom Courses Input */}
        <View style={styles.card}>
          <Text style={styles.sectionHeader}>🔍 What would you like to learn today?</Text>
          <Text style={styles.sectionSub}>
            Enter any computer science, engineering, or technical topic to generate a full learning path:
          </Text>

          <View style={styles.searchRow}>
            <TextInput
              style={styles.searchInput}
              placeholder="e.g. Python, AI, Machine Learning, Web Dev..."
              placeholderTextColor={Colors.textTertiary}
              value={customSubject}
              onChangeText={setCustomSubject}
              onSubmitEditing={handleCustomSubjectSubmit}
            />
            <TouchableOpacity
              style={styles.searchButton}
              onPress={handleCustomSubjectSubmit}
              disabled={submittingCourse}
            >
              {submittingCourse ? (
                <ActivityIndicator color="#FFFFFF" size="small" />
              ) : (
                <Text style={styles.searchButtonText}>Generate →</Text>
              )}
            </TouchableOpacity>
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

        {/* Recommended Courses Section */}
        <View style={styles.section}>
          <View style={styles.sectionTitleRow}>
            <Text style={styles.sectionTitle}>Recommended Courses</Text>
            <Text style={styles.sectionBadge}>Personalized</Text>
          </View>
          <Text style={styles.sectionSub}>
            Dynamically curated based on your branch ({branch}), semester, and quiz mastery:
          </Text>

          {loading ? (
            <ActivityIndicator color={Colors.primary} size="small" style={{ marginVertical: 20 }} />
          ) : recommendations.length === 0 ? (
            <Text style={styles.emptyText}>No recommendations generated yet.</Text>
          ) : (
            recommendations.map((rec: any, idx: number) => {
              const isFaculty = rec.isFacultyAssigned;
              return (
                <View
                  key={idx}
                  style={[
                    styles.recommendationCard,
                    isFaculty && styles.facultyAssignedCard,
                  ]}
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

                  <Text style={styles.recDesc}>{rec.description || rec.reason}</Text>

                  <View style={styles.recMetaRow}>
                    <Text style={styles.metaLabel}>
                      Difficulty: <Text style={{ fontWeight: '700', color: Colors.textPrimary }}>{rec.difficulty || 'Intermediate'}</Text>
                    </Text>
                    <Text style={styles.metaLabel}>
                      Duration: <Text style={{ fontWeight: '700', color: Colors.textPrimary }}>{rec.duration || '4 Weeks'}</Text>
                    </Text>
                  </View>

                  <TouchableOpacity
                    style={[
                      styles.enrollButton,
                      isFaculty && styles.facultyEnrollButton,
                    ]}
                    onPress={() => handleEnrollCourse(rec.title)}
                  >
                    <Text style={styles.enrollButtonText}>Start Learning Path →</Text>
                  </TouchableOpacity>
                </View>
              );
            })
          )}
        </View>

        {/* My Enrolled Courses Section */}
        <View style={styles.section}>
          <View style={styles.sectionTitleRow}>
            <Text style={styles.sectionTitle}>My Learning ({activeCourses.length})</Text>
            <TouchableOpacity onPress={() => onNavigate('Roadmap')}>
              <Text style={styles.viewAllText}>View All Roadmaps →</Text>
            </TouchableOpacity>
          </View>

          {loading ? (
            <ActivityIndicator color={Colors.primary} size="small" style={{ marginVertical: 20 }} />
          ) : activeCourses.length === 0 ? (
            <View style={styles.emptyCard}>
              <Text style={styles.emptyIcon}>📖</Text>
              <Text style={styles.emptyTitle}>No courses enrolled yet</Text>
              <Text style={styles.emptySubtitle}>
                Select a recommended course above or type a custom topic to begin your journey.
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
                      Current Phase: {c.currentPhase || 1} of {c.totalPhases || 4}
                    </Text>
                    <Text style={styles.continueLink}>Continue Learning →</Text>
                  </View>
                </TouchableOpacity>
              );
            })
          )}
        </View>

      </ScrollView>
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
  card: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.xl,
    padding: 20,
    marginBottom: 20,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  sectionHeader: {
    fontSize: 16,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 4,
  },
  sectionSub: {
    fontSize: 13,
    color: Colors.textSecondary,
    lineHeight: 18,
    marginBottom: 16,
  },
  searchRow: {
    flexDirection: 'row',
    gap: 10,
  },
  searchInput: {
    flex: 1,
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: Radii.md,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 14,
    color: Colors.textPrimary,
  },
  searchButton: {
    backgroundColor: Colors.primary,
    paddingHorizontal: 16,
    borderRadius: Radii.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  searchButtonText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 13,
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
  section: {
    marginBottom: 24,
  },
  sectionTitleRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '800',
    color: Colors.textPrimary,
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
  enrollButton: {
    backgroundColor: Colors.primary,
    paddingVertical: 12,
    borderRadius: Radii.md,
    alignItems: 'center',
  },
  facultyEnrollButton: {
    backgroundColor: Colors.primaryDark,
  },
  enrollButtonText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 13,
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
  emptyIcon: {
    fontSize: 32,
    marginBottom: 8,
  },
  emptyTitle: {
    fontSize: 16,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 4,
  },
  emptySubtitle: {
    fontSize: 13,
    color: Colors.textSecondary,
    textAlign: 'center',
    lineHeight: 18,
  },
  emptyText: {
    fontSize: 13,
    color: Colors.textSecondary,
    fontStyle: 'italic',
  },
});
