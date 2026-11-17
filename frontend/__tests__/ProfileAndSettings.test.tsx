import React from 'react';
import ReactTestRenderer from 'react-test-renderer';
import { Alert, Text } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import ProfileScreen from '../src/screens/main/ProfileScreen';
import SettingsScreen from '../src/screens/SettingsScreen';
import { api } from '../src/services/api';

jest.mock('../src/services/api', () => ({
  api: {
    deleteAccount: jest.fn().mockImplementation(() => Promise.resolve({ success: true })),
    getProfile: jest.fn().mockImplementation(() => Promise.resolve({})),
    updateProfile: jest.fn().mockImplementation(() => Promise.resolve({})),
  },
}));

describe('ProfileScreen and SettingsScreen Module Verification', () => {
  const mockUser = {
    username: 'Vivek',
    email: '24eu04191@vrsec.ac.in',
    role: 'STUDENT',
    branch: 'CSE',
    academicYear: '2nd Year',
    semester: '1st Sem',
    cgpa: 8.85,
    areaOfInterest: 'Artificial Intelligence & Machine Learning',
  };

  it('renders ProfileScreen with user details, CGPA badge, and Danger Zone', async () => {
    let renderer: ReactTestRenderer.ReactTestRenderer;
    await ReactTestRenderer.act(async () => {
      renderer = ReactTestRenderer.create(
        <ProfileScreen userData={mockUser} onLogout={jest.fn()} />
      );
    });

    const root = renderer!.root;
    const textNodes = root.findAllByType(Text);
    const texts = textNodes.map((n) => n.props.children).flat().join(' ');

    expect(texts).toContain('Vivek');
    expect(texts).toContain('24eu04191@vrsec.ac.in');
    expect(texts).toContain('8.85');
    expect(texts).toContain('CSE');
    expect(texts).toContain('Danger Zone');
    expect(texts).toContain('Delete Account Permanently');
  });

  it('shows confirmation Alert with destructive button when Delete Account is clicked', async () => {
    const alertSpy = jest.spyOn(Alert, 'alert').mockImplementation(() => {});
    const onLogoutMock = jest.fn();

    let renderer: ReactTestRenderer.ReactTestRenderer;
    await ReactTestRenderer.act(async () => {
      renderer = ReactTestRenderer.create(
        <ProfileScreen userData={mockUser} onLogout={onLogoutMock} />
      );
    });

    const root = renderer!.root;

    // Find the Delete Account button
    const deleteBtn = root.findAllByProps({ activeOpacity: 0.7 }).find((node) => {
      try {
        const text = node.findByType(Text);
        return text.props.children && text.props.children.toString().includes('Delete Account Permanently');
      } catch {
        return false;
      }
    });

    expect(deleteBtn).toBeDefined();

    await ReactTestRenderer.act(async () => {
      deleteBtn!.props.onPress();
    });

    expect(alertSpy).toHaveBeenCalledWith(
      'Delete Account Permanently',
      expect.stringContaining('permanently delete your account'),
      expect.arrayContaining([
        expect.objectContaining({ text: 'Cancel', style: 'cancel' }),
        expect.objectContaining({ text: 'Delete Forever', style: 'destructive' }),
      ])
    );

    alertSpy.mockRestore();
  });

  const testMetrics = {
    insets: { top: 38, left: 0, right: 0, bottom: 0 },
    frame: { x: 0, y: 0, width: 390, height: 844 },
  };

  it('renders SettingsScreen with safe top header and back button', async () => {
    const onBackMock = jest.fn();
    let renderer: ReactTestRenderer.ReactTestRenderer;
    await ReactTestRenderer.act(async () => {
      renderer = ReactTestRenderer.create(
        <SafeAreaProvider initialMetrics={testMetrics}>
          <SettingsScreen onBack={onBackMock} onLogout={jest.fn()} />
        </SafeAreaProvider>
      );
    });

    const root = renderer!.root;
    const headerTitle = root.findAllByProps({ children: 'Settings' });
    expect(headerTitle.length).toBeGreaterThan(0);

    const backBtn = root.findByProps({ accessibilityLabel: 'Back' });
    expect(backBtn).toBeDefined();

    await ReactTestRenderer.act(async () => {
      backBtn.props.onPress();
    });

    expect(onBackMock).toHaveBeenCalledTimes(1);
  });
});
