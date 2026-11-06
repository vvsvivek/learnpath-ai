import { ColorPalette, darkColors, lightColors } from './colors';
import { TypographyScale, typography } from './typography';
import { borderRadius, shadows, spacing } from './spacing';

export interface Theme {
  dark: boolean;
  colors: ColorPalette;
  typography: TypographyScale;
  spacing: typeof spacing;
  borderRadius: typeof borderRadius;
  shadows: typeof shadows;
}

export const lightTheme: Theme = {
  dark: false,
  colors: lightColors,
  typography,
  spacing,
  borderRadius,
  shadows,
};

export const darkTheme: Theme = {
  dark: true,
  colors: darkColors,
  typography,
  spacing,
  borderRadius,
  shadows,
};

export * from './colors';
export * from './typography';
export * from './spacing';
