export type MessageSenderRole = 'user' | 'assistant' | 'system';

export interface ChatMessage {
  id: string;
  conversation_id: string;
  role: MessageSenderRole;
  content: string;
  created_at: string;
  is_streaming?: boolean;
}

export interface Conversation {
  id: string;
  user_id: string;
  title: string;
  course_id?: string;
  course_title?: string;
  updated_at: string;
  created_at: string;
  last_message?: string;
}

export interface SendMessageRequest {
  conversation_id?: string;
  message: string;
  course_id?: string;
  context?: string;
}

export interface SendMessageResponse {
  conversation_id: string;
  message: ChatMessage;
}
