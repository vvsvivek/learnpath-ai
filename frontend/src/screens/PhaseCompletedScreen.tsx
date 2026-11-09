import React from 'react';
import {
  StyleSheet,
  Text,
  View,
  SafeAreaView,
  TouchableOpacity,
  StatusBar,
} from 'react-native';
import { Colors } from '../styles/theme';

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
          <Text style={{ fontSize: 64 }}>🏆</Text>
        </View>

        <Text style={styles.title}>Phase Completed!</Text>
        <Text style={styles.subtitle}>Great job, Vivek!{'\n'}You've completed Phase 1.</Text>

        {/* Score Card */}
        <View style={styles.scoreCard}>
          <Text style={styles.scoreLabel}>⭐ Your Score</Text>
          <Text style={styles.scoreVal}>95%</Text>
        </View>

        {/* Action Buttons */}
        <TouchableOpacity style={styles.primaryBtn} onPress={onContinue}>
          <Text style={styles.primaryBtnText}>Continue to Next Phase →</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.secondaryBtn} onPress={onReview}>
          <Text style={styles.secondaryBtnText}>Review Topics</Text>
        </TouchableOpacity>

      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background, justifyContent: 'center' },
  content: { padding: 24, alignItems: 'center' },
  
  trophyBox: {
    width: 120,
    height: 120,
    borderRadius: 60,
    backgroundColor: Colors.primaryLight,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 24,
    borderWidth: 2,
    borderColor: Colors.border,
  },
  title: { fontSize: 26, fontWeight: '900', color: Colors.textPrimary, marginBottom: 8, textAlign: 'center' },
  subtitle: { fontSize: 14, color: Colors.textSecondary, textAlign: 'center', lineHeight: 22, marginBottom: 28 },

  scoreCard: {
    width: '100%',
    backgroundColor: Colors.cardBackground,
    borderRadius: 24,
    padding: 22,
    alignItems: 'center',
    marginBottom: 28,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  scoreLabel: { fontSize: 13, fontWeight: '700', color: Colors.textSecondary, marginBottom: 4 },
  scoreVal: { fontSize: 36, fontWeight: '900', color: Colors.textPrimary },

  primaryBtn: {
    width: '100%',
    backgroundColor: Colors.primary,
    padding: 18,
    borderRadius: 16,
    alignItems: 'center',
    marginBottom: 14,
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 4,
  },
  primaryBtnText: { color: '#ffffff', fontWeight: '800', fontSize: 16 },

  secondaryBtn: { padding: 12 },
  secondaryBtnText: { color: Colors.primary, fontWeight: '700', fontSize: 15 },
});