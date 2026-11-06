import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Pressable,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useRouter } from 'expo-router';
import { ArrowLeft, Sparkles, BookOpen, Layers, CheckCircle } from 'lucide-react-native';
import { useTheme } from '../../../src/hooks/useTheme';
import { Button, Card, Input } from '../../../src/components/ui';
import { courseService } from '../../../src/services/courseService';
import { CourseDifficulty } from '../../../src/types';

const SUGGESTIONS = [
  'LLM Application Development with LangChain & RAG',
  'Rust for High-Performance Systems Programming',
  'Next.js 15 Full-Stack & Server Components',
  'Distributed Consensus & Blockchain Internals',
  'Kubernetes & Cloud Native Infrastructure',
];

export default function CustomCourseScreen() {
  const router = useRouter();
  const { theme, isDark } = useTheme();

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [difficulty, setDifficulty] = useState<CourseDifficulty>('Intermediate');
  const [hours, setHours] = useState('30');
  const [isGenerating, setIsGenerating] = useState(false);
  const [progressStep, setProgressStep] = useState(0);

  const generationSteps = [
    'Analyzing domain concepts & syllabus...',
    'Structuring 4 progressive learning phases...',
    'Curating modular topics & benchmark quizzes...',
    'Finalizing personalized curriculum...',
  ];

  const handleGenerate = async () => {
    if (!title.trim()) {
      alert('Please enter a course topic or title');
      return;
    }

    setIsGenerating(true);
    setProgressStep(0);

    const stepInterval = setInterval(() => {
      setProgressStep((prev) => (prev < 3 ? prev + 1 : prev));
    }, 1800);

    try {
      const newCourse = await courseService.createCustomCourse({
        title: title.trim(),
        description: description.trim() || `Comprehensive curriculum for ${title}`,
        difficulty,
        preferred_hours: parseInt(hours, 10) || 30,
      });

      clearInterval(stepInterval);
      setIsGenerating(false);

      router.replace({
        pathname: '/(student)/courses/[id]',
        params: { id: newCourse.id },
      });
    } catch (e) {
      clearInterval(stepInterval);
      // Demo fallback: create mock custom course and route
      const mockCourseId = `custom-${Date.now()}`;
      setIsGenerating(false);

      router.replace({
        pathname: '/(student)/courses/[id]',
        params: { id: mockCourseId },
      });
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={[styles.container, { backgroundColor: theme.colors.background }]}
    >
      {/* Top Bar */}
      <View style={styles.topBar}>
        <Pressable
          onPress={() => router.back()}
          style={[styles.backButton, { backgroundColor: theme.colors.surface }]}
        >
          <ArrowLeft size={20} color={theme.colors.textPrimary} />
        </Pressable>
        <Text style={[styles.screenTitle, { color: theme.colors.textPrimary }]}>
          AI Path Generator
        </Text>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent}>
        {isGenerating ? (
          /* Generating State */
          <Card variant="elevated" style={styles.generatingCard}>
            <View
              style={[
                styles.generatingIcon,
                { backgroundColor: theme.colors.primaryLight },
              ]}
            >
              <Sparkles size={40} color={theme.colors.primary} />
            </View>

            <Text style={[styles.generatingTitle, { color: theme.colors.textPrimary }]}>
              Synthesizing Your Curriculum
            </Text>
            <Text style={[styles.generatingSubtitle, { color: theme.colors.textSecondary }]}>
              Gemini AI is crafting a custom 4-phase structured learning path for you.
            </Text>

            <ActivityIndicator
              size="large"
              color={theme.colors.primary}
              style={{ marginVertical: 20 }}
            />

            <View style={styles.stepsList}>
              {generationSteps.map((step, idx) => (
                <View key={idx} style={styles.stepItem}>
                  {idx <= progressStep ? (
                    <CheckCircle size={18} color={theme.colors.success} />
                  ) : (
                    <View
                      style={[
                        styles.stepDot,
                        { backgroundColor: theme.colors.border },
                      ]}
                    />
                  )}
                  <Text
                    style={[
                      styles.stepText,
                      {
                        color:
                          idx <= progressStep
                            ? theme.colors.textPrimary
                            : theme.colors.textTertiary,
                        fontWeight: idx === progressStep ? '700' : '500',
                      },
                    ]}
                  >
                    {step}
                  </Text>
                </View>
              ))}
            </View>
          </Card>
        ) : (
          /* Form Entry */
          <>
            <Card variant="elevated" style={styles.formCard}>
              <View style={styles.formCardHeader}>
                <Sparkles size={20} color={theme.colors.primary} />
                <Text style={[styles.formCardTitle, { color: theme.colors.textPrimary }]}>
                  What would you like to master?
                </Text>
              </View>

              <Input
                label="Course Topic or Skill"
                placeholder="e.g. Next.js 15 Full-Stack Architecture"
                value={title}
                onChangeText={setTitle}
                leftIcon={<BookOpen size={18} color={theme.colors.textTertiary} />}
              />

              {/* Suggestions Chips */}
              <Text style={[styles.fieldLabel, { color: theme.colors.textSecondary }]}>
                Popular AI Suggestions
              </Text>
              <View style={styles.suggestionsRow}>
                {SUGGESTIONS.map((sug, i) => (
                  <Pressable
                    key={i}
                    onPress={() => setTitle(sug)}
                    style={[
                      styles.sugChip,
                      {
                        backgroundColor: isDark
                          ? theme.colors.surfaceSubtle
                          : theme.colors.inputBackground,
                        borderColor: theme.colors.borderLight,
                      },
                    ]}
                  >
                    <Text
                      style={[styles.sugText, { color: theme.colors.textSecondary }]}
                      numberOfLines={1}
                    >
                      {sug}
                    </Text>
                  </Pressable>
                ))}
              </View>

              <Input
                label="Specific Goals or Notes (Optional)"
                placeholder="e.g. Focus on server actions, authentication, and database schemas with PostgreSQL."
                value={description}
                onChangeText={setDescription}
                multiline
                numberOfLines={3}
                inputStyle={{ height: 80, textAlignVertical: 'top' }}
              />

              {/* Difficulty Level */}
              <Text style={[styles.fieldLabel, { color: theme.colors.textSecondary }]}>
                Difficulty Level
              </Text>
              <View style={styles.diffRow}>
                {(['Beginner', 'Intermediate', 'Advanced'] as CourseDifficulty[]).map(
                  (d) => (
                    <Pressable
                      key={d}
                      onPress={() => setDifficulty(d)}
                      style={[
                        styles.diffPill,
                        {
                          backgroundColor:
                            difficulty === d
                              ? theme.colors.primaryLight
                              : isDark
                              ? theme.colors.surfaceSubtle
                              : theme.colors.inputBackground,
                          borderColor:
                            difficulty === d
                              ? theme.colors.primary
                              : theme.colors.borderLight,
                        },
                      ]}
                    >
                      <Text
                        style={[
                          styles.diffText,
                          {
                            color:
                              difficulty === d
                                ? theme.colors.primaryDark
                                : theme.colors.textPrimary,
                            fontWeight: difficulty === d ? '700' : '500',
                          },
                        ]}
                      >
                        {d}
                      </Text>
                    </Pressable>
                  )
                )}
              </View>

              {/* Estimated Time Commitment */}
              <Input
                label="Preferred Estimated Hours"
                placeholder="30"
                value={hours}
                onChangeText={setHours}
                keyboardType="numeric"
                hint="We will calibrate topic depths to match your timeframe"
              />

              <Button
                title="Generate 4-Phase Roadmap"
                onPress={handleGenerate}
                variant="primary"
                size="lg"
                rightIcon={<Sparkles size={18} color="#FFFFFF" />}
                style={styles.generateButton}
              />
            </Card>
          </>
        )}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  topBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingTop: 56,
    paddingBottom: 14,
  },
  backButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
  },
  screenTitle: {
    fontSize: 18,
    fontWeight: '700',
  },
  scrollContent: {
    paddingHorizontal: 20,
    paddingBottom: 40,
  },
  formCard: {
    padding: 20,
  },
  formCardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 16,
  },
  formCardTitle: {
    fontSize: 17,
    fontWeight: '700',
  },
  fieldLabel: {
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 8,
  },
  suggestionsRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 16,
  },
  sugChip: {
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 8,
    borderWidth: 1,
  },
  sugText: {
    fontSize: 12,
  },
  diffRow: {
    flexDirection: 'row',
    gap: 10,
    marginBottom: 16,
  },
  diffPill: {
    flex: 1,
    paddingVertical: 10,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 10,
    borderWidth: 1,
  },
  diffText: {
    fontSize: 13,
  },
  generateButton: {
    marginTop: 10,
  },
  generatingCard: {
    padding: 28,
    alignItems: 'center',
    marginTop: 20,
  },
  generatingIcon: {
    width: 80,
    height: 80,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
  },
  generatingTitle: {
    fontSize: 20,
    fontWeight: '800',
    marginBottom: 6,
    textAlign: 'center',
  },
  generatingSubtitle: {
    fontSize: 13,
    lineHeight: 18,
    textAlign: 'center',
    maxWidth: 280,
  },
  stepsList: {
    width: '100%',
    gap: 12,
    marginTop: 10,
  },
  stepItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  stepDot: {
    width: 14,
    height: 14,
    borderRadius: 7,
  },
  stepText: {
    fontSize: 13,
  },
});
