import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Pressable,
  ActivityIndicator,
  Modal,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import {
  ArrowLeft,
  CheckCircle2,
  Circle,
  ChevronDown,
  ChevronUp,
  FileText,
  ExternalLink,
  HelpCircle,
  Sparkles,
  Check,
  X,
  Clock,
  Play,
} from 'lucide-react-native';
import { useTheme } from '../../../../src/hooks/useTheme';
import { Badge, Button, Card, ProgressRing } from '../../../../src/components/ui';
import { learningPathService } from '../../../../src/services/learningPathService';
import { quizService } from '../../../../src/services/quizService';
import { LearningPhase, PhaseTopic, Quiz, QuizResult } from '../../../../src/types';

export default function PhaseDetailScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ id: string }>();
  const phaseId = params.id;

  const { theme, isDark } = useTheme();

  const [isLoading, setIsLoading] = useState(true);
  const [phase, setPhase] = useState<LearningPhase | null>(null);
  const [expandedTopicId, setExpandedTopicId] = useState<string | null>(null);

  // Quiz Modal State
  const [isQuizModalVisible, setIsQuizModalVisible] = useState(false);
  const [isGeneratingQuiz, setIsGeneratingQuiz] = useState(false);
  const [activeQuiz, setActiveQuiz] = useState<Quiz | null>(null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [selectedAnswers, setSelectedAnswers] = useState<Record<string, number>>({});
  const [quizResult, setQuizResult] = useState<QuizResult | null>(null);

  useEffect(() => {
    const loadPhase = async () => {
      try {
        const data = await learningPathService.getPhaseDetail(phaseId);
        setPhase(data);
      } catch (e) {
        // Mock detailed phase
        setPhase({
          id: phaseId,
          course_id: 'course-ai-core',
          phase_number: 2,
          title: 'Phase 2: Convolutional Neural Networks',
          description:
            'Deep dive into spatial pattern recognition, 2D filters, pooling, ResNet residual bottlenecks, and modern vision backbones.',
          objectives: [
            'Analyze 2D convolution and stride calculations mathematically',
            'Train image classification pipelines with ResNet-50 and PyTorch',
            'Implement data augmentations for real-world robustness',
          ],
          is_locked: false,
          is_completed: false,
          progress_percentage: 50,
          quiz_id: 'quiz-phase-2',
          quiz_completed: false,
          topics: [
            {
              id: 't-201',
              phase_id: phaseId,
              title: 'Convolutional Kernels & Feature Sizing',
              description:
                'Understanding receptive fields, zero-padding modes (valid vs same), dilation, and spatial dimension shrinkage formula: (W - F + 2P)/S + 1.',
              order: 1,
              is_completed: true,
              estimated_hours: 2,
              key_takeaways: [
                'Feature maps preserve spatial hierarchies from edges to textures',
                'Stride controls downsampling rate while dilation expands field of view',
                '1x1 convolutions act as channel-wise cross-feature poolers',
              ],
              resources: [
                {
                  id: 'r-1',
                  title: 'CS231n: Convolutional Neural Networks for Visual Recognition',
                  url: 'https://cs231n.github.io/convolutional-networks/',
                  type: 'documentation',
                  estimated_minutes: 25,
                },
              ],
            },
            {
              id: 't-202',
              phase_id: phaseId,
              title: 'ResNet & Residual Skip Connections',
              description:
                'How identity shortcuts solve the vanishing gradient problem in networks deeper than 20 layers, allowing 152+ layer architectures.',
              order: 2,
              is_completed: false,
              estimated_hours: 3,
              key_takeaways: [
                'Formulation: F(x) + x allows gradients to flow backwards unimpeded',
                'Bottleneck residual blocks reduce matrix multiplication FLOPs',
                'Pre-activation ResNets improve regularization and convergence',
              ],
              resources: [
                {
                  id: 'r-2',
                  title: 'Deep Residual Learning for Image Recognition (He et al.)',
                  url: 'https://arxiv.org/abs/1512.03385',
                  type: 'article',
                  estimated_minutes: 30,
                },
              ],
            },
          ],
        });
      } finally {
        setIsLoading(false);
      }
    };

    loadPhase();
  }, [phaseId]);

  const toggleTopicCompletion = async (topic: PhaseTopic) => {
    if (!phase) return;
    const updatedStatus = !topic.is_completed;

    // Optimistic update
    const updatedTopics = phase.topics.map((t) =>
      t.id === topic.id ? { ...t, is_completed: updatedStatus } : t
    );
    const completedCount = updatedTopics.filter((t) => t.is_completed).length;
    const newProgress = Math.round((completedCount / updatedTopics.length) * 100);

    setPhase({
      ...phase,
      topics: updatedTopics,
      progress_percentage: newProgress,
      is_completed: newProgress === 100,
    });

    try {
      await learningPathService.completeTopic(topic.id);
    } catch (e) {
      // offline fallback maintained
    }
  };

  const handleStartQuiz = async () => {
    setIsQuizModalVisible(true);
    setIsGeneratingQuiz(true);
    setQuizResult(null);
    setSelectedAnswers({});
    setCurrentQuestionIndex(0);

    try {
      const quiz = await quizService.generateQuiz(phase?.course_id || 'course-ai-core', phaseId);
      setActiveQuiz(quiz);
    } catch (e) {
      // Mock AI generated quiz
      setActiveQuiz({
        id: 'quiz-phase-2',
        course_id: phase?.course_id || 'course-ai-core',
        phase_id: phaseId,
        title: 'Phase 2 Assessment: Convolutional Networks',
        description: 'Test your understanding of convolutions, skip connections, and receptive fields.',
        passing_score_percentage: 75,
        created_at: new Date().toISOString(),
        questions: [
          {
            id: 'q-1',
            question: 'What is the primary benefit of the identity skip connection in ResNet?',
            options: [
              'It halves the number of model weights',
              'It provides an unobstructed path for backpropagating gradients',
              'It replaces the need for activation functions',
              'It speeds up GPU matrix multiplication by 10x',
            ],
            correct_index: 1,
            explanation:
              'Identity shortcuts let gradient signals propagate directly across layers without undergoing destructive repeated matrix multiplications.',
          },
          {
            id: 'q-2',
            question:
              'Given an input map of 32x32, filter size 5x5, padding 2, and stride 1, what is the output size?',
            options: ['32x32', '28x28', '30x30', '34x34'],
            correct_index: 0,
            explanation: 'Formula: (32 - 5 + 2*2)/1 + 1 = 32. This is known as "Same" padding.',
          },
          {
            id: 'q-3',
            question: 'What is the function of a 1x1 convolution layer?',
            options: [
              'Downsamples the spatial height and width directly',
              'Changes the number of feature channels while combining cross-channel information',
              'Calculates max pooling across the batch',
              'Normalizes neuron activations to mean zero',
            ],
            correct_index: 1,
            explanation:
              'A 1x1 convolution computes a linear combination across input channels at each pixel location, acting as channel projection.',
          },
        ],
      });
    } finally {
      setIsGeneratingQuiz(false);
    }
  };

  const handleSelectOption = (questionId: string, optionIndex: number) => {
    setSelectedAnswers((prev) => ({
      ...prev,
      [questionId]: optionIndex,
    }));
  };

  const handleSubmitQuiz = () => {
    if (!activeQuiz) return;
    const questions = activeQuiz.questions;
    let correctCount = 0;

    const breakdown = questions.map((q) => {
      const userSelected = selectedAnswers[q.id] ?? -1;
      const isCorrect = userSelected === q.correct_index;
      if (isCorrect) correctCount++;
      return {
        question_id: q.id,
        question_text: q.question,
        options: q.options,
        selected_index: userSelected,
        correct_index: q.correct_index,
        is_correct: isCorrect,
        explanation: q.explanation,
      };
    });

    const scorePct = Math.round((correctCount / questions.length) * 100);
    const passed = scorePct >= activeQuiz.passing_score_percentage;

    setQuizResult({
      id: `result-${Date.now()}`,
      quiz_id: activeQuiz.id,
      score_percentage: scorePct,
      total_questions: questions.length,
      correct_answers: correctCount,
      passed,
      duration_seconds: 45,
      completed_at: new Date().toISOString(),
      breakdown,
    });
  };

  if (isLoading || !phase) {
    return (
      <View
        style={[
          styles.loadingContainer,
          { backgroundColor: theme.colors.background },
        ]}
      >
        <ActivityIndicator size="large" color={theme.colors.primary} />
      </View>
    );
  }

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      {/* Top Bar */}
      <View style={styles.topBar}>
        <Pressable
          onPress={() => router.back()}
          style={[styles.backButton, { backgroundColor: theme.colors.surface }]}
        >
          <ArrowLeft size={20} color={theme.colors.textPrimary} />
        </Pressable>
        <Badge
          label={`Phase ${phase.phase_number}`}
          variant="primary"
          size="sm"
        />
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* Phase Header Card */}
        <Card variant="elevated" style={styles.headerCard}>
          <Text style={[styles.phaseTitle, { color: theme.colors.textPrimary }]}>
            {phase.title}
          </Text>
          <Text style={[styles.phaseDescription, { color: theme.colors.textSecondary }]}>
            {phase.description}
          </Text>

          <View style={styles.phaseProgressRow}>
            <View>
              <Text style={[styles.progressLabel, { color: theme.colors.textPrimary }]}>
                Phase Progress
              </Text>
              <Text style={[styles.progressSub, { color: theme.colors.textTertiary }]}>
                {phase.topics.filter((t) => t.is_completed).length} of{' '}
                {phase.topics.length} Modules Completed
              </Text>
            </View>
            <ProgressRing
              percentage={phase.progress_percentage}
              size={50}
              strokeWidth={5}
            />
          </View>
        </Card>

        {/* Learning Objectives */}
        <Card style={styles.objectivesCard}>
          <Text style={[styles.sectionTitle, { color: theme.colors.textPrimary }]}>
            Target Objectives
          </Text>
          {phase.objectives?.map((obj, i) => (
            <View key={i} style={styles.objectiveRow}>
              <View
                style={[
                  styles.objCircle,
                  { backgroundColor: theme.colors.primaryLight },
                ]}
              >
                <Check size={12} color={theme.colors.primary} />
              </View>
              <Text style={[styles.objText, { color: theme.colors.textSecondary }]}>
                {obj}
              </Text>
            </View>
          ))}
        </Card>

        {/* Topics List */}
        <Text style={[styles.listHeading, { color: theme.colors.textPrimary }]}>
          Phase Modules & Checkpoints
        </Text>

        {phase.topics.map((topic) => {
          const isExpanded = expandedTopicId === topic.id;
          return (
            <Card key={topic.id} style={styles.topicCard}>
              <Pressable
                onPress={() =>
                  setExpandedTopicId(isExpanded ? null : topic.id)
                }
                style={styles.topicHeader}
              >
                <Pressable
                  onPress={() => toggleTopicCompletion(topic)}
                  style={styles.checkboxTouch}
                >
                  {topic.is_completed ? (
                    <CheckCircle2 size={22} color={theme.colors.success} />
                  ) : (
                    <Circle size={22} color={theme.colors.border} />
                  )}
                </Pressable>

                <View style={styles.topicTitleContainer}>
                  <Text
                    style={[
                      styles.topicName,
                      {
                        color: theme.colors.textPrimary,
                        textDecorationLine: topic.is_completed ? 'line-through' : 'none',
                      },
                    ]}
                  >
                    {topic.title}
                  </Text>
                  <Text style={[styles.topicDuration, { color: theme.colors.textTertiary }]}>
                    {topic.estimated_hours}h estimated
                  </Text>
                </View>

                {isExpanded ? (
                  <ChevronUp size={20} color={theme.colors.textTertiary} />
                ) : (
                  <ChevronDown size={20} color={theme.colors.textTertiary} />
                )}
              </Pressable>

              {isExpanded && (
                <View style={styles.topicDetails}>
                  <Text style={[styles.topicDesc, { color: theme.colors.textSecondary }]}>
                    {topic.description}
                  </Text>

                  {/* Key takeaways */}
                  {topic.key_takeaways && (
                    <View style={styles.takeawaysBox}>
                      <Text style={[styles.takeawaysTitle, { color: theme.colors.textPrimary }]}>
                        Core Takeaways:
                      </Text>
                      {topic.key_takeaways.map((takeaway, tIdx) => (
                        <Text
                          key={tIdx}
                          style={[styles.takeawayItem, { color: theme.colors.textSecondary }]}
                        >
                          • {takeaway}
                        </Text>
                      ))}
                    </View>
                  )}

                  {/* Resources */}
                  {topic.resources.map((res) => (
                    <View
                      key={res.id}
                      style={[
                        styles.resItem,
                        {
                          backgroundColor: isDark
                            ? theme.colors.surfaceSubtle
                            : theme.colors.inputBackground,
                        },
                      ]}
                    >
                      <FileText size={16} color={theme.colors.primary} />
                      <Text
                        style={[styles.resTitle, { color: theme.colors.textPrimary }]}
                        numberOfLines={1}
                      >
                        {res.title}
                      </Text>
                      <ExternalLink size={14} color={theme.colors.textTertiary} />
                    </View>
                  ))}
                </View>
              )}
            </Card>
          );
        })}

        {/* Phase Quiz CTA */}
        <Card variant="elevated" style={styles.quizCard}>
          <View style={styles.quizCardHeader}>
            <View
              style={[
                styles.quizIconCircle,
                { backgroundColor: theme.colors.primaryLight },
              ]}
            >
              <Sparkles size={24} color={theme.colors.primary} />
            </View>
            <View style={{ flex: 1 }}>
              <Text style={[styles.quizTitle, { color: theme.colors.textPrimary }]}>
                Phase Assessment Quiz
              </Text>
              <Text style={[styles.quizDesc, { color: theme.colors.textSecondary }]}>
                Test your mastery to unlock the next learning phase.
              </Text>
            </View>
          </View>

          <Button
            title="Launch Dynamic Quiz"
            variant="primary"
            size="lg"
            onPress={handleStartQuiz}
            style={styles.quizButton}
          />
        </Card>
      </ScrollView>

      {/* QUIZ MODAL */}
      <Modal
        visible={isQuizModalVisible}
        animationType="slide"
        presentationStyle="pageSheet"
        onRequestClose={() => setIsQuizModalVisible(false)}
      >
        <View style={[styles.modalContainer, { backgroundColor: theme.colors.background }]}>
          {/* Modal Header */}
          <View style={styles.modalHeader}>
            <Text style={[styles.modalTitle, { color: theme.colors.textPrimary }]}>
              {activeQuiz?.title || 'Phase Quiz'}
            </Text>
            <Pressable
              onPress={() => setIsQuizModalVisible(false)}
              style={[styles.closeModal, { backgroundColor: theme.colors.surfaceSubtle }]}
            >
              <X size={18} color={theme.colors.textPrimary} />
            </Pressable>
          </View>

          {isGeneratingQuiz ? (
            <View style={styles.quizLoading}>
              <ActivityIndicator size="large" color={theme.colors.primary} />
              <Text style={[styles.quizLoadingText, { color: theme.colors.textSecondary }]}>
                Generating tailored assessment questions...
              </Text>
            </View>
          ) : quizResult ? (
            /* Quiz Result Screen */
            <ScrollView contentContainerStyle={styles.resultContent}>
              <View
                style={[
                  styles.resultBadge,
                  {
                    backgroundColor: quizResult.passed
                      ? 'rgba(16, 185, 129, 0.15)'
                      : 'rgba(239, 68, 68, 0.15)',
                  },
                ]}
              >
                <Text
                  style={[
                    styles.resultScore,
                    {
                      color: quizResult.passed
                        ? theme.colors.success
                        : theme.colors.error,
                    },
                  ]}
                >
                  {quizResult.score_percentage}%
                </Text>
              </View>

              <Text style={[styles.resultStatus, { color: theme.colors.textPrimary }]}>
                {quizResult.passed ? 'Phase Assessment Passed! 🎉' : 'Needs Practice'}
              </Text>
              <Text style={[styles.resultSub, { color: theme.colors.textSecondary }]}>
                You answered {quizResult.correct_answers} of{' '}
                {quizResult.total_questions} questions correctly.
              </Text>

              {/* Question Breakdown */}
              <View style={styles.breakdownList}>
                {quizResult.breakdown.map((item, idx) => (
                  <Card key={idx} style={styles.breakdownCard}>
                    <View style={styles.breakdownHeader}>
                      <Text style={[styles.qIndex, { color: theme.colors.textSecondary }]}>
                        Q{idx + 1}.
                      </Text>
                      <Text
                        style={[styles.qQuestion, { color: theme.colors.textPrimary }]}
                      >
                        {item.question_text}
                      </Text>
                      {item.is_correct ? (
                        <CheckCircle2 size={18} color={theme.colors.success} />
                      ) : (
                        <X size={18} color={theme.colors.error} />
                      )}
                    </View>
                    <Text
                      style={[styles.explanationText, { color: theme.colors.textSecondary }]}
                    >
                      {item.explanation}
                    </Text>
                  </Card>
                ))}
              </View>

              <Button
                title="Done"
                variant="primary"
                size="lg"
                onPress={() => setIsQuizModalVisible(false)}
                style={{ marginTop: 20 }}
              />
            </ScrollView>
          ) : activeQuiz ? (
            /* Active Question View */
            <ScrollView contentContainerStyle={styles.activeQuizContent}>
              <View style={styles.quizProgressBar}>
                {activeQuiz.questions.map((_, qIdx) => (
                  <View
                    key={qIdx}
                    style={[
                      styles.progressSegment,
                      {
                        backgroundColor:
                          qIdx <= currentQuestionIndex
                            ? theme.colors.primary
                            : theme.colors.border,
                      },
                    ]}
                  />
                ))}
              </View>

              <Text style={[styles.questionCounter, { color: theme.colors.textTertiary }]}>
                Question {currentQuestionIndex + 1} of {activeQuiz.questions.length}
              </Text>

              <Text style={[styles.questionText, { color: theme.colors.textPrimary }]}>
                {activeQuiz.questions[currentQuestionIndex].question}
              </Text>

              {/* Options */}
              <View style={styles.optionsList}>
                {activeQuiz.questions[currentQuestionIndex].options.map(
                  (opt, optIdx) => {
                    const currentQ = activeQuiz.questions[currentQuestionIndex];
                    const isSelected = selectedAnswers[currentQ.id] === optIdx;

                    return (
                      <Pressable
                        key={optIdx}
                        onPress={() => handleSelectOption(currentQ.id, optIdx)}
                        style={[
                          styles.optionCard,
                          {
                            backgroundColor: isSelected
                              ? theme.colors.primaryLight
                              : isDark
                              ? theme.colors.surface
                              : theme.colors.surfaceElevated,
                            borderColor: isSelected
                              ? theme.colors.primary
                              : theme.colors.border,
                          },
                        ]}
                      >
                        <View
                          style={[
                            styles.optionLetterCircle,
                            {
                              backgroundColor: isSelected
                                ? theme.colors.primary
                                : theme.colors.surfaceSubtle,
                            },
                          ]}
                        >
                          <Text
                            style={[
                              styles.optionLetter,
                              {
                                color: isSelected
                                  ? '#FFFFFF'
                                  : theme.colors.textSecondary,
                              },
                            ]}
                          >
                            {String.fromCharCode(65 + optIdx)}
                          </Text>
                        </View>
                        <Text
                          style={[
                            styles.optionLabel,
                            {
                              color: theme.colors.textPrimary,
                              fontWeight: isSelected ? '700' : '400',
                            },
                          ]}
                        >
                          {opt}
                        </Text>
                      </Pressable>
                    );
                  }
                )}
              </View>

              {/* Navigation buttons */}
              <View style={styles.quizNavRow}>
                {currentQuestionIndex > 0 && (
                  <Button
                    title="Previous"
                    variant="outline"
                    size="md"
                    onPress={() => setCurrentQuestionIndex((prev) => prev - 1)}
                    style={{ flex: 1 }}
                  />
                )}

                {currentQuestionIndex < activeQuiz.questions.length - 1 ? (
                  <Button
                    title="Next Question"
                    variant="primary"
                    size="md"
                    onPress={() => setCurrentQuestionIndex((prev) => prev + 1)}
                    style={{ flex: 1 }}
                  />
                ) : (
                  <Button
                    title="Submit Quiz"
                    variant="primary"
                    size="md"
                    onPress={handleSubmitQuiz}
                    style={{ flex: 1 }}
                  />
                )}
              </View>
            </ScrollView>
          ) : null}
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  loadingContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  topBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingTop: 56,
    paddingBottom: 12,
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
  scrollContent: {
    paddingHorizontal: 20,
    paddingBottom: 40,
  },
  headerCard: {
    padding: 20,
    marginBottom: 16,
  },
  phaseTitle: {
    fontSize: 20,
    fontWeight: '800',
    marginBottom: 8,
  },
  phaseDescription: {
    fontSize: 14,
    lineHeight: 20,
    marginBottom: 16,
  },
  phaseProgressRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: 12,
    borderTopWidth: 1,
    borderColor: 'rgba(150, 150, 150, 0.15)',
  },
  progressLabel: {
    fontSize: 14,
    fontWeight: '700',
  },
  progressSub: {
    fontSize: 12,
    marginTop: 2,
  },
  objectivesCard: {
    padding: 16,
    marginBottom: 20,
  },
  sectionTitle: {
    fontSize: 15,
    fontWeight: '700',
    marginBottom: 12,
  },
  objectiveRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    marginBottom: 8,
  },
  objCircle: {
    width: 20,
    height: 20,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  objText: {
    fontSize: 13,
    flex: 1,
  },
  listHeading: {
    fontSize: 17,
    fontWeight: '700',
    marginBottom: 12,
  },
  topicCard: {
    padding: 16,
    marginBottom: 12,
  },
  topicHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  checkboxTouch: {
    padding: 2,
  },
  topicTitleContainer: {
    flex: 1,
  },
  topicName: {
    fontSize: 15,
    fontWeight: '600',
  },
  topicDuration: {
    fontSize: 12,
    marginTop: 2,
  },
  topicDetails: {
    marginTop: 12,
    paddingTop: 12,
    borderTopWidth: 1,
    borderColor: 'rgba(150, 150, 150, 0.12)',
  },
  topicDesc: {
    fontSize: 13,
    lineHeight: 18,
    marginBottom: 12,
  },
  takeawaysBox: {
    marginBottom: 12,
  },
  takeawaysTitle: {
    fontSize: 12,
    fontWeight: '700',
    marginBottom: 4,
  },
  takeawayItem: {
    fontSize: 12,
    lineHeight: 16,
    marginBottom: 3,
  },
  resItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    padding: 10,
    borderRadius: 8,
    marginTop: 6,
  },
  resTitle: {
    flex: 1,
    fontSize: 12,
  },
  quizCard: {
    padding: 20,
    marginTop: 12,
  },
  quizCardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginBottom: 16,
  },
  quizIconCircle: {
    width: 48,
    height: 48,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  quizTitle: {
    fontSize: 16,
    fontWeight: '700',
  },
  quizDesc: {
    fontSize: 13,
    marginTop: 2,
  },
  quizButton: {
    width: '100%',
  },
  modalContainer: {
    flex: 1,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingTop: 20,
    paddingBottom: 14,
    borderBottomWidth: 1,
    borderColor: 'rgba(150, 150, 150, 0.15)',
  },
  modalTitle: {
    fontSize: 17,
    fontWeight: '700',
  },
  closeModal: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  quizLoading: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  quizLoadingText: {
    fontSize: 14,
    marginTop: 14,
    textAlign: 'center',
  },
  activeQuizContent: {
    padding: 20,
  },
  quizProgressBar: {
    flexDirection: 'row',
    gap: 6,
    marginBottom: 16,
  },
  progressSegment: {
    flex: 1,
    height: 5,
    borderRadius: 2.5,
  },
  questionCounter: {
    fontSize: 12,
    fontWeight: '600',
    marginBottom: 8,
  },
  questionText: {
    fontSize: 17,
    fontWeight: '700',
    lineHeight: 24,
    marginBottom: 20,
  },
  optionsList: {
    gap: 12,
    marginBottom: 24,
  },
  optionCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 14,
    borderRadius: 12,
    borderWidth: 1.5,
    gap: 12,
  },
  optionLetterCircle: {
    width: 30,
    height: 30,
    borderRadius: 15,
    alignItems: 'center',
    justifyContent: 'center',
  },
  optionLetter: {
    fontSize: 13,
    fontWeight: '700',
  },
  optionLabel: {
    fontSize: 14,
    flex: 1,
    lineHeight: 20,
  },
  quizNavRow: {
    flexDirection: 'row',
    gap: 12,
  },
  resultContent: {
    padding: 24,
    alignItems: 'center',
  },
  resultBadge: {
    width: 90,
    height: 90,
    borderRadius: 45,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
  },
  resultScore: {
    fontSize: 28,
    fontWeight: '800',
  },
  resultStatus: {
    fontSize: 20,
    fontWeight: '800',
    marginBottom: 6,
  },
  resultSub: {
    fontSize: 14,
    textAlign: 'center',
    marginBottom: 24,
  },
  breakdownList: {
    width: '100%',
    gap: 12,
  },
  breakdownCard: {
    padding: 14,
  },
  breakdownHeader: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 6,
    marginBottom: 6,
  },
  qIndex: {
    fontSize: 13,
    fontWeight: '700',
  },
  qQuestion: {
    flex: 1,
    fontSize: 13,
    fontWeight: '600',
  },
  explanationText: {
    fontSize: 12,
    lineHeight: 16,
  },
});
