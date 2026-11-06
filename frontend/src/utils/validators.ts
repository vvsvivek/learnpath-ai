export const validateEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email.trim());
};

export const isFacultyEmail = (email: string, domain: string = 'siddhartha.ac.in'): boolean => {
  if (!validateEmail(email)) return false;
  const lower = email.trim().toLowerCase();
  return lower.endsWith(`@${domain}`) || lower.endsWith('.edu') || lower.endsWith('.ac.in');
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
