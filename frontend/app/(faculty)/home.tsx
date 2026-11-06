import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  RefreshControl,
  Pressable,
} from 'react-native';
import { useRouter } from 'expo-router';
import {
  Users,
  BookOpen,
  TrendingUp,
  Award,
  Plus,
  ArrowRight,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  GraduationCap,
} from 'lucide-react-native';
import { useTheme } from '../../src/hooks/useTheme';
import { useAuth } from '../../src/hooks/useAuth';
import { Avatar, Badge, Button, Card, Skeleton } from '../../src/components/ui';
import { facultyService } from '../../src/services/facultyService';
import { FacultyReport } from '../../src/types';

export default function FacultyHomeScreen() {
  const router = useRouter();
  const { theme, isDark } = useTheme();
  const { user } = useAuth();

  const [isLoading, setIsLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [report, setReport] = useState<FacultyReport | null>(null);

  const fetchData = async () => {
    try {
      const data = await facultyService.getDashboard();
      setReport(data);
    } catch (e) {
      // Mock faculty report
      setReport({
        total_students: 184,
        active_students_count: 168,
        total_assigned_courses: 5,
        average_class_cgpa: 8.42,
        average_quiz_score: 83.8,
        category_distribution: {
          high_performing: 72,
          average: 88,
          needs_support: 24,
        },
        branch_performance: [
          { branch: 'CSE', student_count: 94, avg_score: 86.4 },
          { branch: 'IT', student_count: 48, avg_score: 82.1 },
          { branch: 'AIML', student_count: 42, avg_score: 84.8 },
        ],
        weekly_engagement: [
          { week: 'W1', active_count: 140 },
          { week: 'W2', active_count: 155 },
          { week: 'W3', active_count: 168 },
        ],
        top_struggling_topics: [
          { topic: 'Dynamic Programming Formulations', fail_rate_percentage: 36 },
          { topic: 'Distributed Consensus & Raft Protocol', fail_rate_percentage: 28 },
          { topic: 'Convolution Kernel Dimension Calculations', fail_rate_percentage: 22 },
        ],
      });
    } finally {
      setIsLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const onRefresh = () => {
    setRefreshing(true);
    fetchData();
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
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
        {/* Header / Welcome Section */}
        <View style={styles.header}>
          <View style={styles.headerLeft}>
            <Text style={[styles.welcomeGreeting, { color: theme.colors.textSecondary }]}>
              Faculty Administration
            </Text>
            <Text style={[styles.facultyName, { color: theme.colors.textPrimary }]}>
              {user?.name || 'Prof. Ramesh Kumar'}
            </Text>
            <Text style={[styles.deptText, { color: theme.colors.primary }]}>
              Department of Computer Science & Engineering
            </Text>
          </View>

          <Pressable onPress={() => router.push('/(faculty)/profile')}>
            <Avatar
              name={user?.name || 'Faculty Member'}
              imageUrl={user?.avatar_url}
              size="md"
            />
          </Pressable>
        </View>

        {/* Quick Actions Bar */}
        <View style={styles.actionRow}>
          <Button
            title="Assign Course"
            variant="primary"
            size="md"
            leftIcon={<Plus size={16} color="#FFFFFF" />}
            onPress={() => router.push('/(faculty)/students/assign')}
            style={{ flex: 1 }}
          />
          <Button
            title="Student Directory"
            variant="outline"
            size="md"
            leftIcon={<Users size={16} color={theme.colors.primary} />}
            onPress={() => router.push('/(faculty)/students/index')}
            style={{ flex: 1 }}
          />
        </View>

        {/* Metrics Grid */}
        <View style={styles.statsGrid}>
          {/* Total Students */}
          <Card variant="elevated" style={styles.statCard}>
            <View
              style={[
                styles.statIconBox,
                { backgroundColor: isDark ? 'rgba(74, 144, 217, 0.15)' : '#EBF4FC' },
              ]}
            >
              <Users size={20} color={theme.colors.primary} />
            </View>
            <Text style={[styles.statValue, { color: theme.colors.textPrimary }]}>
              {report?.total_students || 184}
            </Text>
            <Text style={[styles.statLabel, { color: theme.colors.textSecondary }]}>
              Total Students
            </Text>
          </Card>

          {/* Active This Week */}
          <Card variant="elevated" style={styles.statCard}>
            <View
              style={[
                styles.statIconBox,
                { backgroundColor: isDark ? 'rgba(16, 185, 129, 0.15)' : '#ECFDF5' },
              ]}
            >
              <CheckCircle2 size={20} color={theme.colors.success} />
            </View>
            <Text style={[styles.statValue, { color: theme.colors.success }]}>
              {report?.active_students_count || 168}
            </Text>
            <Text style={[styles.statLabel, { color: theme.colors.textSecondary }]}>
              Active This Week
            </Text>
          </Card>

          {/* Assigned Courses */}
          <Card variant="elevated" style={styles.statCard}>
            <View
              style={[
                styles.statIconBox,
                { backgroundColor: isDark ? 'rgba(59, 130, 246, 0.15)' : '#EFF6FF' },
              ]}
            >
              <BookOpen size={20} color={theme.colors.info} />
            </View>
            <Text style={[styles.statValue, { color: theme.colors.textPrimary }]}>
              {report?.total_assigned_courses || 5}
            </Text>
            <Text style={[styles.statLabel, { color: theme.colors.textSecondary }]}>
              Assigned Courses
            </Text>
          </Card>

          {/* Avg Quiz Score */}
          <Card variant="elevated" style={styles.statCard}>
            <View
              style={[
                styles.statIconBox,
                { backgroundColor: isDark ? 'rgba(245, 158, 11, 0.15)' : '#FFFBEB' },
              ]}
            >
              <Award size={20} color={theme.colors.warning} />
            </View>
            <Text style={[styles.statValue, { color: theme.colors.textPrimary }]}>
              {report?.average_quiz_score || 83.8}%
            </Text>
            <Text style={[styles.statLabel, { color: theme.colors.textSecondary }]}>
              Average Mastery
            </Text>
          </Card>
        </View>

        {/* Student Category Distribution */}
        <Card variant="elevated" style={styles.distributionCard}>
          <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>
            Cohort Mastery Distribution
          </Text>

          <View style={styles.distBarContainer}>
            <View
              style={[
                styles.distSegment,
                {
                  flex: report?.category_distribution.high_performing || 72,
                  backgroundColor: theme.colors.success,
                  borderTopLeftRadius: 6,
                  borderBottomLeftRadius: 6,
                },
              ]}
            />
            <View
              style={[
                styles.distSegment,
                {
                  flex: report?.category_distribution.average || 88,
                  backgroundColor: theme.colors.primary,
                },
              ]}
            />
            <View
              style={[
                styles.distSegment,
                {
                  flex: report?.category_distribution.needs_support || 24,
                  backgroundColor: theme.colors.error,
                  borderTopRightRadius: 6,
                  borderBottomRightRadius: 6,
                },
              ]}
            />
          </View>

          <View style={styles.distLegendRow}>
            <View style={styles.legendItem}>
              <View
                style={[styles.legendDot, { backgroundColor: theme.colors.success }]}
              />
              <Text style={[styles.legendLabel, { color: theme.colors.textSecondary }]}>
                High ({report?.category_distribution.high_performing || 72})
              </Text>
            </View>

            <View style={styles.legendItem}>
              <View
                style={[styles.legendDot, { backgroundColor: theme.colors.primary }]}
              />
              <Text style={[styles.legendLabel, { color: theme.colors.textSecondary }]}>
                Average ({report?.category_distribution.average || 88})
              </Text>
            </View>

            <View style={styles.legendItem}>
              <View
                style={[styles.legendDot, { backgroundColor: theme.colors.error }]}
              />
              <Text style={[styles.legendLabel, { color: theme.colors.textSecondary }]}>
                Needs Support ({report?.category_distribution.needs_support || 24})
              </Text>
            </View>
          </View>
        </Card>

        {/* Top Struggling Topics */}
        <Card style={styles.strugglingCard}>
          <View style={styles.strugglingHeader}>
            <AlertCircle size={18} color={theme.colors.error} />
            <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>
              Topics Requiring Intervention
            </Text>
          </View>

          <View style={styles.strugglingList}>
            {report?.top_struggling_topics.map((item, idx) => (
              <View key={idx} style={styles.strugglingRow}>
                <View style={{ flex: 1 }}>
                  <Text style={[styles.topicText, { color: theme.colors.textPrimary }]}>
                    {item.topic}
                  </Text>
                  <Text style={[styles.struggleSub, { color: theme.colors.textTertiary }]}>
                    {item.fail_rate_percentage}% quiz failure rate
                  </Text>
                </View>
                <Badge
                  label={`${item.fail_rate_percentage}% Fail Rate`}
                  variant="error"
                  size="sm"
                />
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
  scrollContent: {
    paddingHorizontal: 20,
    paddingTop: 56,
    paddingBottom: 40,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },
  headerLeft: {
    flex: 1,
  },
  welcomeGreeting: {
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.6,
    textTransform: 'uppercase',
  },
  facultyName: {
    fontSize: 22,
    fontWeight: '800',
    letterSpacing: -0.4,
    marginTop: 2,
  },
  deptText: {
    fontSize: 13,
    fontWeight: '600',
    marginTop: 2,
  },
  actionRow: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 20,
  },
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
    marginBottom: 20,
  },
  statCard: {
    flex: 1,
    minWidth: '46%',
    padding: 16,
    borderRadius: 16,
  },
  statIconBox: {
    width: 38,
    height: 38,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 10,
  },
  statValue: {
    fontSize: 22,
    fontWeight: '800',
    marginBottom: 2,
  },
  statLabel: {
    fontSize: 12,
    fontWeight: '500',
  },
  distributionCard: {
    padding: 18,
    marginBottom: 20,
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 14,
  },
  distBarContainer: {
    flexDirection: 'row',
    height: 12,
    borderRadius: 6,
    overflow: 'hidden',
    marginBottom: 14,
  },
  distSegment: {
    height: '100%',
  },
  distLegendRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  legendDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  legendLabel: {
    fontSize: 12,
  },
  strugglingCard: {
    padding: 18,
  },
  strugglingHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 12,
  },
  strugglingList: {
    gap: 12,
  },
  strugglingRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  topicText: {
    fontSize: 13,
    fontWeight: '600',
  },
  struggleSub: {
    fontSize: 11,
    marginTop: 2,
  },
});
