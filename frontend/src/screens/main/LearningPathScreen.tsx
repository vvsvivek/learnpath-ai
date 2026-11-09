import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  SafeAreaView,
  TouchableOpacity,
  ActivityIndicator,
  Modal,
  Alert,
  StatusBar,
  Dimensions,
} from 'react-native';
import { Colors, Shadows, Radii } from '../../styles/theme';
import { api } from '../../services/api';

const { width } = Dimensions.get('window');

interface LearningPathScreenProps {
  userData: any;
  activeSubject: string;
  onSelectSubject: (subject: string) => void;
  onCompletePhase?: (subject: string, phaseId: number) => void;
}

export default function LearningPathScreen({
  userData,
  activeSubject,
  onSelectSubject,
  onCompletePhase,
}: LearningPathScreenProps) {
  const email = userData?.email || '';

  const [courseList, setCourseList] = useState<any[]>([]);
  const [selectedCourse, setSelectedCourse] = useState<any>(null);
  const [loadingCourses, setLoadingCourses] = useState(true);

  // Active Phase Reading Material Viewer State
  const [activePhaseIndex, setActivePhaseIndex] = useState(0);

  // 10-Question Quiz Assessment Modal State
  const [quizModalVisible, setQuizModalVisible] = useState(false);
  const [quizLoading, setQuizLoading] = useState(false);
  const [quizQuestions, setQuizQuestions] = useState<any[]>([]);
  const [currentQuestionIdx, setCurrentQuestionIdx] = useState(0);
  const [selectedAnswers, setSelectedAnswers] = useState<Record<number, string>>({}); // { questionId: 'A' | 'B' | 'C' | 'D' }

  // Post-Quiz Submission Result State
  const [quizSubmitting, setQuizSubmitting] = useState(false);
  const [quizResult, setQuizResult] = useState<any>(null);
  const [showResultModal, setShowResultModal] = useState(false);

  // Fetch student courses and phases
  const fetchCoursesAndPhases = async () => {
    if (!email) return;
    try {
      setLoadingCourses(true);
      const dash = await api.getDashboard(email);
      const courses = dash?.activeCourses || [];
      setCourseList(courses);

      if (courses.length > 0) {
        const match = courses.find((c: any) => c.title.toLowerCase() === activeSubject.toLowerCase()) || courses[0];
        setSelectedCourse(match);
        onSelectSubject(match.title);
      }
    } catch (err: any) {
      console.warn('Failed to load courses:', err.message);
    } finally {
      setLoadingCourses(false);
    }
  };

  useEffect(() => {
    fetchCoursesAndPhases();
  }, [email, activeSubject]);

  // Start 10-Question Quiz for a Phase
  const handleLaunchQuiz = async (phaseNumber: number) => {
    if (!selectedCourse) return;
    try {
      setQuizLoading(true);
      setQuizResult(null);
      setSelectedAnswers({});
      setCurrentQuestionIdx(0);
      setQuizModalVisible(true);

      const res = await api.generateQuiz(selectedCourse.id, selectedCourse.title, phaseNumber);
      if (res && Array.isArray(res.questions) && res.questions.length > 0) {
        setQuizQuestions(res.questions);
      } else {
        Alert.alert('Quiz Generation', 'Unable to load 10 questions for this phase.');
        setQuizModalVisible(false);
      }
    } catch (err: any) {
      Alert.alert('Assessment Error', err.message || 'Failed to start phase quiz.');
      setQuizModalVisible(false);
    } finally {
      setQuizLoading(false);
    }
  };

  const handleSelectOption = (questionId: number, optionLetter: string) => {
    setSelectedAnswers((prev) => ({ ...prev, [questionId]: optionLetter }));
  };

  const handleSubmitQuiz = async () => {
    const answeredCount = Object.keys(selectedAnswers).length;
    if (answeredCount < quizQuestions.length) {
      Alert.alert(
        'Incomplete Assessment',
        `You have answered ${answeredCount} of ${quizQuestions.length} questions. Please answer all questions before submitting.`
      );
      return;
    }

    try {
      setQuizSubmitting(true);
      const phaseNum = activePhaseIndex + 1;
      const res = await api.submitQuiz({
        email,
        courseId: selectedCourse.id,
        subject: selectedCourse.title,
        phaseNumber: phaseNum,
        answers: selectedAnswers,
      });

      setQuizResult(res);
      setQuizModalVisible(false);
      setShowResultModal(true);

      // Refresh course roadmap to update unlocked/completed status from backend
      fetchCoursesAndPhases();
      if (res.passed && onCompletePhase) {
        onCompletePhase(selectedCourse.title, phaseNum);
      }
    } catch (err: any) {
      Alert.alert('Submission Error', err.message || 'Failed to submit quiz.');
    } finally {
      setQuizSubmitting(false);
    }
  };

  const phases = selectedCourse?.phases || [];
  const activePhase = phases[activePhaseIndex] || phases[0];

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />

      {/* Top Course Selection Ribbon */}
      <View style={styles.ribbonContainer}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.ribbonScroll}>
          {courseList.map((c: any, idx: number) => {
            const isSelected = selectedCourse?.id === c.id;
            return (
              <TouchableOpacity
                key={idx}
                style={[styles.ribbonPill, isSelected && styles.ribbonPillActive]}
                onPress={() => {
                  setSelectedCourse(c);
                  onSelectSubject(c.title);
                  setActivePhaseIndex(0);
                }}
              >
                <Text style={[styles.ribbonPillText, isSelected && styles.ribbonPillTextActive]}>
                  {c.title}
                </Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>
      </View>

      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        
        {loadingCourses ? (
          <ActivityIndicator color={Colors.primary} size="large" style={{ marginTop: 40 }} />
        ) : !selectedCourse ? (
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyTitle}>No Active Course Selected</Text>
            <Text style={styles.emptySubtitle}>Go to the Dashboard to select or generate a course.</Text>
          </View>
        ) : (
          <>
            {/* Course Overview Banner */}
            <View style={styles.bannerCard}>
              <View style={styles.bannerHeader}>
                <Text style={styles.bannerSubjectTag}>DYNAMIC LEARNING PATH</Text>
                <Text style={styles.bannerProgressText}>{selectedCourse.progress || 0}% Completed</Text>
              </View>
              <Text style={styles.bannerTitle}>{selectedCourse.title}</Text>
              <Text style={styles.bannerDesc}>
                {selectedCourse.description || 'Structured multi-phase curriculum tailored to your academic progression.'}
              </Text>

              {/* Progress Bar */}
              <View style={styles.progressTrack}>
                <View style={[styles.progressFill, { width: `${Math.min(selectedCourse.progress || 0, 100)}%` }]} />
              </View>
            </View>

            {/* Dynamic Phases Horizontal Stepper */}
            <View style={styles.stepperContainer}>
              <Text style={styles.stepperTitle}>Learning Phases ({phases.length})</Text>
              <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.stepperScroll}>
                {phases.map((p: any, idx: number) => {
                  const isSelected = activePhaseIndex === idx;
                  const isCompleted = p.isCompleted;
                  const isUnlocked = p.isUnlocked || idx === 0 || isCompleted;

                  return (
                    <TouchableOpacity
                      key={idx}
                      style={[
                        styles.phaseStepCard,
                        isSelected && styles.phaseStepCardSelected,
                        !isUnlocked && styles.phaseStepCardLocked,
                      ]}
                      onPress={() => {
                        if (!isUnlocked) {
                          Alert.alert('Phase Locked', `You must score strictly greater than 60% on Phase ${idx}'s quiz to unlock this phase.`);
                          return;
                        }
                        setActivePhaseIndex(idx);
                      }}
                      activeOpacity={0.8}
                    >
                      <View style={styles.stepBadge}>
                        <Text style={styles.stepBadgeText}>
                          {isCompleted ? '✓' : isUnlocked ? `${idx + 1}` : '🔒'}
                        </Text>
                      </View>
                      <Text style={[styles.stepTitleText, isSelected && styles.stepTitleTextSelected]} numberOfLines={2}>
                        {p.title}
                      </Text>
                      <Text style={styles.stepStatusLabel}>
                        {isCompleted ? 'Completed' : isUnlocked ? 'Unlocked' : 'Locked'}
                      </Text>
                    </TouchableOpacity>
                  );
                })}
              </ScrollView>
            </View>

            {/* Active Phase Study Material Card */}
            {activePhase && (
              <View style={styles.studyCard}>
                <View style={styles.studyHeader}>
                  <View style={styles.phasePill}>
                    <Text style={styles.phasePillText}>PHASE {activePhase.phaseNumber || activePhaseIndex + 1}</Text>
                  </View>
                  <Text style={styles.durationBadge}>⏱️ {activePhase.duration || '2-3 Hours'}</Text>
                </View>

                <Text style={styles.activePhaseTitle}>{activePhase.title}</Text>
                
                <View style={styles.objectiveBox}>
                  <Text style={styles.objectiveTitle}>🎯 Learning Objectives</Text>
                  <Text style={styles.objectiveText}>
                    {activePhase.objectives || 'Master foundational paradigms and solve practical challenges.'}
                  </Text>
                </View>

                {/* Detailed Study Content */}
                <View style={styles.contentBox}>
                  <Text style={styles.contentBoxTitle}>📖 Study Material &amp; Concepts</Text>
                  <Text style={styles.studyMaterialText}>
                    {activePhase.studyMaterial || 'Comprehensive curriculum materials for this phase.'}
                  </Text>
                </View>

                {/* Phase Action Buttons */}
                <View style={styles.actionRow}>
                  <TouchableOpacity
                    style={[
                      styles.takeQuizBtn,
                      (!activePhase.isUnlocked && activePhaseIndex !== 0) && styles.takeQuizBtnDisabled,
                    ]}
                    onPress={() => handleLaunchQuiz(activePhase.phaseNumber || activePhaseIndex + 1)}
                    disabled={!activePhase.isUnlocked && activePhaseIndex !== 0}
                  >
                    <Text style={styles.takeQuizBtnText}>
                      📝 Take Phase {activePhase.phaseNumber || activePhaseIndex + 1} Quiz (10 Questions) →
                    </Text>
                  </TouchableOpacity>
                </View>
              </View>
            )}
          </>
        )}

      </ScrollView>

      {/* =========================================================================
          10-QUESTION QUIZ MODAL
      ========================================================================= */}
      <Modal visible={quizModalVisible} animationType="slide" transparent>
        <SafeAreaView style={styles.modalOverlay}>
          <View style={styles.quizModalContent}>
            
            {/* Modal Header */}
            <View style={styles.quizHeader}>
              <View>
                <Text style={styles.quizHeaderSub}>ASSESSMENT IN PROGRESS</Text>
                <Text style={styles.quizHeaderTitle}>
                  Question {currentQuestionIdx + 1} of {quizQuestions.length}
                </Text>
              </View>
              <TouchableOpacity
                onPress={() => {
                  Alert.alert('Exit Quiz', 'Are you sure you want to exit? Your answers will not be saved.', [
                    { text: 'Cancel', style: 'cancel' },
                    { text: 'Exit', style: 'destructive', onPress: () => setQuizModalVisible(false) },
                  ]);
                }}
              >
                <Text style={styles.closeBtnText}>✕ Close</Text>
              </TouchableOpacity>
            </View>

            {/* Quiz Question Progress Indicator */}
            <View style={styles.quizProgressTrack}>
              <View
                style={[
                  styles.quizProgressFill,
                  { width: `${((currentQuestionIdx + 1) / Math.max(quizQuestions.length, 1)) * 100}%` },
                ]}
              />
            </View>

            {quizLoading ? (
              <ActivityIndicator color={Colors.primary} size="large" style={{ marginVertical: 60 }} />
            ) : quizQuestions.length === 0 ? (
              <Text style={styles.emptyText}>No questions generated for this phase.</Text>
            ) : (
              <ScrollView style={{ flex: 1 }} showsVerticalScrollIndicator={false}>
                {(() => {
                  const q = quizQuestions[currentQuestionIdx];
                  if (!q) return null;
                  const currentAnswer = selectedAnswers[q.id];
                  const letters = ['A', 'B', 'C', 'D'];

                  return (
                    <View style={styles.questionCard}>
                      <Text style={styles.questionText}>{q.questionText}</Text>

                      {/* 4 Selectable Option Cards */}
                      {Array.isArray(q.options) &&
                        q.options.map((opt: string, optIdx: number) => {
                          const letter = letters[optIdx];
                          const isSelected = currentAnswer === letter;

                          return (
                            <TouchableOpacity
                              key={optIdx}
                              style={[styles.optionCard, isSelected && styles.optionCardSelected]}
                              onPress={() => handleSelectOption(q.id, letter)}
                              activeOpacity={0.8}
                            >
                              <View style={[styles.optionLetterBadge, isSelected && styles.optionLetterBadgeSelected]}>
                                <Text style={[styles.optionLetterText, isSelected && styles.optionLetterTextSelected]}>
                                  {letter}
                                </Text>
                              </View>
                              <Text style={[styles.optionContentText, isSelected && styles.optionContentTextSelected]}>
                                {opt}
                              </Text>
                            </TouchableOpacity>
                          );
                        })}
                    </View>
                  );
                })()}
              </ScrollView>
            )}

            {/* Quiz Navigation Buttons (Prev / Next / Submit) */}
            <View style={styles.quizNavRow}>
              <TouchableOpacity
                style={[styles.navBtn, currentQuestionIdx === 0 && styles.navBtnDisabled]}
                onPress={() => setCurrentQuestionIdx((p) => Math.max(p - 1, 0))}
                disabled={currentQuestionIdx === 0}
              >
                <Text style={styles.navBtnText}>← Previous</Text>
              </TouchableOpacity>

              {currentQuestionIdx < quizQuestions.length - 1 ? (
                <TouchableOpacity
                  style={[styles.navBtn, styles.navBtnPrimary]}
                  onPress={() => setCurrentQuestionIdx((p) => Math.min(p + 1, quizQuestions.length - 1))}
                >
                  <Text style={styles.navBtnPrimaryText}>Next Question →</Text>
                </TouchableOpacity>
              ) : (
                <TouchableOpacity
                  style={[styles.navBtn, styles.submitBtn]}
                  onPress={handleSubmitQuiz}
                  disabled={quizSubmitting}
                >
                  {quizSubmitting ? (
                    <ActivityIndicator color="#FFFFFF" size="small" />
                  ) : (
                    <Text style={styles.submitBtnText}>Submit Assessment ✓</Text>
                  )}
                </TouchableOpacity>
              )}
            </View>

          </View>
        </SafeAreaView>
      </Modal>

      {/* =========================================================================
          POST-QUIZ RESULT MODAL (STRICTLY > 60% UNLOCKING FEEDBACK)
      ========================================================================= */}
      <Modal visible={showResultModal} animationType="fade" transparent>
        <SafeAreaView style={styles.modalOverlay}>
          <View style={styles.resultCard}>
            
            <View style={[styles.resultIconCircle, quizResult?.passed ? styles.iconPassed : styles.iconFailed]}>
              <Text style={{ fontSize: 40 }}>{quizResult?.passed ? '🎉' : '⚠️'}</Text>
            </View>

            <Text style={styles.resultTitle}>
              {quizResult?.passed ? 'Phase Assessment Passed!' : 'Assessment Incomplete'}
            </Text>

            <View style={styles.scoreRow}>
              <Text style={styles.scoreBigText}>
                {quizResult?.score} / {quizResult?.totalQuestions || 10}
              </Text>
              <Text style={[styles.pctBadge, quizResult?.passed ? styles.pctPassed : styles.pctFailed]}>
                {quizResult?.percentage}%
              </Text>
            </View>

            <Text style={styles.resultRuleExplanation}>
              {quizResult?.passed
                ? 'Score strictly > 60% achieved (7/10 or higher). The next learning phase has been unlocked automatically!'
                : 'Progression rule: A score strictly greater than 60% (at least 7/10) is required to unlock the next phase. Review the concepts below and retake the quiz.'}
            </Text>

            {/* Incorrect Concepts Review */}
            {quizResult?.review && (
              <ScrollView style={styles.reviewScroll} showsVerticalScrollIndicator={false}>
                <Text style={styles.reviewHeader}>Question Analysis &amp; Explanations:</Text>
                {quizResult.review.map((item: any, rIdx: number) => (
                  <View key={rIdx} style={styles.reviewItem}>
                    <Text style={styles.reviewQuestionText}>
                      Q{rIdx + 1}: {item.questionText}
                    </Text>
                    <Text style={item.isCorrect ? styles.correctReview : styles.wrongReview}>
                      Your Answer: {item.selectedOption || 'None'} {item.isCorrect ? '✓' : `✗ (Correct: ${item.correctOption})`}
                    </Text>
                    <Text style={styles.explanationText}>💡 {item.explanation}</Text>
                  </View>
                ))}
              </ScrollView>
            )}

            {/* Action Buttons */}
            <TouchableOpacity
              style={styles.resultDoneBtn}
              onPress={() => setShowResultModal(false)}
            >
              <Text style={styles.resultDoneBtnText}>
                {quizResult?.passed ? 'Continue Learning →' : 'Return to Study Material'}
              </Text>
            </TouchableOpacity>

          </View>
        </SafeAreaView>
      </Modal>

    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  ribbonContainer: {
    backgroundColor: Colors.surface,
    borderBottomWidth: 1,
    borderColor: Colors.border,
    paddingVertical: 10,
  },
  ribbonScroll: {
    paddingHorizontal: 16,
    gap: 10,
  },
  ribbonPill: {
    backgroundColor: Colors.surfaceMuted,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: Radii.full,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  ribbonPillActive: {
    backgroundColor: Colors.primaryDark,
    borderColor: Colors.primaryDark,
  },
  ribbonPillText: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.textSecondary,
  },
  ribbonPillTextActive: {
    color: '#FFFFFF',
  },
  scroll: {
    paddingHorizontal: 18,
    paddingTop: 16,
    paddingBottom: 110,
  },
  bannerCard: {
    backgroundColor: Colors.primaryDark,
    borderRadius: Radii.xl,
    padding: 22,
    marginBottom: 20,
    ...Shadows.medium,
  },
  bannerHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  bannerSubjectTag: {
    fontSize: 11,
    fontWeight: '800',
    color: '#93C5FD',
    letterSpacing: 0.8,
  },
  bannerProgressText: {
    fontSize: 13,
    fontWeight: '800',
    color: '#FFFFFF',
  },
  bannerTitle: {
    fontSize: 22,
    fontWeight: '900',
    color: '#FFFFFF',
    marginBottom: 6,
  },
  bannerDesc: {
    fontSize: 13,
    color: '#E2E8F0',
    lineHeight: 18,
    marginBottom: 16,
  },
  progressTrack: {
    height: 6,
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
    borderRadius: Radii.full,
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    backgroundColor: Colors.primary,
    borderRadius: Radii.full,
  },
  stepperContainer: {
    marginBottom: 20,
  },
  stepperTitle: {
    fontSize: 16,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 12,
  },
  stepperScroll: {
    gap: 12,
  },
  phaseStepCard: {
    width: 140,
    backgroundColor: Colors.surface,
    borderRadius: Radii.lg,
    padding: 14,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  phaseStepCardSelected: {
    borderColor: Colors.primary,
    borderWidth: 2,
    backgroundColor: Colors.primarySubtle,
  },
  phaseStepCardLocked: {
    opacity: 0.6,
    backgroundColor: '#F1F5F9',
  },
  stepBadge: {
    width: 28,
    height: 28,
    borderRadius: Radii.full,
    backgroundColor: Colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  stepBadgeText: {
    fontSize: 12,
    fontWeight: '800',
    color: Colors.primaryDark,
  },
  stepTitleText: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.textPrimary,
    lineHeight: 17,
    marginBottom: 6,
  },
  stepTitleTextSelected: {
    color: Colors.primaryDark,
    fontWeight: '800',
  },
  stepStatusLabel: {
    fontSize: 11,
    fontWeight: '600',
    color: Colors.textSecondary,
  },
  studyCard: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.xl,
    padding: 22,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  studyHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  phasePill: {
    backgroundColor: Colors.primaryLight,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: Radii.full,
  },
  phasePillText: {
    color: Colors.primaryDark,
    fontSize: 11,
    fontWeight: '800',
  },
  durationBadge: {
    fontSize: 12,
    color: Colors.textSecondary,
    fontWeight: '600',
  },
  activePhaseTitle: {
    fontSize: 20,
    fontWeight: '900',
    color: Colors.textPrimary,
    marginBottom: 14,
  },
  objectiveBox: {
    backgroundColor: Colors.primarySubtle,
    borderLeftWidth: 3,
    borderLeftColor: Colors.primary,
    padding: 12,
    borderRadius: Radii.sm,
    marginBottom: 16,
  },
  objectiveTitle: {
    fontSize: 13,
    fontWeight: '800',
    color: Colors.primaryDark,
    marginBottom: 4,
  },
  objectiveText: {
    fontSize: 13,
    color: Colors.textPrimary,
    lineHeight: 18,
  },
  contentBox: {
    marginBottom: 20,
  },
  contentBoxTitle: {
    fontSize: 15,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 8,
  },
  studyMaterialText: {
    fontSize: 14,
    color: Colors.textPrimary,
    lineHeight: 22,
  },
  actionRow: {
    marginTop: 10,
  },
  takeQuizBtn: {
    backgroundColor: Colors.primary,
    paddingVertical: 16,
    borderRadius: Radii.lg,
    alignItems: 'center',
    ...Shadows.small,
  },
  takeQuizBtnDisabled: {
    backgroundColor: Colors.textTertiary,
  },
  takeQuizBtnText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 14,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.7)',
    justifyContent: 'center',
    padding: 16,
  },
  quizModalContent: {
    flex: 1,
    backgroundColor: Colors.surface,
    borderRadius: Radii.xl,
    padding: 20,
    maxHeight: '94%',
  },
  quizHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  quizHeaderSub: {
    fontSize: 11,
    fontWeight: '800',
    color: Colors.primary,
  },
  quizHeaderTitle: {
    fontSize: 18,
    fontWeight: '900',
    color: Colors.textPrimary,
  },
  closeBtnText: {
    fontSize: 14,
    color: Colors.textSecondary,
    fontWeight: '700',
  },
  quizProgressTrack: {
    height: 4,
    backgroundColor: Colors.surfaceMuted,
    borderRadius: Radii.full,
    marginBottom: 20,
  },
  quizProgressFill: {
    height: '100%',
    backgroundColor: Colors.primary,
    borderRadius: Radii.full,
  },
  questionCard: {
    marginBottom: 20,
  },
  questionText: {
    fontSize: 16,
    fontWeight: '800',
    color: Colors.textPrimary,
    lineHeight: 24,
    marginBottom: 20,
  },
  optionCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1.5,
    borderColor: Colors.border,
    borderRadius: Radii.lg,
    padding: 14,
    marginBottom: 12,
  },
  optionCardSelected: {
    borderColor: Colors.primary,
    backgroundColor: Colors.primaryLight,
  },
  optionLetterBadge: {
    width: 32,
    height: 32,
    borderRadius: Radii.full,
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  optionLetterBadgeSelected: {
    backgroundColor: Colors.primary,
    borderColor: Colors.primary,
  },
  optionLetterText: {
    fontSize: 13,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  optionLetterTextSelected: {
    color: '#FFFFFF',
  },
  optionContentText: {
    flex: 1,
    fontSize: 14,
    color: Colors.textPrimary,
    lineHeight: 20,
  },
  optionContentTextSelected: {
    fontWeight: '700',
    color: Colors.primaryDark,
  },
  quizNavRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingTop: 14,
    borderTopWidth: 1,
    borderColor: Colors.border,
    gap: 12,
  },
  navBtn: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: Radii.md,
    alignItems: 'center',
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  navBtnDisabled: {
    opacity: 0.4,
  },
  navBtnText: {
    fontSize: 14,
    fontWeight: '700',
    color: Colors.textSecondary,
  },
  navBtnPrimary: {
    backgroundColor: Colors.primary,
    borderColor: Colors.primary,
  },
  navBtnPrimaryText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 14,
  },
  submitBtn: {
    backgroundColor: Colors.success,
    borderColor: Colors.success,
  },
  submitBtnText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 14,
  },
  resultCard: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.xl,
    padding: 24,
    alignItems: 'center',
    maxHeight: '85%',
  },
  resultIconCircle: {
    width: 80,
    height: 80,
    borderRadius: Radii.full,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
  },
  iconPassed: {
    backgroundColor: Colors.successLight,
  },
  iconFailed: {
    backgroundColor: Colors.dangerLight,
  },
  resultTitle: {
    fontSize: 20,
    fontWeight: '900',
    color: Colors.textPrimary,
    marginBottom: 10,
    textAlign: 'center',
  },
  scoreRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginBottom: 14,
  },
  scoreBigText: {
    fontSize: 32,
    fontWeight: '900',
    color: Colors.textPrimary,
  },
  pctBadge: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: Radii.full,
    fontSize: 15,
    fontWeight: '900',
  },
  pctPassed: {
    backgroundColor: Colors.successLight,
    color: Colors.success,
  },
  pctFailed: {
    backgroundColor: Colors.dangerLight,
    color: Colors.danger,
  },
  resultRuleExplanation: {
    fontSize: 13,
    color: Colors.textSecondary,
    textAlign: 'center',
    lineHeight: 18,
    marginBottom: 16,
  },
  reviewScroll: {
    width: '100%',
    maxHeight: 200,
    marginBottom: 16,
  },
  reviewHeader: {
    fontSize: 13,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 8,
  },
  reviewItem: {
    backgroundColor: Colors.surfaceMuted,
    padding: 10,
    borderRadius: Radii.md,
    marginBottom: 8,
  },
  reviewQuestionText: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginBottom: 4,
  },
  correctReview: {
    fontSize: 12,
    fontWeight: '800',
    color: Colors.success,
  },
  wrongReview: {
    fontSize: 12,
    fontWeight: '800',
    color: Colors.danger,
  },
  explanationText: {
    fontSize: 11,
    color: Colors.textSecondary,
    marginTop: 4,
  },
  resultDoneBtn: {
    width: '100%',
    backgroundColor: Colors.primary,
    paddingVertical: 14,
    borderRadius: Radii.md,
    alignItems: 'center',
  },
  resultDoneBtnText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 15,
  },
  emptyContainer: {
    alignItems: 'center',
    marginTop: 60,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 6,
  },
  emptySubtitle: {
    fontSize: 14,
    color: Colors.textSecondary,
    textAlign: 'center',
  },
  emptyText: {
    fontSize: 14,
    color: Colors.textSecondary,
    textAlign: 'center',
    marginVertical: 20,
  },
});
