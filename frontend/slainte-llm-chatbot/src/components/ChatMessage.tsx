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
    p: (props: any) => <p className="text-lg leading-relaxed mb-1" {...props} />,
    h1: (props: any) => <h1 className="text-lg font-semibold mt-2 mb-1" {...props} />,
    h2: (props: any) => <h2 className="text-lg font-semibold mt-2 mb-1" {...props} />,
    h3: (props: any) => <h3 className="text-lg font-semibold mt-1 mb-1" {...props} />,
    ul: (props: any) => <ul className="text-lg pl-4 mb-1" {...props} />,
    ol: (props: any) => <ol className="text-lg pl-4 mb-1" {...props} />,
    li: (props: any) => <li className="mb-0.5" {...props} />,
    a: (props: any) => <a className="underline text-[#73E6C2]" {...props} />,
    code: (props: any) => <code className="text-[0.7rem] bg-black bg-opacity-20 px-1 rounded" {...props} />,
    pre: (props: any) => <pre className="text-[0.7rem] bg-black bg-opacity-20 p-2 rounded my-1 overflow-x-auto" {...props.children} />
  };

  return (
    // Remove fixed max-height to allow content to expand as needed
    <div ref={scrollContentRef} className="space-y-4 w-full">
      {messages.map(({ role, content, loading, error, timestamp }, idx) => (
        <div
          key={idx}
          className={`flex ${role === "user" ? "justify-end" : "justify-start"} w-full`}
        >
          {/* Assistant Message */}
          {role === "ai" && (
            <div className={`flex items-start space-x-2 ${isFullScreen ? "max-w-[60%]" : "max-w-[95%]"}`}>
              <img className="h-[30px] w-[30px] shrink-0 rounded-full" src={HSELogo} alt="HSE logo" />
              <div className="flex flex-col bg-[#006354] text-white p-3 rounded-lg shadow-md">
                {loading ? (
                  <Spinner />
                ) : (
                  <Markdown components={components}>{content as string}</Markdown>
                )}
                <span className="text-xs text-gray-300 mt-1">{timestamp}</span>
              </div>
            </div>
          )}
          {/* User Message */}
          {role === "user" && (
            <div className="flex items-end space-x-2 max-w-[80%]">
              <div className="flex flex-col bg-[#f3f3f3] text-black p-3 rounded-lg shadow-md">
                <div className="text-lg leading-relaxed whitespace-pre-line">{content}</div>
                <span className="text-xs text-gray-500 mt-1 text-right">{timestamp}</span>
              </div>
            </div>
          )}
          {/* Error Message */}
          {error && (
            <div className="flex items-center gap-1 text-xs text-error-red mt-2">
              <img className="h-5 w-5" src={errorIcon} alt="error" />
              <span>Error generating the response</span>
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

export default ChatMessages;