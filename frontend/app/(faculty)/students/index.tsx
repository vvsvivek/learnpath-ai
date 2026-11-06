import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Pressable,
  RefreshControl,
  ActivityIndicator,
} from 'react-native';
import { useRouter } from 'expo-router';
import {
  Search,
  Filter,
  UserCheck,
  AlertTriangle,
  Award,
  ChevronRight,
  GraduationCap,
} from 'lucide-react-native';
import { useTheme } from '../../../src/hooks/useTheme';
import { Avatar, Badge, Card, EmptyState, Input } from '../../../src/components/ui';
import { facultyService } from '../../../src/services/facultyService';
import { FacultyStudentSummary } from '../../../src/types';

export default function FacultyStudentsScreen() {
  const router = useRouter();
  const { theme, isDark } = useTheme();

  const [isLoading, setIsLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [students, setStudents] = useState<FacultyStudentSummary[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<'all' | 'high' | 'average' | 'needs_support'>('all');
  const [selectedBranch, setSelectedBranch] = useState<string>('All');

  const fetchStudents = async () => {
    try {
      const data = await facultyService.getStudents();
      setStudents(data);
    } catch (e) {
      // Mock student directory
      setStudents([
        {
          id: 'std-101',
          name: 'Vivek Varanasi',
          email: 'vivek@siddhartha.ac.in',
          branch: 'CSE',
          academic_year: 3,
          semester: 5,
          cgpa: 8.92,
          courses_enrolled: 3,
          progress_percentage: 65,
          avg_quiz_score: 88,
          study_minutes: 680,
          category: 'high',
          last_active: '2 hours ago',
        },
        {
          id: 'std-102',
          name: 'Ananya Sharma',
          email: 'ananya.s@siddhartha.ac.in',
          branch: 'CSE',
          academic_year: 3,
          semester: 5,
          cgpa: 9.15,
          courses_enrolled: 3,
          progress_percentage: 82,
          avg_quiz_score: 94,
          study_minutes: 740,
          category: 'high',
          last_active: '30 mins ago',
        },
        {
          id: 'std-103',
          name: 'Sai Teja K.',
          email: 'teja.k@siddhartha.ac.in',
          branch: 'CSE',
          academic_year: 3,
          semester: 5,
          cgpa: 7.42,
          courses_enrolled: 2,
          progress_percentage: 45,
          avg_quiz_score: 72,
          study_minutes: 310,
          category: 'average',
          last_active: '1 day ago',
        },
        {
          id: 'std-104',
          name: 'Priya Reddy',
          email: 'priya.r@siddhartha.ac.in',
          branch: 'IT',
          academic_year: 3,
          semester: 5,
          cgpa: 6.85,
          courses_enrolled: 2,
          progress_percentage: 28,
          avg_quiz_score: 61,
          study_minutes: 180,
          category: 'needs_support',
          last_active: '3 days ago',
        },
        {
          id: 'std-105',
          name: 'Karthik Varma',
          email: 'karthik.v@siddhartha.ac.in',
          branch: 'AIML',
          academic_year: 3,
          semester: 5,
          cgpa: 8.45,
          courses_enrolled: 3,
          progress_percentage: 70,
          avg_quiz_score: 85,
          study_minutes: 520,
          category: 'average',
          last_active: '4 hours ago',
        },
      ]);
    } finally {
      setIsLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchStudents();
  }, []);

  const onRefresh = () => {
    setRefreshing(true);
    fetchStudents();
  };

  // Filter students based on category, branch, and search
  const filteredStudents = students.filter((s) => {
    const matchesSearch =
      s.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.email.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesCategory =
      categoryFilter === 'all' ? true : s.category === categoryFilter;

    const matchesBranch =
      selectedBranch === 'All' ? true : s.branch === selectedBranch;

    return matchesSearch && matchesCategory && matchesBranch;
  });

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      {/* Top Header */}
      <View style={styles.header}>
        <Text style={[styles.headerTitle, { color: theme.colors.textPrimary }]}>
          Student Directory
        </Text>
        <Text style={[styles.headerSubtitle, { color: theme.colors.textSecondary }]}>
          Monitor student learning paths, quiz retention, and progress
        </Text>

        {/* Search Bar */}
        <Input
          placeholder="Search student by name or email..."
          value={searchQuery}
          onChangeText={setSearchQuery}
          leftIcon={<Search size={18} color={theme.colors.textTertiary} />}
          containerStyle={{ marginTop: 12, marginBottom: 8 }}
        />

        {/* Category Tabs */}
        <View style={styles.tabRow}>
          <Pressable
            onPress={() => setCategoryFilter('all')}
            style={[
              styles.tabPill,
              categoryFilter === 'all' && {
                backgroundColor: theme.colors.primary,
              },
            ]}
          >
            <Text
              style={[
                styles.tabPillText,
                { color: categoryFilter === 'all' ? '#FFFFFF' : theme.colors.textSecondary },
              ]}
            >
              All ({students.length})
            </Text>
          </Pressable>

          <Pressable
            onPress={() => setCategoryFilter('high')}
            style={[
              styles.tabPill,
              categoryFilter === 'high' && {
                backgroundColor: theme.colors.success,
              },
            ]}
          >
            <Text
              style={[
                styles.tabPillText,
                { color: categoryFilter === 'high' ? '#FFFFFF' : theme.colors.textSecondary },
              ]}
            >
              High ({students.filter((s) => s.category === 'high').length})
            </Text>
          </Pressable>

          <Pressable
            onPress={() => setCategoryFilter('average')}
            style={[
              styles.tabPill,
              categoryFilter === 'average' && {
                backgroundColor: theme.colors.primary,
              },
            ]}
          >
            <Text
              style={[
                styles.tabPillText,
                { color: categoryFilter === 'average' ? '#FFFFFF' : theme.colors.textSecondary },
              ]}
            >
              Average ({students.filter((s) => s.category === 'average').length})
            </Text>
          </Pressable>

          <Pressable
            onPress={() => setCategoryFilter('needs_support')}
            style={[
              styles.tabPill,
              categoryFilter === 'needs_support' && {
                backgroundColor: theme.colors.error,
              },
            ]}
          >
            <Text
              style={[
                styles.tabPillText,
                { color: categoryFilter === 'needs_support' ? '#FFFFFF' : theme.colors.textSecondary },
              ]}
            >
              Support ({students.filter((s) => s.category === 'needs_support').length})
            </Text>
          </Pressable>
        </View>
      </View>

      {/* Student List */}
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
        {isLoading ? (
          <ActivityIndicator size="large" color={theme.colors.primary} style={{ marginTop: 40 }} />
        ) : filteredStudents.length === 0 ? (
          <EmptyState
            icon={<GraduationCap size={32} color={theme.colors.primary} />}
            title="No Students Found"
            description="No student records match the active search or category filters."
          />
        ) : (
          filteredStudents.map((student) => (
            <Card
              key={student.id}
              variant="elevated"
              style={styles.studentCard}
              onPress={() =>
                router.push({
                  pathname: '/(faculty)/students/[id]',
                  params: { id: student.id },
                })
              }
            >
              <View style={styles.cardTop}>
                <Avatar name={student.name} size="md" />

                <View style={styles.studentInfo}>
                  <Text style={[styles.studentName, { color: theme.colors.textPrimary }]}>
                    {student.name}
                  </Text>
                  <Text style={[styles.studentEmail, { color: theme.colors.textTertiary }]}>
                    {student.email}
                  </Text>
                  <View style={styles.badgeRow}>
                    <Badge label={student.branch} variant="neutral" size="sm" />
                    <Badge label={`CGPA ${student.cgpa.toFixed(2)}`} variant="primary" size="sm" />
                  </View>
                </View>

                <Badge
                  label={
                    student.category === 'high'
                      ? 'High'
                      : student.category === 'average'
                      ? 'Average'
                      : 'Needs Support'
                  }
                  variant={
                    student.category === 'high'
                      ? 'success'
                      : student.category === 'average'
                      ? 'info'
                      : 'error'
                  }
                  size="sm"
                />
              </View>

              <View
                style={[
                  styles.cardMetrics,
                  {
                    backgroundColor: isDark
                      ? theme.colors.surfaceSubtle
                      : theme.colors.inputBackground,
                  },
                ]}
              >
                <View style={styles.metricItem}>
                  <Text style={[styles.metricLabel, { color: theme.colors.textTertiary }]}>
                    Progress
                  </Text>
                  <Text style={[styles.metricValue, { color: theme.colors.textPrimary }]}>
                    {student.progress_percentage}%
                  </Text>
                </View>

                <View style={styles.metricDivider} />

                <View style={styles.metricItem}>
                  <Text style={[styles.metricLabel, { color: theme.colors.textTertiary }]}>
                    Avg Quiz
                  </Text>
                  <Text style={[styles.metricValue, { color: theme.colors.textPrimary }]}>
                    {student.avg_quiz_score}%
                  </Text>
                </View>

                <View style={styles.metricDivider} />

                <View style={styles.metricItem}>
                  <Text style={[styles.metricLabel, { color: theme.colors.textTertiary }]}>
                    Courses
                  </Text>
                  <Text style={[styles.metricValue, { color: theme.colors.textPrimary }]}>
                    {student.courses_enrolled}
                  </Text>
                </View>
              </View>
            </Card>
          ))
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    paddingHorizontal: 20,
    paddingTop: 56,
    paddingBottom: 12,
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
  tabRow: {
    flexDirection: 'row',
    gap: 8,
    marginTop: 4,
  },
  tabPill: {
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 8,
  },
  tabPillText: {
    fontSize: 12,
    fontWeight: '600',
  },
  scrollContent: {
    paddingHorizontal: 20,
    paddingTop: 10,
    paddingBottom: 40,
  },
  studentCard: {
    padding: 16,
    marginBottom: 12,
  },
  cardTop: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginBottom: 14,
  },
  studentInfo: {
    flex: 1,
  },
  studentName: {
    fontSize: 15,
    fontWeight: '700',
  },
  studentEmail: {
    fontSize: 12,
    marginTop: 2,
    marginBottom: 6,
  },
  badgeRow: {
    flexDirection: 'row',
    gap: 6,
  },
  cardMetrics: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    paddingVertical: 10,
    borderRadius: 10,
  },
  metricItem: {
    alignItems: 'center',
  },
  metricLabel: {
    fontSize: 11,
    marginBottom: 2,
  },
  metricValue: {
    fontSize: 14,
    fontWeight: '700',
  },
  metricDivider: {
    width: 1,
    height: 20,
    backgroundColor: 'rgba(150, 150, 150, 0.2)',
  },
});
