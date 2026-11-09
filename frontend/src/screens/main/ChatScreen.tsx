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
import { Colors, Shadows, Radii } from '../../styles/theme';
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
    'What should I study today?',
    'Explain the next topic with examples',
    'Why did I struggle in my last quiz?',
    'Give me 3 practice coding exercises',
    'How much of my course is completed?',
  ];

  const facultyChips = [
    'How many eligible @vrsec.ac.in students are registered?',
    'Which students need additional academic support?',
    'Which branch has the lowest quiz pass rate?',
    'Summarize overall VRSEC student completion progress',
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
      ? `Hello Professor ${userName}. I am your **LearnPath Faculty AI Advisor**. I analyze real-time database records for eligible @vrsec.ac.in students. Ask me any question regarding departmental performance trends, cohort progress, or slow learner interventions.`
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
      setDrawerVisible(false);
    }
  };

  const loadPastSession = async (session: any) => {
    setCurrentSessionId(session.id);
    setSessionTitle(session.title);
    setDrawerVisible(false);
    try {
      const history = await api.getChatHistory(session.id);
      setMessages(history || []);
    } catch {
      setInitialGreeting();
    }
  };

  useEffect(() => {
    initChat();
  }, [email]);

  const handleSendMessage = async (textToSend?: string) => {
    const text = (textToSend || inputMessage).trim();
    if (!text || sending) return;

    setInputMessage('');
    const userMsg: Message = {
      id: `usr-${Date.now()}`,
      sender: 'user',
      content: text,
    };
    setMessages((prev) => [...prev, userMsg]);
    setSending(true);

    try {
      const res = await api.sendMessage(email, currentSessionId, text);
      const aiReply = res.reply || res.message || 'I have analyzed your query with the current database records.';

      const aiMsg: Message = {
        id: `ai-${Date.now()}`,
        sender: 'ai',
        content: aiReply,
      };
      setMessages((prev) => [...prev, aiMsg]);
      if (res.title && sessionTitle === 'New Session') {
        setSessionTitle(res.title);
      }
    } catch (err: any) {
      const errorMsg: Message = {
        id: `err-${Date.now()}`,
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

      {/* Top Bar with Session Drawer Trigger */}
      <View style={styles.header}>
        <TouchableOpacity style={styles.drawerBtn} onPress={() => setDrawerVisible(true)}>
          <Text style={styles.drawerBtnText}>☰ Sessions</Text>
        </TouchableOpacity>

        <View style={styles.headerTitleWrap}>
          <Text style={styles.headerSubtitle}>
            {isFaculty ? '🏛️ FACULTY ADVISORY HUB' : '🤖 PERSONALIZED AI TUTOR'}
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
            <View style={[styles.bubble, styles.aiBubble, { paddingVertical: 14 }]}>
              <ActivityIndicator color={Colors.primary} size="small" />
            </View>
          </View>
        )}
      </ScrollView>

      {/* Fast Recommendation Suggestion Chips */}
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

      {/* Message Input Bar */}
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={styles.inputBar}>
          <TextInput
            style={styles.textInput}
            placeholder={isFaculty ? 'Ask department analytics question...' : 'Ask your AI tutor anything...'}
            placeholderTextColor={Colors.textTertiary}
            value={inputMessage}
            onChangeText={setInputMessage}
            onSubmitEditing={() => handleSendMessage()}
            multiline
          />
          <TouchableOpacity
            style={[styles.sendBtn, (!inputMessage.trim() || sending) && styles.sendBtnDisabled]}
            onPress={() => handleSendMessage()}
            disabled={!inputMessage.trim() || sending}
          >
            <Text style={styles.sendBtnText}>↑</Text>
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>

      {/* =========================================================================
          SESSIONS DRAWER MODAL
      ========================================================================= */}
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
                <ActivityIndicator color={Colors.primary} size="small" style={{ marginVertical: 30 }} />
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
                      <Text style={[styles.sessionItemTitle, isCurrent && styles.sessionItemTitleActive]}>
                        {s.title || 'Session'}
                      </Text>
                      <Text style={styles.sessionStatusTag}>
                        {s.status || 'ACTIVE'}
                      </Text>
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
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: Colors.surface,
    borderBottomWidth: 1,
    borderColor: Colors.border,
  },
  drawerBtn: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: Radii.sm,
    backgroundColor: Colors.surfaceMuted,
  },
  drawerBtnText: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.textSecondary,
  },
  headerTitleWrap: {
    flex: 1,
    alignItems: 'center',
    paddingHorizontal: 10,
  },
  headerSubtitle: {
    fontSize: 10,
    fontWeight: '800',
    color: Colors.primary,
    letterSpacing: 0.5,
  },
  headerTitle: {
    fontSize: 14,
    fontWeight: '800',
    color: Colors.textPrimary,
  },
  newChatBtn: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: Radii.sm,
    backgroundColor: Colors.primaryLight,
  },
  newChatBtnText: {
    fontSize: 12,
    fontWeight: '800',
    color: Colors.primaryDark,
  },
  messageScroll: {
    flex: 1,
  },
  messageContent: {
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 20,
    gap: 14,
  },
  messageRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
  },
  userRow: {
    justifyContent: 'flex-end',
  },
  aiRow: {
    justifyContent: 'flex-start',
  },
  aiAvatar: {
    width: 32,
    height: 32,
    borderRadius: Radii.full,
    backgroundColor: Colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 8,
    marginTop: 2,
  },
  aiAvatarText: {
    fontSize: 16,
  },
  bubble: {
    maxWidth: '82%',
    borderRadius: Radii.lg,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  userBubble: {
    backgroundColor: Colors.primaryDark,
    borderBottomRightRadius: 4,
  },
  aiBubble: {
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
    borderBottomLeftRadius: 4,
    ...Shadows.small,
  },
  bubbleText: {
    fontSize: 14,
    lineHeight: 22,
  },
  userBubbleText: {
    color: '#FFFFFF',
  },
  aiBubbleText: {
    color: Colors.textPrimary,
  },
  chipsContainer: {
    backgroundColor: Colors.surface,
    borderTopWidth: 1,
    borderColor: Colors.border,
    paddingVertical: 8,
  },
  chipsScroll: {
    paddingHorizontal: 14,
    gap: 8,
  },
  chipPill: {
    backgroundColor: Colors.primaryLight,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: Radii.full,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  chipText: {
    fontSize: 12,
    fontWeight: '700',
    color: Colors.primaryDark,
  },
  inputBar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 14,
    paddingVertical: 10,
    backgroundColor: Colors.surface,
    borderTopWidth: 1,
    borderColor: Colors.border,
    gap: 10,
  },
  textInput: {
    flex: 1,
    backgroundColor: Colors.surfaceMuted,
    borderRadius: Radii.lg,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 14,
    color: Colors.textPrimary,
    maxHeight: 100,
  },
  sendBtn: {
    width: 40,
    height: 40,
    borderRadius: Radii.full,
    backgroundColor: Colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sendBtnDisabled: {
    backgroundColor: Colors.textTertiary,
  },
  sendBtnText: {
    color: '#FFFFFF',
    fontSize: 20,
    fontWeight: '900',
  },
  drawerOverlay: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.7)',
    justifyContent: 'flex-end',
  },
  drawerContent: {
    backgroundColor: Colors.surface,
    borderTopLeftRadius: Radii.xl,
    borderTopRightRadius: Radii.xl,
    padding: 22,
    maxHeight: '80%',
  },
  drawerHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  drawerTitle: {
    fontSize: 18,
    fontWeight: '900',
    color: Colors.textPrimary,
  },
  closeDrawerText: {
    fontSize: 14,
    color: Colors.textSecondary,
    fontWeight: '700',
  },
  drawerNewBtn: {
    backgroundColor: Colors.primary,
    paddingVertical: 12,
    borderRadius: Radii.md,
    alignItems: 'center',
    marginBottom: 16,
  },
  drawerNewBtnText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 14,
  },
  sessionItem: {
    backgroundColor: Colors.surfaceMuted,
    padding: 14,
    borderRadius: Radii.md,
    marginBottom: 10,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  sessionItemActive: {
    backgroundColor: Colors.primaryLight,
    borderWidth: 1.5,
    borderColor: Colors.primary,
  },
  sessionItemTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: Colors.textPrimary,
    flex: 1,
  },
  sessionItemTitleActive: {
    color: Colors.primaryDark,
    fontWeight: '800',
  },
  sessionStatusTag: {
    fontSize: 10,
    fontWeight: '800',
    color: Colors.textSecondary,
  },
  emptySessionText: {
    textAlign: 'center',
    color: Colors.textSecondary,
    fontSize: 13,
    marginVertical: 20,
  },
});
