import { useState, useEffect } from "react";
import { Maximize, Minimize, RotateCcw } from "lucide-react"; // Added RotateCcw for reset icon
import ChatInput from "@/components/ChatInput";
import ChatMessage from "@/components/ChatMessage";
import ChatButton from "@/components/ChatButton";
import { getResponse } from "@/lib/api";
import { Message } from "@/models/message";

function formatChatGPTDateTime(): string {
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

const Chatbot: React.FC = () => {
  // Start with an empty messages array
  const [messages, setMessages] = useState<Message[]>([]);
  const [newMessage, setNewMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const [isFullScreen, setIsFullScreen] = useState(false);
  const [hasInitialInteraction, setHasInitialInteraction] = useState(false);
  const [conversationId, setConversationId] = useState<string>("");

  // Generate a new conversation ID when the component mounts
  useEffect(() => {
    setConversationId(`conv-${Date.now()}`);
  }, []);

  const toggleFullScreen = () => {
    setIsFullScreen(!isFullScreen);
  };

  // Close chat and optionally clear conversation
  const closeChat = () => {
    setIsOpen(false);
  };

  // Reset the conversation completely
  const resetConversation = async () => {
    setMessages([]);
    setHasInitialInteraction(false);
    setConversationId(`conv-${Date.now()}`);
    
    if (isOpen) {
      setIsLoading(true);
      await sendInitialMessage();
      setIsLoading(false);
    }
  };

  // Toggle chat and trigger initial message
  const toggleChat = async () => {
    const newOpenState = !isOpen;
    setIsOpen(newOpenState);
    
    // If opening the chat and we haven't had an initial interaction yet, send one
    if (newOpenState && !hasInitialInteraction) {
      await sendInitialMessage();
      setHasInitialInteraction(true);
    }
  };

  // Function for sending the initial message
  const sendInitialMessage = async () => {
    setIsLoading(true);
    
    // Create a system message that won't be displayed to the user
    const initialMessage: Message = {
      id: Date.now().toString(),
      content: "start conversation with opening message",
      role: "user",
      loading: false,
      timestamp: formatChatGPTDateTime(),
      error: "",
      hidden: true, // Hide from UI
      conversationId: conversationId // Add conversation ID
    };

    // Create a placeholder for the AI response that will be shown
    const aiMessage: Message = {
      id: (Date.now() + 1).toString(),
      content: "",
      role: "ai",
      loading: true,
      timestamp: formatChatGPTDateTime(),
      error: "",
      conversationId: conversationId // Add conversation ID
    };

    // Add the AI message to the visible messages and the hidden message to the state
    setMessages([initialMessage, aiMessage]);

    try {
      // Send the hidden system message to get the welcome response
      let responseData = await getResponse([initialMessage]);

      // Extract content from the response (clean up <think> tags)
      const cleanResponse = responseData
        .replace(/<think>.*?<\/think>/gs, "")
        .replace(/\\u003c.*?\\u003e/g, "")  // Remove any unicode escapes like \u003c
        .trim();

      // Update state with cleaned AI response
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
  };

  const sendMessage = async () => {
    if (!newMessage.trim()) return;
    setIsLoading(true);

    const userMessage: Message = {
      id: Date.now().toString(),
      content: newMessage,
      role: "user",
      loading: false,
      timestamp: formatChatGPTDateTime(),
      error: "",
      conversationId: conversationId // Add conversation ID
    };

    const aiMessage: Message = {
      id: (Date.now() + 1).toString(),
      content: "",
      role: "ai",
      loading: true,
      timestamp: formatChatGPTDateTime(),
      error: "",
      conversationId: conversationId // Add conversation ID
    };

    setMessages((prevMessages) => [...prevMessages, userMessage, aiMessage]);
    setNewMessage("");

    try {
      // Get the current conversation messages
      const currentConversationMessages = messages.filter(
        msg => msg.conversationId === conversationId
      );
      
      let responseData = await getResponse([...currentConversationMessages, userMessage]);

      // Extract content from the response (clean up <think> tags)
      const cleanResponse = responseData
        .replace(/<think>.*?<\/think>/gs, "")
        .replace(/\\u003c.*?\\u003e/g, "")  // Remove any unicode escapes like \u003c
        .trim();

      // Update state with cleaned AI response
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
  };

  return (
    <>
      {/* Chat Toggle Button */}
      <ChatButton isOpen={isOpen} onToggle={toggleChat} />

      {/* Chatbot Window */}
      <div
        className={`fixed ${isFullScreen ? "inset-0 w-full h-full" : "bottom-20 right-4 w-80 sm:w-96 h-[600px]"} 
        bg-white shadow-lg border border-gray-300 flex flex-col transition-all duration-300 ${
          isOpen ? "translate-y-0 opacity-100" : "translate-y-4 opacity-0 pointer-events-none"
        }`}
      >
        {/* Chat Header */}
        <div className="p-4 bg-[#006354] text-white font-semibold text-lg flex justify-between items-center">
          <span>Slainte Chat Assistant</span>
          <div className="flex space-x-2">
            {/* Reset Button */}
            <button 
              onClick={resetConversation} 
              className="text-white hover:text-gray-300" 
              title="Reset conversation">
              <RotateCcw size={16} />
            </button>
            
            {/* Fullscreen Toggle Button */}
            <button 
              onClick={toggleFullScreen} 
              className="text-white hover:text-gray-300"
              title={isFullScreen ? "Exit fullscreen" : "Fullscreen"}>
              {isFullScreen ? <Minimize size={16} /> : <Maximize size={16} />}
            </button>

            {/* Close Button */}
            <button 
              onClick={closeChat} 
              className="text-white hover:text-gray-300"
              title="Close chat">
              ✖
            </button>
          </div>
        </div>

        {/* Chat Messages */}
        <div className="flex-1 overflow-auto p-2">
          <ChatMessage 
            messages={messages.filter(msg => !msg.hidden && msg.role !== "system")} 
            isLoading={isLoading} 
          />
        </div>

        {/* Chat Input */}
        <div className="border-t border-gray-300 p-0">
          <ChatInput
            isLoading={isLoading}
            sendMessage={sendMessage}
            newMessage={newMessage}
            setNewMessage={setNewMessage}
          />
        </div>
      </div>
    </>
  );
};

export default Chatbot;