import { useState, useEffect, useCallback } from 'react';
import { CourseEnrollment, CourseRecommendation } from '../types';
import { courseService } from '../services/courseService';

export const useCourses = () => {
  const [enrolledCourses, setEnrolledCourses] = useState<CourseEnrollment[]>([]);
  const [recommendations, setRecommendations] = useState<CourseRecommendation[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchCoursesData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [enrolled, recs] = await Promise.allSettled([
        courseService.getEnrolledCourses(),
        courseService.getRecommendations(),
      ]);

      if (enrolled.status === 'fulfilled') {
        setEnrolledCourses(enrolled.value || []);
      }
      if (recs.status === 'fulfilled') {
        setRecommendations(recs.value || []);
      }
    } catch (err: any) {
      setError(err?.message || 'Failed to load courses');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCoursesData();
  }, [fetchCoursesData]);

  const dismissRecommendation = async (recId: string) => {
    try {
      await courseService.dismissRecommendation(recId);
      setRecommendations((prev) => prev.filter((r) => r.id !== recId));
    } catch (err: any) {
      console.warn('Failed to dismiss recommendation:', err);
    }
  };

  const enroll = async (courseId: string) => {
    try {
      const newEnrollment = await courseService.enrollInCourse(courseId);
      setEnrolledCourses((prev) => [newEnrollment, ...prev]);
      setRecommendations((prev) => prev.filter((r) => r.course.id !== courseId));
      return newEnrollment;
    } catch (err: any) {
      throw err;
    }
  };

  return {
    enrolledCourses,
    recommendations,
    isLoading,
    error,
    refreshCourses: fetchCoursesData,
    dismissRecommendation,
    enroll,
  };
};
