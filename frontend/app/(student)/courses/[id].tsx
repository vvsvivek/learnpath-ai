import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Pressable,
  ActivityIndicator,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import {
  ArrowLeft,
  Lock,
  CheckCircle2,
  PlayCircle,
  HelpCircle,
  FileText,
  Clock,
  Sparkles,
  ChevronRight,
  ExternalLink,
} from 'lucide-react-native';
import { useTheme } from '../../../src/hooks/useTheme';
import { Badge, Button, Card, ProgressRing } from '../../../src/components/ui';
import { courseService } from '../../../src/services/courseService';
import { Course, LearningPhase } from '../../../src/types';

export default function CourseDetailScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ id: string }>();
  const courseId = params.id;

  const { theme, isDark } = useTheme();

  const [isLoading, setIsLoading] = useState(true);
  const [course, setCourse] = useState<Course | null>(null);

  useEffect(() => {
    const loadCourse = async () => {
      try {
        const data = await courseService.getCourseDetail(courseId);
        setCourse(data);
      } catch (e) {
        // Mock 4-phase structured course
        setCourse({
          id: courseId,
          title: 'Deep Learning & Neural Architectures',
          description:
            'A rigorous curriculum covering deep feedforward networks, convolutional neural networks for computer vision, sequence modeling with RNNs and LSTMs, and modern Transformer attention mechanisms.',
          category: 'Artificial Intelligence',
          difficulty: 'Intermediate',
          estimated_hours: 32,
          is_custom: false,
          created_at: new Date().toISOString(),
          phases: [
            {
              id: 'phase-1',
              course_id: courseId,
              phase_number: 1,
              title: 'Phase 1: Foundations of Deep Learning',
              description: 'Linear algebra, loss functions, gradient descent, and backpropagation in PyTorch',
              objectives: [
                'Understand computational graphs and autograd',
                'Implement Multi-Layer Perceptrons from scratch',
                'Master regularization techniques (Dropout, BatchNorm)',
              ],
              is_locked: false,
              is_completed: true,
              progress_percentage: 100,
              quiz_id: 'quiz-phase-1',
              quiz_completed: true,
              quiz_score: 92,
              topics: [
                {
                  id: 't-1',
                  phase_id: 'phase-1',
                  title: 'Computational Graphs & PyTorch Tensors',
                  description: 'Tensor operations, GPU acceleration, and broadcasting rules',
                  order: 1,
                  is_completed: true,
                  estimated_hours: 2,
                  resources: [],
                },
                {
                  id: 't-2',
                  phase_id: 'phase-1',
                  title: 'Backpropagation & Loss Optimization',
                  description: 'Chain rule, Cross-Entropy loss, Adam and SGD optimizers',
                  order: 2,
                  is_completed: true,
                  estimated_hours: 3,
                  resources: [],
                },
              ],
            },
            {
              id: 'phase-2',
              course_id: courseId,
              phase_number: 2,
              title: 'Phase 2: Convolutional Neural Networks',
              description: 'Kernel convolutions, pooling, ResNet residual blocks, and transfer learning',
              objectives: [
                'Analyze 2D convolution and stride calculations',
                'Train image classification pipelines with ResNet-50',
                'Implement data augmentations for computer vision',
              ],
              is_locked: false,
              is_completed: false,
              progress_percentage: 60,
              quiz_id: 'quiz-phase-2',
              quiz_completed: false,
              topics: [
                {
                  id: 't-3',
                  phase_id: 'phase-2',
                  title: 'Convolution & Pooling Math',
                  description: 'Receptive field, padding strategies, and feature map sizing',
                  order: 1,
                  is_completed: true,
                  estimated_hours: 3,
                  resources: [],
                },
                {
                  id: 't-4',
                  phase_id: 'phase-2',
                  title: 'ResNet & Modern Vision Backbones',
                  description: 'Skip connections, vanishing gradient resolution, and fine-tuning',
                  order: 2,
                  is_completed: false,
                  estimated_hours: 4,
                  resources: [],
                },
              ],
            },
            {
              id: 'phase-3',
              course_id: courseId,
              phase_number: 3,
              title: 'Phase 3: Recurrent Models & Attention',
              description: 'Sequence-to-sequence modeling, GRUs, LSTMs, and scaled dot-product attention',
              objectives: [
                'Understand vanishing gradients across time steps',
                'Implement Encoder-Decoder RNN architecture',
                'Calculate self-attention query-key-value projections',
              ],
              is_locked: false,
              is_completed: false,
              progress_percentage: 0,
              quiz_id: 'quiz-phase-3',
              quiz_completed: false,
              topics: [
                {
                  id: 't-5',
                  phase_id: 'phase-3',
                  title: 'LSTMs & Gated Recurrent Units',
                  description: 'Cell states, forget gates, and hidden vectors',
                  order: 1,
                  is_completed: false,
                  estimated_hours: 3,
                  resources: [],
                },
              ],
            },
            {
              id: 'phase-4',
              course_id: courseId,
              phase_number: 4,
              title: 'Phase 4: Transformers & Capstone Project',
              description: 'Multi-head attention, positional encodings, fine-tuning LLMs on custom datasets',
              objectives: [
                'Build a full Transformer decoder block',
                'Fine-tune an open-source model using Hugging Face PEFT/LoRA',
                'Deploy neural network inference endpoint with FastAPI',
              ],
              is_locked: true,
              is_completed: false,
              progress_percentage: 0,
              quiz_id: 'quiz-phase-4',
              quiz_completed: false,
              topics: [
                {
                  id: 't-6',
                  phase_id: 'phase-4',
                  title: 'Transformer Architecture Deep Dive',
                  description: 'Scaled dot-product attention, multi-head projection, feedforward layers',
                  order: 1,
                  is_completed: false,
                  estimated_hours: 5,
                  resources: [],
                },
              ],
            },
          ],
        });
      } finally {
        setIsLoading(false);
      }
    };

    loadCourse();
  }, [courseId]);

  if (isLoading || !course) {
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

  // Calculate overall progress across 4 phases
  const phases = course.phases || [];
  const completedPhases = phases.filter((p) => p.is_completed).length;
  const overallPercentage =
    phases.length > 0
      ? Math.round(
          phases.reduce((acc, p) => acc + (p.progress_percentage || 0), 0) /
            phases.length
        )
      : 0;

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      {/* Top Header Bar */}
      <View style={styles.topBar}>
        <Pressable
          onPress={() => router.back()}
          style={[styles.backButton, { backgroundColor: theme.colors.surface }]}
        >
          <ArrowLeft size={20} color={theme.colors.textPrimary} />
        </Pressable>

        <Badge label={course.category} variant="primary" size="sm" />
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* Course Header Overview */}
        <Card variant="elevated" style={styles.headerCard}>
          <Text style={[styles.courseTitle, { color: theme.colors.textPrimary }]}>
            {course.title}
          </Text>

          <Text style={[styles.courseDescription, { color: theme.colors.textSecondary }]}>
            {course.description}
          </Text>

          <View style={styles.progressRow}>
            <View style={styles.progressInfo}>
              <Text style={[styles.progressTitle, { color: theme.colors.textPrimary }]}>
                Overall Course Progress
              </Text>
              <Text style={[styles.progressSubtitle, { color: theme.colors.textTertiary }]}>
                {completedPhases} of {phases.length} Phases Completed
              </Text>
            </View>
            <ProgressRing
              percentage={overallPercentage}
              size={54}
              strokeWidth={6}
            />
          </View>

          <View style={styles.metaRow}>
            <View style={styles.metaBadge}>
              <Clock size={14} color={theme.colors.textSecondary} />
              <Text style={[styles.metaText, { color: theme.colors.textSecondary }]}>
                {course.estimated_hours} Hours Total
              </Text>
            </View>
            <View style={styles.metaBadge}>
              <Sparkles size={14} color={theme.colors.textSecondary} />
              <Text style={[styles.metaText, { color: theme.colors.textSecondary }]}>
                {course.difficulty} Level
              </Text>
            </View>
          </View>
        </Card>

        {/* 4 Phases List */}
        <View style={styles.phasesSection}>
          <Text style={[styles.sectionHeading, { color: theme.colors.textPrimary }]}>
            Learning Roadmap (4 Phases)
          </Text>

          {phases.map((phase, idx) => {
            const isCompleted = phase.is_completed;
            const isLocked = phase.is_locked;

            return (
              <Card
                key={phase.id}
                variant={isLocked ? 'outline' : 'elevated'}
                style={[
                  styles.phaseCard,
                  isLocked && { opacity: 0.65 },
                ]}
                onPress={
                  isLocked
                    ? undefined
                    : () =>
                        router.push({
                          pathname: '/(student)/courses/phase/[id]',
                          params: { id: phase.id },
                        })
                }
              >
                <View style={styles.phaseHeaderRow}>
                  <View style={styles.phaseNumberBadge}>
                    {isCompleted ? (
                      <CheckCircle2 size={24} color={theme.colors.success} />
                    ) : isLocked ? (
                      <Lock size={20} color={theme.colors.textTertiary} />
                    ) : (
                      <View
                        style={[
                          styles.activeNumberCircle,
                          { backgroundColor: theme.colors.primary },
                        ]}
                      >
                        <Text style={styles.activeNumberText}>{phase.phase_number}</Text>
                      </View>
                    )}
                  </View>

                  <View style={styles.phaseTitleContainer}>
                    <Text
                      style={[styles.phaseTitle, { color: theme.colors.textPrimary }]}
                    >
                      {phase.title}
                    </Text>
                    <Text
                      style={[
                        styles.phaseDesc,
                        { color: theme.colors.textSecondary },
                      ]}
                      numberOfLines={2}
                    >
                      {phase.description}
                    </Text>
                  </View>
                </View>

                {/* Phase Objectives */}
                <View style={styles.objectivesList}>
                  {phase.objectives?.slice(0, 2).map((obj, oIdx) => (
                    <View key={oIdx} style={styles.objectiveRow}>
                      <View
                        style={[
                          styles.bulletPoint,
                          { backgroundColor: theme.colors.primary },
                        ]}
                      />
                      <Text
                        style={[
                          styles.objectiveText,
                          { color: theme.colors.textSecondary },
                        ]}
                      >
                        {obj}
                      </Text>
                    </View>
                  ))}
                </View>

                {/* Phase Footer */}
                <View style={styles.phaseFooter}>
                  <Text
                    style={[styles.topicsCount, { color: theme.colors.textTertiary }]}
                  >
                    {phase.topics?.length || 0} Modules •{' '}
                    {phase.progress_percentage}% Done
                  </Text>

                  {!isLocked && (
                    <Button
                      title={
                        isCompleted
                          ? 'Review Topics'
                          : phase.progress_percentage > 0
                          ? 'Continue'
                          : 'Start Phase'
                      }
                      size="sm"
                      variant={isCompleted ? 'outline' : 'primary'}
                      rightIcon={<ChevronRight size={16} color={isCompleted ? theme.colors.primary : '#FFFFFF'} />}
                      onPress={() =>
                        router.push({
                          pathname: '/(student)/courses/phase/[id]',
                          params: { id: phase.id },
                        })
                      }
                    />
                  )}
                </View>
              </Card>
            );
          })}
        </View>

        {/* Resources & References Section */}
        <Card style={styles.resourcesCard}>
          <Text style={[styles.resourcesHeading, { color: theme.colors.textPrimary }]}>
            Course References & Readings
          </Text>
          <View style={styles.resourceItem}>
            <FileText size={18} color={theme.colors.primary} />
            <View style={styles.resourceInfo}>
              <Text style={[styles.resourceTitle, { color: theme.colors.textPrimary }]}>
                PyTorch Deep Learning Official Documentation
              </Text>
              <Text style={[styles.resourceType, { color: theme.colors.textTertiary }]}>
                Interactive Documentation
              </Text>
            </View>
            <ExternalLink size={16} color={theme.colors.textTertiary} />
          </View>
        </Card>
      </ScrollView>
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
    marginBottom: 24,
  },
  courseTitle: {
    fontSize: 22,
    fontWeight: '800',
    letterSpacing: -0.4,
    marginBottom: 8,
  },
  courseDescription: {
    fontSize: 14,
    lineHeight: 20,
    marginBottom: 18,
  },
  progressRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderTopWidth: 1,
    borderBottomWidth: 1,
    borderColor: 'rgba(150, 150, 150, 0.15)',
    marginBottom: 14,
  },
  progressInfo: {
    flex: 1,
  },
  progressTitle: {
    fontSize: 15,
    fontWeight: '700',
  },
  progressSubtitle: {
    fontSize: 13,
    marginTop: 2,
  },
  metaRow: {
    flexDirection: 'row',
    gap: 16,
  },
  metaBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  metaText: {
    fontSize: 13,
    fontWeight: '500',
  },
  phasesSection: {
    marginBottom: 20,
  },
  sectionHeading: {
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 14,
  },
  phaseCard: {
    padding: 16,
    marginBottom: 14,
  },
  phaseHeaderRow: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 12,
  },
  phaseNumberBadge: {
    width: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
  activeNumberCircle: {
    width: 28,
    height: 28,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
  },
  activeNumberText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 14,
  },
  phaseTitleContainer: {
    flex: 1,
  },
  phaseTitle: {
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 4,
  },
  phaseDesc: {
    fontSize: 13,
    lineHeight: 18,
  },
  objectivesList: {
    paddingLeft: 44,
    marginBottom: 14,
    gap: 6,
  },
  objectiveRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  bulletPoint: {
    width: 5,
    height: 5,
    borderRadius: 2.5,
  },
  objectiveText: {
    fontSize: 12,
    flex: 1,
  },
  phaseFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingLeft: 44,
  },
  topicsCount: {
    fontSize: 12,
  },
  resourcesCard: {
    padding: 16,
  },
  resourcesHeading: {
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 12,
  },
  resourceItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  resourceInfo: {
    flex: 1,
  },
  resourceTitle: {
    fontSize: 13,
    fontWeight: '600',
  },
  resourceType: {
    fontSize: 11,
    marginTop: 2,
  },
});
