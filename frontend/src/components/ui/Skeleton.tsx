import React from 'react';
import { View, ViewStyle, DimensionValue } from 'react-native';
import Animated from 'react-native-reanimated';
import { useThemeContext } from '../../context/ThemeContext';
import { useShimmer } from '../../animations/microInteractions';

export interface SkeletonProps {
  width?: DimensionValue;
  height?: DimensionValue;
  borderRadius?: number;
  style?: ViewStyle;
}

export const Skeleton: React.FC<SkeletonProps> = ({
  width = '100%',
  height = 20,
  borderRadius = 8,
  style,
}) => {
  const { theme } = useThemeContext();
  const animatedShimmer = useShimmer(1400);

  const baseColor = theme.dark ? '#232A3B' : '#E6ECF2';

  return (
    <Animated.View
      style={[
        {
          width,
          height,
          borderRadius,
          backgroundColor: baseColor,
        },
        animatedShimmer,
        style,
      ]}
    />
  );
};
