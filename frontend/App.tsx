import React, { useState } from 'react';
import { Text } from 'react-native';
import GetStartedScreen from './src/screens/auth/GetStartedScreen';
import AuthScreen from './src/screens/auth/AuthScreen';
import MainLayout from './src/screens/main/MainLayout';
import SettingsScreen from './src/screens/SettingsScreen';

// Disable system font scaling globally across the app so layouts match emulator scaling on physical phones
if ((Text as any).defaultProps) {
  (Text as any).defaultProps.allowFontScaling = false;
} else {
  (Text as any).defaultProps = {};
  (Text as any).defaultProps.allowFontScaling = false;
}

type ScreenState = 'GetStarted' | 'Auth' | 'Main' | 'Settings';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<ScreenState>('GetStarted');
  const [authRole, setAuthRole] = useState<'STUDENT' | 'FACULTY'>('STUDENT');
  const [authMode, setAuthMode] = useState<'SIGN_IN' | 'REGISTER'>('SIGN_IN');
  const [userData, setUserData] = useState<any>(null);
  const [facultyBranch, setFacultyBranch] = useState<string>('CSE');

  if (currentScreen === 'GetStarted') {
    return (
      <GetStartedScreen
        onSelectRole={(role, mode) => {
          setAuthRole(role);
          setAuthMode(mode);
          setCurrentScreen('Auth');
        }}
        onGetStarted={() => {
          setAuthRole('STUDENT');
          setAuthMode('REGISTER');
          setCurrentScreen('Auth');
        }}
        onSignIn={() => {
          setAuthRole('STUDENT');
          setAuthMode('SIGN_IN');
          setCurrentScreen('Auth');
        }}
      />
    );
  }

  if (currentScreen === 'Auth') {
    return (
      <AuthScreen
        initialRole={authRole}
        initialMode={authMode}
        onBack={() => setCurrentScreen('GetStarted')}
        onLoginComplete={(data: any, branch?: string) => {
          setUserData(data);
          if (branch) setFacultyBranch(branch);
          setCurrentScreen('Main');
        }}
      />
    );
  }

  if (currentScreen === 'Settings') {
    return (
      <SettingsScreen
        onBack={() => setCurrentScreen('Main')}
        onLogout={() => {
          setUserData(null);
          setCurrentScreen('Auth');
        }}
      />
    );
  }

  return (
    <MainLayout
      userData={userData}
      facultyBranch={facultyBranch}
      onNavigateSettings={() => setCurrentScreen('Settings')}
      onLogout={() => {
        setUserData(null);
        setCurrentScreen('Auth');
      }}
    />
  );
}
