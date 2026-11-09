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
import { Colors } from '../../styles/theme';

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
        const res = await fetch(
          `http://10.0.2.2:5000/api/main/dashboard?email=${encodeURIComponent(email)}`
        );
        if (res.ok) {
          const data = await res.json();
          if (Array.isArray(data?.activeCourses)) {
            setActiveCourses(data.activeCourses);
          }
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
  scroll: { padding: 20, paddingBottom: 110 },
  bannerCard: {
    backgroundColor: '#1e3a8a',
    borderRadius: 20,
    padding: 20,
    marginBottom: 16,
  },
  bannerSub: { color: '#93c5fd', fontSize: 11, fontWeight: '700', marginBottom: 2 },
  bannerTitle: { color: '#ffffff', fontSize: 20, fontWeight: '900', marginBottom: 4 },
  bannerDesc: { color: '#bfdbfe', fontSize: 12, lineHeight: 16 },
  card: {
    backgroundColor: Colors.cardBackground,
    borderRadius: 20,
    padding: 20,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  cardTitle: { fontSize: 15, fontWeight: '800', color: Colors.textPrimary, marginBottom: 14 },
  emptyText: { textAlign: 'center', color: Colors.textSecondary, paddingVertical: 20, fontSize: 12 },
  courseBox: {
    backgroundColor: '#f8fafc',
    borderRadius: 16,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  rowBetween: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  courseTitle: { fontSize: 14, fontWeight: '800', color: Colors.textPrimary, flex: 1, marginRight: 8 },
  progressText: { fontSize: 11.5, fontWeight: '700', color: Colors.primary },
  progressBarBg: {
    height: 6,
    backgroundColor: '#e2e8f0',
    borderRadius: 3,
    overflow: 'hidden',
    marginBottom: 12,
  },
  progressBarFill: {
    height: '100%',
    backgroundColor: Colors.primary,
  },
  openLink: { fontSize: 12.5, fontWeight: '800', color: Colors.primary },
});