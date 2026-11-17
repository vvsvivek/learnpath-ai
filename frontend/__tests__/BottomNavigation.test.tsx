import React from 'react';
import ReactTestRenderer from 'react-test-renderer';
import { DeviceEventEmitter, Platform } from 'react-native';
import MainLayout from '../src/screens/main/MainLayout';

// Mock sub-screens using string identifiers so MainLayout bottom navigation is tested in pure isolation
jest.mock('../src/screens/main/HomeScreen', () => 'HomeScreen');
jest.mock('../src/screens/main/LearningPathScreen', () => 'LearningPathScreen');
jest.mock('../src/screens/main/ChatScreen', () => 'ChatScreen');
jest.mock('../src/screens/main/ProfileScreen', () => 'ProfileScreen');
jest.mock('../src/screens/main/FacultyHomeScreen', () => 'FacultyHomeScreen');
jest.mock('../src/screens/main/FacultyDashboardScreen', () => 'FacultyDashboardScreen');

jest.mock('../src/services/api', () => ({
  api: {
    updateProfile: jest.fn().mockResolvedValue({}),
  },
}));

describe('Floating Pill Bottom Navigation Bar', () => {
  const mockStudentUser = {
    email: 'student@example.com',
    role: 'STUDENT',
    branch: 'CSE',
    academicYear: '2nd Year',
    semester: '1st Sem',
    cgpa: 8.5,
  };

  const mockFacultyUser = {
    email: 'faculty@siddhartha.ac.in',
    role: 'FACULTY',
    branch: 'CSE',
  };

  it('renders all four student navigation tabs with icons and labels', async () => {
    let renderer: ReactTestRenderer.ReactTestRenderer;
    await ReactTestRenderer.act(() => {
      renderer = ReactTestRenderer.create(
        <MainLayout
          userData={mockStudentUser}
          onLogout={jest.fn()}
          onNavigateSettings={jest.fn()}
        />
      );
    });

    const root = renderer!.root;
    const tabHome = root.findByProps({ testID: 'bottom-nav-Home' });
    const tabRoadmap = root.findByProps({ testID: 'bottom-nav-Roadmap' });
    const tabChat = root.findByProps({ testID: 'bottom-nav-Chat' });
    const tabProfile = root.findByProps({ testID: 'bottom-nav-Profile' });

    expect(tabHome).toBeDefined();
    expect(tabRoadmap).toBeDefined();
    expect(tabChat).toBeDefined();
    expect(tabProfile).toBeDefined();

    expect(tabHome.props.accessibilityLabel).toBe('Dashboard');
    expect(tabRoadmap.props.accessibilityLabel).toBe('Curriculum');
    expect(tabChat.props.accessibilityLabel).toBe('AI Tutor');
    expect(tabProfile.props.accessibilityLabel).toBe('Profile');
  });

  it('renders all four faculty navigation tabs with correct labels', async () => {
    let renderer: ReactTestRenderer.ReactTestRenderer;
    await ReactTestRenderer.act(() => {
      renderer = ReactTestRenderer.create(
        <MainLayout
          userData={mockFacultyUser}
          onLogout={jest.fn()}
          onNavigateSettings={jest.fn()}
        />
      );
    });

    const root = renderer!.root;
    const tabHome = root.findByProps({ testID: 'bottom-nav-Home' });
    const tabCourses = root.findByProps({ testID: 'bottom-nav-Courses' });
    const tabChat = root.findByProps({ testID: 'bottom-nav-Chat' });
    const tabProfile = root.findByProps({ testID: 'bottom-nav-Profile' });

    expect(tabHome.props.accessibilityLabel).toBe('Overview');
    expect(tabCourses.props.accessibilityLabel).toBe('Audit & Assign');
    expect(tabChat.props.accessibilityLabel).toBe('Faculty AI');
    expect(tabProfile.props.accessibilityLabel).toBe('Profile');
  });

  it('enforces single line labels with numberOfLines=1 and ellipsizeMode', async () => {
    let renderer: ReactTestRenderer.ReactTestRenderer;
    await ReactTestRenderer.act(() => {
      renderer = ReactTestRenderer.create(
        <MainLayout
          userData={mockStudentUser}
          onLogout={jest.fn()}
          onNavigateSettings={jest.fn()}
        />
      );
    });

    const root = renderer!.root;
    const curriculumNode = root.findAll((n) => n.props.children === 'Curriculum')[0];
    expect(curriculumNode.props.numberOfLines).toBe(1);
    expect(curriculumNode.props.ellipsizeMode).toBe('tail');

    const dashboardNode = root.findAll((n) => n.props.children === 'Dashboard')[0];
    expect(dashboardNode.props.numberOfLines).toBe(1);
    expect(dashboardNode.props.ellipsizeMode).toBe('tail');
  });

  it('hides bottom navigation bar when keyboard opens and restores it when keyboard closes', async () => {
    let renderer: ReactTestRenderer.ReactTestRenderer;
    await ReactTestRenderer.act(() => {
      renderer = ReactTestRenderer.create(
        <MainLayout
          userData={mockStudentUser}
          onLogout={jest.fn()}
          onNavigateSettings={jest.fn()}
        />
      );
    });

    // Initially navigation tab is present
    expect(renderer!.root.findByProps({ testID: 'bottom-nav-Home' })).toBeDefined();

    // Simulate keyboard show (matching MainLayout's platform listener)
    const showEvent = Platform.OS === 'ios' ? 'keyboardWillShow' : 'keyboardDidShow';
    const hideEvent = Platform.OS === 'ios' ? 'keyboardWillHide' : 'keyboardDidHide';

    await ReactTestRenderer.act(() => {
      DeviceEventEmitter.emit(showEvent, {});
    });

    // When keyboard is visible, bottom navigation bar is hidden
    expect(() => renderer!.root.findByProps({ testID: 'bottom-nav-Home' })).toThrow();

    // Simulate keyboard hide
    await ReactTestRenderer.act(() => {
      DeviceEventEmitter.emit(hideEvent, {});
    });

    // When keyboard closes, bottom navigation bar is restored
    expect(renderer!.root.findByProps({ testID: 'bottom-nav-Home' })).toBeDefined();
  });

  it('switches active tab cleanly and updates selection state', async () => {
    let renderer: ReactTestRenderer.ReactTestRenderer;
    await ReactTestRenderer.act(() => {
      renderer = ReactTestRenderer.create(
        <MainLayout
          userData={mockStudentUser}
          onLogout={jest.fn()}
          onNavigateSettings={jest.fn()}
        />
      );
    });

    const root = renderer!.root;
    const tabHome = root.findByProps({ testID: 'bottom-nav-Home' });
    const tabRoadmap = root.findByProps({ testID: 'bottom-nav-Roadmap' });

    // Default active tab is Dashboard (Home)
    expect(tabHome.props.accessibilityState.selected).toBe(true);
    expect(tabRoadmap.props.accessibilityState.selected).toBe(false);

    // Press Curriculum tab
    await ReactTestRenderer.act(() => {
      tabRoadmap.props.onPress();
    });

    const updatedTabHome = root.findByProps({ testID: 'bottom-nav-Home' });
    const updatedTabRoadmap = root.findByProps({ testID: 'bottom-nav-Roadmap' });

    expect(updatedTabHome.props.accessibilityState.selected).toBe(false);
    expect(updatedTabRoadmap.props.accessibilityState.selected).toBe(true);
  });
});
