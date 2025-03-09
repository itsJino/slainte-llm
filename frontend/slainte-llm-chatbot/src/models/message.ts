export interface Message {
  id: string;
  content: string;
  role: "user" | "ai" | "system";
  loading?: boolean;
  timestamp: string;
  error?: string;
  hidden?: boolean;
  conversationId?: string; // Added to track which conversation a message belongs to
}