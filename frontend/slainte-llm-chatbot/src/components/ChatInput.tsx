import useAutoSize from "@/hooks/useAutoSize";
import { ArrowUpIcon } from "./icons";
import React, { useState } from "react";
import axios from "axios"; // To make HTTP requests

interface ChatInputProps {
  isLoading: boolean;
  newMessage: string;
  setNewMessage: (message: string) => void;
  sendMessage: () => void;
}

const ChatInput: React.FC<ChatInputProps> = ({
  isLoading,
  newMessage,
  setNewMessage,
  sendMessage,
}) => {
  const textareaRef = useAutoSize<HTMLTextAreaElement>(newMessage);

  async function handleSendMessage() {
    if (isLoading || !newMessage.trim()) return;

    // Start loading state
    setNewMessage(""); // Clear input

    try {
      // Send the message to the backend
      const response = await axios.get(`/api/llm/${newMessage}`);
      console.log(response.data); // Handle the response
    } catch (error) {
      console.error("Error sending message:", error);
    }
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    console.log("Key pressed:", e.key); // Debugging keypress
  
    if (e.key === "Enter" && !e.shiftKey && !isLoading) {
      e.preventDefault();
      console.log("Calling sendMessage...");
      sendMessage();
    }
  }

  return (
    <div className="sticky bottom-0 py-4">
      <div className="p-1.5 bg-primary-blue/35 rounded-3xl z-50 font-mono origin-bottom animate-chat duration-400">
        <div className="relative shrink-0 rounded-3xl overflow-hidden ring-[#006354] ring-1 
            focus-within:ring-2 transition-all">
          <textarea
            placeholder="Ask anything"
            className="block w-full max-h-[140px] py-2 px-4 pr-11 rounded-3xl resize-none 
            placeholder:text-[#006354] placeholder:leading-4 placeholder:-translate-y-1 
            sm:placeholder:leading-normal sm:placeholder:translate-y-0 focus:outline-none focus:ring-0"
            ref={textareaRef}
            rows={1}
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            onKeyDown={handleKeyDown}
          />
          <button
            className="absolute top-1/2 -translate-y-1/2 right-3 p-1 rounded-md bg-white text-black"
            onClick={handleSendMessage}
          >
            <ArrowUpIcon />
          </button>
        </div>
      </div>
    </div>
  );
};

export default ChatInput;
