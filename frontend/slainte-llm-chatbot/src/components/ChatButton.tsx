import { MessageCircle } from "lucide-react";

interface ChatButtonProps {
  isOpen: boolean;
  onToggle: () => void;
}

const ChatButton: React.FC<ChatButtonProps> = ({ isOpen, onToggle }) => {
  return (
    <button
      onClick={onToggle}
      className={`fixed bottom-4 right-4 z-60 rounded-full p-4 shadow-md ${
        isOpen
          ? "bg-[#02A78B] hover:bg-[#73e6c2] hover:text-[#212B32]"
          : "bg-[#006354] hover:bg-[#73e6c2] hover:text-[#212B32]"
      } text-white flex items-center justify-center`}
    >
      <MessageCircle className="mr-2" />
      {isOpen ? "Close" : "Chat"}
    </button>
  );
};

export default ChatButton;
