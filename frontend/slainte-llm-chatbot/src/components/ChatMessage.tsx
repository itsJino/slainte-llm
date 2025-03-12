import Markdown from "react-markdown";
import useAutoScroll from "@/hooks/useAutoScroll";
import Spinner from "@/components/Spinner";
import errorIcon from "@/assets/images/error.svg";
import HSELogo from "@/assets/images/hse_logo_green.png";
import { Message } from "@/models/message";

interface ChatMessagesProps {
  messages: Message[];
  isLoading?: boolean;
  isFullScreen?: boolean;
}

const ChatMessages: React.FC<ChatMessagesProps> = ({ messages, isLoading, isFullScreen }) => {
  const scrollContentRef = useAutoScroll();

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
    <div ref={scrollContentRef} className="space-y-6 max-h-[400px]"> {/* Increased space between messages */}
      {messages.map(({ role, content, loading, error, timestamp }, idx) => (
        <div
          key={idx}
          className={`flex ${role === "user" ? "justify-end" : "justify-start"} w-full`}
        >
          {/* Assistant Message - Enhanced for readability */}
          {role === "ai" && (
            <div className={`flex items-start space-x-3 ${isFullScreen ? "max-w-[60%]" : "max-w-[95%]"}`}>
              <img className="h-[30px] w-[30px] shrink-0 rounded-full mt-1" src={HSELogo} alt="HSE logo" />
              <div className="flex flex-col bg-[#006354] text-white p-4 rounded-lg shadow-lg"> {/* Enhanced padding and shadow */}
                {loading ? (
                  <Spinner />
                ) : (
                  <div className="markdown-content"> {/* We can target this with custom CSS if needed */}
                    <Markdown components={components}>{content as string}</Markdown>
                  </div>
                )}
                <span className="text-xs text-gray-300 mt-3 opacity-80">{timestamp}</span>
              </div>
            </div>
          )}
          
          {/* User Message - Minor enhancements */}
          {role === "user" && (
            <div className="flex items-end space-x-2 max-w-[80%]">
              <div className="flex flex-col bg-[#f3f3f3] text-black p-4 rounded-lg shadow-md"> {/* Enhanced padding */}
                <div className="text-lg leading-relaxed whitespace-pre-line">{content}</div>
                <span className="text-xs text-gray-500 mt-2 text-right">{timestamp}</span>
              </div>
            </div>
          )}
          
          {/* Error Message - Enhanced */}
          {error && (
            <div className="flex items-center gap-2 text-sm text-red-500 mt-2 bg-red-50 px-3 py-2 rounded-md">
              <img className="h-5 w-5" src={errorIcon} alt="error" />
              <span>Error generating the response. Please try again.</span>
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

export default ChatMessages;