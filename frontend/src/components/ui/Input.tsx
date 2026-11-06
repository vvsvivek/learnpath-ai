import React, { useState } from 'react';
import {
  View,
  TextInput,
  Text,
  StyleSheet,
  TextInputProps,
  ViewStyle,
  TextStyle,
} from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
} from 'react-native-reanimated';
import { useThemeContext } from '../../context/ThemeContext';

export interface InputProps extends TextInputProps {
  label?: string;
  error?: string;
  hint?: string;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
  containerStyle?: ViewStyle;
  inputStyle?: TextStyle;
}

export const Input: React.FC<InputProps> = ({
  label,
  error,
  hint,
  leftIcon,
  rightIcon,
  containerStyle,
  inputStyle,
  onFocus,
  onBlur,
  ...restProps
}) => {
  const { theme } = useThemeContext();
  const [isFocused, setIsFocused] = useState(false);
  const borderProgress = useSharedValue(0);

  const handleFocus = (e: any) => {
    setIsFocused(true);
    borderProgress.value = withTiming(1, { duration: 200 });
    onFocus?.(e);
  };

  const handleBlur = (e: any) => {
    setIsFocused(false);
    borderProgress.value = withTiming(0, { duration: 200 });
    onBlur?.(e);
  };

  const isDark = theme.dark;

  const getBorderColor = (): string => {
    if (error) return theme.colors.error;
    if (isFocused) return theme.colors.primary;
    return isDark ? theme.colors.border : theme.colors.borderLight;
  };

  const animatedBorderStyle = useAnimatedStyle(() => {
    return {
      borderColor: getBorderColor(),
    };
  });

  return (
    <View style={[styles.wrapper, containerStyle]}>
      {label && (
        <Text
          style={[
            styles.label,
            {
              color: error
                ? theme.colors.error
                : isFocused
                ? theme.colors.primary
                : theme.colors.textSecondary,
            },
          ]}
        >
          {label}
        </Text>
      )}

      <Animated.View
        style={[
          styles.inputContainer,
          {
            backgroundColor: isDark
              ? theme.colors.surfaceSubtle
              : theme.colors.inputBackground,
            borderWidth: 1.5,
          },
          animatedBorderStyle,
        ]}
      >
        {leftIcon && <View style={styles.iconLeft}>{leftIcon}</View>}

        <TextInput
          placeholderTextColor={theme.colors.textTertiary}
          style={[
            styles.input,
            {
              color: theme.colors.textPrimary,
            },
            inputStyle,
          ]}
          onFocus={handleFocus}
          onBlur={handleBlur}
          {...restProps}
        />

        {rightIcon && <View style={styles.iconRight}>{rightIcon}</View>}
      </Animated.View>

      {error ? (
        <Text style={[styles.message, { color: theme.colors.error }]}>
          {error}
        </Text>
      ) : hint ? (
        <Text style={[styles.message, { color: theme.colors.textTertiary }]}>
          {hint}
        </Text>
      ) : null}
    </View>
  );
};

const styles = StyleSheet.create({
  wrapper: {
    marginBottom: 16,
    width: '100%',
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 6,
    letterSpacing: 0.2,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 12,
    paddingHorizontal: 12,
    minHeight: 48,
  },
  input: {
    flex: 1,
    fontSize: 15,
    paddingVertical: 10,
  },
  iconLeft: {
    marginRight: 10,
  },
  iconRight: {
    marginLeft: 10,
  },
  message: {
    fontSize: 12,
    marginTop: 4,
    marginLeft: 2,
  },
});
