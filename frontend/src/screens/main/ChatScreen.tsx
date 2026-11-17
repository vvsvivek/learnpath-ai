import React, { useState, useEffect, useRef } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TextInput,
  TouchableOpacity,
  SafeAreaView,
  StatusBar,
  KeyboardAvoidingView,
  Platform,
  ActivityIndicator,
  Modal,
} from 'react-native';
import { Colors, Shadows, Radii, Spacing, Typography } from '../../styles/theme';
import { api } from '../../services/api';

interface Message {
  id: string;
  sender: 'user' | 'ai' | 'bot';
  content: string;
}

interface ChatScreenProps {
  userData?: any;
  activeSubject?: string;
}

export default function ChatScreen({ userData, activeSubject }: ChatScreenProps) {
  const email = (userData?.email || '').trim().toLowerCase();
  const userName = userData?.username || 'Learner';
  const rawRole = (userData?.role || 'STUDENT').toUpperCase();
  const isFaculty = rawRole === 'FACULTY' || email.endsWith('@siddhartha.ac.in') || email === 'vivekvaranasi99@gmail.com';

  const [currentSessionId, setCurrentSessionId] = useState<number>(() => Date.now());
  const [sessionTitle, setSessionTitle] = useState('New Session');
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [sending, setSending] = useState(false);

  // Session Drawer State
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [sessions, setSessions] = useState<any[]>([]);
  const [loadingSessions, setLoadingSessions] = useState(false);

  const scrollRef = useRef<any>(null);

  // Student vs Faculty Suggestion Chips
  const studentChips = [
    'Explain a concept',
    'Analyze my learning progress',
    'Review my quiz mistakes',
    'Create a study plan',
    'What courses am I enrolled in?',
  ];

  const facultyChips = [
    'Analyze student performance',
    'Identify students needing support',
    'Review course assessment results',
    'Suggest remedial activities',
    'Summarize department cohort progress',
  ];

  const suggestionChips = isFaculty ? facultyChips : studentChips;

  // Initialize or load active session
  const initChat = async () => {
    if (!email) return;
    try {
      setLoadingSessions(true);
      const existingSessions = await api.getChatSessions(email);
      setSessions(existingSessions || []);

      if (existingSessions && existingSessions.length > 0) {
        const latest = existingSessions[0];
        setCurrentSessionId(latest.id);
        setSessionTitle(latest.title);
        const history = await api.getChatHistory(latest.id);
        if (history && history.length > 0) {
          setMessages(history);
        } else {
          setInitialGreeting();
        }
      } else {
        await startNewChatSession();
      }
    } catch {
      setInitialGreeting();
    } finally {
      setLoadingSessions(false);
    }
  };

  const setInitialGreeting = () => {
    const greetingText = isFaculty
      ? `Hello Professor ${userName}. I am your **LearnPath Faculty AI Advisor**. I analyze real-time records for @vrsec.ac.in students. Ask me regarding departmental performance trends, cohort progress, or slow learner interventions.`
      : `Hello **${userName}**! 👋 I am your dedicated **LearnPath AI Tutor**. I have verified your enrolled courses and quiz metrics. Ask me to explain concepts, give code examples, review mistakes from your quizzes, or generate a tailored study plan!`;

    setMessages([
      {
        id: 'init-1',
        sender: 'ai',
        content: greetingText,
      },
    ]);
  };

  const startNewChatSession = async () => {
    try {
      const newSession = await api.createChatSession(email, isFaculty ? 'FACULTY' : 'STUDENT');
      setCurrentSessionId(newSession.id);
      setSessionTitle(newSession.title || 'New Session');
      setInitialGreeting();
      setDrawerVisible(false);
      const updatedList = await api.getChatSessions(email);
      setSessions(updatedList || []);
    } catch {
      setCurrentSessionId(Date.now());
      setInitialGreeting();
    }
  };

  const loadPastSession = async (session: any) => {
    try {
      setCurrentSessionId(session.id);
      setSessionTitle(session.title || 'Session');
      setDrawerVisible(false);
      const history = await api.getChatHistory(session.id);
      if (history && history.length > 0) {
        setMessages(history);
      } else {
        setInitialGreeting();
      }
    } catch {
      setInitialGreeting();
    }
  };

  useEffect(() => {
    initChat();
  }, [email]);

  const handleSendMessage = async (textToSend?: string) => {
    const messageContent = (textToSend || inputMessage).trim();
    if (!messageContent || sending) return;

    const userMsg: Message = {
      id: Date.now().toString(),
      sender: 'user',
      content: messageContent,
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputMessage('');
    setSending(true);

    setTimeout(() => {
      scrollRef.current?.scrollToEnd({ animated: true });
    }, 50);

    try {
      const response = await api.sendMessage(
        email,
        currentSessionId,
        messageContent
      );

      if (response?.isError) {
        const errorText = response?.reply || response?.error || '⚠️ AI service is currently unavailable. Please retry in a few moments.';
        const errorMsg: Message = {
          id: (Date.now() + 1).toString(),
          sender: 'ai',
          content: errorText,
        };
        setMessages((prev) => [...prev, errorMsg]);
      } else {
        const aiText = response?.reply || response?.content || response?.message || '⚠️ No response received from AI service. Please retry.';
        const aiMsg: Message = {
          id: (Date.now() + 1).toString(),
          sender: 'ai',
          content: aiText,
        };
        setMessages((prev) => [...prev, aiMsg]);
        if (response?.title && sessionTitle === 'New Session') {
          setSessionTitle(response.title);
        }
      }
    } catch (err: any) {
      const errorMsg: Message = {
        id: (Date.now() + 1).toString(),
        sender: 'ai',
        content: `⚠️ Error communicating with AI: ${err.message || 'Please check your connection and try again.'}`,
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setSending(false);
      setTimeout(() => {
        scrollRef.current?.scrollToEnd({ animated: true });
      }, 100);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />

      <KeyboardAvoidingView
        style={styles.keyboardContainer}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        {/* Compact Top Bar */}
        <View style={styles.header}>
          <TouchableOpacity style={styles.drawerBtn} onPress={() => setDrawerVisible(true)}>
            <Text style={styles.drawerBtnText}>☰ Sessions</Text>
          </TouchableOpacity>

          <View style={styles.headerTitleWrap}>
            <Text style={styles.headerSubtitle}>
              {isFaculty ? 'FACULTY AI ADVISOR' : 'AI TUTOR'}
            </Text>
            <Text style={styles.headerTitle} numberOfLines={1}>
              {sessionTitle}
            </Text>
          </View>

          <TouchableOpacity style={styles.newChatBtn} onPress={startNewChatSession}>
            <Text style={styles.newChatBtnText}>+ New</Text>
          </TouchableOpacity>
        </View>

        {/* Chat Messages List */}
        <ScrollView
          ref={scrollRef}
          style={styles.messageScroll}
          contentContainerStyle={styles.messageContent}
          showsVerticalScrollIndicator={false}
          onContentSizeChange={() => scrollRef.current?.scrollToEnd({ animated: false })}
        >
          {messages.map((m, idx) => {
            const isUser = m.sender === 'user';
            return (
              <View key={idx} style={[styles.messageRow, isUser ? styles.userRow : styles.aiRow]}>
                {!isUser && (
                  <View style={styles.aiAvatar}>
                    <Text style={styles.aiAvatarText}>{isFaculty ? '🏛️' : '🤖'}</Text>
                  </View>
                )}
                <View style={[styles.bubble, isUser ? styles.userBubble : styles.aiBubble]}>
                  <Text style={[styles.bubbleText, isUser ? styles.userBubbleText : styles.aiBubbleText]}>
                    {m.content}
                  </Text>
                </View>
              </View>
            );
          })}

          {sending && (
            <View style={styles.aiRow}>
              <View style={styles.aiAvatar}>
                <Text style={styles.aiAvatarText}>{isFaculty ? '🏛️' : '🤖'}</Text>
              </View>
              <View style={[styles.bubble, styles.aiBubble, { paddingVertical: 10, paddingHorizontal: 14 }]}>
                <ActivityIndicator color={Colors.primary} size="small" />
              </View>
            </View>
          )}
        </ScrollView>

        {/* Fast Suggestion Chips */}
        <View style={styles.chipsContainer}>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chipsScroll}>
            {suggestionChips.map((chip, idx) => (
              <TouchableOpacity
                key={idx}
                style={styles.chipPill}
                onPress={() => handleSendMessage(chip)}
                disabled={sending}
              >
                <Text style={styles.chipText}>{chip}</Text>
              </TouchableOpacity>
            ))}
          </ScrollView>
        </View>

        {/* Clean Docked Message Input Bar */}
        <View style={styles.inputBar}>
          <TextInput
            style={styles.textInput}
            placeholder={isFaculty ? 'Ask department analytics question...' : 'Ask your AI tutor anything...'}
            placeholderTextColor={Colors.textTertiary}
            value={inputMessage}
            onChangeText={setInputMessage}
            multiline
            maxLength={1000}
          />
          <TouchableOpacity
            style={[styles.sendBtn, (!inputMessage.trim() || sending) && styles.sendBtnDisabled]}
            onPress={() => handleSendMessage()}
            disabled={!inputMessage.trim() || sending}
            activeOpacity={0.7}
          >
            <Text style={styles.sendBtnText}>↑</Text>
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>

      {/* Sessions Drawer Modal */}
      <Modal visible={drawerVisible} animationType="slide" transparent>
        <SafeAreaView style={styles.drawerOverlay}>
          <View style={styles.drawerContent}>
            <View style={styles.drawerHeader}>
              <Text style={styles.drawerTitle}>Saved AI Conversations</Text>
              <TouchableOpacity onPress={() => setDrawerVisible(false)}>
                <Text style={styles.closeDrawerText}>✕ Close</Text>
              </TouchableOpacity>
            </View>

            <TouchableOpacity style={styles.drawerNewBtn} onPress={startNewChatSession}>
              <Text style={styles.drawerNewBtnText}>+ Start Fresh Session</Text>
            </TouchableOpacity>

            <ScrollView showsVerticalScrollIndicator={false}>
              {loadingSessions ? (
                <ActivityIndicator color={Colors.primary} size="small" style={{ marginVertical: 20 }} />
              ) : sessions.length === 0 ? (
                <Text style={styles.emptySessionText}>No past conversations stored.</Text>
              ) : (
                sessions.map((s: any, idx: number) => {
                  const isCurrent = s.id === currentSessionId;
                  return (
                    <TouchableOpacity
                      key={idx}
                      style={[styles.sessionItem, isCurrent && styles.sessionItemActive]}
                      onPress={() => loadPastSession(s)}
                    >
                      <Text
                        style={[styles.sessionItemTitle, isCurrent && styles.sessionItemTitleActive]}
                        numberOfLines={1}
                      >
                        {s.title || 'Session'}
                      </Text>
                      <Text style={styles.sessionStatusTag}>{s.status || 'ACTIVE'}</Text>
                    </TouchableOpacity>
                  );
                })
              )}
            </ScrollView>
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
  keyboardContainer: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.md,
    paddingVertical: 8,
    backgroundColor: Colors.surface,
    borderBottomWidth: 1,
    borderColor: Colors.border,
    minHeight: 46,
  },
  drawerBtn: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: Radii.sm,
    backgroundColor: Colors.surfaceMuted,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  drawerBtnText: {
    fontSize: 11,
    fontWeight: '600',
    color: Colors.textSecondary,
  },
  headerTitleWrap: {
    flex: 1,
    alignItems: 'center',
    paddingHorizontal: 8,
  },
  headerSubtitle: {
    fontSize: 9,
    fontWeight: '700',
    color: Colors.primary,
    letterSpacing: 0.5,
  },
  headerTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  newChatBtn: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: Radii.sm,
    backgroundColor: Colors.primaryLight,
  },
  newChatBtnText: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.primaryDark,
  },
  messageScroll: {
    flex: 1,
  },
  messageContent: {
    paddingHorizontal: Spacing.md,
    paddingTop: Spacing.md,
    paddingBottom: Spacing.md,
  },
  messageRow: {
    flexDirection: 'row',
    marginBottom: 10,
    alignItems: 'flex-end',
  },
  userRow: {
    justifyContent: 'flex-end',
  },
  aiRow: {
    justifyContent: 'flex-start',
  },
  aiAvatar: {
    width: 28,
    height: 28,
    borderRadius: Radii.full,
    backgroundColor: Colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 6,
    marginBottom: 2,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  aiAvatarText: {
    fontSize: 13,
  },
  bubble: {
    maxWidth: '82%',
    borderRadius: Radii.md,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  userBubble: {
    backgroundColor: Colors.primary,
    borderBottomRightRadius: 2,
  },
  aiBubble: {
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
    borderBottomLeftRadius: 2,
    ...Shadows.small,
  },
  bubbleText: {
    fontSize: 13,
    lineHeight: 18,
  },
  userBubbleText: {
    color: '#FFFFFF',
    fontWeight: '400',
  },
  aiBubbleText: {
    color: Colors.textPrimary,
    fontWeight: '400',
  },
  chipsContainer: {
    backgroundColor: Colors.surface,
    borderTopWidth: 1,
    borderTopColor: Colors.borderLight,
    paddingVertical: 6,
  },
  chipsScroll: {
    paddingHorizontal: Spacing.md,
    gap: 6,
  },
  chipPill: {
    backgroundColor: Colors.surfaceMuted,
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: Radii.full,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  chipText: {
    fontSize: 11,
    color: Colors.textSecondary,
    fontWeight: '500',
  },
  inputBar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 10,
    paddingVertical: 6,
    backgroundColor: Colors.surface,
    borderTopWidth: 1,
    borderColor: Colors.border,
    gap: 8,
    minHeight: 48,
  },
  textInput: {
    flex: 1,
    backgroundColor: Colors.surfaceMuted,
    borderRadius: Radii.md,
    paddingHorizontal: 12,
    paddingVertical: 7,
    fontSize: 13,
    color: Colors.textPrimary,
    maxHeight: 80,
  },
  sendBtn: {
    width: 32,
    height: 32,
    borderRadius: Radii.full,
    backgroundColor: Colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sendBtnDisabled: {
    backgroundColor: Colors.border,
  },
  sendBtnText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
  drawerOverlay: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.65)',
    justifyContent: 'flex-end',
  },
  drawerContent: {
    backgroundColor: Colors.surface,
    borderTopLeftRadius: Radii.lg,
    borderTopRightRadius: Radii.lg,
    padding: Spacing.lg,
    maxHeight: '75%',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  drawerHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.md,
  },
  drawerTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  closeDrawerText: {
    fontSize: 12,
    color: Colors.textSecondary,
    fontWeight: '600',
  },
  drawerNewBtn: {
    backgroundColor: Colors.primary,
    paddingVertical: 9,
    borderRadius: Radii.md,
    alignItems: 'center',
    marginBottom: Spacing.md,
  },
  drawerNewBtnText: {
    color: '#FFFFFF',
    fontWeight: '600',
    fontSize: 12,
  },
  sessionItem: {
    backgroundColor: Colors.surfaceMuted,
    padding: 10,
    borderRadius: Radii.md,
    marginBottom: 8,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  sessionItemActive: {
    backgroundColor: Colors.primaryLight,
    borderColor: Colors.primary,
  },
  sessionItemTitle: {
    fontSize: 12,
    fontWeight: '600',
    color: Colors.textPrimary,
    flex: 1,
    marginRight: 8,
  },
  sessionItemTitleActive: {
    color: Colors.primaryDark,
    fontWeight: '700',
  },
  sessionStatusTag: {
    fontSize: 9,
    fontWeight: '700',
    color: Colors.textTertiary,
  },
  emptySessionText: {
    fontSize: 12,
    color: Colors.textSecondary,
    textAlign: 'center',
    marginVertical: 20,
  },
});
