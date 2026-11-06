import React from 'react';
import { View, Text, StyleSheet, ViewStyle, TextStyle } from 'react-native';
import { useThemeContext } from '../../context/ThemeContext';

export type BadgeVariant = 'primary' | 'success' | 'warning' | 'error' | 'info' | 'neutral';

export interface BadgeProps {
  label: string;
  variant?: BadgeVariant;
  size?: 'sm' | 'md';
  icon?: React.ReactNode;
  style?: ViewStyle;
  textStyle?: TextStyle;
  outlined?: boolean;
}

export const Badge: React.FC<BadgeProps> = ({
  label,
  variant = 'primary',
  size = 'md',
  icon,
  style,
  textStyle,
  outlined = false,
}) => {
  const { theme } = useThemeContext();
  const isDark = theme.dark;

  const getColorConfig = () => {
    switch (variant) {
      case 'success':
        return {
          bg: isDark ? 'rgba(16, 185, 129, 0.2)' : '#ECFDF5',
          text: theme.colors.success,
          border: theme.colors.success,
        };
      case 'warning':
        return {
          bg: isDark ? 'rgba(245, 158, 11, 0.2)' : '#FFFBEB',
          text: theme.colors.warning,
          border: theme.colors.warning,
        };
      case 'error':
        return {
          bg: isDark ? 'rgba(239, 68, 68, 0.2)' : '#FEF2F2',
          text: theme.colors.error,
          border: theme.colors.error,
        };
      case 'info':
        return {
          bg: isDark ? 'rgba(59, 130, 246, 0.2)' : '#EFF6FF',
          text: theme.colors.info,
          border: theme.colors.info,
        };
      case 'neutral':
        return {
          bg: isDark ? theme.colors.surfaceSubtle : '#F1F5F9',
          text: theme.colors.textSecondary,
          border: theme.colors.border,
        };
      case 'primary':
      default:
        return {
          bg: isDark ? 'rgba(74, 144, 217, 0.2)' : '#EBF4FC',
          text: theme.colors.primary,
          border: theme.colors.primary,
        };
    }
  };

  const { bg, text, border } = getColorConfig();

  const isSmall = size === 'sm';

  return (
    <View
      style={[
        styles.badge,
        {
          backgroundColor: outlined ? 'transparent' : bg,
          borderColor: border,
          borderWidth: outlined ? 1 : 0,
          paddingVertical: isSmall ? 2 : 4,
          paddingHorizontal: isSmall ? 8 : 10,
          borderRadius: 8,
        },
        style,
      ]}
    >
      {icon && <View style={styles.iconContainer}>{icon}</View>}
      <Text
        style={[
          styles.text,
          {
            color: text,
            fontSize: isSmall ? 11 : 12,
          },
          textStyle,
        ]}
      >
        {label}
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
  },
  iconContainer: {
    marginRight: 4,
  },
  text: {
    fontWeight: '600',
    letterSpacing: 0.1,
  },
});
