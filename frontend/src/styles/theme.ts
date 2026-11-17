import { StyleSheet, Platform } from 'react-native';

export const Colors = {
  // Brand Palette (Light Blue & Navy Design System)
  primary: '#3B82F6',        // Clear Vibrant Blue
  primaryDark: '#1E3A8A',    // Deep Royal Navy
  primaryLight: '#EFF6FF',   // Subtle soft blue tint
  primarySubtle: '#F8FAFC',  // Canvas subtle tint
  accent: '#0284C7',         // Focused Sky Blue

  // Neutrals & Surfaces
  background: '#F8FAFC',     // Clean crisp canvas
  surface: '#FFFFFF',        // Pure white card
  cardBackground: '#FFFFFF', // Card surface alias
  surfaceMuted: '#F1F5F9',   // Light slate fill
  border: '#E2E8F0',         // Crisp modern border
  borderLight: '#F1F5F9',    // Very soft separator
  borderDark: '#CBD5E1',

  // Typography Colors
  textPrimary: '#0F172A',    // Slate 900
  textSecondary: '#475569',  // Slate 600
  textTertiary: '#94A3B8',   // Slate 400
  textInverse: '#FFFFFF',

  // Status Colors
  success: '#10B981',        // Emerald 500
  successLight: '#ECFDF5',   // Emerald 50
  warning: '#F59E0B',        // Amber 500
  warningLight: '#FFFBEB',   // Amber 50
  danger: '#EF4444',         // Red 500
  dangerLight: '#FEF2F2',    // Red 50

  // Shadows
  shadowColor: '#0F172A',
};

export const Spacing = {
  xxs: 2,
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 24,
  section: 16,
};

export const Shadows = {
  none: {
    shadowColor: 'transparent',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0,
    shadowRadius: 0,
    elevation: 0,
  },
  small: {
    shadowColor: Colors.shadowColor,
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.04,
    shadowRadius: 2,
    elevation: 1,
  },
  medium: {
    shadowColor: Colors.shadowColor,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06,
    shadowRadius: 6,
    elevation: 2,
  },
  elevated: {
    shadowColor: Colors.shadowColor,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.08,
    shadowRadius: 12,
    elevation: 4,
  },
};

export const Radii = {
  xs: 4,
  sm: 6,
  md: 10,
  lg: 12,
  xl: 16,
  full: 9999,
};

export const Typography = {
  fontFamily: Platform.OS === 'ios' ? 'System' : 'sans-serif',
  // Standalone style definitions
  hero: { fontSize: 22, lineHeight: 28 },
  h1: { fontSize: 20, lineHeight: 26 },
  h2: { fontSize: 17, lineHeight: 22 },
  h3: { fontSize: 15, lineHeight: 20 },
  body: { fontSize: 13, lineHeight: 18 },
  bodySmall: { fontSize: 12, lineHeight: 16 },
  caption: { fontSize: 11, lineHeight: 14 },
  badge: { fontSize: 10, lineHeight: 12 },
  tiny: { fontSize: 9, lineHeight: 12 },

  // Standardized Mobile Typography Scale (sp)
  fontSize: {
    hero: 22,
    h1: 20,
    h2: 17,
    h3: 15,
    body: 13,
    bodySmall: 12,
    caption: 11,
    badge: 10,
    tiny: 9,
  },
  lineHeight: {
    hero: 28,
    h1: 26,
    h2: 22,
    h3: 20,
    body: 18,
    bodySmall: 16,
    caption: 14,
    badge: 12,
  },
  fontWeight: {
    regular: '400' as const,
    medium: '500' as const,
    semibold: '600' as const,
    bold: '700' as const,
  },
};
