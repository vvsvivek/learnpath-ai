import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TextInput,
  Pressable,
  KeyboardAvoidingView,
  Platform,
  ActivityIndicator,
} from 'react-native';
import { Send, Bot, Plus, Sparkles } from 'lucide-react-native';
import { useTheme } from '../../src/hooks/useTheme';
import { useAuth } from '../../src/hooks/useAuth';
import { chatService } from '../../src/services/chatService';
import { ChatMessage } from '../../src/types';
import { formatTime } from '../../src/utils/formatters';

const FACULTY_PROMPTS = [
  'Generate 5 tricky multiple-choice questions on Dijkstra vs Bellman-Ford with explanations',
  'Draft a 4-phase curriculum for Cloud Microservices & Kubernetes',
  'Suggest pedagogical strategies for students failing Dynamic Programming quizzes',
  'Create a rubric for evaluating B.Tech Capstone Project Git repositories',
];

export default function FacultyAIChatScreen() {
  const { theme, isDark } = useTheme();
  const { user } = useAuth();

  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'm-f0',
      conversation_id: 'conv-faculty-init',
      role: 'assistant',
      content: `Welcome Professor ${user?.name ? user.name.split(' ').slice(1).join(' ') || user.name : 'Faculty'}! 🎓\n\nI am your Faculty AI Curriculum & Analytics Copilot. I can assist you with:\n• Generating dynamic multi-choice quiz banks\n• Designing 4-phase structured course roadmaps\n• Analyzing student cohort comprehension gaps\n• Drafting assignment specs and rubrics\n\nHow can I support your teaching objectives today?`,
      created_at: new Date().toISOString(),
    },
  ]);
  const [inputText, setInputText] = useState('');
  const [isTyping, setIsTyping] = useState(false);

  const scrollViewRef = useRef<any>(null);

  useEffect(() => {
    scrollViewRef.current?.scrollToEnd({ animated: true });
  }, [messages, isTyping]);

  const handleSendMessage = async (textToSend?: string) => {
    const content = (textToSend || inputText).trim();
    if (!content || isTyping) return;

    setInputText('');

    const userMessage: ChatMessage = {
      id: `msg-${Date.now()}`,
      conversation_id: 'conv-faculty-init',
      role: 'user',
      content,
      created_at: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setIsTyping(true);

    try {
      const response = await chatService.sendFacultyMessage({
        conversation_id: 'conv-faculty-init',
        message: content,
        context: `Faculty member ${user?.name || ''}, Department ${user?.department || 'CSE'}`,
      });

      setMessages((prev) => [...prev, response.message]);
    } catch (e) {
      // Offline fallback simulator
      setTimeout(() => {
        const fallbackAiMessage: ChatMessage = {
          id: `ai-${Date.now()}`,
          conversation_id: 'conv-faculty-init',
          role: 'assistant',
          content: generateFacultyFallback(content),
          created_at: new Date().toISOString(),
        };
        setMessages((prev) => [...prev, fallbackAiMessage]);
        setIsTyping(false);
      }, 1000);
      return;
    } finally {
      setIsTyping(false);
    }
  };

  const generateFacultyFallback = (query: string): string => {
    const q = query.toLowerCase();
    if (q.includes('dijkstra') || q.includes('quiz') || q.includes('question')) {
      return `Here is a curated assessment question on Shortest Path algorithms:

**Question:** Under which condition will Dijkstra's algorithm fail to find the correct shortest path, whereas the Bellman-Ford algorithm succeeds?
• A) Graphs containing directed cycles with all positive weights
• B) Graphs containing edges with negative weights (without negative cycles)
• C) Graphs that are disconnected
• D) Undirected dense graphs where E ≈ V²

**Correct Answer:** B
**Pedagogical Rationale:** Dijkstra greedily marks vertices as finalized assuming edge weights are strictly non-negative. Bellman-Ford relaxes all edges V-1 times, correctly propagating negative weight values.`;
    }
    return `Here is a structured recommendation for your curriculum:

1. **Prerequisite Foundation (Phase 1):** Ensure all enrolled students test out of foundational math and data structures before complex implementations.
2. **Interactive Checkpoints (Phase 2 & 3):** Pair conceptual readings with automated 5-question micro-quizzes to assess comprehension before mid-term exams.
3. **Capstone Artifact (Phase 4):** Have each student deploy a working prototype to GitHub with automated test suites.

Would you like me to generate a complete syllabus markdown file or auto-assign this to your Semester 5 batch?`;
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={[styles.container, { backgroundColor: theme.colors.background }]}
    >
      {/* Top Bar */}
      <View style={styles.topBar}>
        <View style={styles.topBarLeft}>
          <View
            style={[
              styles.botIconCircle,
              { backgroundColor: theme.colors.primaryLight },
            ]}
          >
            <Bot size={22} color={theme.colors.primary} />
          </View>
          <View>
            <Text style={[styles.topBarTitle, { color: theme.colors.textPrimary }]}>
              Faculty AI Copilot
            </Text>
            <Text style={[styles.topBarStatus, { color: theme.colors.success }]}>
              ● Online • Academic Model
            </Text>
          </View>
        </View>

        <Pressable
          onPress={() =>
            setMessages([
              {
                id: `m-${Date.now()}`,
                conversation_id: 'conv-f-new',
                role: 'assistant',
                content: 'Reset conversation. How can I assist with your course planning?',
                created_at: new Date().toISOString(),
              },
            ])
          }
          style={[styles.iconButton, { backgroundColor: theme.colors.surface }]}
        >
          <Plus size={18} color={theme.colors.textPrimary} />
        </Pressable>
      </View>

      {/* Messages */}
      <ScrollView
        ref={scrollViewRef}
        contentContainerStyle={styles.messagesContainer}
        keyboardShouldPersistTaps="handled"
      >
        {messages.map((msg) => {
          const isUser = msg.role === 'user';
          return (
            <View
              key={msg.id}
              style={[
                styles.messageRow,
                isUser ? styles.userMessageRow : styles.aiMessageRow,
              ]}
            >
              {!isUser && (
                <View
                  style={[
                    styles.aiAvatarSmall,
                    { backgroundColor: theme.colors.primaryLight },
                  ]}
                >
                  <Bot size={15} color={theme.colors.primary} />
                </View>
              )}

              <View
                style={[
                  styles.messageBubble,
                  isUser
                    ? [
                        styles.userBubble,
                        { backgroundColor: theme.colors.primary },
                      ]
                    : [
                        styles.aiBubble,
                        {
                          backgroundColor: isDark
                            ? theme.colors.surface
                            : theme.colors.surfaceElevated,
                          borderColor: theme.colors.borderLight,
                        },
                      ],
                ]}
              >
                <Text
                  style={[
                    styles.messageText,
                    {
                      color: isUser ? '#FFFFFF' : theme.colors.textPrimary,
                    },
                  ]}
                >
                  {msg.content}
                </Text>
                <Text
                  style={[
                    styles.messageTime,
                    {
                      color: isUser
                        ? 'rgba(255, 255, 255, 0.7)'
                        : theme.colors.textTertiary,
                    },
                  ]}
                >
                  {formatTime(msg.created_at)}
                </Text>
              </View>
            </View>
          );
        })}

        {isTyping && (
          <View style={[styles.messageRow, styles.aiMessageRow]}>
            <View
              style={[
                styles.aiAvatarSmall,
                { backgroundColor: theme.colors.primaryLight },
              ]}
            >
              <Bot size={15} color={theme.colors.primary} />
            </View>
            <View
              style={[
                styles.typingBubble,
                {
                  backgroundColor: isDark
                    ? theme.colors.surface
                    : theme.colors.surfaceElevated,
                },
              ]}
            >
              <ActivityIndicator size="small" color={theme.colors.primary} />
              <Text style={[styles.typingText, { color: theme.colors.textSecondary }]}>
                Generating academic synthesis...
              </Text>
            </View>
          </View>
        )}

        {messages.length <= 2 && !isTyping && (
          <View style={styles.starterContainer}>
            <Text style={[styles.starterHeading, { color: theme.colors.textTertiary }]}>
              FACULTY PROMPTS & TEMPLATES
            </Text>
            {FACULTY_PROMPTS.map((prompt, idx) => (
              <Pressable
                key={idx}
                onPress={() => handleSendMessage(prompt)}
                style={[
                  styles.starterChip,
                  {
                    backgroundColor: isDark
                      ? theme.colors.surface
                      : theme.colors.surfaceElevated,
                    borderColor: theme.colors.borderLight,
                  },
                ]}
              >
                <Sparkles size={14} color={theme.colors.primary} />
                <Text
                  style={[styles.starterText, { color: theme.colors.textPrimary }]}
                  numberOfLines={2}
                >
                  {prompt}
                </Text>
              </Pressable>
            ))}
          </View>
        )}
      </ScrollView>

      {/* Input Bar */}
      <View
        style={[
          styles.inputBar,
          {
            backgroundColor: isDark ? theme.colors.surface : '#FFFFFF',
            borderTopColor: isDark ? theme.colors.border : theme.colors.borderLight,
          },
        ]}
      >
        <TextInput
          placeholder="Ask for test questions, rubric specs, or syllabi..."
          placeholderTextColor={theme.colors.textTertiary}
          value={inputText}
          onChangeText={setInputText}
          multiline
          maxLength={1000}
          style={[
            styles.textInput,
            {
              backgroundColor: isDark
                ? theme.colors.surfaceSubtle
                : theme.colors.inputBackground,
              color: theme.colors.textPrimary,
            },
          ]}
        />

        <Pressable
          onPress={() => handleSendMessage()}
          disabled={!inputText.trim() || isTyping}
          style={[
            styles.sendButton,
            {
              backgroundColor: inputText.trim()
                ? theme.colors.primary
                : theme.colors.border,
            },
          ]}
        >
          <Send size={18} color="#FFFFFF" />
        </Pressable>
      </View>
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
    borderBottomWidth: 1,
    borderColor: 'rgba(150, 150, 150, 0.1)',
  },
  topBarLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  botIconCircle: {
    width: 38,
    height: 38,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
  },
  topBarTitle: {
    fontSize: 16,
    fontWeight: '700',
  },
  topBarStatus: {
    fontSize: 11,
    fontWeight: '600',
  },
  iconButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
  messagesContainer: {
    paddingHorizontal: 16,
    paddingVertical: 20,
  },
  messageRow: {
    flexDirection: 'row',
    marginBottom: 16,
    alignItems: 'flex-end',
  },
  userMessageRow: {
    justifyContent: 'flex-end',
  },
  aiMessageRow: {
    justifyContent: 'flex-start',
    gap: 8,
  },
  aiAvatarSmall: {
    width: 26,
    height: 26,
    borderRadius: 13,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 4,
  },
  messageBubble: {
    maxWidth: '82%',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 18,
  },
  userBubble: {
    borderBottomRightRadius: 4,
  },
  aiBubble: {
    borderBottomLeftRadius: 4,
    borderWidth: 1,
  },
  messageText: {
    fontSize: 14,
    lineHeight: 21,
  },
  messageTime: {
    fontSize: 10,
    marginTop: 6,
    alignSelf: 'flex-end',
  },
  typingBubble: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingVertical: 10,
    paddingHorizontal: 14,
    borderRadius: 16,
    borderBottomLeftRadius: 4,
  },
  typingText: {
    fontSize: 12,
    fontStyle: 'italic',
  },
  starterContainer: {
    marginTop: 20,
    gap: 8,
  },
  starterHeading: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.8,
    marginBottom: 4,
  },
  starterChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    padding: 12,
    borderRadius: 12,
    borderWidth: 1,
  },
  starterText: {
    fontSize: 13,
    flex: 1,
  },
  inputBar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderTopWidth: 1,
    gap: 10,
  },
  textInput: {
    flex: 1,
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 8,
    fontSize: 14,
    maxHeight: 100,
  },
  sendButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
