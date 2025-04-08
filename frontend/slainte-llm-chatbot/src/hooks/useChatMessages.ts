import { useState, useCallback } from 'react';
import { Message } from '@/models/message';
import { getResponse } from '@/lib/api';
import { SYSTEM_PROMPTS } from "@/lib/prompt-templates";

// Helper function to format date/time for chat messages
export function formatChatDateTime(): string {
  const now = new Date();
  return new Intl.DateTimeFormat("en-US", {
    weekday: "short",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: true,
  }).format(now);
}

export function useChatMessages(conversationId: string, currentFlow: string | null, addApiLog: (log: any) => void) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [newMessage, setNewMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  // Function to send a message
  const sendMessage = useCallback(async () => {
    if (!newMessage.trim()) return;
    setIsLoading(true);

    // Create the user message
    const userMessage: Message = {
      id: Date.now().toString(),
      content: newMessage,
      role: "user",
      loading: false,
      timestamp: formatChatDateTime(),
      error: "",
      conversationId: conversationId
    };

    // Add user message to the conversation
    setMessages(prevMessages => [...prevMessages, userMessage]);
    setNewMessage("");

    // Create placeholder for AI response
    const aiMessage: Message = {
      id: (Date.now() + 1).toString(),
      content: "",
      role: "ai",
      loading: true,
      timestamp: formatChatDateTime(),
      error: "",
      conversationId: conversationId
    };

    // Add AI message placeholder
    setMessages(prevMessages => [...prevMessages, aiMessage]);

    try {
      // Get ALL messages from the current conversation, including system message
      const allConversationMessages = messages.filter(
        msg => msg.conversationId === conversationId
      );

      // Find system message - this contains the full prompt content
      const existingSystemMessage = allConversationMessages.find(
        msg => msg.role === "system" && msg.hidden
      );

      // Get visible messages for context (non-hidden)
      const visibleMessages = allConversationMessages.filter(
        msg => !msg.hidden && msg.role !== "system"
      );

      // Get the system message based on the current flow
      const systemPrompt = currentFlow 
        ? SYSTEM_PROMPTS[currentFlow as keyof typeof SYSTEM_PROMPTS] 
        : SYSTEM_PROMPTS.general_information;

      const systemMessage: Message = existingSystemMessage || {
        id: `system-${Date.now()}`,
        content: systemPrompt,
        role: "system",
        loading: false,
        timestamp: formatChatDateTime(),
        error: "",
        hidden: true,
        conversationId: conversationId
      };

      // Use RAG for general information flow AND for final symptom assessments
      const useRag = currentFlow === "general_information" || 
                     (currentFlow === "symptom_checking" && userMessage.content.includes("final assessment"));

      // Create the messages array to send to the API
      const apiMessages: Message[] = [
        systemMessage,
        ...visibleMessages,
        userMessage
      ];

      // Prepare request payload for logging
      const requestPayload = {
        messages: apiMessages,
        useRag: useRag
      };

      // Debug logging
      console.log("Sending messages to API:", apiMessages);

      // Make the API call
      let responseData = await getResponse(apiMessages, useRag);

      // Add log entry
      addApiLog({
        request: requestPayload,
        response: responseData
      });

      // Clean the response
      const cleanResponse = responseData
        .replace(/<think>.*?<\/think>/gs, "")
        .replace(/\\u003c.*?\\u003e/g, "")
        .trim();

      // Update the AI response
      setMessages((prevMessages) =>
        prevMessages.map((msg) =>
          msg.id === aiMessage.id
            ? { ...msg, content: cleanResponse, loading: false }
            : msg
        )
      );
    } catch (error: unknown) {
      console.error("Error:", error);

      let errorMessage = "An unknown error occurred.";
      if (error instanceof Error) {
        errorMessage = error.message;
      }

      // Log error
      const systemPrompt = currentFlow 
        ? SYSTEM_PROMPTS[currentFlow as keyof typeof SYSTEM_PROMPTS] 
        : SYSTEM_PROMPTS.general_information;
      
      addApiLog({
        request: {
          messages: [
            {
              content: systemPrompt,
              role: "system"
            },
            {
              content: newMessage,
              role: "user"
            }
          ],
          useRag: currentFlow === "general_information"
        },
        error: errorMessage
      });

      setMessages((prevMessages) =>
        prevMessages.map((msg) =>
          msg.id === aiMessage.id
            ? { ...msg, content: "Error retrieving response.", loading: false, error: errorMessage }
            : msg
        )
      );
    } finally {
      setIsLoading(false);
    }
  }, [newMessage, messages, conversationId, currentFlow, addApiLog]);

  // Function to start a conversation with a specific prompt
  const startConversation = useCallback(async (promptText: string, specificPromptText: string, aiResponse: string = "") => {
    setIsLoading(true);

    const PromptMessage: Message = {
        id: Date.now().toString(),
        content: promptText,
        role: "system",
        loading: false,
        timestamp: formatChatDateTime(),
        error: "",
        hidden: true, // Hide from UI
        conversationId: conversationId
        };

    // Create a system message that won't be displayed to the user
    const initialMessage: Message = {
      id: Date.now().toString(),
      content: specificPromptText,
      role: "user",
      loading: false,
      timestamp: formatChatDateTime(),
      error: "",
      hidden: true, // Hide from UI
      conversationId: conversationId
    };

    // Create a placeholder for the AI response that will be shown
    const aiMessage: Message = {
      id: (Date.now() + 1).toString(),
      content: "",
      role: "ai",
      loading: true,
      timestamp: formatChatDateTime(),
      error: "",
      conversationId: conversationId
    };

    // Append the AI message to the existing messages rather than replacing them
    setMessages(prevMessages => [...prevMessages, aiMessage]);

    try {
      // If we have a predefined response, use it
      if (aiResponse) {
        setTimeout(() => {
          setMessages((prevMessages) =>
            prevMessages.map((msg) =>
              msg.id === aiMessage.id
                ? { ...msg, content: aiResponse, loading: false }
                : msg
            )
          );
          setIsLoading(false);
        }, 500); // Simulate a short delay
      } else {
        // Enable RAG for final symptom assessment
        const useRag = currentFlow === "general_information" || 
                      (currentFlow === "symptom_checking" && 
                       specificPromptText.includes("Please analyze the following patient information"));

        // Prepare request payload
        const requestPayload = {
          messages: [PromptMessage, initialMessage],
          useRag: useRag
        };

        // Make the actual API call
        let responseData = await getResponse([PromptMessage, initialMessage], useRag);

        // Log request and response
        addApiLog({
          request: requestPayload,
          response: responseData
        });

        // Extract content from the response
        const cleanResponse = responseData
          .replace(/<think>.*?<\/think>/gs, "")
          .replace(/\\u003c.*?\\u003e/g, "")
          .trim();

        // Update state with cleaned AI response
        setMessages((prevMessages) =>
          prevMessages.map((msg) =>
            msg.id === aiMessage.id
              ? { ...msg, content: cleanResponse, loading: false }
              : msg
          )
        );
      }
    } catch (error: unknown) {
      console.error("Error:", error);

      let errorMessage = "An unknown error occurred.";
      if (error instanceof Error) {
        errorMessage = error.message;
      }

      // Log error
      addApiLog({
        request: {
          messages: [initialMessage],
          useRag: currentFlow === "general_information" || 
                 (currentFlow === "symptom_checking" && 
                  specificPromptText.includes("Please analyze the following patient information"))
        },
        error: errorMessage
      });

      setMessages((prevMessages) =>
        prevMessages.map((msg) =>
          msg.id === aiMessage.id
            ? { ...msg, content: "Error retrieving response.", loading: false, error: errorMessage }
            : msg
        )
      );
    } finally {
      setIsLoading(false);
    }
  }, [conversationId, currentFlow, addApiLog]);

  // Function to add a message directly to the conversation
  const addMessage = useCallback((messageContent: string, role: "user" | "ai" | "system", hidden: boolean = false) => {
    const newMsg: Message = {
      id: Date.now().toString(),
      content: messageContent,
      role: role,
      loading: false,
      timestamp: formatChatDateTime(),
      error: "",
      hidden: hidden,
      conversationId: conversationId
    };
    
    setMessages(prevMessages => [...prevMessages, newMsg]);
    return newMsg;
  }, [conversationId]);

  // Reset messages
  const resetMessages = useCallback(() => {
    setMessages([]);
    setNewMessage("");
  }, []);

  return {
    messages,
    setMessages,
    newMessage,
    setNewMessage,
    isLoading,
    sendMessage,
    startConversation,
    addMessage,
    resetMessages
  };
}