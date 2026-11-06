export const validateEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email.trim());
};

export const isFacultyEmail = (email: string): boolean => {
  if (!validateEmail(email)) return false;
  const lower = email.trim().toLowerCase();
  return lower.endsWith('@siddhartha.ac.in') || lower === 'vivekvaranasi99@gmail.com';
};

export const isStudentEmail = (email: string): boolean => {
  if (!validateEmail(email)) return false;
  const lower = email.trim().toLowerCase();
  return lower.endsWith('@vrsec.ac.in');
};

export const getDetectedRole = (email: string): 'student' | 'faculty' | null => {
  if (isFacultyEmail(email)) return 'faculty';
  if (isStudentEmail(email)) return 'student';
  return null;
};

export const validateOTP = (otp: string): boolean => {
  const otpRegex = /^\d{6}$/;
  return otpRegex.test(otp.trim());
};

export const validateCGPA = (cgpa: number | string): boolean => {
  const numeric = typeof cgpa === 'string' ? parseFloat(cgpa) : cgpa;
  if (isNaN(numeric)) return false;
  return numeric >= 0.0 && numeric <= 10.0;
};
