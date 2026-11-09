import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  SafeAreaView,
  StatusBar,
  TouchableOpacity,
  ActivityIndicator,
} from 'react-native';
import { Colors, Shadows, Radii } from '../../styles/theme';
import { api } from '../../services/api';

interface FacultyHomeScreenProps {
  userData?: any;
  onNavigateTab: (tab: string) => void;
}

export default function FacultyHomeScreen({ userData, onNavigateTab }: FacultyHomeScreenProps) {
  const rawName = userData?.name || userData?.username || userData?.email?.split('@')[0] || 'Professor';
  const facultyName = rawName.charAt(0).toUpperCase() + rawName.slice(1);
  const facultyBranch = userData?.branch || 'CSE';

  const [overview, setOverview] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setLoading(true);
        const data = await api.getFacultyOverview(facultyBranch);
        setOverview(data);
      } catch (err: any) {
        console.warn('Faculty home error:', err.message);
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, [facultyBranch]);

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        
        {/* Welcome Header Banner */}
        <View style={styles.headerCard}>
          <Text style={styles.headerSubtitle}>🏛️ Siddhartha Academy • VRSEC Faculty</Text>
          <Text style={styles.headerTitle}>Welcome, {facultyName} 👋</Text>
          <Text style={styles.headerDesc}>
            Department: <Text style={{ fontWeight: '800', color: '#FFFFFF' }}>{facultyBranch} Engineering</Text> • Monitoring verified <Text style={{ fontWeight: '800', color: '#93C5FD' }}>@vrsec.ac.in</Text> student progression.
          </Text>
        </View>

        {/* Real-time Participation Overview */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>📊 Verified Student Engagement Overview</Text>
          <Text style={styles.cardSub}>
            Real-time tracking across {facultyBranch} academic cohorts. Faculty accounts are strictly isolated.
          </Text>

          {loading ? (
            <ActivityIndicator color={Colors.primary} size="small" style={{ marginVertical: 20 }} />
          ) : (
            <View style={styles.statsRow}>
              <View style={styles.statBox}>
                <Text style={styles.statNumber}>{overview?.totalEligibleStudents || 0}</Text>
                <Text style={styles.statLabel}>Eligible Students</Text>
              </View>

              <View style={styles.statBox}>
                <Text style={styles.statNumber}>{overview?.activeLearners || 0}</Text>
                <Text style={styles.statLabel}>Active Courses</Text>
              </View>

              <View style={[styles.statBox, { borderColor: '#FECACA', backgroundColor: '#FFF5F5' }]}>
                <Text style={[styles.statNumber, { color: Colors.danger }]}>
                  {overview?.studentsNeedingSupportCount || 0}
                </Text>
                <Text style={styles.statLabel}>Needs Attention</Text>
              </View>
            </View>
          )}
        </View>

        {/* Quick Action Navigation Shortcuts */}
        <View style={styles.actionGrid}>
          <TouchableOpacity
            style={styles.actionCard}
            onPress={() => onNavigateTab('Courses')}
            activeOpacity={0.85}
          >
            <Text style={styles.actionIcon}>📋</Text>
            <Text style={styles.actionTitle}>Department Audit</Text>
            <Text style={styles.actionDesc}>Inspect individual @vrsec.ac.in learner progress and assign recommended courses.</Text>
            <Text style={styles.actionLink}>Open Audit &amp; Assign →</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.actionCard}
            onPress={() => onNavigateTab('Chat')}
            activeOpacity={0.85}
          >
            <Text style={styles.actionIcon}>🤖</Text>
            <Text style={styles.actionTitle}>Faculty AI Advisor</Text>
            <Text style={styles.actionDesc}>Ask real-time questions about VRSEC student performance trends and pass rates.</Text>
            <Text style={styles.actionLink}>Launch AI Advisor →</Text>
          </TouchableOpacity>
        </View>

        {/* Instructions Card */}
        <View style={styles.instructionsCard}>
          <Text style={styles.instructTitle}>💡 Faculty Department Guidelines</Text>
          <Text style={styles.instructText}>
            • Use the <Text style={{ fontWeight: '800', color: Colors.primaryDark }}>Courses</Text> tab to assign courses to specific branches and semesters.{'\n'}
            • Only eligible students registered with <Text style={{ fontWeight: '800' }}>@vrsec.ac.in</Text> receive recommendations and appear in department analytics.{'\n'}
            • Students scoring &lt;= 60% on phase quizzes or with CGPA &lt; 6.5 are automatically highlighted under "Needs Attention" for academic intervention.
          </Text>
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
  headerSubtitle: {
    fontSize: 12,
    color: '#93C5FD',
    fontWeight: '800',
    letterSpacing: 0.5,
    marginBottom: 6,
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: '900',
    color: '#FFFFFF',
    marginBottom: 8,
  },
  headerDesc: {
    fontSize: 13,
    color: '#CBD5E1',
    lineHeight: 18,
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
  cardTitle: {
    fontSize: 16,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 4,
  },
  cardSub: {
    fontSize: 12,
    color: Colors.textSecondary,
    lineHeight: 17,
    marginBottom: 16,
  },
  statsRow: {
    flexDirection: 'row',
    gap: 10,
  },
  statBox: {
    flex: 1,
    backgroundColor: Colors.surfaceMuted,
    borderRadius: Radii.lg,
    padding: 12,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  statNumber: {
    fontSize: 22,
    fontWeight: '900',
    color: Colors.primaryDark,
    marginBottom: 2,
  },
  statLabel: {
    fontSize: 11,
    color: Colors.textSecondary,
    fontWeight: '700',
    textAlign: 'center',
  },
  actionGrid: {
    gap: 14,
    marginBottom: 20,
  },
  actionCard: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.xl,
    padding: 20,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  actionIcon: {
    fontSize: 28,
    marginBottom: 8,
  },
  actionTitle: {
    fontSize: 16,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 4,
  },
  actionDesc: {
    fontSize: 13,
    color: Colors.textSecondary,
    lineHeight: 18,
    marginBottom: 12,
  },
  actionLink: {
    fontSize: 13,
    fontWeight: '800',
    color: Colors.primary,
  },
  instructionsCard: {
    backgroundColor: Colors.primaryLight,
    borderRadius: Radii.lg,
    padding: 16,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  instructTitle: {
    fontSize: 13,
    fontWeight: '800',
    color: Colors.primaryDark,
    marginBottom: 6,
  },
  instructText: {
    fontSize: 12,
    color: Colors.primaryDark,
    lineHeight: 18,
  },
});
