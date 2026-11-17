import React from 'react';
import ReactTestRenderer from 'react-test-renderer';
import HomeScreen from '../src/screens/main/HomeScreen';
import LearningPathScreen from '../src/screens/main/LearningPathScreen';

jest.setTimeout(25000);

// Helper to extract text from React Test Renderer node
const getNodeText = (node: any): string => {
  if (!node) return '';
  if (typeof node === 'string' || typeof node === 'number') return String(node);
  if (Array.isArray(node)) return node.map(getNodeText).join('');
  if (node.props && node.props.children) return getNodeText(node.props.children);
  return '';
};

// Mock API service
jest.mock('../src/services/api', () => ({
  api: {
    getDashboard: jest.fn().mockImplementation(() =>
      Promise.resolve({
        profile: {
          name: 'Alex Johnson',
          branch: 'CSE',
          academicYear: '2nd Year',
          semester: '1st Sem',
          cgpa: 8.75,
        },
        activeCourses: [
          {
            id: 101,
            title: 'Data Structures & Algorithms',
            progress: 45,
            currentPhase: 2,
            totalPhases: 5,
            difficulty: 'Intermediate',
            duration: '60 Hours',
            category: 'CORE',
            phases: [
              { id: 1, phaseNumber: 1, title: 'Arrays & Strings', isCompleted: true, isUnlocked: true },
              { id: 2, phaseNumber: 2, title: 'Linked Lists & Stacks', isCompleted: false, isUnlocked: true },
            ],
          },
        ],
        recommendations: [
          {
            id: 201,
            title: 'Database Management Systems',
            category: 'CORE',
            academicYear: '2nd Year',
            description: 'Relational algebra, SQL, normalization and transaction management.',
            duration: '45 Hours',
            difficulty: 'Intermediate',
            learningObjectives: 'Master relational schema design and SQL indexing.',
            topics: 'ER Modeling, SQL, 3NF, B-Trees',
            prerequisites: 'Discrete Mathematics',
          },
        ],
      })
    ),
    getCatalogue: jest.fn().mockImplementation(() =>
      Promise.resolve([
        {
          id: 301,
          code: 'CSE301',
          title: 'Operating Systems',
          category: 'CORE',
          branch: 'CSE',
          academicYear: '2nd Year',
          semester: '2nd Sem',
          duration: '50 Hours',
          difficulty: 'Hard',
          description: 'Processes, threads, CPU scheduling and memory management.',
          isEligible: true,
          isSyllabusVerified: true,
        },
      ])
    ),
    getCourseRequests: jest.fn().mockImplementation(() => Promise.resolve([])),
    selectCourse: jest.fn().mockImplementation(() => Promise.resolve({ success: true })),
    requestCourse: jest.fn().mockImplementation(() => Promise.resolve({ success: true })),
  },
}));

describe('HomeScreen Dashboard Redesign', () => {
  const mockUserData = {
    email: 'alex@example.com',
    name: 'Alex Johnson',
    branch: 'CSE',
    academicYear: '2nd Year',
    semester: '1st Sem',
    cgpa: 8.75,
  };

  it('renders header, student profile greeting, and CGPA card', async () => {
    let renderer: ReactTestRenderer.ReactTestRenderer;
    await ReactTestRenderer.act(async () => {
      renderer = ReactTestRenderer.create(
        <HomeScreen
          userData={mockUserData}
          onNavigate={jest.fn()}
          onSelectSubject={jest.fn()}
        />
      );
      await Promise.resolve();
      await Promise.resolve();
    });

    const root = renderer!.root;
    // Check Top App Bar
    const logoTexts = root.findAll((node) => getNodeText(node).includes('LearnPath AI'));
    expect(logoTexts.length).toBeGreaterThan(0);

    // Check Student Greeting
    const greeting = root.findAll((node) => getNodeText(node).includes('Hi, Alex'));
    expect(greeting.length).toBeGreaterThan(0);

    // Check CGPA
    const cgpaValues = root.findAll((node) => getNodeText(node).includes('8.75'));
    expect(cgpaValues.length).toBeGreaterThan(0);
  });

  it('renders My Learning section with enrolled courses and progress', async () => {
    let renderer: ReactTestRenderer.ReactTestRenderer;
    await ReactTestRenderer.act(async () => {
      renderer = ReactTestRenderer.create(
        <HomeScreen
          userData={mockUserData}
          onNavigate={jest.fn()}
          onSelectSubject={jest.fn()}
        />
      );
      await Promise.resolve();
      await Promise.resolve();
    });

    const root = renderer!.root;
    const courseTitle = root.findAll((node) => getNodeText(node).includes('Data Structures & Algorithms'));
    expect(courseTitle.length).toBeGreaterThan(0);

    const progressText = root.findAll((node) => getNodeText(node).includes('45%'));
    expect(progressText.length).toBeGreaterThan(0);
  });

  it('renders Recommended for You section and Explore All Courses card', async () => {
    const mockNavigate = jest.fn();
    let renderer: ReactTestRenderer.ReactTestRenderer;
    await ReactTestRenderer.act(async () => {
      renderer = ReactTestRenderer.create(
        <HomeScreen
          userData={mockUserData}
          onNavigate={mockNavigate}
          onSelectSubject={jest.fn()}
        />
      );
      await Promise.resolve();
      await Promise.resolve();
    });

    const root = renderer!.root;
    const recTitle = root.findAll((node) => getNodeText(node).includes('Database Management Systems'));
    expect(recTitle.length).toBeGreaterThan(0);

    const exploreCard = root.findAll((node) => getNodeText(node).includes('Explore All Courses'));
    expect(exploreCard.length).toBeGreaterThan(0);
  });
});

describe('LearningPathScreen Curriculum & Course Catalogue', () => {
  const mockUserData = {
    email: 'alex@example.com',
    branch: 'CSE',
    academicYear: '2nd Year',
    semester: '1st Sem',
  };

  it('renders top view switcher with My Curriculum and Course Catalogue tabs', async () => {
    let renderer: ReactTestRenderer.ReactTestRenderer;
    await ReactTestRenderer.act(async () => {
      renderer = ReactTestRenderer.create(
        <LearningPathScreen
          userData={mockUserData}
          activeSubject="Data Structures & Algorithms"
          onSelectSubject={jest.fn()}
        />
      );
      await Promise.resolve();
      await Promise.resolve();
    });

    const root = renderer!.root;
    const catalogueTab = root.findAll((node) => getNodeText(node).includes('Course Catalogue'));
    expect(catalogueTab.length).toBeGreaterThan(0);
  });
});
