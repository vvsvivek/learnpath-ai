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
import { Colors, Shadows, Radii, Typography, Spacing } from '../../styles/theme';
import { api } from '../../services/api';

interface FacultyHomeScreenProps {
  userData?: any;
  onNavigateTab: (tab: string) => void;
}

export default function FacultyHomeScreen({ userData, onNavigateTab }: FacultyHomeScreenProps) {
  const rawName = userData?.name || userData?.username || userData?.email?.split('@')[0] || 'Professor';
  const facultyName = rawName.charAt(0).toUpperCase() + rawName.slice(1);
  const facultyBranch = (userData?.branch || 'CSE').toUpperCase();

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

  const slowCount = overview?.slowLearnersCount != null ? overview.slowLearnersCount : (overview?.studentsNeedingSupportCount || 0);
  const fastCount = overview?.fastLearnersCount != null ? overview.fastLearnersCount : 0;
  const consistentCount = overview?.consistentLearnersCount != null ? overview.consistentLearnersCount : 0;

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        
        {/* Welcome Header Banner */}
        <View style={styles.headerCard}>
          <Text style={styles.headerSubtitle}>🏛️ Siddhartha Academy • VRSEC Faculty</Text>
          <Text style={styles.headerTitle}>Welcome, {facultyName} 👋</Text>
          <Text style={styles.headerDesc}>
            Department: <Text style={{ fontWeight: '800', color: '#FFFFFF' }}>{facultyBranch} Engineering</Text> • Exclusively monitoring verified <Text style={{ fontWeight: '800', color: '#93C5FD' }}>@{facultyBranch}</Text> (@vrsec.ac.in) learners.
          </Text>
        </View>

        {/* Real-time Department Cohort Overview */}
        <View style={styles.card}>
          <View style={styles.cardHeaderRow}>
            <View>
              <Text style={styles.cardTitle}>📊 Verified {facultyBranch} Student Engagement</Text>
              <Text style={styles.cardSub}>
                Strict Department Isolation: Only students belonging to {facultyBranch} are shown.
              </Text>
            </View>
          </View>

          {loading ? (
            <ActivityIndicator color={Colors.primary} size="small" style={{ marginVertical: 20 }} />
          ) : (
            <View style={styles.statsRow}>
              <View style={styles.statBox}>
                <Text style={styles.statNumber}>{overview?.totalEligibleStudents || 0}</Text>
                <Text style={styles.statLabel}>Total {facultyBranch}</Text>
              </View>

              <View style={[styles.statBox, { borderColor: '#FECACA', backgroundColor: '#FFF5F5' }]}>
                <Text style={[styles.statNumber, { color: Colors.danger }]}>
                  {slowCount}
                </Text>
                <Text style={styles.statLabel}>⚠️ Slow Learners</Text>
              </View>

              <View style={[styles.statBox, { borderColor: '#A7F3D0', backgroundColor: '#F0FDF4' }]}>
                <Text style={[styles.statNumber, { color: '#059669' }]}>
                  {fastCount}
                </Text>
                <Text style={styles.statLabel}>⭐ Fast Learners</Text>
              </View>

              <View style={styles.statBox}>
                <Text style={styles.statNumber}>{consistentCount}</Text>
                <Text style={styles.statLabel}>📘 Consistent</Text>
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
            <Text style={styles.actionIcon}>📊</Text>
            <Text style={styles.actionTitle}>Student Progress Hub</Text>
            <Text style={styles.actionDesc}>
              Filter Slow Learners, Fast Learners, Year-wise, and Sem-wise with individual audit reports.
            </Text>
            <Text style={styles.actionLink}>Open Progress Hub →</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.actionCard}
            onPress={() => onNavigateTab('Chat')}
            activeOpacity={0.85}
          >
            <Text style={styles.actionIcon}>🤖</Text>
            <Text style={styles.actionTitle}>Faculty AI Advisor</Text>
            <Text style={styles.actionDesc}>Ask real-time questions about {facultyBranch} student performance trends and pass rates.</Text>
            <Text style={styles.actionLink}>Launch AI Advisor →</Text>
          </TouchableOpacity>
        </View>

        {/* Instructions Card */}
        <View style={styles.instructionsCard}>
          <Text style={styles.instructTitle}>💡 Faculty Department Guidelines</Text>
          <Text style={styles.instructText}>
            • <Text style={{ fontWeight: '800', color: Colors.primaryDark }}>Student Progress Hub:</Text> Open the Courses tab to access the multi-parameter filter (Slow Learners, Fast Learners, Year-wise, Semester-wise).{'\n'}
            • <Text style={{ fontWeight: '800', color: Colors.primaryDark }}>Department Privacy:</Text> Faculty members only see students from their own registered department ({facultyBranch}).{'\n'}
            • <Text style={{ fontWeight: '800', color: Colors.primaryDark }}>Automated Remediation:</Text> Students scoring &lt;= 60% or with CGPA &lt; 6.5 are highlighted as Slow Learners so faculty can assign remedial modules directly.
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
    paddingHorizontal: Spacing.lg,
    paddingTop: Spacing.md,
    paddingBottom: Spacing.section,
  },
  headerCard: {
    backgroundColor: Colors.primaryDark,
    borderRadius: Radii.lg,
    padding: Spacing.md,
    marginBottom: Spacing.md,
    ...Shadows.small,
  },
  headerSubtitle: {
    fontSize: Typography.badge.fontSize,
    color: '#93C5FD',
    fontWeight: '700',
    letterSpacing: 0.5,
    marginBottom: 4,
  },
  headerTitle: {
    fontSize: Typography.h2.fontSize,
    fontWeight: '800',
    color: '#FFFFFF',
    marginBottom: 4,
  },
  headerDesc: {
    fontSize: Typography.caption.fontSize,
    color: '#CBD5E1',
    lineHeight: 16,
  },
  card: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.lg,
    padding: Spacing.md,
    marginBottom: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  cardHeaderRow: {
    marginBottom: 8,
  },
  cardTitle: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 2,
  },
  cardSub: {
    fontSize: Typography.caption.fontSize,
    color: Colors.textSecondary,
  },
  statsRow: {
    flexDirection: 'row',
    gap: 6,
    marginTop: 6,
  },
  statBox: {
    flex: 1,
    backgroundColor: Colors.surfaceMuted,
    padding: 8,
    borderRadius: Radii.md,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  statNumber: {
    fontSize: Typography.h3.fontSize,
    fontWeight: '800',
    color: Colors.primaryDark,
  },
  statLabel: {
    fontSize: 9,
    fontWeight: '700',
    color: Colors.textSecondary,
    marginTop: 2,
    textAlign: 'center',
  },
  actionGrid: {
    gap: Spacing.sm,
    marginBottom: Spacing.md,
  },
  actionCard: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.lg,
    padding: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  actionIcon: {
    fontSize: 24,
    marginBottom: 4,
  },
  actionTitle: {
    fontSize: Typography.body.fontSize,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 2,
  },
  actionDesc: {
    fontSize: Typography.caption.fontSize,
    color: Colors.textSecondary,
    marginBottom: 6,
    lineHeight: 15,
  },
  actionLink: {
    fontSize: Typography.caption.fontSize,
    fontWeight: '700',
    color: Colors.primary,
  },
  instructionsCard: {
    backgroundColor: '#F8FAFC',
    borderRadius: Radii.md,
    padding: Spacing.md,
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  instructTitle: {
    fontSize: Typography.caption.fontSize,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 6,
  },
  instructText: {
    fontSize: Typography.caption.fontSize,
    color: Colors.textSecondary,
    lineHeight: 18,
  },
});
