import { Easing, WithTimingConfig, WithSpringConfig } from 'react-native-reanimated';

export const springConfig: WithSpringConfig = {
  damping: 18,
  stiffness: 160,
  mass: 0.8,
  overshootClamping: false,
};

export const timingConfig: WithTimingConfig = {
  duration: 250,
  easing: Easing.bezier(0.25, 0.1, 0.25, 1),
};

export const slowTimingConfig: WithTimingConfig = {
  duration: 400,
  easing: Easing.out(Easing.cubic),
};

export const cardTransitionConfig = {
  enterDuration: 300,
  staggerDelay: 60,
};
