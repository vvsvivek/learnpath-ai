import { StyleSheet, Platform } from 'react-native';

export const Colors = {
  // Brand Palette (Light Blue & Navy Design System)
  primary: '#4A90D9',        // Soft Blue
  primaryDark: '#1E3A5F',    // Deep Navy
  primaryLight: '#EBF3FC',   // Soft light-blue background tint
  primarySubtle: '#F4F9FF',  // Subtle canvas tint
  accent: '#38BDF8',         // Interactive Sky Blue

  // Neutrals & Surfaces
  background: '#F8FAFC',     // Modern Crisp Canvas
  surface: '#FFFFFF',        // Pure White Card Surface
  cardBackground: '#FFFFFF', // Card surface alias
  surfaceMuted: '#F1F5F9',   // Light gray fill
  border: '#E2E8F0',         // Refined subtle border
  borderLight: '#F1F5F9',

  // Typography Colors
  textPrimary: '#0F172A',    // Deep modern slate for titles
  textSecondary: '#64748B',  // Balanced blue-gray for descriptions
  textTertiary: '#94A3B8',   // Light caption text
  textInverse: '#FFFFFF',

  // Status Colors
  success: '#10B981',        // Emerald Green (Unlocked / Passed)
  successLight: '#ECFDF5',
  warning: '#F59E0B',        // Amber (In-Progress)
  warningLight: '#FFFBEB',
  danger: '#EF4444',         // Coral Red (Locked / Requires Support)
  dangerLight: '#FEF2F2',

  // Shadows
  shadowColor: '#1E3A5F',
};

export const Shadows = {
  small: {
    shadowColor: Colors.shadowColor,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
  },
  medium: {
    shadowColor: Colors.shadowColor,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.08,
    shadowRadius: 10,
    elevation: 4,
  },
  elevated: {
    shadowColor: Colors.shadowColor,
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.12,
    shadowRadius: 18,
    elevation: 8,
  },
};

export const Radii = {
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  full: 9999,
};

export const Typography = {
  fontFamily: Platform.OS === 'ios' ? 'System' : 'sans-serif',
};
