import Markdown from "react-markdown";
import useAutoScroll from "@/hooks/useAutoScroll";
import Spinner from "@/components/Spinner";
import errorIcon from "@/assets/images/error.svg";
import HSELogo from "@/assets/images/hse_logo_green.png";
import { Message } from "@/models/message";

interface ChatMessagesProps {
  messages: Message[];
  isLoading: boolean;
}

const ChatMessages: React.FC<ChatMessagesProps> = ({ messages }) => {
  const scrollContentRef = useAutoScroll();

  return (
    <div ref={scrollContentRef} className="space-y-4 max-h-[400px]">
      {messages.map(({ role, content, loading, error, timestamp }, idx) => (
        <div
          key={idx}
          className={`flex ${role === "user" ? "justify-end" : "justify-start"} w-full`}
        >
          {/* Assistant Message */}
          {role === "ai" && (
            <div className="flex items-start space-x-2 max-w-[80%]">
              <img className="h-[30px] w-[30px] shrink-0 rounded-full" src={HSELogo} alt="HSE logo" />
              <div className="flex flex-col bg-[#006354] text-white p-3 rounded-lg shadow-md">
                {loading ? (
                  <Spinner />
                ) : (
                  <Markdown className="text-sm leading-relaxed">{content as string}</Markdown>
                )}
                <span className="text-xs text-gray-300 mt-1">{timestamp}</span>
              </div>
            </div>
          )}

          {/* User Message */}
          {role === "user" && (
            <div className="flex items-end space-x-2 max-w-[80%]">
              <div className="flex flex-col bg-[#f3f3f3] text-black p-3 rounded-lg shadow-md">
                <div className="text-sm leading-relaxed whitespace-pre-line">{content}</div>
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
