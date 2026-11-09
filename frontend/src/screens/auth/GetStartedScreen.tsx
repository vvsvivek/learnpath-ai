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
} from 'react-native';
import { Colors, Shadows, Radii } from '../../styles/theme';
import { WalkingStudentIllustration } from '../../components/Illustrations';

const { width } = Dimensions.get('window');

interface GetStartedScreenProps {
  onGetStarted: () => void;
  onSignIn: () => void;
}

export default function GetStartedScreen({ onGetStarted, onSignIn }: GetStartedScreenProps) {
  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        
        {/* Brand Header */}
        <View style={styles.brandRow}>
          <View style={styles.brandBadge}>
            <Text style={styles.brandBadgeText}>AI</Text>
          </View>
          <Text style={styles.brandTitle}>LearnPath AI</Text>
        </View>

        {/* Hero Vector Illustration: Walking Student with Books */}
        <View style={styles.illustrationWrapper}>
          <WalkingStudentIllustration width={Math.min(width * 0.85, 320)} height={240} />
        </View>

        {/* Value Proposition */}
        <View style={styles.contentSection}>
          <View style={styles.tagPill}>
            <Text style={styles.tagText}>✨ AI-POWERED PERSONALIZED EDUCATION</Text>
          </View>

          <Text style={styles.heroHeading}>
            Master Every Subject At Your Own Pace
          </Text>

          <Text style={styles.heroSubheading}>
            Dynamically generated learning phases, adaptive 10-question quizzes, and individualized university recommendations.
          </Text>

          {/* Feature Highlights Grid */}
          <View style={styles.featureGrid}>
            <View style={styles.featureCard}>
              <Text style={styles.featureIcon}>🎯</Text>
              <Text style={styles.featureTitle}>Adaptive Quizzes</Text>
              <Text style={styles.featureDesc}>10 questions per phase. Score strictly &gt;60% to progress.</Text>
            </View>

            <View style={styles.featureCard}>
              <Text style={styles.featureIcon}>🗺️</Text>
              <Text style={styles.featureTitle}>Dynamic Paths</Text>
              <Text style={styles.featureDesc}>Tailored phases for any custom subject you choose.</Text>
            </View>
          </View>

          {/* Call to Actions */}
          <TouchableOpacity
            style={styles.primaryButton}
            onPress={onGetStarted}
            activeOpacity={0.88}
          >
            <Text style={styles.primaryButtonText}>Get Started Today →</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.secondaryButton}
            onPress={onSignIn}
            activeOpacity={0.7}
          >
            <Text style={styles.secondaryButtonText}>
              Already have an account? <Text style={styles.signInBold}>Sign In</Text>
            </Text>
          </TouchableOpacity>
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
    paddingHorizontal: 24,
    paddingTop: 16,
    paddingBottom: 40,
    alignItems: 'center',
  },
  brandRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 20,
    alignSelf: 'flex-start',
  },
  brandBadge: {
    backgroundColor: Colors.primaryDark,
    borderRadius: Radii.sm,
    paddingHorizontal: 8,
    paddingVertical: 4,
    marginRight: 10,
  },
  brandBadgeText: {
    color: '#FFFFFF',
    fontWeight: '900',
    fontSize: 12,
    letterSpacing: 0.5,
  },
  brandTitle: {
    fontSize: 22,
    fontWeight: '800',
    color: Colors.primaryDark,
    letterSpacing: -0.3,
  },
  illustrationWrapper: {
    marginVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  contentSection: {
    width: '100%',
    alignItems: 'center',
    marginTop: 10,
  },
  tagPill: {
    backgroundColor: Colors.primaryLight,
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: Radii.full,
    marginBottom: 16,
  },
  tagText: {
    color: Colors.primary,
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 0.8,
  },
  heroHeading: {
    fontSize: 28,
    fontWeight: '900',
    color: Colors.textPrimary,
    textAlign: 'center',
    lineHeight: 36,
    marginBottom: 12,
    letterSpacing: -0.5,
  },
  heroSubheading: {
    fontSize: 15,
    color: Colors.textSecondary,
    textAlign: 'center',
    lineHeight: 22,
    marginBottom: 24,
    paddingHorizontal: 8,
  },
  featureGrid: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    width: '100%',
    marginBottom: 28,
    gap: 12,
  },
  featureCard: {
    flex: 1,
    backgroundColor: Colors.surface,
    padding: 16,
    borderRadius: Radii.lg,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  featureIcon: {
    fontSize: 24,
    marginBottom: 8,
  },
  featureTitle: {
    fontSize: 14,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 4,
  },
  featureDesc: {
    fontSize: 12,
    color: Colors.textSecondary,
    lineHeight: 16,
  },
  primaryButton: {
    width: '100%',
    backgroundColor: Colors.primary,
    paddingVertical: 16,
    borderRadius: Radii.lg,
    alignItems: 'center',
    ...Shadows.medium,
    marginBottom: 14,
  },
  primaryButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '800',
    letterSpacing: 0.3,
  },
  secondaryButton: {
    paddingVertical: 8,
  },
  secondaryButtonText: {
    fontSize: 14,
    color: Colors.textSecondary,
  },
  signInBold: {
    color: Colors.primary,
    fontWeight: '800',
  },
});
