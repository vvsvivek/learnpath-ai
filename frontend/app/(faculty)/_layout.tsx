import React from 'react';
import { Tabs } from 'expo-router';
import { Home, Users, Bot, User } from 'lucide-react-native';
import { useTheme } from '../../src/hooks/useTheme';

export default function FacultyLayout() {
  const { theme, isDark } = useTheme();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: theme.colors.primary,
        tabBarInactiveTintColor: theme.colors.textTertiary,
        tabBarStyle: {
          backgroundColor: isDark ? theme.colors.surface : '#FFFFFF',
          borderTopColor: isDark ? theme.colors.border : theme.colors.borderLight,
          borderTopWidth: 1,
          height: 62,
          paddingBottom: 8,
          paddingTop: 8,
          elevation: 8,
          shadowColor: '#000',
          shadowOffset: { width: 0, height: -2 },
          shadowOpacity: 0.05,
          shadowRadius: 4,
        },
        tabBarLabelStyle: {
          fontSize: 11,
          fontWeight: '600',
        },
      }}
    >
      <Tabs.Screen
        name="home"
        options={{
          title: 'Dashboard',
          tabBarIcon: ({ color, size }) => <Home size={size || 22} color={color} />,
        }}
      />
      <Tabs.Screen
        name="students/index"
        options={{
          title: 'Students',
          tabBarIcon: ({ color, size }) => <Users size={size || 22} color={color} />,
        }}
      />
      <Tabs.Screen
        name="ai"
        options={{
          title: 'AI Copilot',
          tabBarIcon: ({ color, size }) => <Bot size={size || 22} color={color} />,
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: 'Profile',
          tabBarIcon: ({ color, size }) => <User size={size || 22} color={color} />,
        }}
      />

      {/* Hidden nested screens from the bottom tab bar */}
      <Tabs.Screen
        name="students/[id]"
        options={{
          href: null,
        }}
      />
      <Tabs.Screen
        name="students/assign"
        options={{
          href: null,
        }}
      />
    </Tabs>
  );
}
