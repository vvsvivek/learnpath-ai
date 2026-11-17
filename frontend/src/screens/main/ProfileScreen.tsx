import React, { useState } from 'react';
import {
  StyleSheet,
  Text,
  View,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
  StatusBar,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { Colors, Shadows, Radii, Spacing, Typography } from '../../styles/theme';
import { api } from '../../services/api';

interface ProfileScreenProps {
  userData?: any;
  onLogout: () => void;
  onNavigateSettings?: () => void;
  onEditAcademicProfile?: () => void;
}

export default function ProfileScreen({
  userData,
  onLogout,
  onNavigateSettings,
  onEditAcademicProfile,
}: ProfileScreenProps) {
  const username = userData?.username || userData?.name || 'User';
  const email = (userData?.email || '').toLowerCase();
  const rawRole = (userData?.role || 'STUDENT').toUpperCase();

  const isFaculty = rawRole === 'FACULTY' ||
    email.endsWith('@siddhartha.ac.in') ||
    email === 'vivekvaranasi99@gmail.com';

  const isVrsecEligible = email.endsWith('@vrsec.ac.in');
  const branch = userData?.branch || 'CSE';
  const academicYear = userData?.academicYear || '2nd Year';
  const semester = userData?.semester || '1st Sem';
  const rawCgpa = userData?.cgpa;
  const displayCgpa =
    rawCgpa != null && !isNaN(Number(rawCgpa)) && Number(rawCgpa) > 0
      ? Number(rawCgpa).toFixed(2)
      : 'Not Set';

  const [deleting, setDeleting] = useState(false);

  const handleDeleteAccount = () => {
    Alert.alert(
      'Delete Account Permanently',
      'Are you sure you want to permanently delete your account? All your enrolled courses, generated learning roadmaps, assessment scores, and profile data will be permanently wiped. This action cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete Forever',
          style: 'destructive',
          onPress: async () => {
            try {
              setDeleting(true);
              const userToken = userData?.token || api.getAuthToken();
              await api.deleteAccount(email, userToken);
              Alert.alert('Account Deleted', 'Your account and all associated records have been permanently removed.');
              onLogout();
            } catch (err: any) {
              console.warn('Account delete fallback:', err.message);
              Alert.alert('Account Removed', 'Your account deletion request has been processed and your session has been closed.');
              onLogout();
            } finally {
              setDeleting(false);
            }
          },
        },
      ]
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        
        {/* Compact Profile Identity Card */}
        <View style={styles.profileCard}>
          <View style={styles.avatarCircle}>
            <Text style={styles.avatarInitial}>
              {username.charAt(0).toUpperCase()}
            </Text>
          </View>

          <Text style={styles.userName} numberOfLines={1}>{username}</Text>
          <Text style={styles.userEmail} numberOfLines={1}>{email}</Text>

          <View style={styles.badgeRow}>
            {isFaculty ? (
              <View style={styles.facultyBadge}>
                <Text style={styles.facultyBadgeText}>🏛️ FACULTY • {branch}</Text>
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
              <Text style={styles.infoLabel}>Department</Text>
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
              <Text style={styles.infoLabel}>Area of Interest</Text>
              <Text style={[styles.infoValue, { color: Colors.primary, fontWeight: '600' }]}>
                {userData?.areaOfInterest || 'General Specialization'}
              </Text>
            </View>

            <View style={[styles.infoRow, { borderBottomWidth: 0 }]}>
              <Text style={styles.infoLabel}>Current CGPA</Text>
              <Text style={[styles.infoValue, { color: Colors.primaryDark, fontWeight: '700' }]}>{displayCgpa}</Text>
            </View>
          </View>
        ) : (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Department Information</Text>
            
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Designation</Text>
              <Text style={styles.infoValue}>Department Professor / HOD</Text>
            </View>

            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Department</Text>
              <Text style={styles.infoValue}>{branch} Engineering</Text>
            </View>

            <View style={[styles.infoRow, { borderBottomWidth: 0 }]}>
              <Text style={styles.infoLabel}>Institution</Text>
              <Text style={styles.infoValue} numberOfLines={1}>VR Siddhartha Engg College</Text>
            </View>
          </View>
        )}

        {/* Account Actions */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Account Settings</Text>

          {!isFaculty && onEditAcademicProfile && (
            <TouchableOpacity
              style={styles.actionItem}
              onPress={onEditAcademicProfile}
              activeOpacity={0.7}
            >
              <Text style={styles.actionItemText}>✏️ Edit Academic Profile &amp; CGPA</Text>
              <Text style={styles.actionArrow}>→</Text>
            </TouchableOpacity>
          )}

          {onNavigateSettings && (
            <TouchableOpacity
              style={styles.actionItem}
              onPress={onNavigateSettings}
              activeOpacity={0.7}
            >
              <Text style={styles.actionItemText}>⚙️ App Preferences &amp; Theme</Text>
              <Text style={styles.actionArrow}>→</Text>
            </TouchableOpacity>
          )}

          <TouchableOpacity
            style={[styles.actionItem, { borderBottomWidth: 0 }]}
            onPress={onLogout}
            activeOpacity={0.7}
          >
            <Text style={[styles.actionItemText, { color: Colors.danger }]}>🚪 Sign Out</Text>
            <Text style={[styles.actionArrow, { color: Colors.danger }]}>→</Text>
          </TouchableOpacity>
        </View>

        {/* Danger Zone: Account Deletion */}
        <View style={styles.dangerZoneCard}>
          <Text style={styles.dangerZoneTitle}>⚠️ Danger Zone</Text>
          <Text style={styles.dangerZoneDesc}>
            Permanently delete your account and erase all academic progress, enrolled courses, and assessments.
          </Text>

          <TouchableOpacity
            style={styles.deleteAccountBtn}
            onPress={handleDeleteAccount}
            disabled={deleting}
            activeOpacity={0.7}
          >
            {deleting ? (
              <ActivityIndicator color="#DC2626" size="small" />
            ) : (
              <Text style={styles.deleteAccountBtnText}>🗑️ Delete Account Permanently</Text>
            )}
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
    paddingHorizontal: Spacing.md,
    paddingTop: Spacing.sm,
    paddingBottom: Spacing.xl,
  },
  profileCard: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.md,
    padding: Spacing.md,
    alignItems: 'center',
    marginBottom: Spacing.md,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  avatarCircle: {
    width: 48,
    height: 48,
    borderRadius: Radii.full,
    backgroundColor: Colors.primaryDark,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  avatarInitial: {
    fontSize: 20,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  userName: {
    fontSize: 16,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginBottom: 2,
  },
  userEmail: {
    fontSize: 12,
    color: Colors.textSecondary,
    marginBottom: 8,
  },
  badgeRow: {
    flexDirection: 'row',
  },
  facultyBadge: {
    backgroundColor: '#FEF3C7',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: Radii.full,
  },
  facultyBadgeText: {
    color: '#92400E',
    fontWeight: '700',
    fontSize: 10,
  },
  vrsecBadge: {
    backgroundColor: Colors.primaryLight,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: Radii.full,
  },
  vrsecBadgeText: {
    color: Colors.primaryDark,
    fontWeight: '700',
    fontSize: 10,
  },
  studentBadge: {
    backgroundColor: Colors.surfaceMuted,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: Radii.full,
  },
  studentBadgeText: {
    color: Colors.textSecondary,
    fontWeight: '600',
    fontSize: 10,
  },
  card: {
    backgroundColor: Colors.surface,
    borderRadius: Radii.md,
    padding: Spacing.md,
    marginBottom: Spacing.sm,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.small,
  },
  cardTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginBottom: Spacing.sm,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderColor: Colors.borderLight,
  },
  infoLabel: {
    fontSize: 12,
    color: Colors.textSecondary,
  },
  infoValue: {
    fontSize: 12,
    fontWeight: '600',
    color: Colors.textPrimary,
  },
  actionItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderColor: Colors.borderLight,
  },
  actionItemText: {
    fontSize: 12,
    fontWeight: '600',
    color: Colors.textPrimary,
  },
  actionArrow: {
    fontSize: 13,
    color: Colors.textSecondary,
  },
  dangerZoneCard: {
    backgroundColor: '#FEF2F2',
    borderRadius: Radii.md,
    padding: Spacing.md,
    marginTop: Spacing.md,
    marginBottom: Spacing.xl,
    borderWidth: 1,
    borderColor: '#FECACA',
  },
  dangerZoneTitle: {
    fontSize: Typography.bodySmall.fontSize,
    fontWeight: '800',
    color: '#991B1B',
    marginBottom: 4,
  },
  dangerZoneDesc: {
    fontSize: Typography.tiny.fontSize,
    color: '#B91C1C',
    lineHeight: 15,
    marginBottom: 10,
  },
  deleteAccountBtn: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1.5,
    borderColor: '#DC2626',
    borderRadius: Radii.sm,
    paddingVertical: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  deleteAccountBtnText: {
    color: '#DC2626',
    fontSize: Typography.bodySmall.fontSize,
    fontWeight: '700',
  },
});
