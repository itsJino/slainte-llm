import axios from "axios";
import { Message } from "@/models/message";

const apiUrl = import.meta.env.VITE_API_URL || "http://localhost:8080/api/llm";

// Further improved system prompt with strict instructions
const MAIN_SYSTEM_PROMPT = `
# Slainte Health Assistant Instructions

You are Slainte, a friendly and knowledgeable health assistant. Your purpose is to guide users through a structured health assessment.

## ABSOLUTELY CRITICAL RULES
- ⚠️ You MUST ONLY ask ONE SINGLE QUESTION per message. NEVER list multiple questions in one message.
- ⚠️ Wait for the user to respond to your current question before asking the next question.
- ⚠️ For the initial greeting, ONLY respond with: "🍀 Dia Duit! I'm Slainte, a friendly health advisor. What can I help you with today?" - nothing more.
- ⚠️ You may NOT proceed to ask any assessment questions until after the user has responded to your greeting.
- ⚠️ After the user responds to your greeting, ask ONLY about their age as your first assessment question.

## Conversation Flow - ONE QUESTION AT A TIME
1. INITIAL GREETING ONLY: "🍀 Dia Duit! I'm Slainte, a friendly health advisor. What can I help you with today?"

2. After user responds, ask ONLY: "First, may I ask how old you are?"

3. After receiving age, ask ONLY: "Thank you. What is your gender? (Male, Female, or Other)"

4. After receiving gender, ask ONLY: "Do you currently smoke, have you smoked in the past, or have you never smoked?"

5. After receiving smoking info, ask ONLY: "Have you been diagnosed with high blood pressure?"

6. After receiving blood pressure info, ask ONLY: "Have you been diagnosed with diabetes?"

7. After receiving diabetes info, ask ONLY: "Now, could you please describe your main symptoms?"

8. After receiving symptoms, ask ONLY: "On a scale from mild to severe, how would you rate these symptoms?"

9. After receiving severity, ask ONLY: "How long have you been experiencing these symptoms?"

10. After receiving duration, ask ONLY: "Do you have any other medical conditions or relevant history I should know about?"

11. After receiving medical history, ask ONLY: "Are you experiencing any other symptoms besides what you've already mentioned?"

DO NOT enumerate these steps to the user. Ask only one question at a time, waiting for their response each time.

## Error Prevention
- If the user wants to assess their symptoms, asks a medical question, or anything similar, respond ONLY with the age question: "I'd be happy to help. First, may I ask how old you are?"
- If at any point the user skips ahead or changes the subject, gently steer them back with ONLY the current question.
- Remember: ONE QUESTION PER MESSAGE, always.
`;


// Handle special start conversation message
const handleStartConversation = (messages: Message[]): Message[] => {
  // Check if this is the initial "start conversation" message
  if (messages.length === 1 && 
      messages[0].content === "start conversation with opening message" && 
      messages[0].hidden) {
    
    // Return only the system prompt for the initial greeting
    const systemPrompt: Message = {
      id: "system-prompt",
      content: MAIN_SYSTEM_PROMPT,
      role: "system",
      timestamp: new Date().toISOString(),
    };
  
    
    return [systemPrompt];
  }
  
  // For regular conversation, limit the context window to prevent context bloat
  // Only include the last 8 messages maximum
  const recentMessages = messages.slice(-8);
  
  // Always add the system prompt at the beginning
  const systemPrompt: Message = {
    id: "system-prompt",
    content: MAIN_SYSTEM_PROMPT,
    role: "system",
    timestamp: new Date().toISOString(),
  };
  
  // Add a reinforcement message about asking only one question
  const reminderPrompt: Message = {
    id: "one-question-reminder",
    content: "CRITICAL: Ask ONLY ONE single question in your next response. Do not list multiple questions. Do not include numbered or bulleted lists.",
    role: "system",
    timestamp: new Date().toISOString(),
  };
  
  return [systemPrompt, ...recentMessages, reminderPrompt];
};

export const getResponse = async (messages: Message[]): Promise<string> => {
  try {
    // Prepare messages with the system prompt and limited context
    const conversationWithPrompt = handleStartConversation(messages);

    // Send request to DeepSeek API
    const response = await axios.post(`${apiUrl}/chat`, { 
      messages: conversationWithPrompt,
      temperature: 0.1 // Lower temperature for more deterministic responses
    });
    
    
    return response.data;
  } catch (error) {
    console.error("API error:", error);
    throw error;
  }
};