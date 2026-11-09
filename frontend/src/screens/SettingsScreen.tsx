import React, { useState } from 'react';
import {
  StyleSheet,
  Text,
  View,
  SafeAreaView,
  TouchableOpacity,
  Switch,
  StatusBar,
  ScrollView,
  Modal,
  TextInput,
  Alert,
  Platform,
} from 'react-native';
import { Colors } from '../styles/theme';

interface SettingsScreenProps {
  onBack: () => void;
  onLogout: () => void;
}

type ModalType = 'none' | 'appearance' | 'language' | 'privacy' | 'help' | 'about';

export default function SettingsScreen({ onBack, onLogout }: SettingsScreenProps) {
  const [notifications, setNotifications] = useState(true);
  const [appearance, setAppearance] = useState<'Light' | 'Soft Blue' | 'Dark'>('Light');
  const [language, setLanguage] = useState<'English' | 'Telugu' | 'Hindi'>('English');
  const [activeModal, setActiveModal] = useState<ModalType>('none');

  // Privacy & Security states
  const [twoFactorEnabled, setTwoFactorEnabled] = useState(true);
  const [biometricLock, setBiometricLock] = useState(false);
  const [newPassword, setNewPassword] = useState('');

  // Help & Support state
  const [supportMessage, setSupportMessage] = useState('');

  // Dynamic theme colors based on selected Appearance
  const isDark = appearance === 'Dark';
  const bgColor =
    appearance === 'Dark'
      ? '#0f172a'
      : appearance === 'Soft Blue'
      ? '#e0f2fe'
      : Colors.background;
  const cardBg = isDark ? '#1e293b' : Colors.cardBackground;
  const textPrimary = isDark ? '#f8fafc' : Colors.textPrimary;
  const textSecondary = isDark ? '#94a3b8' : Colors.textSecondary;
  const borderColor = isDark ? '#334155' : Colors.border;

  const handleSavePassword = () => {
    if (newPassword.trim().length < 6) {
      Alert.alert('Weak Password', 'Please enter at least 6 characters.');
      return;
    }
    setNewPassword('');
    setActiveModal('none');
    Alert.alert('Success', 'Your password has been updated securely.');
  };

  const handleSendSupport = () => {
    if (!supportMessage.trim()) {
      Alert.alert('Empty Message', 'Please describe your issue before submitting.');
      return;
    }
    setSupportMessage('');
    setActiveModal('none');
    Alert.alert('Support Ticket Sent', 'Our team will get back to you within 24 hours.');
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: bgColor }]}>
      <StatusBar barStyle={isDark ? 'light-content' : 'dark-content'} />

      {/* Header with Safe Camera Cutout Padding */}
      <View
        style={[
          styles.header,
          { backgroundColor: cardBg, borderBottomColor: borderColor },
        ]}
      >
        <TouchableOpacity onPress={onBack} style={styles.backBtn}>
          <Text style={[styles.backArrow, { color: textPrimary }]}>←</Text>
        </TouchableOpacity>
        <Text style={[styles.headerTitle, { color: textPrimary }]}>Settings</Text>
        <View style={{ width: 36 }} />
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View
          style={[
            styles.settingsCard,
            { backgroundColor: cardBg, borderColor: borderColor },
          ]}
        >
          {/* 1. Notifications */}
          <View style={[styles.row, { borderBottomColor: borderColor }]}>
            <View style={styles.rowLeft}>
              <Text style={styles.rowIcon}>🔔</Text>
              <View>
                <Text style={[styles.rowLabel, { color: textPrimary }]}>
                  Notifications
                </Text>
                <Text style={[styles.rowSub, { color: textSecondary }]}>
                  {notifications ? 'Push & study reminders active' : 'Muted'}
                </Text>
              </View>
            </View>
            <Switch
              value={notifications}
              onValueChange={setNotifications}
              trackColor={{ false: '#cbd5e1', true: '#93c5fd' }}
              thumbColor={notifications ? Colors.primary : '#64748b'}
            />
          </View>

          {/* 2. Appearance */}
          <TouchableOpacity
            style={[styles.row, { borderBottomColor: borderColor }]}
            onPress={() => setActiveModal('appearance')}
          >
            <View style={styles.rowLeft}>
              <Text style={styles.rowIcon}>🌙</Text>
              <Text style={[styles.rowLabel, { color: textPrimary }]}>
                Appearance
              </Text>
            </View>
            <Text style={[styles.rowValue, { color: textSecondary }]}>
              {appearance} ›
            </Text>
          </TouchableOpacity>

          {/* 3. Language */}
          <TouchableOpacity
            style={[styles.row, { borderBottomColor: borderColor }]}
            onPress={() => setActiveModal('language')}
          >
            <View style={styles.rowLeft}>
              <Text style={styles.rowIcon}>🌐</Text>
              <Text style={[styles.rowLabel, { color: textPrimary }]}>
                Language
              </Text>
            </View>
            <Text style={[styles.rowValue, { color: textSecondary }]}>
              {language} ›
            </Text>
          </TouchableOpacity>

          {/* 4. Privacy & Security */}
          <TouchableOpacity
            style={[styles.row, { borderBottomColor: borderColor }]}
            onPress={() => setActiveModal('privacy')}
          >
            <View style={styles.rowLeft}>
              <Text style={styles.rowIcon}>🔒</Text>
              <Text style={[styles.rowLabel, { color: textPrimary }]}>
                Privacy & Security
              </Text>
            </View>
            <Text style={[styles.rowValue, { color: textSecondary }]}>›</Text>
          </TouchableOpacity>

          {/* 5. Help & Support */}
          <TouchableOpacity
            style={[styles.row, { borderBottomColor: borderColor }]}
            onPress={() => setActiveModal('help')}
          >
            <View style={styles.rowLeft}>
              <Text style={styles.rowIcon}>❓</Text>
              <Text style={[styles.rowLabel, { color: textPrimary }]}>
                Help & Support
              </Text>
            </View>
            <Text style={[styles.rowValue, { color: textSecondary }]}>›</Text>
          </TouchableOpacity>

          {/* 6. About App */}
          <TouchableOpacity
            style={[styles.row, { borderBottomWidth: 0 }]}
            onPress={() => setActiveModal('about')}
          >
            <View style={styles.rowLeft}>
              <Text style={styles.rowIcon}>ℹ️</Text>
              <Text style={[styles.rowLabel, { color: textPrimary }]}>
                About App
              </Text>
            </View>
            <Text style={[styles.rowValue, { color: textSecondary }]}>›</Text>
          </TouchableOpacity>
        </View>

        {/* Log Out Button */}
        <TouchableOpacity
          style={styles.logoutBtn}
          onPress={() =>
            Alert.alert('Log Out', 'Are you sure you want to log out?', [
              { text: 'Cancel', style: 'cancel' },
              { text: 'Log Out', style: 'destructive', onPress: onLogout },
            ])
          }
        >
          <Text style={styles.logoutText}>🚪 Log Out</Text>
        </TouchableOpacity>
      </ScrollView>

      {/* INTERACTIVE MODAL FOR ALL SETTINGS OPTIONS */}
      <Modal
        visible={activeModal !== 'none'}
        transparent
        animationType="fade"
        onRequestClose={() => setActiveModal('none')}
      >
        <View style={styles.modalOverlay}>
          <View style={[styles.modalCard, { backgroundColor: cardBg }]}>
            {/* APPEARANCE MODAL */}
            {activeModal === 'appearance' && (
              <>
                <Text style={[styles.modalTitle, { color: textPrimary }]}>
                  Select Appearance
                </Text>
                {(['Light', 'Soft Blue', 'Dark'] as const).map((mode) => (
                  <TouchableOpacity
                    key={mode}
                    style={[
                      styles.optionItem,
                      appearance === mode && styles.optionItemActive,
                    ]}
                    onPress={() => {
                      setAppearance(mode);
                      setActiveModal('none');
                    }}
                  >
                    <Text
                      style={[
                        styles.optionText,
                        { color: appearance === mode ? Colors.primary : textPrimary },
                      ]}
                    >
                      {mode} Theme
                    </Text>
                    {appearance === mode && (
                      <Text style={{ color: Colors.primary, fontWeight: '800' }}>
                        ✓
                      </Text>
                    )}
                  </TouchableOpacity>
                ))}
              </>
            )}

            {/* LANGUAGE MODAL */}
            {activeModal === 'language' && (
              <>
                <Text style={[styles.modalTitle, { color: textPrimary }]}>
                  Choose Language
                </Text>
                {(['English', 'Telugu', 'Hindi'] as const).map((lang) => (
                  <TouchableOpacity
                    key={lang}
                    style={[
                      styles.optionItem,
                      language === lang && styles.optionItemActive,
                    ]}
                    onPress={() => {
                      setLanguage(lang);
                      setActiveModal('none');
                    }}
                  >
                    <Text
                      style={[
                        styles.optionText,
                        { color: language === lang ? Colors.primary : textPrimary },
                      ]}
                    >
                      {lang}
                    </Text>
                    {language === lang && (
                      <Text style={{ color: Colors.primary, fontWeight: '800' }}>
                        ✓
                      </Text>
                    )}
                  </TouchableOpacity>
                ))}
              </>
            )}

            {/* PRIVACY & SECURITY MODAL */}
            {activeModal === 'privacy' && (
              <>
                <Text style={[styles.modalTitle, { color: textPrimary }]}>
                  Privacy & Security
                </Text>
                <View style={styles.modalToggleRow}>
                  <Text style={[styles.optionText, { color: textPrimary }]}>
                    Email OTP Verification
                  </Text>
                  <Switch
                    value={twoFactorEnabled}
                    onValueChange={setTwoFactorEnabled}
                  />
                </View>
                <View style={styles.modalToggleRow}>
                  <Text style={[styles.optionText, { color: textPrimary }]}>
                    Biometric App Lock
                  </Text>
                  <Switch
                    value={biometricLock}
                    onValueChange={setBiometricLock}
                  />
                </View>
                <Text style={[styles.inputLabel, { color: textSecondary }]}>
                  Update Password
                </Text>
                <TextInput
                  style={[
                    styles.modalInput,
                    { color: textPrimary, borderColor: borderColor },
                  ]}
                  placeholder="Enter new password"
                  placeholderTextColor="#94a3b8"
                  secureTextEntry
                  value={newPassword}
                  onChangeText={setNewPassword}
                />
                <TouchableOpacity
                  style={styles.modalPrimaryBtn}
                  onPress={handleSavePassword}
                >
                  <Text style={styles.modalPrimaryBtnText}>Save Security Settings</Text>
                </TouchableOpacity>
              </>
            )}

            {/* HELP & SUPPORT MODAL */}
            {activeModal === 'help' && (
              <>
                <Text style={[styles.modalTitle, { color: textPrimary }]}>
                  Help & Support
                </Text>
                <Text style={[styles.faqText, { color: textSecondary }]}>
                  • How does AI Roadmap work?{'\n'}  It adapts phases to your branch and CGPA.{'\n'}
                  • Need faculty assistance?{'\n'}  Send a message below:
                </Text>
                <TextInput
                  style={[
                    styles.modalInput,
                    { height: 80, textAlignVertical: 'top', color: textPrimary, borderColor: borderColor },
                  ]}
                  placeholder="Describe your question or issue..."
                  placeholderTextColor="#94a3b8"
                  multiline
                  value={supportMessage}
                  onChangeText={setSupportMessage}
                />
                <TouchableOpacity
                  style={styles.modalPrimaryBtn}
                  onPress={handleSendSupport}
                >
                  <Text style={styles.modalPrimaryBtnText}>Submit Ticket</Text>
                </TouchableOpacity>
              </>
            )}

            {/* ABOUT APP MODAL */}
            {activeModal === 'about' && (
              <>
                <Text style={[styles.modalTitle, { color: textPrimary }]}>
                  About AI Learning Path
                </Text>
                <Text style={[styles.faqText, { color: textSecondary }]}>
                  Version: 1.0.0 (Production Build){'\n'}
                  Stack: React Native, Spring Boot & PostgreSQL{'\n\n'}
                  Designed to empower students with personalized AI roadmaps, real-time progress tracking, and interactive mentorship.
                </Text>
              </>
            )}

            <TouchableOpacity
              style={styles.modalCloseBtn}
              onPress={() => setActiveModal('none')}
            >
              <Text style={styles.modalCloseText}>Close</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingTop: Platform.OS === 'android' ? (StatusBar.currentHeight || 28) + 12 : 16,
    paddingBottom: 16,
    borderBottomWidth: 1,
  },
  backBtn: {
    padding: 4,
    width: 36,
  },
  backArrow: {
    fontSize: 22,
    fontWeight: '800',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: '800',
  },
  scrollContent: {
    padding: 20,
  },
  settingsCard: {
    borderRadius: 24,
    paddingHorizontal: 18,
    paddingVertical: 6,
    borderWidth: 1,
    marginBottom: 24,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 18,
    borderBottomWidth: 1,
  },
  rowLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  rowIcon: {
    fontSize: 20,
    marginRight: 14,
  },
  rowLabel: {
    fontSize: 16,
    fontWeight: '700',
  },
  rowSub: {
    fontSize: 11,
    marginTop: 2,
  },
  rowValue: {
    fontSize: 15,
    fontWeight: '600',
  },
  logoutBtn: {
    backgroundColor: '#fee2e2',
    borderRadius: 20,
    paddingVertical: 18,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#fecaca',
  },
  logoutText: {
    color: '#b91c1c',
    fontSize: 16,
    fontWeight: '800',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.45)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  modalCard: {
    width: '100%',
    borderRadius: 24,
    padding: 22,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '800',
    marginBottom: 16,
  },
  optionItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 14,
    borderRadius: 14,
    marginBottom: 8,
  },
  optionItemActive: {
    backgroundColor: '#e0f2fe',
  },
  optionText: {
    fontSize: 15,
    fontWeight: '700',
  },
  modalToggleRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 10,
  },
  inputLabel: {
    fontSize: 13,
    fontWeight: '700',
    marginTop: 12,
    marginBottom: 6,
  },
  modalInput: {
    borderWidth: 1,
    borderRadius: 14,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 14,
    marginBottom: 14,
  },
  modalPrimaryBtn: {
    backgroundColor: Colors.primary,
    borderRadius: 14,
    paddingVertical: 13,
    alignItems: 'center',
    marginTop: 4,
  },
  modalPrimaryBtnText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '800',
  },
  faqText: {
    fontSize: 14,
    lineHeight: 21,
    marginBottom: 14,
  },
  modalCloseBtn: {
    marginTop: 12,
    paddingVertical: 10,
    alignItems: 'center',
  },
  modalCloseText: {
    color: Colors.primary,
    fontSize: 14,
    fontWeight: '700',
  },
});