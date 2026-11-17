// LearnPath AI — Central API Configuration
import { Platform } from 'react-native';

const RENDER_CLOUD_URL = 'https://ai-learning-backend-ztk7.onrender.com';
const LOCAL_DEV_URL = Platform.OS === 'android' ? 'http://10.0.2.2:5000' : 'http://localhost:5000';

// Default to production cloud URL (Render)
export const API_BASE_URL = RENDER_CLOUD_URL;
export const API_ROOT = `${API_BASE_URL}/api`;

export default {
  API_BASE_URL,
  API_ROOT,
  RENDER_CLOUD_URL,
  LOCAL_DEV_URL,
};
