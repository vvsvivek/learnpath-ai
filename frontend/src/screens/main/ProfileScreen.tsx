import React from 'react';
import {
  StyleSheet,
  Text,
  View,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
  StatusBar,
} from 'react-native';
import { Colors, Shadows, Radii } from '../../styles/theme';

interface ProfileScreenProps {
  userData?: any;
  onLogout: () => void;
  onNavigateSettings?: () => void;
}

export default function ProfileScreen({
  userData,
  onLogout,
  onNavigateSettings,
}: ProfileScreenProps) {
  const username = userData?.username || userData?.name || 'User';
  const email = (userData?.email || '').toLowerCase();
  const rawRole = (userData?.role || 'STUDENT').toUpperCase();

  // STRICT ROLE EVALUATION:
  // Faculty: @siddhartha.ac.in or vivekvaranasi99@gmail.com
  // Student: All other emails (including @vrsec.ac.in students!)
  const isFaculty = rawRole === 'FACULTY' ||
    email.endsWith('@siddhartha.ac.in') ||
    email === 'vivekvaranasi99@gmail.com';

  const isVrsecEligible = email.endsWith('@vrsec.ac.in');
  const branch = userData?.branch || 'CSE';
  const academicYear = userData?.academicYear || '2nd Year';
  const semester = userData?.semester || '1st Sem';
  const cgpa = userData?.cgpa !== undefined ? userData?.cgpa : 8.5;

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        
        {/* Profile Identity Card */}
        <View style={styles.profileCard}>
          <View style={styles.avatarCircle}>
            <Text style={styles.avatarInitial}>
              {username.charAt(0).toUpperCase()}
            </Text>
          </View>

          <Text style={styles.userName}>{username}</Text>
          <Text style={styles.userEmail}>{email}</Text>

          <View style={styles.badgeRow}>
            {isFaculty ? (
              <View style={styles.facultyBadge}>
                <Text style={styles.facultyBadgeText}>🏛️ FACULTY • {branch} DEPARTMENT</Text>
              </View>
            ) : isVrsecEligible ? (
              <View style={styles.vrsecBadge}>
                <Text style={styles.vrsecBadgeText}>🎓 VRSEC STUDENT • {branch}</Text>
              </View>
            ) : (
              <View style={styles.studentBadge}>
                <Text style={styles.studentBadgeText}>📖 LEARNER • {branch}</Text>
              </View>
            )}
          </View>
        </View>

        {/* Academic Profile Details (Students) or Department Profile (Faculty) */}
        {!isFaculty ? (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Academic Records</Text>
            
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Department Branch</Text>
              <Text style={styles.infoValue}>{branch} Engineering</Text>
            </View>

            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Academic Year</Text>
              <Text style={styles.infoValue}>{academicYear}</Text>
            </View>

            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Current Semester</Text>
              <Text style={styles.infoValue}>{semester}</Text>
            </View>

            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Current CGPA</Text>
              <Text style={[styles.infoValue, { color: Colors.primaryDark, fontWeight: '900' }]}>{cgpa}</Text>
            </View>
          </View>
        ) : (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Faculty Department Information</Text>
            
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Role Designation</Text>
              <Text style={styles.infoValue}>Department Professor / HOD</Text>
            </View>

            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Department</Text>
              <Text style={styles.infoValue}>{branch} Engineering</Text>
            </View>

            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Institution</Text>
              <Text style={styles.infoValue}>Velagapudi Ramakrishna Siddhartha Engineering College</Text>
            </View>
          </View>
        )}

        {/* Account Actions */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Account Settings</Text>

          {onNavigateSettings && (
            <TouchableOpacity
              style={styles.actionItem}
              onPress={onNavigateSettings}
            >
              <Text style={styles.actionItemText}>⚙️ App Preferences &amp; Theme</Text>
              <Text style={styles.actionArrow}>→</Text>
            </TouchableOpacity>
          )}

          <TouchableOpacity
            style={[styles.actionItem, { borderBottomWidth: 0 }]}
            onPress={onLogout}
          >
            <Text style={[styles.actionItemText, { color: Colors.danger }]}>🚪 Sign Out of LearnPath AI</Text>
            <Text style={[styles.actionArrow, { color: Colors.danger }]}>→</Text>
          </TouchableOpacity>
        </View>

      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  scroll: {
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 110,
  },
  profileCard: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.xl,
    padding: 24,
    alignItems: 'center',
    marginBottom: 20,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  avatarCircle: {
    width: 72,
    height: 72,
    borderRadius: Radii.full,
    backgroundColor: Colors.primaryDark,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 14,
    ...Shadows.small,
  },
  avatarInitial: {
    fontSize: 28,
    fontWeight: '900',
    color: '#FFFFFF',
  },
  userName: {
    fontSize: 22,
    fontWeight: '900',
    color: Colors.textPrimary,
    marginBottom: 4,
  },
  userEmail: {
    fontSize: 14,
    color: Colors.textSecondary,
    marginBottom: 14,
  },
  badgeRow: {
    flexDirection: 'row',
  },
  facultyBadge: {
    backgroundColor: '#FEF3C7',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: Radii.full,
  },
  facultyBadgeText: {
    color: '#92400E',
    fontWeight: '900',
    fontSize: 11,
  },
  vrsecBadge: {
    backgroundColor: Colors.primaryLight,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: Radii.full,
  },
  vrsecBadgeText: {
    color: Colors.primaryDark,
    fontWeight: '800',
    fontSize: 11,
  },
  studentBadge: {
    backgroundColor: Colors.surfaceMuted,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: Radii.full,
  },
  studentBadgeText: {
    color: Colors.textSecondary,
    fontWeight: '800',
    fontSize: 11,
  },
  card: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.xl,
    padding: 20,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  cardTitle: {
    fontSize: 15,
    fontWeight: '800',
    color: Colors.textPrimary,
    marginBottom: 14,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderColor: Colors.borderLight,
  },
  infoLabel: {
    fontSize: 13,
    color: Colors.textSecondary,
  },
  infoValue: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  actionItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderColor: Colors.borderLight,
  },
  actionItemText: {
    fontSize: 14,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  actionArrow: {
    fontSize: 16,
    color: Colors.textSecondary,
  },
});
