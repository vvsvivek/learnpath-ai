import React from 'react';
import { View, StyleSheet } from 'react-native';
import Svg, {
  Path,
  Rect,
  Circle,
  G,
  Defs,
  LinearGradient,
  Stop,
} from 'react-native-svg';
import { Colors } from '../styles/theme';

interface IllustrationProps {
  width?: number;
  height?: number;
}

/**
 * Premium Human-Designed Educational Vector Illustration:
 * A student walking briskly while carrying a stack of books,
 * surrounded by gentle educational elements (floating spark, graduation cap, analytics card).
 */
export const WalkingStudentIllustration: React.FC<IllustrationProps> = ({
  width = 300,
  height = 240,
}) => {
  return (
    <View style={styles.container}>
      <Svg width={width} height={height} viewBox="0 0 320 260" fill="none">
        <Defs>
          <LinearGradient id="bgGlow" x1="0" y1="0" x2="320" y2="260" gradientUnits="userSpaceOnUse">
            <Stop offset="0%" stopColor="#EBF3FC" stopOpacity="0.8" />
            <Stop offset="100%" stopColor="#FFFFFF" stopOpacity="0.2" />
          </LinearGradient>
          <LinearGradient id="bookBlue" x1="0" y1="0" x2="100" y2="50" gradientUnits="userSpaceOnUse">
            <Stop offset="0%" stopColor="#4A90D9" />
            <Stop offset="100%" stopColor="#1E3A5F" />
          </LinearGradient>
          <LinearGradient id="coatNavy" x1="0" y1="0" x2="0" y2="100" gradientUnits="userSpaceOnUse">
            <Stop offset="0%" stopColor="#1E3A5F" />
            <Stop offset="100%" stopColor="#0F172A" />
          </LinearGradient>
        </Defs>

        {/* Soft Ambient Background Aura */}
        <Circle cx="160" cy="140" r="105" fill="url(#bgGlow)" />
        <Circle cx="70" cy="70" r="6" fill="#4A90D9" opacity="0.3" />
        <Circle cx="270" cy="90" r="8" fill="#38BDF8" opacity="0.4" />
        <Circle cx="240" cy="190" r="5" fill="#4A90D9" opacity="0.3" />
        <Circle cx="50" cy="170" r="7" fill="#1E3A5F" opacity="0.2" />

        {/* Floating Academic Badge 1: Top Right Graduation Cap */}
        <G transform="translate(230, 35)">
          <Rect x="0" y="0" width="46" height="46" rx="12" fill="#FFFFFF" />
          <Path d="M12 24 L23 18 L34 24 L23 30 Z" fill="#4A90D9" />
          <Path d="M17 26.5 L17 31 C17 33 29 33 29 31 L29 26.5" stroke="#1E3A5F" strokeWidth="1.5" fill="none" />
          <Path d="M30 24.5 L34 30 L34 33" stroke="#F59E0B" strokeWidth="1.2" strokeLinecap="round" />
        </G>

        {/* Floating Academic Badge 2: Top Left Sparkle & Brain/Code */}
        <G transform="translate(45, 45)">
          <Rect x="0" y="0" width="44" height="44" rx="12" fill="#FFFFFF" />
          <Path d="M16 22 L20 18 M16 22 L20 26 M28 22 L24 18 M28 22 L24 26" stroke="#4A90D9" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          <Circle cx="22" cy="14" r="2" fill="#10B981" />
        </G>

        {/* Walking Path Ground Line */}
        <Path d="M40 235 C110 232 210 232 280 235" stroke="#E2E8F0" strokeWidth="3" strokeLinecap="round" />

        {/* --- WALKING PERSON CARRYING BOOKS --- */}
        {/* Shadow */}
        <Path d="M135 235 C150 233 185 233 200 235 C185 237 150 237 135 235 Z" fill="#CBD5E1" opacity="0.6" />

        {/* Left Leg (Stepping Forward) */}
        <Path d="M162 165 L178 198 L188 232 L198 233" stroke="#1E3A5F" strokeWidth="9" strokeLinecap="round" strokeLinejoin="round" fill="none" />
        {/* Left Shoe */}
        <Path d="M185 232 L202 232 C205 232 206 235 204 237 L185 237 Z" fill="#0F172A" />

        {/* Right Leg (Trailing in stride) */}
        <Path d="M152 165 L140 196 L130 228 L122 232" stroke="#0F172A" strokeWidth="8.5" strokeLinecap="round" strokeLinejoin="round" fill="none" />
        {/* Right Shoe */}
        <Path d="M120 230 L132 230 C134 230 134 233 132 235 L118 235 Z" fill="#0F172A" />

        {/* Torso & Stylish Blue Jacket */}
        <Path d="M148 115 C155 115 170 118 174 135 L168 170 C168 172 152 172 146 170 L142 135 C142 120 145 115 148 115 Z" fill="url(#coatNavy)" />
        {/* Soft Blue Shirt collar */}
        <Path d="M156 115 L162 128 L168 115 Z" fill="#4A90D9" />

        {/* Head & Face (Profile smiling forward) */}
        <Circle cx="162" cy="95" r="14" fill="#FCD34D" /> {/* Warm skin tone */}
        {/* Modern Haircut */}
        <Path d="M150 94 C150 82 174 80 176 90 C176 96 172 99 168 100 C165 96 153 97 150 94 Z" fill="#1E293B" />
        {/* Glasses */}
        <Circle cx="166" cy="94" r="3.5" stroke="#1E3A5F" strokeWidth="1.2" fill="none" />
        <Path d="M162 94 L165 94" stroke="#1E3A5F" strokeWidth="1" />

        {/* Arms Carrying the Stack of Books */}
        {/* Stack of 3 Books held firmly */}
        {/* Book 1 (Bottom, Soft Blue) */}
        <G transform="translate(162, 132) rotate(-6)">
          <Rect x="0" y="0" width="34" height="8" rx="2" fill="#4A90D9" />
          <Rect x="3" y="1" width="28" height="6" fill="#FFFFFF" />
          <Rect x="0" y="0" width="4" height="8" rx="1" fill="#1E3A5F" />
        </G>
        {/* Book 2 (Middle, Deep Navy) */}
        <G transform="translate(164, 124) rotate(-3)">
          <Rect x="0" y="0" width="32" height="7.5" rx="2" fill="#1E3A5F" />
          <Rect x="3" y="1" width="26" height="5.5" fill="#FFFFFF" />
          <Rect x="0" y="0" width="4" height="7.5" rx="1" fill="#38BDF8" />
        </G>
        {/* Book 3 (Top, Gold/Amber Journal) */}
        <G transform="translate(166, 116) rotate(1)">
          <Rect x="0" y="0" width="30" height="7" rx="2" fill="#F59E0B" />
          <Rect x="3" y="1" width="24" height="5" fill="#FFFFFF" />
          <Rect x="0" y="0" width="3.5" height="7" rx="1" fill="#D97706" />
        </G>

        {/* Forearm & Hand clasping the books */}
        <Path d="M148 132 C155 142 168 144 186 138" stroke="#1E3A5F" strokeWidth="7" strokeLinecap="round" fill="none" />
        <Circle cx="187" cy="137" r="4.5" fill="#FCD34D" />

        {/* Floating Academic Starlets */}
        <Path d="M210 110 L212 115 L217 117 L212 119 L210 124 L208 119 L203 117 L208 115 Z" fill="#F59E0B" />
        <Path d="M115 130 L116.5 133.5 L120 135 L116.5 136.5 L115 140 L113.5 136.5 L110 135 L113.5 133.5 Z" fill="#4A90D9" />
      </Svg>
    </View>
  );
};

export const GoogleLogoIcon: React.FC<{ size?: number }> = ({ size = 20 }) => (
  <Svg width={size} height={size} viewBox="0 0 24 24">
    <Path
      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
      fill="#4285F4"
    />
    <Path
      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
      fill="#34A853"
    />
    <Path
      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
      fill="#FBBC05"
    />
    <Path
      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
      fill="#EA4335"
    />
  </Svg>
);

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
  },
});
