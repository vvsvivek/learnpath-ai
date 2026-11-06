import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Pressable,
  ActivityIndicator,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import {
  ArrowLeft,
  Mail,
  Award,
  Clock,
  Target,
  BookOpen,
  Sparkles,
  CheckCircle,
  AlertTriangle,
} from 'lucide-react-native';
import { useTheme } from '../../../src/hooks/useTheme';
import { Avatar, Badge, Button, Card, ProgressRing } from '../../../src/components/ui';
import { facultyService } from '../../../src/services/facultyService';
import { StudentReport } from '../../../src/types';
import { formatDuration } from '../../../src/utils/formatters';

export default function FacultyStudentDetailScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ id: string }>();
  const studentId = params.id;

  const { theme, isDark } = useTheme();

  const [isLoading, setIsLoading] = useState(true);
  const [student, setStudent] = useState<StudentReport | null>(null);

  useEffect(() => {
    const fetchStudent = async () => {
      try {
        const data = await facultyService.getStudentDetail(studentId);
        setStudent(data);
      } catch (e) {
        // Mock detailed student report
        setStudent({
          student_id: studentId,
          name: 'Vivek Varanasi',
          branch: 'CSE',
          academic_year: 3,
          semester: 5,
          cgpa: 8.92,
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
          ],
          quiz_analytics: {
            total_quizzes_taken: 14,
            passed_quizzes: 13,
            average_score_percentage: 87,
            highest_score: 100,
            recent_attempts: [],
          },
          topic_mastery: [
            { topic: 'Dynamic Programming Formulations', category: 'Algorithms', score_percentage: 68, status: 'needs_work' },
            { topic: 'Convolutional Kernels & ResNet', category: 'Deep Learning', score_percentage: 92, status: 'mastered' },
            { topic: 'Raft Consensus Protocol', category: 'Distributed Systems', score_percentage: 78, status: 'improving' },
          ],
          streak_days: 14,
          recommendations: [
            'Student has demonstrated high aptitude in Deep Learning neural networks.',
            'Struggles with DP transition recurrence equations; recommend dedicated problem set.',
          ],
        });
      } finally {
        setIsLoading(false);
      }
    };

    fetchStudent();
  }, [studentId]);

  if (isLoading || !student) {
    return (
      <View style={[styles.loadingContainer, { backgroundColor: theme.colors.background }]}>
        <ActivityIndicator size="large" color={theme.colors.primary} />
      </View>
    );
  }

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      {/* Top Header */}
      <View style={styles.topBar}>
        <Pressable
          onPress={() => router.back()}
          style={[styles.backButton, { backgroundColor: theme.colors.surface }]}
        >
          <ArrowLeft size={20} color={theme.colors.textPrimary} />
        </Pressable>
        <Text style={[styles.screenTitle, { color: theme.colors.textPrimary }]}>
          Student Profile
        </Text>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* Profile Card */}
        <Card variant="elevated" style={styles.profileCard}>
          <View style={styles.profileRow}>
            <Avatar name={student.name} size="lg" />
            <View style={styles.profileInfo}>
              <Text style={[styles.studentName, { color: theme.colors.textPrimary }]}>
                {student.name}
              </Text>
              <Text style={[styles.studentSub, { color: theme.colors.textSecondary }]}>
                {student.branch} • Year {student.academic_year} • Sem {student.semester}
              </Text>
              <View style={styles.badgeRow}>
                <Badge label={`CGPA ${student.cgpa.toFixed(2)}`} variant="success" size="sm" />
                <Badge label={`${student.streak_days} Day Streak`} variant="warning" size="sm" />
              </View>
            </View>
          </View>
        </Card>

        {/* Stats Matrix */}
        <View style={styles.statsGrid}>
          <Card style={styles.statBox}>
            <Clock size={18} color={theme.colors.primary} />
            <Text style={[styles.statValue, { color: theme.colors.textPrimary }]}>
              {formatDuration(student.total_study_minutes)}
            </Text>
            <Text style={[styles.statLabel, { color: theme.colors.textTertiary }]}>
              Study Time
            </Text>
          </Card>

          <Card style={styles.statBox}>
            <Target size={18} color={theme.colors.success} />
            <Text style={[styles.statValue, { color: theme.colors.success }]}>
              {student.quiz_analytics.average_score_percentage}%
            </Text>
            <Text style={[styles.statLabel, { color: theme.colors.textTertiary }]}>
              Quiz Average
            </Text>
          </Card>

          <Card style={styles.statBox}>
            <BookOpen size={18} color={theme.colors.info} />
            <Text style={[styles.statValue, { color: theme.colors.primary }]}>
              {student.course_analytics.length}
            </Text>
            <Text style={[styles.statLabel, { color: theme.colors.textTertiary }]}>
              Active Paths
            </Text>
          </Card>
        </View>

        {/* Enrolled Courses Progress */}
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: theme.colors.textPrimary }]}>
            Active Learning Paths
          </Text>

          {student.course_analytics.map((course) => (
            <Card key={course.course_id} style={styles.courseProgressCard}>
              <View style={styles.courseHeader}>
                <View style={{ flex: 1 }}>
                  <Text style={[styles.courseName, { color: theme.colors.textPrimary }]}>
                    {course.course_title}
                  </Text>
                  <Text style={[styles.phaseText, { color: theme.colors.textTertiary }]}>
                    {course.completed_phases} of {course.total_phases} Phases Mastered
                  </Text>
                </View>

                <ProgressRing percentage={course.progress_percentage} size={44} strokeWidth={4} />
              </View>
            </Card>
          ))}
        </View>

        {/* Weak Topics / Areas for Improvement */}
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: theme.colors.textPrimary }]}>
            Diagnostic Topic Mastery
          </Text>

          {student.topic_mastery.map((topic, i) => (
            <Card key={i} style={styles.topicCard}>
              <View style={styles.topicRow}>
                <View style={{ flex: 1 }}>
                  <Text style={[styles.topicTitle, { color: theme.colors.textPrimary }]}>
                    {topic.topic}
                  </Text>
                  <Text style={[styles.topicCat, { color: theme.colors.textTertiary }]}>
                    {topic.category}
                  </Text>
                </View>

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
              </View>
            </Card>
          ))}
        </View>

        {/* Faculty Action CTA */}
        <Button
          title="Assign Targeted Practice Course"
          variant="primary"
          size="lg"
          leftIcon={<Sparkles size={18} color="#FFFFFF" />}
          onPress={() => router.push('/(faculty)/students/assign')}
          style={{ marginTop: 8 }}
        />
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
  topBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingTop: 56,
    paddingBottom: 14,
  },
  backButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  screenTitle: {
    fontSize: 18,
    fontWeight: '700',
  },
  scrollContent: {
    paddingHorizontal: 20,
    paddingBottom: 40,
  },
  profileCard: {
    padding: 20,
    marginBottom: 16,
  },
  profileRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
  },
  profileInfo: {
    flex: 1,
  },
  studentName: {
    fontSize: 18,
    fontWeight: '800',
  },
  studentSub: {
    fontSize: 13,
    marginTop: 2,
    marginBottom: 8,
  },
  badgeRow: {
    flexDirection: 'row',
    gap: 8,
  },
  statsGrid: {
    flexDirection: 'row',
    gap: 10,
    marginBottom: 20,
  },
  statBox: {
    flex: 1,
    padding: 14,
    alignItems: 'center',
  },
  statValue: {
    fontSize: 17,
    fontWeight: '800',
    marginTop: 6,
    marginBottom: 2,
  },
  statLabel: {
    fontSize: 11,
  },
  section: {
    marginBottom: 20,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 12,
  },
  courseProgressCard: {
    padding: 16,
    marginBottom: 10,
  },
  courseHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  courseName: {
    fontSize: 14,
    fontWeight: '700',
  },
  phaseText: {
    fontSize: 12,
    marginTop: 2,
  },
  topicCard: {
    padding: 14,
    marginBottom: 8,
  },
  topicRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  topicTitle: {
    fontSize: 13,
    fontWeight: '600',
  },
  topicCat: {
    fontSize: 11,
    marginTop: 2,
  },
});
