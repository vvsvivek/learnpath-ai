import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  RefreshControl,
  ActivityIndicator,
} from 'react-native';
import {
  BarChart3,
  Award,
  CheckCircle,
  Clock,
  Target,
  Sparkles,
  TrendingUp,
  AlertTriangle,
} from 'lucide-react-native';
import { useTheme } from '../../src/hooks/useTheme';
import { useAuth } from '../../src/hooks/useAuth';
import { Badge, Card, ProgressRing } from '../../src/components/ui';
import { reportService } from '../../src/services/reportService';
import { StudentReport } from '../../src/types';
import { formatDuration } from '../../src/utils/formatters';

export default function StudentReportsScreen() {
  const { theme, isDark } = useTheme();
  const { user } = useAuth();

  const [isLoading, setIsLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [report, setReport] = useState<StudentReport | null>(null);

  const fetchReport = async () => {
    try {
      const data = await reportService.getStudentReport();
      setReport(data);
    } catch (e) {
      // Demo report data
      setReport({
        student_id: user?.id || 'std-1',
        name: user?.name || 'Vivek Varanasi',
        branch: user?.branch || 'CSE',
        academic_year: user?.academic_year || 3,
        semester: user?.semester || 5,
        cgpa: user?.cgpa || 8.92,
        total_study_minutes: 680,
        weekly_study_data: [
          { day: 'Mon', minutes: 75 },
          { day: 'Tue', minutes: 90 },
          { day: 'Wed', minutes: 120 },
          { day: 'Thu', minutes: 60 },
          { day: 'Fri', minutes: 105 },
          { day: 'Sat', minutes: 140 },
          { day: 'Sun', minutes: 90 },
        ],
        course_analytics: [
          {
            course_id: 'c-1',
            course_title: 'Deep Learning & Neural Architectures',
            progress_percentage: 65,
            completed_phases: 2,
            total_phases: 4,
            time_spent_minutes: 360,
            average_quiz_score: 88,
          },
          {
            course_id: 'c-2',
            course_title: 'Distributed Systems & Microservices',
            progress_percentage: 40,
            completed_phases: 1,
            total_phases: 4,
            time_spent_minutes: 210,
            average_quiz_score: 82,
          },
          {
            course_id: 'c-3',
            course_title: 'Advanced Data Structures & Algorithms',
            progress_percentage: 85,
            completed_phases: 3,
            total_phases: 4,
            time_spent_minutes: 420,
            average_quiz_score: 94,
          },
        ],
        quiz_analytics: {
          total_quizzes_taken: 14,
          passed_quizzes: 13,
          average_score_percentage: 87,
          highest_score: 100,
          recent_attempts: [],
        },
        topic_mastery: [
          { topic: 'Dynamic Programming & Memoization', category: 'Algorithms', score_percentage: 68, status: 'needs_work' },
          { topic: 'Convolutional Feature Maps & ResNet', category: 'Deep Learning', score_percentage: 92, status: 'mastered' },
          { topic: 'Raft Consensus & Log Replication', category: 'Distributed Systems', score_percentage: 78, status: 'improving' },
          { topic: 'Graph Theory & Dijkstra Shortest Path', category: 'Algorithms', score_percentage: 95, status: 'mastered' },
        ],
        streak_days: 14,
        recommendations: [
          'Spend 30 minutes on DP transition formulation before moving to Phase 4.',
          'Consistently scoring >90% in Computer Vision concepts—consider publishing a mini-project.',
          'Weekly study pace is currently 25% higher than peer average in Semester 5.',
        ],
      });
    } finally {
      setIsLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchReport();
  }, []);

  const onRefresh = () => {
    setRefreshing(true);
    fetchReport();
  };

  if (isLoading || !report) {
    return (
      <View style={[styles.loadingContainer, { backgroundColor: theme.colors.background }]}>
        <ActivityIndicator size="large" color={theme.colors.primary} />
      </View>
    );
  }

  const maxWeeklyMinutes = Math.max(...report.weekly_study_data.map((d) => d.minutes), 120);

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      {/* Top Header */}
      <View style={styles.header}>
        <Text style={[styles.headerTitle, { color: theme.colors.textPrimary }]}>
          Performance & Analytics
        </Text>
        <Text style={[styles.headerSubtitle, { color: theme.colors.textSecondary }]}>
          Real-time tracking of skills, retention, and consistency
        </Text>
      </View>

      <ScrollView
        contentContainerStyle={styles.scrollContent}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor={theme.colors.primary}
          />
        }
      >
        {/* Weekly Study Time Bar Chart */}
        <Card variant="elevated" style={styles.chartCard}>
          <View style={styles.cardHeaderRow}>
            <View style={styles.cardHeaderLeft}>
              <Clock size={18} color={theme.colors.primary} />
              <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>
                Study Activity (This Week)
              </Text>
            </View>
            <Text style={[styles.totalStudyLabel, { color: theme.colors.primary }]}>
              {formatDuration(report.total_study_minutes)}
            </Text>
          </View>

          {/* Bar Chart Visualization */}
          <View style={styles.barChartContainer}>
            {report.weekly_study_data.map((item, idx) => {
              const heightPct = Math.round((item.minutes / maxWeeklyMinutes) * 100);
              return (
                <View key={idx} style={styles.barColumn}>
                  <Text style={[styles.barValueText, { color: theme.colors.textTertiary }]}>
                    {item.minutes}m
                  </Text>
                  <View style={styles.barTrack}>
                    <View
                      style={[
                        styles.barFill,
                        {
                          height: `${Math.max(12, heightPct)}%`,
                          backgroundColor:
                            idx === 5 || idx === 2
                              ? theme.colors.primary
                              : theme.colors.primaryLight,
                        },
                      ]}
                    />
                  </View>
                  <Text style={[styles.barDayText, { color: theme.colors.textSecondary }]}>
                    {item.day}
                  </Text>
                </View>
              );
            })}
          </View>
        </Card>

        {/* Quiz Performance Overview */}
        <Card variant="elevated" style={styles.quizAnalyticsCard}>
          <View style={styles.cardHeaderRow}>
            <View style={styles.cardHeaderLeft}>
              <Target size={18} color={theme.colors.success} />
              <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>
                Assessment Accuracy
              </Text>
            </View>
            <Badge
              label={`${report.quiz_analytics.average_score_percentage}% Avg`}
              variant="success"
              size="sm"
            />
          </View>

          <View style={styles.quizStatsGrid}>
            <View style={styles.quizStatBox}>
              <Text style={[styles.quizStatValue, { color: theme.colors.textPrimary }]}>
                {report.quiz_analytics.total_quizzes_taken}
              </Text>
              <Text style={[styles.quizStatLabel, { color: theme.colors.textSecondary }]}>
                Quizzes Taken
              </Text>
            </View>

            <View style={styles.quizStatBox}>
              <Text style={[styles.quizStatValue, { color: theme.colors.success }]}>
                {report.quiz_analytics.passed_quizzes}
              </Text>
              <Text style={[styles.quizStatLabel, { color: theme.colors.textSecondary }]}>
                Passed
              </Text>
            </View>

            <View style={styles.quizStatBox}>
              <Text style={[styles.quizStatValue, { color: theme.colors.primary }]}>
                {report.quiz_analytics.highest_score}%
              </Text>
              <Text style={[styles.quizStatLabel, { color: theme.colors.textSecondary }]}>
                Highest Score
              </Text>
            </View>
          </View>
        </Card>

        {/* Topic Mastery Matrix */}
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: theme.colors.textPrimary }]}>
            Topic Mastery Matrix
          </Text>

          {report.topic_mastery.map((item, idx) => (
            <Card key={idx} style={styles.masteryCard}>
              <View style={styles.masteryHeader}>
                <View style={{ flex: 1 }}>
                  <Text style={[styles.masteryTopic, { color: theme.colors.textPrimary }]}>
                    {item.topic}
                  </Text>
                  <Text style={[styles.masteryCategory, { color: theme.colors.textTertiary }]}>
                    {item.category}
                  </Text>
                </View>

                <Badge
                  label={
                    item.status === 'mastered'
                      ? 'Mastered'
                      : item.status === 'improving'
                      ? 'Improving'
                      : 'Needs Work'
                  }
                  variant={
                    item.status === 'mastered'
                      ? 'success'
                      : item.status === 'improving'
                      ? 'warning'
                      : 'error'
                  }
                  size="sm"
                />
              </View>

              {/* Progress bar */}
              <View
                style={[
                  styles.masteryTrack,
                  {
                    backgroundColor: isDark
                      ? theme.colors.surfaceSubtle
                      : theme.colors.inputBackground,
                  },
                ]}
              >
                <View
                  style={[
                    styles.masteryFill,
                    {
                      width: `${item.score_percentage}%`,
                      backgroundColor:
                        item.status === 'mastered'
                          ? theme.colors.success
                          : item.status === 'improving'
                          ? theme.colors.warning
                          : theme.colors.error,
                    },
                  ]}
                />
              </View>
            </Card>
          ))}
        </View>

        {/* AI Recommendations */}
        <Card variant="elevated" style={styles.recommendationsCard}>
          <View style={styles.cardHeaderLeft}>
            <Sparkles size={18} color={theme.colors.accentAmber} />
            <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>
              AI Actionable Insights
            </Text>
          </View>

          <View style={styles.recommendationsList}>
            {report.recommendations.map((rec, rIdx) => (
              <View key={rIdx} style={styles.recItem}>
                <View
                  style={[
                    styles.recBullet,
                    { backgroundColor: theme.colors.accentAmber },
                  ]}
                />
                <Text style={[styles.recText, { color: theme.colors.textSecondary }]}>
                  {rec}
                </Text>
              </View>
            ))}
          </View>
        </Card>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  loadingContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  header: {
    paddingHorizontal: 20,
    paddingTop: 56,
    paddingBottom: 16,
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: '800',
    letterSpacing: -0.4,
  },
  headerSubtitle: {
    fontSize: 13,
    marginTop: 2,
  },
  scrollContent: {
    paddingHorizontal: 20,
    paddingBottom: 40,
  },
  chartCard: {
    padding: 18,
    marginBottom: 16,
  },
  cardHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  cardHeaderLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  cardTitle: {
    fontSize: 15,
    fontWeight: '700',
  },
  totalStudyLabel: {
    fontSize: 14,
    fontWeight: '700',
  },
  barChartContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    height: 140,
    paddingTop: 10,
  },
  barColumn: {
    flex: 1,
    alignItems: 'center',
    height: '100%',
    justifyContent: 'flex-end',
  },
  barValueText: {
    fontSize: 10,
    marginBottom: 4,
  },
  barTrack: {
    width: 20,
    height: 90,
    justifyContent: 'flex-end',
    borderRadius: 6,
    overflow: 'hidden',
  },
  barFill: {
    width: '100%',
    borderRadius: 6,
  },
  barDayText: {
    fontSize: 11,
    marginTop: 6,
    fontWeight: '600',
  },
  quizAnalyticsCard: {
    padding: 18,
    marginBottom: 20,
  },
  quizStatsGrid: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginTop: 8,
  },
  quizStatBox: {
    alignItems: 'center',
  },
  quizStatValue: {
    fontSize: 22,
    fontWeight: '800',
    marginBottom: 2,
  },
  quizStatLabel: {
    fontSize: 12,
  },
  section: {
    marginBottom: 20,
  },
  sectionTitle: {
    fontSize: 17,
    fontWeight: '700',
    marginBottom: 12,
  },
  masteryCard: {
    padding: 14,
    marginBottom: 10,
  },
  masteryHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  masteryTopic: {
    fontSize: 14,
    fontWeight: '600',
  },
  masteryCategory: {
    fontSize: 11,
    marginTop: 2,
  },
  masteryTrack: {
    height: 6,
    borderRadius: 3,
    overflow: 'hidden',
  },
  masteryFill: {
    height: '100%',
    borderRadius: 3,
  },
  recommendationsCard: {
    padding: 18,
  },
  recommendationsList: {
    marginTop: 12,
    gap: 10,
  },
  recItem: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 8,
  },
  recBullet: {
    width: 6,
    height: 6,
    borderRadius: 3,
    marginTop: 6,
  },
  recText: {
    fontSize: 13,
    lineHeight: 18,
    flex: 1,
  },
});
