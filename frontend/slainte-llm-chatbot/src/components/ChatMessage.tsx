import Markdown from "react-markdown";
import useAutoScroll from "@/hooks/useAutoScroll";
import Spinner from "@/components/Spinner";
import errorIcon from "@/assets/images/error.svg";
import HSELogo from "@/assets/images/hse_logo_green.png";
import { Message } from "@/models/message";
import { useEffect, useState, useRef } from "react";
import "@/styles/message-animations.css";

interface ChatMessagesProps {
  messages: Message[];
  isLoading?: boolean;
  isFullScreen?: boolean;
}

const ChatMessages: React.FC<ChatMessagesProps> = ({ messages, isLoading, isFullScreen }) => {
  const scrollContentRef = useAutoScroll();
  const [animatedMessages, setAnimatedMessages] = useState<Record<string, boolean>>({});
  const [typingMessages, setTypingMessages] = useState<Record<string, string>>({});
  const prevMessagesLengthRef = useRef(0);
  const [inFinalAssessment, setInFinalAssessment] = useState(false);

  // Check if we're in final assessment mode based on message content
  useEffect(() => {
    const lastUserMessage = [...messages].reverse().find(msg => msg.role === "user");
    if (lastUserMessage && lastUserMessage.content.toLowerCase() === "no") {
      setInFinalAssessment(true);
    } else {
      setInFinalAssessment(false);
    }
  }, [messages]);

  // Add animation only to new messages
  useEffect(() => {
    const newMessages = messages.slice(prevMessagesLengthRef.current);
    
    if (newMessages.length > 0) {
      newMessages.forEach((message, index) => {
        const messageIdx = index + prevMessagesLengthRef.current;
        
        setTimeout(() => {
          setAnimatedMessages(prev => ({
            ...prev,
            [messageIdx]: true
          }));
          
          if (message.role === "ai" && !message.loading) {
            setTypingMessages(prev => ({
              ...prev,
              [messageIdx]: message.content as string
            }));
          }
        }, 150 * index);
      });
    }
    
    prevMessagesLengthRef.current = messages.length;
  }, [messages.length]);

  // Critical change: Filter out ALL loading messages when a response has been received
  // This ensures we don't see both loading and response messages simultaneously
  const filteredMessages = messages.filter((message, index, array) => {
    // If we find any non-loading AI message after the current message, 
    // and the current message is a loading message, filter it out
    if (message.loading && message.role === "ai") {
      const laterResponses = array.slice(index).find(m => 
        m.role === "ai" && !m.loading && m.content
      );
      
      if (laterResponses) {
        return false; // Filter out this loading message as we already have a response
      }
    }
    return true;
  });

  // Enhanced custom components for markdown
  const components = {
    p: (props: any) => (
      <p className="text-lg leading-7 mb-4 text-white" {...props} />
    ),
    h1: (props: any) => (
      <h1 className="text-xl font-bold mt-4 mb-3 border-b border-[#73E6C2] pb-1 text-[#73E6C2]" {...props} />
    ),
    h2: (props: any) => (
      <h2 className="text-lg font-bold mt-4 mb-2 text-[#73E6C2]" {...props} />
    ),
    h3: (props: any) => (
      <h3 className="text-lg font-semibold mt-3 mb-2 text-[#73E6C2]" {...props} />
    ),
    ul: (props: any) => (
      <ul className="text-lg pl-6 mb-4 space-y-2 list-disc marker:text-[#73E6C2]" {...props} />
    ),
    ol: (props: any) => (
      <ol className="text-lg pl-6 mb-4 space-y-2 list-decimal marker:text-[#73E6C2]" {...props} />
    ),
    li: (props: any) => (
      <li className="mb-2 pl-1 text-white" {...props} />
    ),
    a: (props: any) => (
      <a className="underline text-[#73E6C2] font-medium hover:brightness-110 transition-all" {...props} />
    ),
    code: (props: any) => (
      <code className="text-sm bg-[#004e41] px-1.5 py-0.5 rounded font-mono text-white" {...props} />
    ),
    pre: (props: any) => (
      <pre className="text-sm bg-[#004e41] p-3 rounded-md my-3 overflow-x-auto font-mono border border-[#73E6C2] border-opacity-30 text-white" {...props.children} />
    ),
    strong: (props: any) => (
      <strong className="font-bold text-[#73E6C2]" {...props} />
    ),
    blockquote: (props: any) => (
      <blockquote className="border-l-4 border-[#73E6C2] pl-4 italic my-4 text-[#e0e0e0]" {...props} />
    ),
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
    hr: () => (
      <hr className="my-6 border-t-2 border-[#73E6C2] border-opacity-30" />
    ),
  };

  return (
    <div ref={scrollContentRef} className="space-y-3 max-h-[400px]">
      {filteredMessages.map(({ role, content, loading, error, timestamp }, idx) => {
        const isAnimated = animatedMessages[idx] || false;
        const displayContent = role === "ai" && typingMessages[idx] !== undefined 
          ? typingMessages[idx] 
          : content;
        
        // Show enhanced loading message for final assessment
        const showEnhancedLoading = loading && role === "ai" && inFinalAssessment;
        
        return (
          <div
            key={idx}
            className={`flex ${role === "user" ? "justify-end" : "justify-start"} w-full`}
          >
            {/* Assistant Message */}
            {role === "ai" && (
              <div className={`flex items-start space-x-3 ${isFullScreen ? "max-w-[60%]" : "max-w-[95%]"}`}>
                <img 
                  className={`h-[30px] w-[30px] shrink-0 rounded-full mt-1 ${isAnimated ? "opacity-100" : "opacity-0"} transition-opacity duration-300`}
                  src={HSELogo} 
                  alt="HSE logo"
                />
                <div 
                  className={`flex flex-col bg-[#006354] text-white p-4 rounded-lg shadow-lg transform ${
                    isAnimated ? "opacity-100 translate-y-0" : "opacity-0 translate-y-3"
                  } transition-all duration-300`}
                > 
                  {loading ? (
                    <div className="flex flex-col items-center">
                      {showEnhancedLoading ? (
                        <Spinner enhanced={true} />
                      ) : (
                        <Spinner />
                      )}
                    </div>
                  ) : (
                    <div className="markdown-content">
                      <Markdown components={components}>{displayContent as string}</Markdown>
                    </div>
                  )}
                  <span className="text-xs text-gray-300 mt-1 opacity-80">
                    {timestamp}
                  </span>
                </div>
              </div>
            )}
            
            {/* User Message */}
            {role === "user" && (
              <div className="flex items-end space-x-2 max-w-[80%]">
                <div 
                  className={`flex flex-col bg-[#f3f3f3] text-black p-4 rounded-lg shadow-md transform ${
                    isAnimated ? "opacity-100 translate-y-0" : "opacity-0 translate-y-3"
                  } transition-all duration-300`}
                > 
                  <div className="text-lg leading-relaxed whitespace-pre-line">{content}</div>
                  <span className="text-xs text-gray-500 mt-1 text-right">
                    {timestamp}
                  </span>
                </div>
              </div>
            )}
            
            {/* Error Message */}
            {error && (
              <div 
                className={`flex items-center gap-2 text-sm text-red-500 mt-2 bg-red-50 px-3 py-2 rounded-md ${
                  isAnimated ? "opacity-100" : "opacity-0"
                } transition-opacity duration-300`}
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