import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  SafeAreaView,
  TouchableOpacity,
  ActivityIndicator,
} from 'react-native';
import { Colors, Shadows, Radii, Typography, Spacing } from '../../styles/theme';
import { api } from '../../services/api';

interface ProgressScreenProps {
  userData: any;
  activeSubject: string;
  courseProgressMap: any;
  onSelectSubject: (subject: string) => void;
}

export default function ProgressScreen({
  userData,
  courseProgressMap,
  onSelectSubject,
}: ProgressScreenProps) {
  const [activeCourses, setActiveCourses] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const email = userData?.email || '';

  useEffect(() => {
    const fetchCourses = async () => {
      try {
        setLoading(true);
        const data = await api.getDashboard(email);
        if (Array.isArray(data?.activeCourses)) {
          setActiveCourses(data.activeCourses);
        }
      } catch (_err) {
        setActiveCourses([]);
      } finally {
        setLoading(false);
      }
    };
    fetchCourses();
  }, [email, courseProgressMap]);

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scroll}>
        {/* Banner Header */}
        <View style={styles.bannerCard}>
          <Text style={styles.bannerSub}>MULTI-COURSE HUB</Text>
          <Text style={styles.bannerTitle}>Your Enrolled Courses</Text>
          <Text style={styles.bannerDesc}>
            Click any course below to view its specific 4-phase roadmap and start learning.
          </Text>
        </View>

        <View style={styles.card}>
          <Text style={styles.cardTitle}>📚 Active Learning Paths ({activeCourses.length})</Text>

          {loading ? (
            <ActivityIndicator size="small" color={Colors.primary} style={{ marginVertical: 20 }} />
          ) : activeCourses.length === 0 ? (
            <Text style={styles.emptyText}>No active courses enrolled yet.</Text>
          ) : (
            activeCourses.map((course: any, idx: number) => {
              const doneCount = courseProgressMap[course.title] || 0;
              const mastery = Math.min(doneCount * 25, 100);

              return (
                <TouchableOpacity
                  key={idx}
                  style={styles.courseBox}
                  onPress={() => onSelectSubject(course.title)}
                  activeOpacity={0.8}
                >
                  <View style={styles.rowBetween}>
                    <Text style={styles.courseTitle}>{course.title}</Text>
                    <Text style={styles.progressText}>
                      {doneCount}/4 Phases ({mastery}%)
                    </Text>
                  </View>

                  <View style={styles.progressBarBg}>
                    <View style={[styles.progressBarFill, { width: `${mastery}%` }]} />
                  </View>

                  <Text style={styles.openLink}>Open Course Roadmap →</Text>
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
  container: { flex: 1, backgroundColor: Colors.background },
  scroll: { padding: Spacing.lg, paddingBottom: Spacing.section },
  bannerCard: {
    backgroundColor: Colors.primaryDark,
    borderRadius: Radii.lg,
    padding: Spacing.md,
    marginBottom: Spacing.md,
    ...Shadows.small,
  },
  bannerSub: { color: '#93C5FD', fontSize: Typography.badge.fontSize, fontWeight: '700', marginBottom: 2 },
  bannerTitle: { color: '#FFFFFF', fontSize: Typography.h2.fontSize, fontWeight: '800', marginBottom: 4 },
  bannerDesc: { color: '#CBD5E1', fontSize: Typography.caption.fontSize, lineHeight: 15 },
  card: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.lg,
    padding: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  cardTitle: { fontSize: Typography.h3.fontSize, fontWeight: '800', color: Colors.textPrimary, marginBottom: Spacing.sm },
  emptyText: { textAlign: 'center', color: Colors.textSecondary, paddingVertical: 16, fontSize: Typography.bodySmall.fontSize },
  courseBox: {
    backgroundColor: Colors.surfaceMuted,
    borderRadius: Radii.md,
    padding: Spacing.sm,
    marginBottom: Spacing.sm,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  rowBetween: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  courseTitle: { fontSize: Typography.body.fontSize, fontWeight: '700', color: Colors.textPrimary, flex: 1, marginRight: 8 },
  progressText: { fontSize: Typography.caption.fontSize, fontWeight: '700', color: Colors.primary },
  progressBarBg: {
    height: 4,
    backgroundColor: '#E2E8F0',
    borderRadius: 2,
    overflow: 'hidden',
    marginBottom: 8,
  },
  progressBarFill: {
    height: '100%',
    backgroundColor: Colors.primary,
  },
  openLink: { fontSize: Typography.caption.fontSize, fontWeight: '700', color: Colors.primary },
});
