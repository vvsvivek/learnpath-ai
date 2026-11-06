import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, Modal, ActivityIndicator } from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withSequence,
  withTiming,
  Easing,
} from 'react-native-reanimated';
import { useThemeContext } from '../../context/ThemeContext';
import { Button } from './Button';

export interface ServerWakeupProps {
  visible: boolean;
  onRetry?: () => void;
  message?: string;
}

export const ServerWakeup: React.FC<ServerWakeupProps> = ({
  visible,
  onRetry,
  message = 'Waking up the AI server. Since the backend is hosted on a free tier, this may take up to 45 seconds on first launch.',
}) => {
  const { theme } = useThemeContext();
  const [seconds, setSeconds] = useState(0);

  const pulseScale = useSharedValue(1);

  useEffect(() => {
    if (visible) {
      setSeconds(0);
      const interval = setInterval(() => {
        setSeconds((prev) => prev + 1);
      }, 1000);

      pulseScale.value = withRepeat(
        withSequence(
          withTiming(1.08, { duration: 1000, easing: Easing.inOut(Easing.ease) }),
          withTiming(1, { duration: 1000, easing: Easing.inOut(Easing.ease) })
        ),
        -1,
        true
      );

      return () => {
        clearInterval(interval);
      };
    }
  }, [visible]);

  const animatedPulse = useAnimatedStyle(() => {
    return {
      transform: [{ scale: pulseScale.value }],
    };
  });

  if (!visible) return null;

  return (
    <Modal visible={visible} transparent animationType="fade">
      <View style={[styles.backdrop, { backgroundColor: theme.colors.overlay }]}>
        <View
          style={[
            styles.card,
            {
              backgroundColor: theme.colors.surface,
              borderColor: theme.colors.border,
            },
          ]}
        >
          <Animated.View
            style={[
              styles.iconCircle,
              { backgroundColor: theme.colors.primaryLight },
              animatedPulse,
            ]}
          >
            <ActivityIndicator size="large" color={theme.colors.primary} />
          </Animated.View>

          <Text style={[styles.title, { color: theme.colors.textPrimary }]}>
            Connecting to Server
          </Text>

          <Text style={[styles.timer, { color: theme.colors.primary }]}>
            {seconds}s elapsed
          </Text>

          <Text style={[styles.message, { color: theme.colors.textSecondary }]}>
            {message}
          </Text>

          {seconds > 30 && onRetry && (
            <Button
              title="Retry Connection"
              variant="outline"
              size="sm"
              onPress={onRetry}
              style={styles.retryButton}
            />
          )}
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  card: {
    width: '100%',
    maxWidth: 340,
    borderRadius: 20,
    borderWidth: 1,
    padding: 24,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.25,
    shadowRadius: 20,
    elevation: 10,
  },
  iconCircle: {
    width: 72,
    height: 72,
    borderRadius: 36,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
    marginBottom: 6,
    textAlign: 'center',
  },
  timer: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 12,
  },
  message: {
    fontSize: 13,
    lineHeight: 18,
    textAlign: 'center',
    marginBottom: 16,
  },
  retryButton: {
    marginTop: 8,
    minWidth: 140,
  },
});
