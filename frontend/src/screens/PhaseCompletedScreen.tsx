import React from 'react';
import {
  StyleSheet,
  Text,
  View,
  SafeAreaView,
  TouchableOpacity,
  StatusBar,
} from 'react-native';
import { Colors, Shadows, Radii, Typography, Spacing } from '../styles/theme';

interface PhaseCompletedProps {
  onContinue: () => void;
  onReview: () => void;
}

export default function PhaseCompletedScreen({ onContinue, onReview }: PhaseCompletedProps) {
  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <View style={styles.content}>
        
        {/* Trophy Illustration Box */}
        <View style={styles.trophyBox}>
          <Text style={{ fontSize: 36 }}>🏆</Text>
        </View>

        <Text style={styles.title}>Phase Completed!</Text>
        <Text style={styles.subtitle}>Great job!{'\n'}You've completed this learning phase.</Text>

        {/* Score Card */}
        <View style={styles.scoreCard}>
          <Text style={styles.scoreLabel}>⭐ Assessment Score</Text>
          <Text style={styles.scoreVal}>95%</Text>
        </View>

        {/* Action Buttons */}
        <TouchableOpacity style={styles.primaryBtn} onPress={onContinue} activeOpacity={0.85}>
          <Text style={styles.primaryBtnText}>Continue to Next Phase →</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.secondaryBtn} onPress={onReview} activeOpacity={0.7}>
          <Text style={styles.secondaryBtnText}>Review Topics</Text>
        </TouchableOpacity>

      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background, justifyContent: 'center' },
  content: { padding: Spacing.xl, alignItems: 'center' },
  
  trophyBox: {
    width: 72,
    height: 72,
    borderRadius: Radii.full,
    backgroundColor: Colors.primaryLight,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  title: { fontSize: Typography.h1.fontSize, fontWeight: '800', color: Colors.textPrimary, marginBottom: 4, textAlign: 'center' },
  subtitle: { fontSize: Typography.bodySmall.fontSize, color: Colors.textSecondary, textAlign: 'center', lineHeight: 16, marginBottom: Spacing.lg },

  scoreCard: {
    width: '100%',
    backgroundColor: Colors.surface,
    borderRadius: Radii.lg,
    padding: Spacing.md,
    alignItems: 'center',
    marginBottom: Spacing.lg,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  scoreLabel: { fontSize: Typography.caption.fontSize, fontWeight: '700', color: Colors.textSecondary, marginBottom: 2 },
  scoreVal: { fontSize: Typography.hero.fontSize, fontWeight: '800', color: Colors.primaryDark },

  primaryBtn: {
    width: '100%',
    backgroundColor: Colors.primary,
    paddingVertical: 10,
    borderRadius: Radii.md,
    alignItems: 'center',
    marginBottom: Spacing.sm,
    ...Shadows.small,
  },
  primaryBtnText: { color: '#FFFFFF', fontWeight: '700', fontSize: Typography.body.fontSize },

  secondaryBtn: { paddingVertical: 8, alignItems: 'center' },
  secondaryBtnText: { color: Colors.primary, fontWeight: '700', fontSize: Typography.bodySmall.fontSize },
});
