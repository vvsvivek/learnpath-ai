import React, { useState } from 'react';
import {
  StyleSheet,
  Text,
  View,
  SafeAreaView,
  TouchableOpacity,
  StatusBar,
  Platform,
} from 'react-native';
import { Colors, Shadows, Radii } from '../../styles/theme';
import HomeScreen from './HomeScreen';
import FacultyHomeScreen from './FacultyHomeScreen';
import LearningPathScreen from './LearningPathScreen';
import FacultyDashboardScreen from './FacultyDashboardScreen';
import ChatScreen from './ChatScreen';
import ProfileScreen from './ProfileScreen';

interface MainLayoutProps {
  userData: any;
  onLogout: () => void;
  onNavigateSettings: () => void;
  facultyBranch?: string;
}

export default function MainLayout({
  userData,
  onLogout,
  onNavigateSettings,
  facultyBranch,
}: MainLayoutProps) {
  const email = (userData?.email || '').toLowerCase();
  const rawRole = (userData?.role || 'STUDENT').toUpperCase();

  // STRICT BACKEND-ALIGNED ROLE CHECK:
  const isFaculty =
    rawRole === 'FACULTY' ||
    email.endsWith('@siddhartha.ac.in') ||
    email === 'vivekvaranasi99@gmail.com';

  const [activeTab, setActiveTab] = useState<string>('Home');
  const [activeSubject, setActiveSubject] = useState('Data Structures & Algorithmic Complexity');

  const renderContent = () => {
    if (isFaculty) {
      switch (activeTab) {
        case 'Courses':
          return <FacultyDashboardScreen userData={userData} />;
        case 'Chat':
          return <ChatScreen userData={userData} activeSubject="Faculty Institutional AI Hub" />;
        case 'Profile':
          return (
            <ProfileScreen
              userData={userData}
              onLogout={onLogout}
              onNavigateSettings={onNavigateSettings}
            />
          );
        case 'Home':
        default:
          return <FacultyHomeScreen userData={userData} onNavigateTab={setActiveTab} />;
      }
    } else {
      switch (activeTab) {
        case 'Roadmap':
          return (
            <LearningPathScreen
              userData={userData}
              activeSubject={activeSubject}
              onSelectSubject={setActiveSubject}
            />
          );
        case 'Chat':
          return <ChatScreen userData={userData} activeSubject={activeSubject} />;
        case 'Profile':
          return (
            <ProfileScreen
              userData={userData}
              onLogout={onLogout}
              onNavigateSettings={onNavigateSettings}
            />
          );
        case 'Home':
        default:
          return (
            <HomeScreen
              userData={userData}
              onNavigate={setActiveTab}
              onSelectSubject={setActiveSubject}
            />
          );
      }
    }
  };

  const navItems = isFaculty
    ? [
        { id: 'Home', label: 'Overview', icon: '🏛️' },
        { id: 'Courses', label: 'Audit & Assign', icon: '📋' },
        { id: 'Chat', label: 'Faculty AI', icon: '🤖' },
        { id: 'Profile', label: 'Profile', icon: '👤' },
      ]
    : [
        { id: 'Home', label: 'Dashboard', icon: '🏠' },
        { id: 'Roadmap', label: 'Curriculum', icon: '🗺️' },
        { id: 'Chat', label: 'AI Tutor', icon: '🤖' },
        { id: 'Profile', label: 'Profile', icon: '👤' },
      ];

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      
      {/* Active Screen View */}
      <View style={styles.screenWrapper}>{renderContent()}</View>

      {/* Floating Modern Bottom Navigation Pill */}
      <View style={styles.navContainer}>
        <View style={styles.navBar}>
          {navItems.map((item) => {
            const isActive = activeTab === item.id;
            return (
              <TouchableOpacity
                key={item.id}
                style={[styles.navButton, isActive && styles.navButtonActive]}
                onPress={() => setActiveTab(item.id)}
                activeOpacity={0.8}
              >
                <Text style={styles.navIcon}>{item.icon}</Text>
                <Text style={[styles.navLabel, isActive && styles.navLabelActive]}>
                  {item.label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  screenWrapper: {
    flex: 1,
  },
  navContainer: {
    position: 'absolute',
    bottom: Platform.OS === 'ios' ? 24 : 16,
    left: 20,
    right: 20,
    alignItems: 'center',
  },
  navBar: {
    flexDirection: 'row',
    backgroundColor: Colors.surface,
    borderRadius: Radii.full,
    paddingHorizontal: 8,
    paddingVertical: 6,
    borderWidth: 1,
    borderColor: Colors.border,
    ...Shadows.elevated,
    width: '100%',
    justifyContent: 'space-around',
  },
  navButton: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 6,
    paddingHorizontal: 6,
    borderRadius: Radii.full,
  },
  navButtonActive: {
    backgroundColor: Colors.primaryLight,
  },
  navIcon: {
    fontSize: 18,
    marginBottom: 2,
  },
  navLabel: {
    fontSize: 11,
    fontWeight: '700',
    color: Colors.textSecondary,
  },
  navLabelActive: {
    color: Colors.primaryDark,
    fontWeight: '800',
  },
});
