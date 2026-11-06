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
import Animated from 'react-native-reanimated';
import {
  Clock,
  Target,
  CheckCircle,
  Flame,
  ArrowRight,
  Sparkles,
  BookMarked,
  GraduationCap,
  ChevronRight,
  TrendingUp,
} from 'lucide-react-native';
import { useTheme } from '../../src/hooks/useTheme';
import { useAuth } from '../../src/hooks/useAuth';
import { Avatar, Badge, Button, Card, Skeleton } from '../../src/components/ui';
import { formatDuration, getGreeting } from '../../src/utils/formatters';
import { courseService } from '../../src/services/courseService';
import { reportService } from '../../src/services/reportService';
import { CourseRecommendation, StudentReport } from '../../src/types';
import { useFadeIn } from '../../src/animations/microInteractions';

export default function StudentHomeScreen() {
  const router = useRouter();
  const { theme, isDark } = useTheme();
  const { user } = useAuth();

  const [isLoading, setIsLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [recommendations, setRecommendations] = useState<CourseRecommendation[]>([]);
  const [report, setReport] = useState<StudentReport | null>(null);

  const headerAnim = useFadeIn(50, 400);

  const fetchData = async () => {
    try {
      const [recsResult, reportResult] = await Promise.allSettled([
        courseService.getRecommendations(),
        reportService.getStudentReport(),
      ]);

      if (recsResult.status === 'fulfilled') {
        setRecommendations(recsResult.value || []);
      } else {
        // Fallback demo recommendations
        setRecommendations([
          {
            id: 'rec-1',
            match_score: 95,
            reason: 'Tailored for 3rd Year CSE students targeting AI/ML specialization',
            source: 'ai',
            course: {
              id: 'course-ai-core',
              title: 'Deep Learning & Neural Architectures',
              description: 'Comprehensive study of CNNs, RNNs, and Transformers with PyTorch',
              category: 'Artificial Intelligence',
              difficulty: 'Intermediate',
              estimated_hours: 32,
              is_custom: false,
              created_at: new Date().toISOString(),
            },
          },
          {
            id: 'rec-2',
            match_score: 91,
            reason: 'Assigned by Faculty for Semester 5 Distributed Systems',
            source: 'faculty',
            faculty_name: 'Dr. Ramesh Kumar',
            course: {
              id: 'course-dist-sys',
              title: 'Distributed Systems & Microservices',
              description: 'Scalable cloud backends, consensus algorithms, and event brokers',
              category: 'Cloud & Systems',
              difficulty: 'Advanced',
              estimated_hours: 28,
              is_custom: false,
              created_at: new Date().toISOString(),
            },
          },
          {
            id: 'rec-3',
            match_score: 88,
            reason: 'Essential preparation for upcoming campus placements',
            source: 'ai',
            course: {
              id: 'course-dsa-master',
              title: 'Advanced Data Structures & Algorithms',
              description: 'Dynamic programming, graph theory, and algorithmic problem solving',
              category: 'Computer Science',
              difficulty: 'Intermediate',
              estimated_hours: 40,
              is_custom: false,
              created_at: new Date().toISOString(),
            },
          },
        ]);
      }

      if (reportResult.status === 'fulfilled') {
        setReport(reportResult.value);
      } else {
        // Fallback demo report stats
        setReport({
          student_id: user?.id || 'std-1',
          name: user?.name || 'Student',
          branch: user?.branch || 'CSE',
          academic_year: user?.academic_year || 3,
          semester: user?.semester || 5,
          cgpa: user?.cgpa || 8.85,
          total_study_minutes: 420,
          weekly_study_data: [
            { day: 'Mon', minutes: 60 },
            { day: 'Tue', minutes: 75 },
            { day: 'Wed', minutes: 90 },
            { day: 'Thu', minutes: 45 },
            { day: 'Fri', minutes: 80 },
            { day: 'Sat', minutes: 50 },
            { day: 'Sun', minutes: 20 },
          ],
          course_analytics: [],
          quiz_analytics: {
            total_quizzes_taken: 8,
            passed_quizzes: 7,
            average_score_percentage: 86,
            highest_score: 100,
            recent_attempts: [],
          },
          topic_mastery: [
            { topic: 'Dynamic Programming', category: 'Algorithms', score_percentage: 64, status: 'needs_work' },
            { topic: 'Backpropagation & Loss', category: 'Deep Learning', score_percentage: 88, status: 'mastered' },
            { topic: 'Raft Consensus Protocol', category: 'Distributed Systems', score_percentage: 72, status: 'improving' },
          ],
          streak_days: 12,
          recommendations: [
            'Revise Memoization in Dynamic Programming before the upcoming assessment',
            'Take a quick quiz on Transformer Attention mechanisms',
          ],
        });
      }
    } catch (e) {
      console.warn('Could not fetch student home data:', e);
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

  const facultyRecs = recommendations.filter((r) => r.source === 'faculty');
  const aiRecs = recommendations.filter((r) => r.source === 'ai');

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
        {/* Welcome Section */}
        <Animated.View style={[styles.welcomeSection, headerAnim]}>
          <View style={styles.welcomeRow}>
            <View style={styles.greetingContainer}>
              <Text style={[styles.greetingText, { color: theme.colors.textSecondary }]}>
                {getGreeting(user?.name ? user.name.split(' ')[0] : 'there')}
              </Text>
              <Text style={[styles.welcomeSubtitle, { color: theme.colors.textPrimary }]}>
                Ready to level up today?
              </Text>
            </View>

            <Pressable onPress={() => router.push('/(student)/profile')}>
              <Avatar
                name={user?.name || 'Student'}
                imageUrl={user?.avatar_url}
                size="md"
                showBadge
              />
            </Pressable>
          </View>

          {/* Academic Profile Badges */}
          <View style={styles.badgeRow}>
            <Badge
              label={`${user?.branch || 'CSE'} - Year ${user?.academic_year || 3}`}
              variant="primary"
              size="sm"
            />
            <Badge
              label={`Sem ${user?.semester || 5}`}
              variant="neutral"
              size="sm"
            />
            <Badge
              label={`CGPA: ${user?.cgpa ? user.cgpa.toFixed(2) : '8.85'}`}
              variant="success"
              size="sm"
            />
          </View>
        </Animated.View>

        {/* Stats Grid */}
        {isLoading ? (
          <View style={styles.statsGrid}>
            <Skeleton height={88} style={styles.statSkeleton} />
            <Skeleton height={88} style={styles.statSkeleton} />
            <Skeleton height={88} style={styles.statSkeleton} />
            <Skeleton height={88} style={styles.statSkeleton} />
          </View>
        ) : (
          <View style={styles.statsGrid}>
            {/* Stat: Study Time */}
            <Card variant="elevated" style={styles.statCard}>
              <View
                style={[
                  styles.statIconBox,
                  { backgroundColor: isDark ? 'rgba(74, 144, 217, 0.15)' : '#EBF4FC' },
                ]}
              >
                <Clock size={20} color={theme.colors.primary} />
              </View>
              <Text style={[styles.statValue, { color: theme.colors.textPrimary }]}>
                {formatDuration(report?.total_study_minutes || 420)}
              </Text>
              <Text style={[styles.statLabel, { color: theme.colors.textSecondary }]}>
                Study Time
              </Text>
            </Card>

            {/* Stat: Quiz Accuracy */}
            <Card variant="elevated" style={styles.statCard}>
              <View
                style={[
                  styles.statIconBox,
                  { backgroundColor: isDark ? 'rgba(16, 185, 129, 0.15)' : '#ECFDF5' },
                ]}
              >
                <Target size={20} color={theme.colors.success} />
              </View>
              <Text style={[styles.statValue, { color: theme.colors.textPrimary }]}>
                {report?.quiz_analytics?.average_score_percentage || 86}%
              </Text>
              <Text style={[styles.statLabel, { color: theme.colors.textSecondary }]}>
                Quiz Accuracy
              </Text>
            </Card>

            {/* Stat: Courses Completed */}
            <Card variant="elevated" style={styles.statCard}>
              <View
                style={[
                  styles.statIconBox,
                  { backgroundColor: isDark ? 'rgba(59, 130, 246, 0.15)' : '#EFF6FF' },
                ]}
              >
                <CheckCircle size={20} color={theme.colors.info} />
              </View>
              <Text style={[styles.statValue, { color: theme.colors.textPrimary }]}>
                {user?.completed_courses_count || 3}
              </Text>
              <Text style={[styles.statLabel, { color: theme.colors.textSecondary }]}>
                Completed
              </Text>
            </Card>

            {/* Stat: Streak */}
            <Card variant="elevated" style={styles.statCard}>
              <View
                style={[
                  styles.statIconBox,
                  { backgroundColor: isDark ? 'rgba(245, 158, 11, 0.15)' : '#FFFBEB' },
                ]}
              >
                <Flame size={20} color={theme.colors.accentAmber} />
              </View>
              <Text style={[styles.statValue, { color: theme.colors.textPrimary }]}>
                {report?.streak_days || 12}d
              </Text>
              <Text style={[styles.statLabel, { color: theme.colors.textSecondary }]}>
                Study Streak
              </Text>
            </Card>
          </View>
        )}

        {/* Focus Banner: AI Tutor CTA */}
        <Pressable
          onPress={() => router.push('/(student)/ai')}
          style={[
            styles.aiBanner,
            {
              backgroundColor: isDark ? '#1C2738' : theme.colors.navy,
            },
          ]}
        >
          <View style={styles.aiBannerLeft}>
            <View style={styles.sparklePill}>
              <Sparkles size={14} color="#FBBF24" />
              <Text style={styles.aiTag}>AI Study Copilot</Text>
            </View>
            <Text style={styles.aiTitle}>Need help understanding a concept?</Text>
            <Text style={styles.aiSubtitle}>
              Ask Gemini AI for step-by-step guidance, code explanations, or practice problems.
            </Text>
          </View>
          <View style={styles.aiBannerArrow}>
            <ArrowRight size={20} color="#FFFFFF" />
          </View>
        </Pressable>

        {/* Section: Topics to Focus On */}
        <View style={styles.sectionHeader}>
          <View style={styles.sectionTitleRow}>
            <TrendingUp size={20} color={theme.colors.accentAmber} />
            <Text style={[styles.sectionTitle, { color: theme.colors.textPrimary }]}>
              Topics to Focus On
            </Text>
          </View>
          <Pressable onPress={() => router.push('/(student)/reports')}>
            <Text style={[styles.seeAllText, { color: theme.colors.primary }]}>
              View Analytics
            </Text>
          </Pressable>
        </View>

        {isLoading ? (
          <Skeleton height={74} style={{ marginBottom: 12, borderRadius: 14 }} />
        ) : (
          <View style={styles.focusList}>
            {report?.topic_mastery.map((topic, index) => (
              <Card
                key={index}
                style={[styles.topicCard, { marginBottom: 10 }]}
                onPress={() => router.push('/(student)/courses/index')}
              >
                <View style={styles.topicInfo}>
                  <Text style={[styles.topicTitle, { color: theme.colors.textPrimary }]}>
                    {topic.topic}
                  </Text>
                  <Text style={[styles.topicCategory, { color: theme.colors.textSecondary }]}>
                    {topic.category}
                  </Text>
                </View>

                <View style={styles.topicAction}>
                  <Badge
                    label={`${topic.score_percentage}%`}
                    variant={
                      topic.status === 'mastered'
                        ? 'success'
                        : topic.status === 'improving'
                        ? 'warning'
                        : 'error'
                    }
                    size="sm"
                  />
                  <ChevronRight size={18} color={theme.colors.textTertiary} />
                </View>
              </Card>
            ))}
          </View>
        )}

        {/* Section: Faculty Recommendations (if any) */}
        {facultyRecs.length > 0 && (
          <View style={styles.sectionBlock}>
            <View style={styles.sectionHeader}>
              <View style={styles.sectionTitleRow}>
                <GraduationCap size={20} color={theme.colors.primary} />
                <Text style={[styles.sectionTitle, { color: theme.colors.textPrimary }]}>
                  Faculty Assignments
                </Text>
              </View>
              <Badge label="Required" variant="warning" size="sm" />
            </View>

            {facultyRecs.map((rec) => (
              <Card
                key={rec.id}
                variant="elevated"
                style={styles.facultyCard}
                onPress={() =>
                  router.push({
                    pathname: '/(student)/courses/[id]',
                    params: { id: rec.course.id },
                  })
                }
              >
                <View style={styles.facultyCardHeader}>
                  <Badge label="Faculty Assigned" variant="info" size="sm" />
                  <Text style={[styles.facultyName, { color: theme.colors.textSecondary }]}>
                    {rec.faculty_name}
                  </Text>
                </View>

                <Text style={[styles.courseTitle, { color: theme.colors.textPrimary }]}>
                  {rec.course.title}
                </Text>
                <Text
                  style={[styles.courseDesc, { color: theme.colors.textSecondary }]}
                  numberOfLines={2}
                >
                  {rec.course.description}
                </Text>

                <View style={styles.courseMeta}>
                  <Text style={[styles.metaItem, { color: theme.colors.textTertiary }]}>
                    ⏱ {rec.course.estimated_hours}h
                  </Text>
                  <Text style={[styles.metaItem, { color: theme.colors.textTertiary }]}>
                    📊 {rec.course.difficulty}
                  </Text>
                  <Button
                    title="View Curriculum"
                    size="sm"
                    variant="outline"
                    onPress={() =>
                      router.push({
                        pathname: '/(student)/courses/[id]',
                        params: { id: rec.course.id },
                      })
                    }
                  />
                </View>
              </Card>
            ))}
          </View>
        )}

        {/* Section: Recommended Courses */}
        <View style={styles.sectionBlock}>
          <View style={styles.sectionHeader}>
            <View style={styles.sectionTitleRow}>
              <BookMarked size={20} color={theme.colors.primary} />
              <Text style={[styles.sectionTitle, { color: theme.colors.textPrimary }]}>
                Recommended for You
              </Text>
            </View>
            <Pressable onPress={() => router.push('/(student)/courses/index')}>
              <Text style={[styles.seeAllText, { color: theme.colors.primary }]}>
                Explore All
              </Text>
            </Pressable>
          </View>

          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.horizontalScroll}
          >
            {aiRecs.map((rec) => (
              <Card
                key={rec.id}
                variant="elevated"
                style={styles.horizontalCourseCard}
                onPress={() =>
                  router.push({
                    pathname: '/(student)/courses/[id]',
                    params: { id: rec.course.id },
                  })
                }
              >
                <View style={styles.matchScoreBadge}>
                  <Sparkles size={12} color={theme.colors.primary} />
                  <Text style={[styles.matchScoreText, { color: theme.colors.primary }]}>
                    {rec.match_score}% Match
                  </Text>
                </View>

                <Text
                  style={[styles.horizCourseTitle, { color: theme.colors.textPrimary }]}
                  numberOfLines={2}
                >
                  {rec.course.title}
                </Text>

                <Text
                  style={[styles.horizCourseDesc, { color: theme.colors.textSecondary }]}
                  numberOfLines={2}
                >
                  {rec.course.description}
                </Text>

                <View style={styles.horizCardFooter}>
                  <Badge label={rec.course.difficulty} variant="neutral" size="sm" />
                  <Text style={[styles.metaItem, { color: theme.colors.textTertiary }]}>
                    {rec.course.estimated_hours}h
                  </Text>
                </View>
              </Card>
            ))}
          </ScrollView>
        </View>
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
  welcomeSection: {
    marginBottom: 24,
  },
  welcomeRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  greetingContainer: {
    flex: 1,
  },
  greetingText: {
    fontSize: 22,
    fontWeight: '800',
    letterSpacing: -0.4,
  },
  welcomeSubtitle: {
    fontSize: 14,
    fontWeight: '500',
    marginTop: 2,
  },
  badgeRow: {
    flexDirection: 'row',
    gap: 8,
  },
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
    marginBottom: 24,
  },
  statCard: {
    flex: 1,
    minWidth: '46%',
    padding: 16,
    borderRadius: 16,
  },
  statSkeleton: {
    flex: 1,
    minWidth: '46%',
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
    fontSize: 20,
    fontWeight: '800',
    marginBottom: 2,
  },
  statLabel: {
    fontSize: 12,
    fontWeight: '500',
  },
  aiBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 18,
    padding: 18,
    marginBottom: 24,
  },
  aiBannerLeft: {
    flex: 1,
  },
  sparklePill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    marginBottom: 6,
  },
  aiTag: {
    color: '#FBBF24',
    fontSize: 12,
    fontWeight: '700',
    textTransform: 'uppercase',
  },
  aiTitle: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 4,
  },
  aiSubtitle: {
    color: '#CBD5E1',
    fontSize: 12,
    lineHeight: 16,
  },
  aiBannerArrow: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: 'rgba(255, 255, 255, 0.15)',
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 12,
  },
  sectionBlock: {
    marginBottom: 24,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 14,
  },
  sectionTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '700',
  },
  seeAllText: {
    fontSize: 13,
    fontWeight: '600',
  },
  focusList: {
    marginBottom: 24,
  },
  topicCard: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
  },
  topicInfo: {
    flex: 1,
    marginRight: 12,
  },
  topicTitle: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 2,
  },
  topicCategory: {
    fontSize: 12,
  },
  topicAction: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  facultyCard: {
    padding: 16,
    marginBottom: 12,
  },
  facultyCardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  facultyName: {
    fontSize: 12,
    fontWeight: '500',
  },
  courseTitle: {
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 4,
  },
  courseDesc: {
    fontSize: 13,
    lineHeight: 18,
    marginBottom: 12,
  },
  courseMeta: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  metaItem: {
    fontSize: 12,
  },
  horizontalScroll: {
    gap: 14,
    paddingRight: 20,
  },
  horizontalCourseCard: {
    width: 240,
    padding: 16,
  },
  matchScoreBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    marginBottom: 8,
  },
  matchScoreText: {
    fontSize: 11,
    fontWeight: '700',
  },
  horizCourseTitle: {
    fontSize: 15,
    fontWeight: '700',
    marginBottom: 4,
    height: 40,
  },
  horizCourseDesc: {
    fontSize: 12,
    lineHeight: 16,
    marginBottom: 12,
    height: 32,
  },
  horizCardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
});
