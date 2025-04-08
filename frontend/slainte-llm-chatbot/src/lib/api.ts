import axios from "axios";
import { Message } from "@/models/message";
import { SYSTEM_PROMPTS, DEFAULT_SYSTEM_PROMPT } from "@/lib/prompt-templates"; // Import prompts from centralized location

const apiUrl = import.meta.env.VITE_API_URL || "http://localhost:8080/api/llm";

// Handle special start conversation message and determine the appropriate flow
const handleStartConversation = (messages: Message[]): Message[] => {
  // Find system message if present
  const systemMessage = messages.find(msg => msg.role === "system");

  let systemPromptContent = DEFAULT_SYSTEM_PROMPT;

  // If a system message exists, use its content or determine which prompt to use
  if (systemMessage) {
    if (systemMessage.content.includes("general_information")) {
      systemPromptContent = SYSTEM_PROMPTS.general_information;
    } else if (systemMessage.content.includes("symptom_checking")) {
      systemPromptContent = SYSTEM_PROMPTS.symptom_checking;
    } else {
      // If the system message already has full prompt content, use it directly
      systemPromptContent = systemMessage.content;
    }
  }

  // Special case for conversation start
  if (messages.length === 1 &&
    messages[0].content === "start conversation with opening message" &&
    messages[0].hidden) {
    return [{
      id: "system-prompt",
      content: systemPromptContent,
      role: "system",
      timestamp: new Date().toISOString(),
    }];
  }

  // Get all non-system messages
  const userMessages = messages.filter(msg => msg.role !== "system");

  // For regular conversation, maintain more context but still limit to prevent overflow
  // Use the last 15 messages to preserve more context for symptom checking
  const recentMessages = userMessages.slice(-15);

  // Create a new system message
  const newSystemMessage: Message = {
    id: "system-prompt",
    content: systemPromptContent,
    role: "system",
    timestamp: new Date().toISOString(),
  };

  // Return the complete message array
  return [newSystemMessage, ...recentMessages];
};

export const getResponse = async (messages: Message[], useRag: boolean = false): Promise<string> => {
  try {
    // Log the messages being sent to the API (for debugging)
    console.log("Messages before processing:", messages);

    // Prepare messages with the system prompt and limited context
    const conversationWithPrompt = handleStartConversation(messages);

    // Log the processed messages (for debugging)
    console.log("Messages after processing:", conversationWithPrompt);

    // Check for final symptom assessment
    const isSymptomAssessment = conversationWithPrompt.some(msg => 
      msg.content.includes("Please analyze the following patient information")
    );
    
    // Force RAG on for symptom assessments
    const shouldUseRag = useRag || isSymptomAssessment;

    // Send request to backend API with the useRag flag
    const response = await axios.post(`${apiUrl}/chat`, {
      messages: conversationWithPrompt,
      temperature: 0.0, // Lower temperature for more deterministic responses
      useRag: shouldUseRag // Pass the flag to the backend
    });

    console.log("Full API response:", response);

    return response.data;
  } catch (error) {
    console.error("API error:", error);
    throw error;
  }
};