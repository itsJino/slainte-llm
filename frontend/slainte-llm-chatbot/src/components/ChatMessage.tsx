import Markdown from "react-markdown";
import useAutoScroll from "@/hooks/useAutoScroll";
import Spinner from "@/components/Spinner";
import errorIcon from "@/assets/images/error.svg";
import HSELogo from "@/assets/images/hse_logo_green.png";
import { Message } from "@/models/message";
import { useEffect, useState, useRef } from "react";
import "@/styles/message-animations.css"; // Import the external CSS file

interface ChatMessagesProps {
  messages: Message[];
  isLoading?: boolean;
  isFullScreen?: boolean;
}

const ChatMessages: React.FC<ChatMessagesProps> = ({ messages, isLoading, isFullScreen }) => {
  const scrollContentRef = useAutoScroll();
  // Track which messages have been animated
  const [animatedMessages, setAnimatedMessages] = useState<Record<string, boolean>>({});
  // Typing animation for AI messages
  const [typingMessages, setTypingMessages] = useState<Record<string, string>>({});
  // Track previously rendered messages count
  const prevMessagesLengthRef = useRef(0);

  // Update animated messages when new ones arrive
  useEffect(() => {
    const newMessages = messages.slice(prevMessagesLengthRef.current);
    
    // Add a small staggered delay to each new message
    newMessages.forEach((message, index) => {
      const messageIdx = index + prevMessagesLengthRef.current;
      
      setTimeout(() => {
        setAnimatedMessages(prev => ({
          ...prev,
          [messageIdx]: true
        }));
        
        // For AI messages, start typing animation
        if (message.role === "ai" && !message.loading) {
          simulateTyping(messageIdx, message.content as string);
        }
      }, 200 * (index + 1)); // Staggered delay
    });
    
    // Update ref for next comparison
    prevMessagesLengthRef.current = messages.length;
  }, [messages.length]);

  // Simulate typing animation for AI messages
  const simulateTyping = (messageIdx: number, content: string) => {
    // Skip typing for very long messages
    if (content.length > 300) {
      setTypingMessages(prev => ({ ...prev, [messageIdx]: content }));
      return;
    }
    
    let currentIndex = 0;
    const typingSpeed = 15; // ms per character
    
    const typingInterval = setInterval(() => {
      if (currentIndex <= content.length) {
        setTypingMessages(prev => ({
          ...prev, 
          [messageIdx]: content.substring(0, currentIndex)
        }));
        currentIndex++;
      } else {
        clearInterval(typingInterval);
      }
    }, typingSpeed);
    
    // Cleanup function
    return () => clearInterval(typingInterval);
  };

  // Enhanced custom components for better readability and accessibility
  const components = {
    // Improved paragraph spacing and line height
    p: (props: any) => (
      <p className="text-lg leading-7 mb-4 text-white" {...props} />
    ),
    
    // More prominent headings with better spacing
    h1: (props: any) => (
      <h1 className="text-xl font-bold mt-4 mb-3 border-b border-[#73E6C2] pb-1 text-[#73E6C2]" {...props} />
    ),
    h2: (props: any) => (
      <h2 className="text-lg font-bold mt-4 mb-2 text-[#73E6C2]" {...props} />
    ),
    h3: (props: any) => (
      <h3 className="text-lg font-semibold mt-3 mb-2 text-[#73E6C2]" {...props} />
    ),
    
    // Improved list formatting with better spacing and more distinct bullets
    ul: (props: any) => (
      <ul className="text-lg pl-6 mb-4 space-y-2 list-disc marker:text-[#73E6C2]" {...props} />
    ),
    ol: (props: any) => (
      <ol className="text-lg pl-6 mb-4 space-y-2 list-decimal marker:text-[#73E6C2]" {...props} />
    ),
    li: (props: any) => (
      <li className="mb-2 pl-1 text-white" {...props} />
    ),
    
    // More visible links
    a: (props: any) => (
      <a className="underline text-[#73E6C2] font-medium hover:brightness-110 transition-all" {...props} />
    ),
    
    // Better code formatting
    code: (props: any) => (
      <code className="text-sm bg-[#004e41] px-1.5 py-0.5 rounded font-mono text-white" {...props} />
    ),
    pre: (props: any) => (
      <pre className="text-sm bg-[#004e41] p-3 rounded-md my-3 overflow-x-auto font-mono border border-[#73E6C2] border-opacity-30 text-white" {...props.children} />
    ),
    
    // Strong/bold text with emphasis
    strong: (props: any) => (
      <strong className="font-bold text-[#73E6C2]" {...props} />
    ),
    
    // Better blockquote styling
    blockquote: (props: any) => (
      <blockquote className="border-l-4 border-[#73E6C2] pl-4 italic my-4 text-[#e0e0e0]" {...props} />
    ),
    
    // Enhanced table styling
    table: (props: any) => (
      <div className="overflow-x-auto my-4">
        <table className="min-w-full border border-[#73E6C2] border-opacity-30 text-white" {...props} />
      </div>
    ),
    thead: (props: any) => (
      <thead className="bg-[#004e41]" {...props} />
    ),
    th: (props: any) => (
      <th className="px-3 py-2 text-left font-medium border-b border-[#73E6C2] border-opacity-30 text-[#73E6C2]" {...props} />
    ),
    td: (props: any) => (
      <td className="px-3 py-2 border-b border-[#73E6C2] border-opacity-10" {...props} />
    ),
    
    // Improved horizontal rule
    hr: () => (
      <hr className="my-6 border-t-2 border-[#73E6C2] border-opacity-30" />
    ),
  };

  return (
    <div ref={scrollContentRef} className="space-y-6 max-h-[400px]">
      {messages.map(({ role, content, loading, error, timestamp }, idx) => {
        const isAnimated = animatedMessages[idx] || false;
        const displayContent = role === "ai" && typingMessages[idx] !== undefined 
          ? typingMessages[idx] 
          : content;
        
        // Determine if we should show the typing cursor
        const showTypingCursor = role === "ai" && 
          typingMessages[idx] !== undefined && 
          typingMessages[idx] !== content;
        
        return (
          <div
            key={idx}
            className={`flex ${role === "user" ? "justify-end" : "justify-start"} w-full message-enter ${
              isAnimated ? "message-enter-active" : ""
            }`}
          >
            {/* Assistant Message */}
            {role === "ai" && (
              <div className={`flex items-start space-x-3 ${isFullScreen ? "max-w-[60%]" : "max-w-[95%]"}`}>
                <img 
                  className={`h-[30px] w-[30px] shrink-0 rounded-full mt-1 ${isAnimated ? "avatar-animation" : "opacity-0"}`}
                  src={HSELogo} 
                  alt="HSE logo"
                />
                <div 
                  className={`flex flex-col bg-[#006354] text-white p-4 rounded-lg shadow-lg ${
                    isAnimated ? "message-animation-ai" : "opacity-0"
                  }`}
                > 
                  {loading ? (
                    <Spinner />
                  ) : (
                    <div className={`markdown-content ${showTypingCursor ? 'cursor-blink' : ''}`}>
                      <Markdown components={components}>{displayContent as string}</Markdown>
                    </div>
                  )}
                  <span className={`text-xs text-gray-300 mt-3 opacity-80 ${isAnimated ? "timestamp-animation-ai" : ""}`}>
                    {timestamp}
                  </span>
                </div>
              </div>
            )}
            
            {/* User Message */}
            {role === "user" && (
              <div className="flex items-end space-x-2 max-w-[80%]">
                <div 
                  className={`flex flex-col bg-[#f3f3f3] text-black p-4 rounded-lg shadow-md ${
                    isAnimated ? "message-animation-user" : "opacity-0"
                  }`}
                > 
                  <div className="text-lg leading-relaxed whitespace-pre-line">{content}</div>
                  <span className={`text-xs text-gray-500 mt-2 text-right ${isAnimated ? "timestamp-animation" : ""}`}>
                    {timestamp}
                  </span>
                </div>
              </div>
            )}
            
            {/* Error Message */}
            {error && (
              <div 
                className={`flex items-center gap-2 text-sm text-red-500 mt-2 bg-red-50 px-3 py-2 rounded-md ${
                  isAnimated ? "message-animation-error" : "opacity-0"
                }`}
              >
                <img className="h-5 w-5" src={errorIcon} alt="error" />
                <span>Error generating the response. Please try again.</span>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};

export default ChatMessages;