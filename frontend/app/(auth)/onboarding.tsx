import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Pressable,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useRouter } from 'expo-router';
import Animated from 'react-native-reanimated';
import { Sparkles, User, Award, BookOpen, Check, ArrowRight } from 'lucide-react-native';
import { useTheme } from '../../src/hooks/useTheme';
import { useAuth } from '../../src/hooks/useAuth';
import { Button, Input, Card } from '../../src/components/ui';
import { ACADEMIC_YEARS, BRANCHES, LEARNING_INTERESTS, SEMESTERS } from '../../src/utils/constants';
import { validateCGPA } from '../../src/utils/validators';
import { authService } from '../../src/services/authService';
import { useFadeIn } from '../../src/animations/microInteractions';

export default function OnboardingScreen() {
  const router = useRouter();
  const { theme, isDark } = useTheme();
  const { user, completeOnboarding } = useAuth();

  const [step, setStep] = useState<1 | 2>(1);
  const [name, setName] = useState(user?.name || '');
  const [year, setYear] = useState<number>(3);
  const [branch, setBranch] = useState<string>('CSE');
  const [semester, setSemester] = useState<number>(5);
  const [cgpa, setCgpa] = useState<string>('8.5');
  const [selectedInterests, setSelectedInterests] = useState<string[]>([
    'Artificial Intelligence & Machine Learning',
    'Data Structures & Algorithms',
  ]);

  const [nameError, setNameError] = useState('');
  const [cgpaError, setCgpaError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const fadeAnim = useFadeIn(50, 300);

  const toggleInterest = (interest: string) => {
    if (selectedInterests.includes(interest)) {
      setSelectedInterests(selectedInterests.filter((i) => i !== interest));
    } else {
      setSelectedInterests([...selectedInterests, interest]);
    }
  };

  const handleNextStep = () => {
    setNameError('');
    setCgpaError('');

    if (!name.trim()) {
      setNameError('Please enter your full name');
      return;
    }

    if (!validateCGPA(cgpa)) {
      setCgpaError('Please enter a valid CGPA between 0.0 and 10.0');
      return;
    }

    setStep(2);
  };

  const handleFinish = async () => {
    if (selectedInterests.length === 0) {
      alert('Please select at least one learning interest');
      return;
    }

    setIsLoading(true);

    const onboardingPayload = {
      name: name.trim(),
      academic_year: year,
      branch,
      semester,
      cgpa: parseFloat(cgpa) || 8.0,
      interests: selectedInterests,
    };

    try {
      await authService.completeOnboarding(onboardingPayload);
      await completeOnboarding(onboardingPayload);
      setIsLoading(false);
      router.replace('/(student)/home');
    } catch (error) {
      // Offline / demo fallback
      await completeOnboarding(onboardingPayload);
      setIsLoading(false);
      router.replace('/(student)/home');
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={[styles.container, { backgroundColor: theme.colors.background }]}
    >
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
      >
        {/* Header */}
        <Animated.View style={[styles.header, fadeAnim]}>
          <View
            style={[
              styles.iconCircle,
              { backgroundColor: theme.colors.primaryLight },
            ]}
          >
            <Sparkles size={28} color={theme.colors.primary} />
          </View>
          <Text style={[styles.title, { color: theme.colors.textPrimary }]}>
            Welcome to LearnPath
          </Text>
          <Text style={[styles.subtitle, { color: theme.colors.textSecondary }]}>
            Let's customize your personalized learning experience
          </Text>

          {/* Step Progress Pills */}
          <View style={styles.stepIndicatorRow}>
            <View
              style={[
                styles.stepPill,
                {
                  backgroundColor: theme.colors.primary,
                  width: step === 1 ? 32 : 12,
                },
              ]}
            />
            <View
              style={[
                styles.stepPill,
                {
                  backgroundColor:
                    step === 2
                      ? theme.colors.primary
                      : isDark
                      ? theme.colors.border
                      : theme.colors.borderLight,
                  width: step === 2 ? 32 : 12,
                },
              ]}
            />
          </View>
        </Animated.View>

        {step === 1 ? (
          /* STEP 1: Academic Profile */
          <Card variant="elevated" style={styles.card}>
            <View style={styles.cardHeader}>
              <Award size={20} color={theme.colors.primary} />
              <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>
                Academic Information
              </Text>
            </View>

            <Input
              label="Full Name"
              placeholder="e.g. Vivek Varanasi"
              value={name}
              onChangeText={(text) => {
                setName(text);
                if (nameError) setNameError('');
              }}
              error={nameError}
              leftIcon={<User size={18} color={theme.colors.textTertiary} />}
            />

            {/* Branch Selection */}
            <Text style={[styles.label, { color: theme.colors.textSecondary }]}>
              Branch of Study
            </Text>
            <View style={styles.chipRow}>
              {BRANCHES.map((b) => (
                <Pressable
                  key={b.value}
                  onPress={() => setBranch(b.value)}
                  style={[
                    styles.chip,
                    {
                      backgroundColor:
                        branch === b.value
                          ? theme.colors.primaryLight
                          : isDark
                          ? theme.colors.surfaceSubtle
                          : theme.colors.inputBackground,
                      borderColor:
                        branch === b.value
                          ? theme.colors.primary
                          : theme.colors.borderLight,
                    },
                  ]}
                >
                  <Text
                    style={[
                      styles.chipText,
                      {
                        color:
                          branch === b.value
                            ? theme.colors.primaryDark
                            : theme.colors.textPrimary,
                        fontWeight: branch === b.value ? '700' : '500',
                      },
                    ]}
                  >
                    {b.value}
                  </Text>
                </Pressable>
              ))}
            </View>

            {/* Academic Year Selection */}
            <Text style={[styles.label, { color: theme.colors.textSecondary }]}>
              Academic Year
            </Text>
            <View style={styles.chipRow}>
              {ACADEMIC_YEARS.map((y) => (
                <Pressable
                  key={y.value}
                  onPress={() => setYear(y.value)}
                  style={[
                    styles.chip,
                    {
                      backgroundColor:
                        year === y.value
                          ? theme.colors.primaryLight
                          : isDark
                          ? theme.colors.surfaceSubtle
                          : theme.colors.inputBackground,
                      borderColor:
                        year === y.value
                          ? theme.colors.primary
                          : theme.colors.borderLight,
                    },
                  ]}
                >
                  <Text
                    style={[
                      styles.chipText,
                      {
                        color:
                          year === y.value
                            ? theme.colors.primaryDark
                            : theme.colors.textPrimary,
                        fontWeight: year === y.value ? '700' : '500',
                      },
                    ]}
                  >
                    {y.label}
                  </Text>
                </Pressable>
              ))}
            </View>

            {/* Semester Selection */}
            <Text style={[styles.label, { color: theme.colors.textSecondary }]}>
              Current Semester
            </Text>
            <View style={styles.chipRow}>
              {SEMESTERS.slice(0, 8).map((s) => (
                <Pressable
                  key={s.value}
                  onPress={() => setSemester(s.value)}
                  style={[
                    styles.chipSmall,
                    {
                      backgroundColor:
                        semester === s.value
                          ? theme.colors.primaryLight
                          : isDark
                          ? theme.colors.surfaceSubtle
                          : theme.colors.inputBackground,
                      borderColor:
                        semester === s.value
                          ? theme.colors.primary
                          : theme.colors.borderLight,
                    },
                  ]}
                >
                  <Text
                    style={[
                      styles.chipText,
                      {
                        color:
                          semester === s.value
                            ? theme.colors.primaryDark
                            : theme.colors.textPrimary,
                        fontWeight: semester === s.value ? '700' : '500',
                      },
                    ]}
                  >
                    Sem {s.value}
                  </Text>
                </Pressable>
              ))}
            </View>

            {/* CGPA */}
            <Input
              label="Current CGPA (Scale 0 - 10)"
              placeholder="e.g. 8.75"
              value={cgpa}
              onChangeText={(text) => {
                setCgpa(text);
                if (cgpaError) setCgpaError('');
              }}
              keyboardType="decimal-pad"
              error={cgpaError}
              hint="Used to tailor course difficulty"
            />

            <Button
              title="Continue to Interests"
              onPress={handleNextStep}
              variant="primary"
              size="lg"
              rightIcon={<ArrowRight size={18} color="#FFFFFF" />}
              style={styles.actionButton}
            />
          </Card>
        ) : (
          /* STEP 2: Learning Interests */
          <Card variant="elevated" style={styles.card}>
            <View style={styles.cardHeader}>
              <BookOpen size={20} color={theme.colors.primary} />
              <Text style={[styles.cardTitle, { color: theme.colors.textPrimary }]}>
                Choose Your Interests
              </Text>
            </View>

            <Text style={[styles.subLabel, { color: theme.colors.textSecondary }]}>
              Select the fields you would like to master. AI will curate your curriculum based on these.
            </Text>

            <View style={styles.interestsGrid}>
              {LEARNING_INTERESTS.map((interest) => {
                const isSelected = selectedInterests.includes(interest);
                return (
                  <Pressable
                    key={interest}
                    onPress={() => toggleInterest(interest)}
                    style={[
                      styles.interestPill,
                      {
                        backgroundColor: isSelected
                          ? theme.colors.primary
                          : isDark
                          ? theme.colors.surfaceSubtle
                          : theme.colors.inputBackground,
                        borderColor: isSelected
                          ? theme.colors.primary
                          : theme.colors.borderLight,
                      },
                    ]}
                  >
                    {isSelected && (
                      <Check size={14} color="#FFFFFF" style={styles.checkIcon} />
                    )}
                    <Text
                      style={[
                        styles.interestText,
                        {
                          color: isSelected ? '#FFFFFF' : theme.colors.textPrimary,
                          fontWeight: isSelected ? '700' : '500',
                        },
                      ]}
                    >
                      {interest}
                    </Text>
                  </Pressable>
                );
              })}
            </View>

            <View style={styles.buttonRow}>
              <Button
                title="Back"
                onPress={() => setStep(1)}
                variant="outline"
                size="lg"
                style={styles.backStepButton}
              />
              <Button
                title="Get Started"
                onPress={handleFinish}
                loading={isLoading}
                variant="primary"
                size="lg"
                style={styles.finishButton}
              />
            </View>
          </Card>
        )}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    paddingHorizontal: 20,
    paddingTop: 50,
    paddingBottom: 40,
  },
  header: {
    alignItems: 'center',
    marginBottom: 24,
  },
  iconCircle: {
    width: 60,
    height: 60,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 14,
  },
  title: {
    fontSize: 24,
    fontWeight: '800',
    marginBottom: 6,
  },
  subtitle: {
    fontSize: 14,
    textAlign: 'center',
    maxWidth: 290,
    marginBottom: 16,
  },
  stepIndicatorRow: {
    flexDirection: 'row',
    gap: 8,
    alignItems: 'center',
  },
  stepPill: {
    height: 6,
    borderRadius: 3,
  },
  card: {
    marginBottom: 20,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 18,
  },
  cardTitle: {
    fontSize: 18,
    fontWeight: '700',
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 8,
    marginTop: 4,
  },
  subLabel: {
    fontSize: 13,
    lineHeight: 18,
    marginBottom: 16,
  },
  chipRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 16,
  },
  chip: {
    paddingVertical: 8,
    paddingHorizontal: 14,
    borderRadius: 10,
    borderWidth: 1,
  },
  chipSmall: {
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 8,
    borderWidth: 1,
  },
  chipText: {
    fontSize: 13,
  },
  interestsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 24,
  },
  interestPill: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 14,
    borderRadius: 12,
    borderWidth: 1,
  },
  checkIcon: {
    marginRight: 6,
  },
  interestText: {
    fontSize: 13,
  },
  actionButton: {
    marginTop: 12,
  },
  buttonRow: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 8,
  },
  backStepButton: {
    flex: 1,
  },
  finishButton: {
    flex: 2,
  },
});
