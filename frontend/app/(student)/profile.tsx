import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Switch,
  Pressable,
  Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import {
  User,
  Moon,
  Sun,
  Smartphone,
  Bell,
  Clock,
  Shield,
  HelpCircle,
  Info,
  LogOut,
  ChevronRight,
  GraduationCap,
  Award,
} from 'lucide-react-native';
import { useTheme } from '../../src/hooks/useTheme';
import { useAuth } from '../../src/hooks/useAuth';
import { Avatar, Badge, Button, Card } from '../../src/components/ui';
import { ThemeMode } from '../../src/context/ThemeContext';

export default function StudentProfileScreen() {
  const router = useRouter();
  const { theme, themeMode, setThemeMode, isDark } = useTheme();
  const { user, logout } = useAuth();

  const [notificationsEnabled, setNotificationsEnabled] = useState(true);
  const [dailyRemindersEnabled, setDailyRemindersEnabled] = useState(true);

  const handleLogout = () => {
    Alert.alert('Sign Out', 'Are you sure you want to log out of LearnPath AI?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Log Out',
        style: 'destructive',
        onPress: async () => {
          await logout();
          router.replace('/(auth)/login');
        },
      },
    ]);
  };

  const handleThemeModeChange = async (mode: ThemeMode) => {
    await setThemeMode(mode);
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      {/* Top Header */}
      <View style={styles.header}>
        <Text style={[styles.headerTitle, { color: theme.colors.textPrimary }]}>
          Profile & Settings
        </Text>
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* Profile Card */}
        <Card variant="elevated" style={styles.profileCard}>
          <View style={styles.profileRow}>
            <Avatar
              name={user?.name || 'Student'}
              imageUrl={user?.avatar_url}
              size="lg"
            />
            <View style={styles.profileInfo}>
              <Text style={[styles.userName, { color: theme.colors.textPrimary }]}>
                {user?.name || 'Vivek Varanasi'}
              </Text>
              <Text style={[styles.userEmail, { color: theme.colors.textSecondary }]}>
                {user?.email || 'student@example.com'}
              </Text>
              <View style={styles.badgeRow}>
                <Badge
                  label={`${user?.branch || 'CSE'} • Year ${user?.academic_year || 3}`}
                  variant="primary"
                  size="sm"
                />
                <Badge
                  label={`CGPA: ${user?.cgpa ? user.cgpa.toFixed(2) : '8.85'}`}
                  variant="success"
                  size="sm"
                />
              </View>
            </View>
          </View>
        </Card>

        {/* Theme Settings Section */}
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: theme.colors.textSecondary }]}>
            APPEARANCE
          </Text>
          <Card style={styles.settingsCard}>
            <View style={styles.themeSelector}>
              {(['light', 'dark', 'system'] as ThemeMode[]).map((mode) => {
                const isSelected = themeMode === mode;
                return (
                  <Pressable
                    key={mode}
                    onPress={() => handleThemeModeChange(mode)}
                    style={[
                      styles.themeOption,
                      isSelected && {
                        backgroundColor: theme.colors.surfaceElevated,
                        shadowColor: '#000',
                        shadowOffset: { width: 0, height: 2 },
                        shadowOpacity: 0.08,
                        shadowRadius: 4,
                        elevation: 2,
                      },
                    ]}
                  >
                    {mode === 'light' ? (
                      <Sun size={18} color={isSelected ? theme.colors.primary : theme.colors.textTertiary} />
                    ) : mode === 'dark' ? (
                      <Moon size={18} color={isSelected ? theme.colors.primary : theme.colors.textTertiary} />
                    ) : (
                      <Smartphone size={18} color={isSelected ? theme.colors.primary : theme.colors.textTertiary} />
                    )}
                    <Text
                      style={[
                        styles.themeOptionText,
                        {
                          color: isSelected ? theme.colors.primary : theme.colors.textSecondary,
                          fontWeight: isSelected ? '700' : '500',
                        },
                      ]}
                    >
                      {mode.charAt(0).toUpperCase() + mode.slice(1)}
                    </Text>
                  </Pressable>
                );
              })}
            </View>
          </Card>
        </View>

        {/* Notifications & Reminders */}
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: theme.colors.textSecondary }]}>
            PREFERENCES
          </Text>
          <Card style={styles.settingsCard}>
            <View style={styles.settingRow}>
              <View style={styles.settingLeft}>
                <Bell size={18} color={theme.colors.primary} />
                <View>
                  <Text style={[styles.settingLabel, { color: theme.colors.textPrimary }]}>
                    Push Notifications
                  </Text>
                  <Text style={[styles.settingSub, { color: theme.colors.textTertiary }]}>
                    Course assignments and study streak alerts
                  </Text>
                </View>
              </View>
              <Switch
                value={notificationsEnabled}
                onValueChange={setNotificationsEnabled}
                trackColor={{ false: theme.colors.border, true: theme.colors.primary }}
              />
            </View>

            <View style={[styles.settingRow, { borderTopWidth: 1, borderColor: 'rgba(150, 150, 150, 0.1)' }]}>
              <View style={styles.settingLeft}>
                <Clock size={18} color={theme.colors.primary} />
                <View>
                  <Text style={[styles.settingLabel, { color: theme.colors.textPrimary }]}>
                    Daily Study Reminders
                  </Text>
                  <Text style={[styles.settingSub, { color: theme.colors.textTertiary }]}>
                    Keep your learning streak active
                  </Text>
                </View>
              </View>
              <Switch
                value={dailyRemindersEnabled}
                onValueChange={setDailyRemindersEnabled}
                trackColor={{ false: theme.colors.border, true: theme.colors.primary }}
              />
            </View>
          </Card>
        </View>

        {/* Academic Details & Department */}
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: theme.colors.textSecondary }]}>
            INSTITUTION
          </Text>
          <Card style={styles.settingsCard}>
            <View style={styles.infoRow}>
              <Text style={[styles.infoLabel, { color: theme.colors.textSecondary }]}>
                College
              </Text>
              <Text style={[styles.infoValue, { color: theme.colors.textPrimary }]}>
                V.R. Siddhartha Engg. College
              </Text>
            </View>
            <View style={[styles.infoRow, { borderTopWidth: 1, borderColor: 'rgba(150, 150, 150, 0.1)' }]}>
              <Text style={[styles.infoLabel, { color: theme.colors.textSecondary }]}>
                Degree
              </Text>
              <Text style={[styles.infoValue, { color: theme.colors.textPrimary }]}>
                B.Tech in {user?.branch || 'CSE'}
              </Text>
            </View>
          </Card>
        </View>

        {/* Support & Privacy */}
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: theme.colors.textSecondary }]}>
            ABOUT & SUPPORT
          </Text>
          <Card style={styles.settingsCard}>
            <Pressable
              onPress={() => Alert.alert('Help & Support', 'Reach out to support@learnpathai.in for any questions or feedback.')}
              style={styles.navRow}
            >
              <View style={styles.settingLeft}>
                <HelpCircle size={18} color={theme.colors.textSecondary} />
                <Text style={[styles.settingLabel, { color: theme.colors.textPrimary }]}>
                  Help & FAQs
                </Text>
              </View>
              <ChevronRight size={18} color={theme.colors.textTertiary} />
            </Pressable>

            <Pressable
              onPress={() => Alert.alert('Privacy Policy', 'Your academic data is encrypted and used strictly for generating your personalized learning roadmaps.')}
              style={[styles.navRow, { borderTopWidth: 1, borderColor: 'rgba(150, 150, 150, 0.1)' }]}
            >
              <View style={styles.settingLeft}>
                <Shield size={18} color={theme.colors.textSecondary} />
                <Text style={[styles.settingLabel, { color: theme.colors.textPrimary }]}>
                  Privacy & Data Policy
                </Text>
              </View>
              <ChevronRight size={18} color={theme.colors.textTertiary} />
            </Pressable>

            <Pressable
              onPress={() => Alert.alert('About LearnPath AI', 'AI Personalized Learning Path Generator v1.0.0\nBuilt for B.Tech students and faculty.')}
              style={[styles.navRow, { borderTopWidth: 1, borderColor: 'rgba(150, 150, 150, 0.1)' }]}
            >
              <View style={styles.settingLeft}>
                <Info size={18} color={theme.colors.textSecondary} />
                <Text style={[styles.settingLabel, { color: theme.colors.textPrimary }]}>
                  About LearnPath AI
                </Text>
              </View>
              <ChevronRight size={18} color={theme.colors.textTertiary} />
            </Pressable>
          </Card>
        </View>

        {/* Logout Button */}
        <Button
          title="Sign Out"
          variant="outline"
          size="lg"
          leftIcon={<LogOut size={18} color={theme.colors.error} />}
          textStyle={{ color: theme.colors.error }}
          style={{ borderColor: theme.colors.error, marginTop: 10, marginBottom: 20 }}
          onPress={handleLogout}
        />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    paddingHorizontal: 20,
    paddingTop: 56,
    paddingBottom: 16,
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: '800',
    letterSpacing: -0.4,
  },
  scrollContent: {
    paddingHorizontal: 20,
    paddingBottom: 40,
  },
  profileCard: {
    padding: 20,
    marginBottom: 20,
  },
  profileRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
  },
  profileInfo: {
    flex: 1,
  },
  userName: {
    fontSize: 18,
    fontWeight: '800',
    marginBottom: 2,
  },
  userEmail: {
    fontSize: 13,
    marginBottom: 8,
  },
  badgeRow: {
    flexDirection: 'row',
    gap: 6,
  },
  section: {
    marginBottom: 20,
  },
  sectionTitle: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.8,
    marginBottom: 8,
  },
  settingsCard: {
    padding: 6,
  },
  themeSelector: {
    flexDirection: 'row',
    borderRadius: 12,
    padding: 4,
  },
  themeOption: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 10,
    borderRadius: 10,
  },
  themeOptionText: {
    fontSize: 13,
  },
  settingRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 12,
  },
  settingLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    flex: 1,
  },
  settingLabel: {
    fontSize: 14,
    fontWeight: '600',
  },
  settingSub: {
    fontSize: 11,
    marginTop: 2,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 14,
    paddingHorizontal: 12,
  },
  infoLabel: {
    fontSize: 13,
  },
  infoValue: {
    fontSize: 13,
    fontWeight: '600',
  },
  navRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 12,
  },
});
