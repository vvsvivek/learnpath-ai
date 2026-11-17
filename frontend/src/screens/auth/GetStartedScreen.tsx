import React from 'react';
import {
  StyleSheet,
  Text,
  View,
  SafeAreaView,
  TouchableOpacity,
  StatusBar,
  ScrollView,
  Dimensions,
  Platform,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Colors, Shadows, Radii, Spacing, Typography } from '../../styles/theme';
import { WalkingStudentIllustration } from '../../components/Illustrations';

const { width } = Dimensions.get('window');

interface GetStartedScreenProps {
  onSelectRole: (role: 'STUDENT' | 'FACULTY', mode: 'SIGN_IN' | 'REGISTER') => void;
  onGetStarted?: () => void;
  onSignIn?: () => void;
}

export default function GetStartedScreen({ onSelectRole, onGetStarted, onSignIn }: GetStartedScreenProps) {
  const handleStudentSignIn = () => {
    if (onSelectRole) onSelectRole('STUDENT', 'SIGN_IN');
    else if (onSignIn) onSignIn();
  };

  const handleStudentRegister = () => {
    if (onSelectRole) onSelectRole('STUDENT', 'REGISTER');
    else if (onGetStarted) onGetStarted();
  };

  const handleFacultySignIn = () => {
    if (onSelectRole) onSelectRole('FACULTY', 'SIGN_IN');
    else if (onSignIn) onSignIn();
  };

  const handleFacultyRegister = () => {
    if (onSelectRole) onSelectRole('FACULTY', 'REGISTER');
    else if (onGetStarted) onGetStarted();
  };

  const insets = useSafeAreaInsets();
  const safeTopPadding = Platform.OS === 'android'
    ? Math.max(insets.top, StatusBar.currentHeight || 0, 36) + 12
    : Math.max(insets.top, 12);

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView
        contentContainerStyle={[styles.scroll, { paddingTop: safeTopPadding }]}
        showsVerticalScrollIndicator={false}
      >
        
        {/* Compact Brand Header */}
        <View style={styles.brandRow}>
          <View style={styles.brandBadge}>
            <Text style={styles.brandBadgeText}>AI</Text>
          </View>
          <Text style={styles.brandTitle}>LearnPath AI</Text>
        </View>

        {/* Hero Vector Illustration (Balanced Sizing) */}
        <View style={styles.illustrationWrapper}>
          <WalkingStudentIllustration width={Math.min(width * 0.55, 180)} height={130} />
        </View>

        {/* Value Proposition */}
        <View style={styles.contentSection}>
          <View style={styles.tagPill}>
            <Text style={styles.tagText}>✨ PERSONALIZED AUTONOMOUS LEARNING</Text>
          </View>

          <Text style={styles.heroHeading}>
            Master Every Subject At Your Own Pace
          </Text>

          <Text style={styles.heroSubheading}>
            Dynamic learning phases, adaptive 10-question quizzes, and individualized engineering curricula.
          </Text>

          {/* Compact Feature Highlights Grid */}
          <View style={styles.featureGrid}>
            <View style={styles.featureCard}>
              <Text style={styles.featureIcon}>🎯</Text>
              <Text style={styles.featureTitle}>Adaptive Quizzes</Text>
              <Text style={styles.featureDesc}>10 questions per phase. Score &gt;60% to progress.</Text>
            </View>

            <View style={styles.featureCard}>
              <Text style={styles.featureIcon}>🗺️</Text>
              <Text style={styles.featureTitle}>Dynamic Paths</Text>
              <Text style={styles.featureDesc}>Tailored syllabus phases for approved courses.</Text>
            </View>
          </View>

          {/* Role-Based Authentication Entry Sections */}
          <View style={styles.rolePortalCard}>
            <View style={styles.roleHeaderRow}>
              <Text style={styles.roleBadgeIcon}>🎓</Text>
              <View style={{ flex: 1 }}>
                <Text style={styles.rolePortalTitle}>Student Portal</Text>
                <Text style={styles.rolePortalSubtitle}>Open to all students • Any valid email accepted</Text>
              </View>
            </View>
            <View style={styles.roleButtonGroup}>
              <TouchableOpacity
                style={styles.portalPrimaryBtn}
                onPress={handleStudentSignIn}
                activeOpacity={0.7}
              >
                <Text style={styles.portalPrimaryBtnText}>Student Sign In →</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.portalSecondaryBtn}
                onPress={handleStudentRegister}
                activeOpacity={0.7}
              >
                <Text style={styles.portalSecondaryBtnText}>New Student? Register Account</Text>
              </TouchableOpacity>
            </View>
          </View>

          <View style={[styles.rolePortalCard, styles.facultyPortalCard]}>
            <View style={styles.roleHeaderRow}>
              <Text style={styles.roleBadgeIcon}>🏛️</Text>
              <View style={{ flex: 1 }}>
                <Text style={styles.rolePortalTitle}>Faculty Portal</Text>
                <Text style={styles.rolePortalSubtitle}>Institutional portal • @siddhartha.ac.in</Text>
              </View>
            </View>
            <View style={styles.roleButtonGroup}>
              <TouchableOpacity
                style={[styles.portalPrimaryBtn, { backgroundColor: Colors.primaryDark }]}
                onPress={handleFacultySignIn}
                activeOpacity={0.7}
              >
                <Text style={styles.portalPrimaryBtnText}>Faculty Sign In →</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.portalSecondaryBtn}
                onPress={handleFacultyRegister}
                activeOpacity={0.7}
              >
                <Text style={styles.portalSecondaryBtnText}>New Faculty? Register Access</Text>
              </TouchableOpacity>
            </View>
          </View>
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
    paddingHorizontal: Spacing.md,
    paddingBottom: Spacing.lg,
    alignItems: 'center',
  },
  brandRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: Spacing.sm,
    alignSelf: 'flex-start',
  },
  brandBadge: {
    backgroundColor: Colors.primaryDark,
    borderRadius: Radii.xs,
    paddingHorizontal: 6,
    paddingVertical: 2,
    marginRight: 8,
  },
  brandBadgeText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 10,
    letterSpacing: 0.5,
  },
  brandTitle: {
    fontSize: 17,
    fontWeight: '700',
    color: Colors.primaryDark,
  },
  illustrationWrapper: {
    marginVertical: 4,
    alignItems: 'center',
    justifyContent: 'center',
  },
  contentSection: {
    width: '100%',
    alignItems: 'center',
    marginTop: 4,
  },
  tagPill: {
    backgroundColor: Colors.primaryLight,
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: Radii.full,
    marginBottom: 8,
  },
  tagText: {
    color: Colors.primary,
    fontSize: 9,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  heroHeading: {
    fontSize: 19,
    fontWeight: '700',
    color: Colors.textPrimary,
    textAlign: 'center',
    lineHeight: 25,
    marginBottom: 6,
  },
  heroSubheading: {
    fontSize: 12,
    color: Colors.textSecondary,
    textAlign: 'center',
    lineHeight: 16,
    marginBottom: 14,
    paddingHorizontal: 6,
  },
  featureGrid: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    width: '100%',
    marginBottom: 14,
    gap: 8,
  },
  featureCard: {
    flex: 1,
    backgroundColor: Colors.surface,
    padding: Spacing.sm,
    borderRadius: Radii.md,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  featureIcon: {
    fontSize: 18,
    marginBottom: 4,
  },
  featureTitle: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginBottom: 2,
  },
  featureDesc: {
    fontSize: 10,
    color: Colors.textSecondary,
    lineHeight: 13,
  },
  rolePortalCard: {
    width: '100%',
    backgroundColor: Colors.surface,
    borderRadius: Radii.md,
    padding: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.primary,
    marginBottom: 10,
    ...Shadows.small,
  },
  facultyPortalCard: {
    borderColor: Colors.primaryDark,
  },
  roleHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 10,
    gap: 10,
  },
  roleBadgeIcon: {
    fontSize: 22,
  },
  rolePortalTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  rolePortalSubtitle: {
    fontSize: 10,
    color: Colors.textSecondary,
    marginTop: 1,
  },
  roleButtonGroup: {
    width: '100%',
    gap: 6,
  },
  portalPrimaryBtn: {
    width: '100%',
    backgroundColor: Colors.primary,
    paddingVertical: 9,
    borderRadius: Radii.sm,
    alignItems: 'center',
  },
  portalPrimaryBtnText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '700',
  },
  portalSecondaryBtn: {
    width: '100%',
    paddingVertical: 5,
    alignItems: 'center',
  },
  portalSecondaryBtnText: {
    color: Colors.textSecondary,
    fontSize: 11,
    fontWeight: '600',
  },
});
