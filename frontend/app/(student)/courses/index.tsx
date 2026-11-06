import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Pressable,
  RefreshControl,
} from 'react-native';
import { useRouter } from 'expo-router';
import {
  Search,
  Plus,
  BookOpen,
  Sparkles,
  ChevronRight,
  Filter,
} from 'lucide-react-native';
import { useTheme } from '../../../src/hooks/useTheme';
import { Badge, Button, Card, EmptyState, Input, ProgressRing, Skeleton } from '../../../src/components/ui';
import { courseService } from '../../../src/services/courseService';
import { CourseEnrollment, CourseRecommendation } from '../../../src/types';

export default function CoursesScreen() {
  const router = useRouter();
  const { theme, isDark } = useTheme();

  const [activeTab, setActiveTab] = useState<'my_courses' | 'recommended'>('my_courses');
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [enrolledCourses, setEnrolledCourses] = useState<CourseEnrollment[]>([]);
  const [recommendations, setRecommendations] = useState<CourseRecommendation[]>([]);

  const fetchCourses = async () => {
    try {
      const [enrolledRes, recsRes] = await Promise.allSettled([
        courseService.getEnrolledCourses(),
        courseService.getRecommendations(),
      ]);

      if (enrolledRes.status === 'fulfilled' && enrolledRes.value?.length) {
        setEnrolledCourses(enrolledRes.value);
      } else {
        // Mock enrolled courses for demonstration
        setEnrolledCourses([
          {
            id: 'enr-1',
            user_id: 'std-1',
            course_id: 'course-ai-core',
            progress_percentage: 65,
            current_phase: 3,
            is_completed: false,
            enrolled_at: new Date().toISOString(),
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
            id: 'enr-2',
            user_id: 'std-1',
            course_id: 'course-dist-sys',
            progress_percentage: 25,
            current_phase: 1,
            is_completed: false,
            enrolled_at: new Date().toISOString(),
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
        ]);
      }

      if (recsRes.status === 'fulfilled' && recsRes.value?.length) {
        setRecommendations(recsRes.value);
      } else {
        setRecommendations([
          {
            id: 'rec-1',
            match_score: 95,
            reason: 'Matches your interest in Artificial Intelligence',
            source: 'ai',
            course: {
              id: 'course-ml-ops',
              title: 'Production MLOps & Model Deployment',
              description: 'Docker, Kubernetes, MLflow, and CI/CD pipelines for ML models',
              category: 'DevOps & AI',
              difficulty: 'Advanced',
              estimated_hours: 26,
              is_custom: false,
              created_at: new Date().toISOString(),
            },
          },
          {
            id: 'rec-2',
            match_score: 90,
            reason: 'Recommended for 3rd year CSE placements',
            source: 'faculty',
            faculty_name: 'Dr. Ramesh Kumar',
            course: {
              id: 'course-dsa-master',
              title: 'Advanced Data Structures & Algorithms',
              description: 'Dynamic programming, graph algorithms, and competitive programming',
              category: 'Algorithms',
              difficulty: 'Intermediate',
              estimated_hours: 40,
              is_custom: false,
              created_at: new Date().toISOString(),
            },
          },
        ]);
      }
    } catch (e) {
      console.warn('Error fetching courses list:', e);
    } finally {
      setIsLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchCourses();
  }, []);

  const onRefresh = () => {
    setRefreshing(true);
    fetchCourses();
  };

  const handleEnroll = async (courseId: string) => {
    try {
      const newEnr = await courseService.enrollInCourse(courseId);
      setEnrolledCourses((prev) => [newEnr, ...prev]);
      setActiveTab('my_courses');
    } catch (e) {
      // Offline fallback
      const rec = recommendations.find((r) => r.course.id === courseId);
      if (rec) {
        const mockEnr: CourseEnrollment = {
          id: `enr-${Date.now()}`,
          user_id: 'std-1',
          course_id: courseId,
          progress_percentage: 0,
          current_phase: 1,
          is_completed: false,
          enrolled_at: new Date().toISOString(),
          course: rec.course,
        };
        setEnrolledCourses((prev) => [mockEnr, ...prev]);
        setActiveTab('my_courses');
      }
    }
  };

  // Filter lists based on search
  const filteredEnrolled = enrolledCourses.filter(
    (item) =>
      item.course?.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.course?.category.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const filteredRecommended = recommendations.filter(
    (item) =>
      item.course?.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.course?.category.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      {/* Top Header */}
      <View style={styles.header}>
        <View style={styles.headerTop}>
          <View>
            <Text style={[styles.headerTitle, { color: theme.colors.textPrimary }]}>
              Courses & Paths
            </Text>
            <Text style={[styles.headerSubtitle, { color: theme.colors.textSecondary }]}>
              Master B.Tech competencies step-by-step
            </Text>
          </View>

          <Button
            title="Custom Path"
            variant="outline"
            size="sm"
            leftIcon={<Plus size={16} color={theme.colors.primary} />}
            onPress={() => router.push('/(student)/courses/custom')}
          />
        </View>

        {/* Search Bar */}
        <Input
          placeholder="Search courses, skills, or topics..."
          value={searchQuery}
          onChangeText={setSearchQuery}
          leftIcon={<Search size={18} color={theme.colors.textTertiary} />}
          containerStyle={styles.searchContainer}
        />

        {/* Segmented Tabs */}
        <View
          style={[
            styles.tabBar,
            {
              backgroundColor: isDark
                ? theme.colors.surfaceSubtle
                : theme.colors.inputBackground,
            },
          ]}
        >
          <Pressable
            onPress={() => setActiveTab('my_courses')}
            style={[
              styles.tabItem,
              activeTab === 'my_courses' && {
                backgroundColor: theme.colors.surfaceElevated,
                shadowColor: '#000',
                shadowOffset: { width: 0, height: 2 },
                shadowOpacity: 0.08,
                shadowRadius: 4,
                elevation: 2,
              },
            ]}
          >
            <Text
              style={[
                styles.tabText,
                {
                  color:
                    activeTab === 'my_courses'
                      ? theme.colors.primary
                      : theme.colors.textSecondary,
                  fontWeight: activeTab === 'my_courses' ? '700' : '500',
                },
              ]}
            >
              Enrolled ({enrolledCourses.length})
            </Text>
          </Pressable>

          <Pressable
            onPress={() => setActiveTab('recommended')}
            style={[
              styles.tabItem,
              activeTab === 'recommended' && {
                backgroundColor: theme.colors.surfaceElevated,
                shadowColor: '#000',
                shadowOffset: { width: 0, height: 2 },
                shadowOpacity: 0.08,
                shadowRadius: 4,
                elevation: 2,
              },
            ]}
          >
            <Text
              style={[
                styles.tabText,
                {
                  color:
                    activeTab === 'recommended'
                      ? theme.colors.primary
                      : theme.colors.textSecondary,
                  fontWeight: activeTab === 'recommended' ? '700' : '500',
                },
              ]}
            >
              Recommended ({recommendations.length})
            </Text>
          </Pressable>
        </View>
      </View>

      {/* Courses List */}
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
          <View style={styles.skeletonContainer}>
            <Skeleton height={140} style={{ borderRadius: 16, marginBottom: 14 }} />
            <Skeleton height={140} style={{ borderRadius: 16, marginBottom: 14 }} />
          </View>
        ) : activeTab === 'my_courses' ? (
          filteredEnrolled.length === 0 ? (
            <EmptyState
              icon={<BookOpen size={32} color={theme.colors.primary} />}
              title="No Courses Enrolled Yet"
              description="Browse the recommended tab or create an AI-powered custom learning path tailored to your goals."
              actionTitle="Explore Recommendations"
              onAction={() => setActiveTab('recommended')}
            />
          ) : (
            filteredEnrolled.map((enrollment) => (
              <Card
                key={enrollment.id}
                variant="elevated"
                style={styles.courseCard}
                onPress={() =>
                  router.push({
                    pathname: '/(student)/courses/[id]',
                    params: { id: enrollment.course.id },
                  })
                }
              >
                <View style={styles.courseCardHeader}>
                  <View style={styles.courseHeaderLeft}>
                    <Badge
                      label={enrollment.course.category}
                      variant="primary"
                      size="sm"
                    />
                    <Badge
                      label={`Phase ${enrollment.current_phase || 1}/4`}
                      variant="neutral"
                      size="sm"
                    />
                  </View>

                  <ProgressRing
                    percentage={enrollment.progress_percentage}
                    size={46}
                    strokeWidth={5}
                  />
                </View>

                <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>
                  {enrollment.course.title}
                </Text>
                <Text
                  style={[styles.cardDescription, { color: theme.colors.textSecondary }]}
                  numberOfLines={2}
                >
                  {enrollment.course.description}
                </Text>

                <View style={styles.cardFooter}>
                  <View style={styles.footerMeta}>
                    <Text style={[styles.metaText, { color: theme.colors.textTertiary }]}>
                      Difficulty: {enrollment.course.difficulty}
                    </Text>
                    <Text style={[styles.metaText, { color: theme.colors.textTertiary }]}>
                      • {enrollment.course.estimated_hours}h
                    </Text>
                  </View>

                  <Button
                    title="Continue"
                    size="sm"
                    variant="primary"
                    rightIcon={<ChevronRight size={16} color="#FFFFFF" />}
                    onPress={() =>
                      router.push({
                        pathname: '/(student)/courses/[id]',
                        params: { id: enrollment.course.id },
                      })
                    }
                  />
                </View>
              </Card>
            ))
          )
        ) : filteredRecommended.length === 0 ? (
          <EmptyState
            icon={<Sparkles size={32} color={theme.colors.primary} />}
            title="No Matches Found"
            description="Try searching with different keywords or generate a personalized syllabus."
            actionTitle="Generate Custom Course"
            onAction={() => router.push('/(student)/courses/custom')}
          />
        ) : (
          filteredRecommended.map((rec) => (
            <Card
              key={rec.id}
              variant="elevated"
              style={styles.courseCard}
              onPress={() =>
                router.push({
                  pathname: '/(student)/courses/[id]',
                  params: { id: rec.course.id },
                })
              }
            >
              <View style={styles.courseCardHeader}>
                <View style={styles.courseHeaderLeft}>
                  <Badge
                    label={`${rec.match_score}% Match`}
                    variant="success"
                    size="sm"
                  />
                  <Badge
                    label={rec.source === 'faculty' ? 'Faculty Assigned' : 'AI Curated'}
                    variant={rec.source === 'faculty' ? 'warning' : 'primary'}
                    size="sm"
                  />
                </View>
                <Badge label={rec.course.difficulty} variant="neutral" size="sm" />
              </View>

              <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>
                {rec.course.title}
              </Text>
              <Text
                style={[styles.cardDescription, { color: theme.colors.textSecondary }]}
                numberOfLines={2}
              >
                {rec.course.description}
              </Text>

              <View style={styles.cardFooter}>
                <Text style={[styles.metaText, { color: theme.colors.textTertiary }]}>
                  {rec.course.estimated_hours} hours total
                </Text>

                <Button
                  title="Enroll Now"
                  size="sm"
                  variant="primary"
                  onPress={() => handleEnroll(rec.course.id)}
                />
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
    paddingBottom: 14,
  },
  headerTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
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
  searchContainer: {
    marginBottom: 12,
  },
  tabBar: {
    flexDirection: 'row',
    borderRadius: 12,
    padding: 4,
  },
  tabItem: {
    flex: 1,
    paddingVertical: 9,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 10,
  },
  tabText: {
    fontSize: 13,
  },
  scrollContent: {
    paddingHorizontal: 20,
    paddingTop: 10,
    paddingBottom: 40,
  },
  skeletonContainer: {
    gap: 12,
  },
  courseCard: {
    padding: 16,
    marginBottom: 14,
  },
  courseCardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  courseHeaderLeft: {
    flexDirection: 'row',
    gap: 6,
  },
  cardTitle: {
    fontSize: 17,
    fontWeight: '700',
    marginBottom: 6,
  },
  cardDescription: {
    fontSize: 13,
    lineHeight: 18,
    marginBottom: 14,
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  footerMeta: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  metaText: {
    fontSize: 12,
  },
});
