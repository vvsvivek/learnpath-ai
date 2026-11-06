import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import Svg, { Rect, Text as SvgText, Line } from 'react-native-svg';
import Animated, { FadeIn } from 'react-native-reanimated';
import { useTheme } from '../../hooks/useTheme';

interface BarChartData {
  label: string;
  value: number;
  color?: string;
}

interface BarChartProps {
  data: BarChartData[];
  title?: string;
  height?: number;
  barWidth?: number;
  maxValue?: number;
  showValues?: boolean;
}

export function BarChart({
  data,
  title,
  height = 200,
  barWidth = 32,
  maxValue,
  showValues = true,
}: BarChartProps) {
  const { theme } = useTheme();
  const max = maxValue || Math.max(...data.map((d) => d.value), 1);
  const chartWidth = Math.max(data.length * (barWidth + 20) + 40, 280);
  const chartHeight = height - 40;
  const paddingBottom = 30;
  const paddingLeft = 35;

  return (
    <Animated.View entering={FadeIn.duration(400)} style={styles.container}>
      {title && (
        <Text style={[styles.title, { color: theme.colors.textPrimary }]}>{title}</Text>
      )}
      <Svg
        width={chartWidth}
        height={height}
        viewBox={`0 0 ${chartWidth} ${height}`}
      >
        {/* Y-axis lines */}
        {[0, 0.25, 0.5, 0.75, 1].map((ratio) => {
          const y = paddingBottom + chartHeight * (1 - ratio);
          return (
            <React.Fragment key={ratio}>
              <Line
                x1={paddingLeft}
                y1={y}
                x2={chartWidth - 10}
                y2={y}
                stroke={theme.colors.border}
                strokeWidth={0.5}
                strokeDasharray="4,4"
              />
              <SvgText
                x={paddingLeft - 5}
                y={y + 4}
                fontSize={10}
                fill={theme.colors.textSecondary}
                textAnchor="end"
              >
                {Math.round(max * ratio)}
              </SvgText>
            </React.Fragment>
          );
        })}
        {/* Bars */}
        {data.map((item, index) => {
          const barHeight = (item.value / max) * chartHeight;
          const x = paddingLeft + 15 + index * (barWidth + 20);
          const y = paddingBottom + chartHeight - barHeight;
          const barColor = item.color || theme.colors.primary;

          return (
            <React.Fragment key={index}>
              <Rect
                x={x}
                y={y}
                width={barWidth}
                height={barHeight}
                rx={4}
                ry={4}
                fill={barColor}
                opacity={0.85}
              />
              {showValues && (
                <SvgText
                  x={x + barWidth / 2}
                  y={y - 6}
                  fontSize={10}
                  fill={theme.colors.textPrimary}
                  textAnchor="middle"
                  fontWeight="600"
                >
                  {item.value}
                </SvgText>
              )}
              <SvgText
                x={x + barWidth / 2}
                y={height - 5}
                fontSize={9}
                fill={theme.colors.textSecondary}
                textAnchor="middle"
              >
                {item.label}
              </SvgText>
            </React.Fragment>
          );
        })}
      </Svg>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    paddingVertical: 8,
  },
  title: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 12,
  },
});
