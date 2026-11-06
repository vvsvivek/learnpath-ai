import React from 'react';
import {
  Pressable,
  Text,
  ActivityIndicator,
  StyleSheet,
  View,
  ViewStyle,
  TextStyle,
} from 'react-native';
import Animated from 'react-native-reanimated';
import { useThemeContext } from '../../context/ThemeContext';
import { useScaleAnimation } from '../../animations/microInteractions';

const AnimatedPressable = Animated.createAnimatedComponent(Pressable);

export type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
export type ButtonSize = 'sm' | 'md' | 'lg';

export interface ButtonProps {
  title: string;
  onPress: () => void;
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
  disabled?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
  style?: ViewStyle;
  textStyle?: TextStyle;
  fullWidth?: boolean;
}

export const Button: React.FC<ButtonProps> = ({
  title,
  onPress,
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  leftIcon,
  rightIcon,
  style,
  textStyle,
  fullWidth = false,
}) => {
  const { theme } = useThemeContext();
  const { animatedStyle, onPressIn, onPressOut } = useScaleAnimation(0.97);

  const getContainerStyle = (): ViewStyle => {
    const isDark = theme.dark;

    let baseBg = theme.colors.primary;
    let baseBorder = 'transparent';
    let borderWidth = 0;

    switch (variant) {
      case 'primary':
        baseBg = theme.colors.primary;
        break;
      case 'secondary':
        baseBg = isDark ? theme.colors.surfaceElevated : theme.colors.navyLight;
        break;
      case 'outline':
        baseBg = 'transparent';
        baseBorder = theme.colors.primary;
        borderWidth = 1.5;
        break;
      case 'ghost':
        baseBg = 'transparent';
        break;
      case 'danger':
        baseBg = theme.colors.error;
        break;
    }

    if (disabled) {
      baseBg = isDark ? '#262D3D' : '#E2E8F0';
      baseBorder = 'transparent';
    }

    let paddingVertical = 12;
    let paddingHorizontal = 20;
    let borderRadius = 12;

    if (size === 'sm') {
      paddingVertical = 8;
      paddingHorizontal = 14;
      borderRadius = 8;
    } else if (size === 'lg') {
      paddingVertical = 16;
      paddingHorizontal = 28;
      borderRadius = 14;
    }

    return {
      backgroundColor: baseBg,
      borderColor: baseBorder,
      borderWidth,
      paddingVertical,
      paddingHorizontal,
      borderRadius,
      alignItems: 'center',
      justifyContent: 'center',
      flexDirection: 'row',
      width: fullWidth ? '100%' : 'auto',
      opacity: disabled && !loading ? 0.6 : 1,
    };
  };

  const getTextColor = (): string => {
    if (disabled) {
      return theme.colors.textTertiary;
    }
    switch (variant) {
      case 'primary':
      case 'secondary':
      case 'danger':
        return '#FFFFFF';
      case 'outline':
      case 'ghost':
        return theme.colors.primary;
      default:
        return '#FFFFFF';
    }
  };

  const getTextSizeStyle = (): TextStyle => {
    if (size === 'sm') {
      return { fontSize: 13, fontWeight: '600' };
    } else if (size === 'lg') {
      return { fontSize: 16, fontWeight: '600' };
    }
    return { fontSize: 15, fontWeight: '600' };
  };

  return (
    <AnimatedPressable
      onPress={onPress}
      onPressIn={onPressIn}
      onPressOut={onPressOut}
      disabled={disabled || loading}
      style={[animatedStyle, getContainerStyle(), style]}
    >
      {loading ? (
        <ActivityIndicator
          size="small"
          color={getTextColor()}
          style={styles.spinner}
        />
      ) : (
        <>
          {leftIcon && <View style={styles.leftIconContainer}>{leftIcon}</View>}
          <Text
            style={[
              { color: getTextColor() },
              getTextSizeStyle(),
              textStyle,
            ]}
          >
            {title}
          </Text>
          {rightIcon && <View style={styles.rightIconContainer}>{rightIcon}</View>}
        </>
      )}
    </AnimatedPressable>
  );
};

const styles = StyleSheet.create({
  spinner: {
    paddingVertical: 2,
  },
  leftIconContainer: {
    marginRight: 8,
  },
  rightIconContainer: {
    marginLeft: 8,
  },
});
