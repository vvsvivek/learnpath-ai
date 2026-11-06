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
  Modal,
} from 'react-native';
import {
  Send,
  Bot,
  User,
  Plus,
  History,
  Sparkles,
  X,
  MessageSquare,
} from 'lucide-react-native';
import { useTheme } from '../../src/hooks/useTheme';
import { useAuth } from '../../src/hooks/useAuth';
import { Card } from '../../src/components/ui';
import { chatService } from '../../src/services/chatService';
import { ChatMessage, Conversation } from '../../src/types';
import { formatTime } from '../../src/utils/formatters';

const STARTER_PROMPTS = [
  'Explain backpropagation in neural networks using an analogy',
  'Give me an interview practice problem on Dynamic Programming',
  'How does the Raft consensus algorithm handle leader election?',
  'Help me design a database schema for an e-commerce platform',
];

export default function StudentAIChatScreen() {
  const { theme, isDark } = useTheme();
  const { user } = useAuth();

  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'm-0',
      conversation_id: 'conv-init',
      role: 'assistant',
      content: `Hello ${user?.name ? user.name.split(' ')[0] : 'there'}! 👋 I'm your AI Personalized Study Copilot. How can I help you master your curriculum today? Ask me any engineering concept, request code explanations, or get quiz prep!`,
      created_at: new Date().toISOString(),
    },
  ]);
  const [inputText, setInputText] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [currentConversationId, setCurrentConversationId] = useState<string>('conv-init');
  const [isHistoryModalOpen, setIsHistoryModalOpen] = useState(false);

  const scrollViewRef = useRef<any>(null);

  useEffect(() => {
    // Scroll to bottom on new message
    scrollViewRef.current?.scrollToEnd({ animated: true });
  }, [messages, isTyping]);

  const handleSendMessage = async (textToSend?: string) => {
    const content = (textToSend || inputText).trim();
    if (!content || isTyping) return;

    setInputText('');

    const userMessage: ChatMessage = {
      id: `msg-${Date.now()}`,
      conversation_id: currentConversationId,
      role: 'user',
      content,
      created_at: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setIsTyping(true);

    try {
      const response = await chatService.sendStudentMessage({
        conversation_id: currentConversationId,
        message: content,
        context: `Student ${user?.name || ''}, Year ${user?.academic_year || 3}, Branch ${user?.branch || 'CSE'}`,
      });

      setMessages((prev) => [...prev, response.message]);
    } catch (error) {
      // Offline fallback AI response simulator
      setTimeout(() => {
        const fallbackAiMessage: ChatMessage = {
          id: `ai-${Date.now()}`,
          conversation_id: currentConversationId,
          role: 'assistant',
          content: generateFallbackResponse(content),
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

  const generateFallbackResponse = (query: string): string => {
    const q = query.toLowerCase();
    if (q.includes('backprop') || q.includes('neural')) {
      return `Backpropagation is like adjusting a sound equalizer board:

1. **Forward Pass:** The audio signal (input data) passes through sliders (weights). You hear the song and realize the bass is too loud (the error/loss).
2. **Backward Pass:** Starting from the output speakers backwards to the knobs, you compute partial derivatives (gradients) using the chain rule to determine which slider caused the most distortion.
3. **Weight Update:** The optimizer (like Adam or SGD) nudges each knob slightly in the opposite direction of the error!`;
    }
    if (q.includes('dynamic programming') || q.includes('dp')) {
      return `Here is a high-frequency DP practice problem:

**Longest Increasing Subsequence (LIS)**
Given an integer array \`nums\`, return the length of the longest strictly increasing subsequence.

*Example:*
\`\`\`
Input: nums = [10, 9, 2, 5, 3, 7, 101, 18]
Output: 4
Explanation: The longest increasing subsequence is [2, 3, 7, 101], therefore the length is 4.
\`\`\`

*Hint:*
Define \`dp[i]\` as the length of LIS ending at index \`i\`.
Transition: \`dp[i] = max(dp[j] + 1)\` for all \`j < i\` where \`nums[j] < nums[i]\`.
Time complexity: O(N²) standard DP, or O(N log N) using binary search!`;
    }
    return `That's an important concept in B.Tech ${user?.branch || 'CSE'}!

In summary:
• **Core Theory:** Ensure you understand the underlying mathematical invariants and trade-offs before implementation.
• **Time & Space Complexity:** Always analyze how data scales in memory vs compute.
• **Application:** Consider how this integrates into production cloud services.

Would you like me to walk through a concrete code implementation in Python or C++?`;
  };

  const handleStartNewChat = () => {
    const newId = `conv-${Date.now()}`;
    setCurrentConversationId(newId);
    setMessages([
      {
        id: `m-${Date.now()}`,
        conversation_id: newId,
        role: 'assistant',
        content: `Started a fresh conversation. What would you like to explore next?`,
        created_at: new Date().toISOString(),
      },
    ]);
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
              AI Study Copilot
            </Text>
            <Text style={[styles.topBarStatus, { color: theme.colors.success }]}>
              ● Online • Powered by Gemini
            </Text>
          </View>
        </View>

        <View style={styles.topBarRight}>
          <Pressable
            onPress={handleStartNewChat}
            style={[styles.iconButton, { backgroundColor: theme.colors.surface }]}
          >
            <Plus size={18} color={theme.colors.textPrimary} />
          </Pressable>
        </View>
      </View>

      {/* Messages Scroll Area */}
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

        {/* Typing Indicator */}
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
                Gemini is thinking...
              </Text>
            </View>
          </View>
        )}

        {/* Starter Prompts (Show when short conversation) */}
        {messages.length <= 2 && !isTyping && (
          <View style={styles.starterContainer}>
            <Text style={[styles.starterHeading, { color: theme.colors.textTertiary }]}>
              SUGGESTED QUESTIONS
            </Text>
            {STARTER_PROMPTS.map((prompt, idx) => (
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
          placeholder="Ask a question about your courses..."
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
  topBarRight: {
    flexDirection: 'row',
    gap: 8,
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
