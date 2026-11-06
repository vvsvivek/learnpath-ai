import React from 'react';
import { View, Text, Image, StyleSheet, ViewStyle } from 'react-native';
import { useThemeContext } from '../../context/ThemeContext';
import { getInitials } from '../../utils/formatters';

export type AvatarSize = 'sm' | 'md' | 'lg' | 'xl';

export interface AvatarProps {
  name?: string;
  imageUrl?: string;
  size?: AvatarSize;
  style?: ViewStyle;
  showBadge?: boolean;
  badgeColor?: string;
}

export const Avatar: React.FC<AvatarProps> = ({
  name,
  imageUrl,
  size = 'md',
  style,
  showBadge = false,
  badgeColor,
}) => {
  const { theme } = useThemeContext();

  const getDimension = () => {
    switch (size) {
      case 'sm':
        return 34;
      case 'lg':
        return 60;
      case 'xl':
        return 88;
      case 'md':
      default:
        return 44;
    }
  };

  const getFontSize = () => {
    switch (size) {
      case 'sm':
        return 13;
      case 'lg':
        return 22;
      case 'xl':
        return 32;
      case 'md':
      default:
        return 16;
    }
  };

  const dimension = getDimension();
  const initials = getInitials(name);

  return (
    <View style={[{ width: dimension, height: dimension }, style]}>
      {imageUrl ? (
        <Image
          source={{ uri: imageUrl }}
          style={[
            styles.image,
            {
              width: dimension,
              height: dimension,
              borderRadius: dimension / 2,
              backgroundColor: theme.colors.surfaceSubtle,
            },
          ]}
        />
      ) : (
        <View
          style={[
            styles.fallback,
            {
              width: dimension,
              height: dimension,
              borderRadius: dimension / 2,
              backgroundColor: theme.colors.primaryLight,
              borderColor: theme.colors.primary,
              borderWidth: 1.5,
            },
          ]}
        >
          <Text
            style={[
              styles.initials,
              {
                color: theme.colors.primaryDark,
                fontSize: getFontSize(),
              },
            ]}
          >
            {initials}
          </Text>
        </View>
      )}

      {showBadge && (
        <View
          style={[
            styles.badge,
            {
              backgroundColor: badgeColor || theme.colors.success,
              borderColor: theme.colors.surface,
              width: Math.max(10, dimension * 0.28),
              height: Math.max(10, dimension * 0.28),
              borderRadius: dimension * 0.14,
            },
          ]}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  image: {
    resizeMode: 'cover',
  },
  fallback: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  initials: {
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  badge: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    borderWidth: 2,
  },
});
