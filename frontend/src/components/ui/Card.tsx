import React from 'react';
import {
  View,
  Pressable,
  StyleSheet,
  ViewStyle,
  StyleProp,
} from 'react-native';
import Animated from 'react-native-reanimated';
import { useThemeContext } from '../../context/ThemeContext';
import { useScaleAnimation } from '../../animations/microInteractions';

const AnimatedPressable = Animated.createAnimatedComponent(Pressable);

export type CardVariant = 'default' | 'elevated' | 'outline';

export interface CardProps {
  children: React.ReactNode;
  variant?: CardVariant;
  onPress?: () => void;
  style?: StyleProp<ViewStyle>;
  padding?: number;
}

export const Card: React.FC<CardProps> = ({
  children,
  variant = 'default',
  onPress,
  style,
  padding = 16,
}) => {
  const { theme } = useThemeContext();
  const { animatedStyle, onPressIn, onPressOut } = useScaleAnimation(0.985);

  const getCardStyle = (): ViewStyle => {
    const isDark = theme.dark;

    let backgroundColor = isDark ? theme.colors.surface : theme.colors.surface;
    let borderColor = isDark ? theme.colors.border : theme.colors.borderLight;
    let borderWidth = 1;
    let shadowStyle = {};

    if (variant === 'elevated') {
      backgroundColor = isDark ? theme.colors.surfaceElevated : '#FFFFFF';
      borderWidth = isDark ? 1 : 0;
      shadowStyle = isDark
        ? {
            shadowColor: '#000',
            shadowOffset: { width: 0, height: 4 },
            shadowOpacity: 0.35,
            shadowRadius: 10,
            elevation: 6,
          }
        : theme.shadows.card;
    } else if (variant === 'outline') {
      backgroundColor = 'transparent';
      borderWidth = 1.5;
      borderColor = isDark ? theme.colors.border : theme.colors.border;
      shadowStyle = {};
    } else {
      // default
      shadowStyle = isDark
        ? {
            shadowColor: '#000',
            shadowOffset: { width: 0, height: 2 },
            shadowOpacity: 0.2,
            shadowRadius: 6,
            elevation: 3,
          }
        : theme.shadows.subtle;
    }

    return {
      backgroundColor,
      borderColor,
      borderWidth,
      borderRadius: 16,
      padding,
      ...shadowStyle,
    };
  };

  if (onPress) {
    return (
      <AnimatedPressable
        onPress={onPress}
        onPressIn={onPressIn}
        onPressOut={onPressOut}
        style={[getCardStyle(), animatedStyle, style]}
      >
        {children}
      </AnimatedPressable>
    );
  }

  return <View style={[getCardStyle(), style]}>{children}</View>;
};
