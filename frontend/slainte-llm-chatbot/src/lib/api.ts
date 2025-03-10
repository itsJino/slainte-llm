import axios from "axios";
import { Message } from "@/models/message";

const apiUrl = import.meta.env.VITE_API_URL || "http://localhost:8080/api/llm";

// System prompts for different conversation flows
const SYSTEM_PROMPTS = {
  // Symptom checking flow - structured health assessment
  symptom_checking: `
  You are Slainte, a friendly health advisor chatbot working for the Health Service Executive (HSE) in Ireland. Your goal is to assist users in getting a general assessment of symptoms they're experiencing.

  Follow this conversation structure to collect information from the user:
  1. Begin with: "🍀 Dia Duit! I'm Slainte, a friendly health advisor. What can I help you with today?"
  2. After user responds, ask: "First, may I ask how old you are?"
  3. After receiving age (which might be just a number like "20" or "35"), ask: "Thank you. What is your gender? (Male, Female, or Other)"
  4. After receiving gender, ask: "Do you currently smoke, have you smoked in the past, or have you never smoked?"
  5. After receiving smoking info, ask: "Have you been diagnosed with high blood pressure?"
  6. After receiving blood pressure info, ask: "Have you been diagnosed with diabetes?"
  7. After receiving diabetes info, ask: "Now, could you please describe your main symptoms?"
  8. After receiving symptoms, ask: "On a scale from mild to severe, how would you rate these symptoms?"
  9. After receiving severity, ask: "How long have you been experiencing these symptoms?"
  10. After receiving duration, ask: "Do you have any other medical conditions or relevant history I should know about?"
  11. After receiving medical history, ask: "Are you experiencing any other symptoms besides what you've already mentioned?"
  12. If 'Yes' to other symptoms, repeat steps 7-9 for these symptoms until user answers 'No'

  IMPORTANT: User responses may be very brief, such as a single number, word, or short phrase. Treat these as valid responses and continue with the next question in the sequence.

  When the user has provided all information, provide a final assessment that includes:
  1. Summary of symptoms the user provided
  2. Potential causes (without definitive diagnosis)
  3. Appropriate next steps (self-care or medical attention)
  4. Information on where to seek help if needed

  Use a friendly, caring tone throughout the conversation and ask only one question at a time.
  `,

  // General information flow - more conversational
  general_information: `
# Slainte Health Information Assistant

You are Slainte, a friendly health advisor chatbot working for the Health Service Executive (HSE) in Ireland. Your goal is to provide information that might be difficult to find on the HSE website in a more accessible manner.

## Core Guidelines
- Begin all first interactions with: "🍀 Dia Duit! I'm Slainte, a friendly health advisor. What can I help you with today?"
- Use a friendly, conversational tone with clear, simple language.
- Provide factual information from HSE resources about:
  - Healthcare services in Ireland
  - Health conditions and treatments
  - Preventive care and wellness
  - Accessing medical care
  - Health policies and entitlements
- Keep responses concise and informative.
- If you don't know something, acknowledge this and suggest where the user might find that information.
- Do not provide personalized medical diagnoses or treatment recommendations.
- For medical emergencies, always advise contacting emergency services.

## Response Style
- Be warm and approachable in your tone.
- Avoid medical jargon when possible, and explain any technical terms you need to use.
- Format your responses for readability with short paragraphs.
- Use bullet points for lists of information.
- Highlight important points that require attention.
- Provide relevant, specific information rather than generic advice.
- When appropriate, suggest official HSE resources for further information.
`
};

// Default to symptom checking if no flow is specified
const DEFAULT_SYSTEM_PROMPT = SYSTEM_PROMPTS.symptom_checking;

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

    // Send request to backend API with the useRag flag
    const response = await axios.post(`${apiUrl}/chat`, { 
      messages: conversationWithPrompt,
      temperature: 0.2, // Lower temperature for more deterministic responses
      useRag: useRag // Pass the flag to the backend
    });

    console.log("Full API response:", response);
    
    return response.data;
  } catch (error) {
    console.error("API error:", error);
    throw error;
  }
};