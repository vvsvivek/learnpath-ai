import {
  BRANCH_AREAS_OF_INTEREST,
  getAreasOfInterestForBranch,
  getSpecializationCoursesForInterest,
  SPECIALIZATION_COURSE_REGISTRY,
} from '../src/config/academicData';

describe('Area of Interest Course Recommendations Engine', () => {
  it('should define areas of interest for all engineering branches', () => {
    const branches = ['CSE', 'IT', 'ECE', 'EEE', 'MECH', 'CIVIL', 'AIML', 'AIDS'];
    branches.forEach((b) => {
      const interests = getAreasOfInterestForBranch(b);
      expect(Array.isArray(interests)).toBe(true);
      expect(interests.length).toBeGreaterThan(0);
    });
  });

  it('should return AI/ML courses for Artificial Intelligence & Machine Learning interest', () => {
    const courses = getSpecializationCoursesForInterest('Artificial Intelligence & Machine Learning', 'CSE');
    expect(courses.length).toBeGreaterThanOrEqual(3);
    const titles = courses.map((c) => c.title);
    expect(titles).toContain('Deep Learning & Neural Network Architectures');
    expect(titles).toContain('Generative AI, LLMs & Prompt Engineering');
  });

  it('should return Cybersecurity courses for Cybersecurity & Ethical Hacking interest', () => {
    const courses = getSpecializationCoursesForInterest('Cybersecurity & Ethical Hacking', 'CSE');
    expect(courses.length).toBeGreaterThanOrEqual(3);
    const titles = courses.map((c) => c.title);
    expect(titles).toContain('Information & Cyber Security Engineering');
    expect(titles).toContain('Ethical Hacking & Network Defense Systems');
  });

  it('should return Cloud/DevOps courses for Cloud Computing & DevOps interest', () => {
    const courses = getSpecializationCoursesForInterest('Cloud Computing & DevOps', 'IT');
    expect(courses.length).toBeGreaterThanOrEqual(3);
    const titles = courses.map((c) => c.title);
    expect(titles).toContain('Cloud Infrastructure & DevOps Automation');
    expect(titles).toContain('Distributed Systems & Cloud Computing Architectures');
  });

  it('should return EV Powertrains courses for Electric Vehicles & Powertrains interest', () => {
    const courses = getSpecializationCoursesForInterest('Electric Vehicles & Powertrains', 'EEE');
    expect(courses.length).toBeGreaterThanOrEqual(3);
    const titles = courses.map((c) => c.title);
    expect(titles).toContain('Electric Vehicle Powertrains & Renewable Energy');
  });

  it('should return Robotics & Mechatronics courses for Robotics interest', () => {
    const courses = getSpecializationCoursesForInterest('Robotics & Mechatronics', 'MECH');
    expect(courses.length).toBeGreaterThanOrEqual(3);
    const titles = courses.map((c) => c.title);
    expect(titles).toContain('Robotics & Industrial Automation Systems');
  });

  it('should return Structural BIM courses for Structural Engineering & BIM interest', () => {
    const courses = getSpecializationCoursesForInterest('Structural Engineering & BIM', 'CIVIL');
    expect(courses.length).toBeGreaterThanOrEqual(3);
    const titles = courses.map((c) => c.title);
    expect(titles).toContain('Design of Reinforced Concrete Structures (RCC)');
  });

  it('should fuzzy match partial interest strings gracefully', () => {
    const courses = getSpecializationCoursesForInterest('Machine Learning', 'CSE');
    expect(courses.length).toBeGreaterThanOrEqual(1);
    expect(courses[0].category).toBe('Specialization AI');
  });
});
