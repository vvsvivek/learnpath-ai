import apiClient, { aiApiClient } from './api';
import { API_ENDPOINTS } from '../utils/constants';
import {
  ChatMessage,
  Conversation,
  SendMessageRequest,
  SendMessageResponse,
} from '../types';

export const chatService = {
  getConversations: async (): Promise<Conversation[]> => {
    const response = await apiClient.get<Conversation[]>(
      API_ENDPOINTS.CONVERSATIONS
    );
    return response.data;
  },

  getMessages: async (conversationId: string): Promise<ChatMessage[]> => {
    const response = await apiClient.get<ChatMessage[]>(
      API_ENDPOINTS.MESSAGES(conversationId)
    );
    return response.data;
  },

  createConversation: async (
    title: string = 'New Conversation',
    courseId?: string
  ): Promise<Conversation> => {
    const response = await apiClient.post<Conversation>(
      API_ENDPOINTS.CONVERSATIONS,
      {
        title,
        course_id: courseId,
      }
    );
    return response.data;
  },

  sendStudentMessage: async (req: SendMessageRequest): Promise<SendMessageResponse> => {
    // Student chat with Google Gemini AI tutor
    const response = await aiApiClient.post<SendMessageResponse>(
      API_ENDPOINTS.SEND_STUDENT_MESSAGE,
      req
    );
    return response.data;
  },

  sendFacultyMessage: async (req: SendMessageRequest): Promise<SendMessageResponse> => {
    // Faculty chat with Google Gemini AI analytics & curriculum copilot
    const response = await aiApiClient.post<SendMessageResponse>(
      API_ENDPOINTS.SEND_FACULTY_MESSAGE,
      req
    );
    return response.data;
  },
};
