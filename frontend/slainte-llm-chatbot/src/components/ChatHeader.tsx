import React from 'react';
import { Maximize, Minimize, RotateCcw, Download } from 'lucide-react';
import HSELogo from '@/assets/images/HSE_bg_logo.png';
import SavePDFButton from './SavePDFButton';
import { Message } from '@/models/message';

interface ChatHeaderProps {
  isFullScreen: boolean;
  toggleFullScreen: () => void;
  resetConversation: () => void;
  closeChat: () => void;
  toggleApiLogs: () => void;
  showMenu: boolean;
  messages: Message[];
  isLoading: boolean;
}

const ChatHeader: React.FC<ChatHeaderProps> = ({
  isFullScreen,
  toggleFullScreen,
  resetConversation,
  closeChat,
  toggleApiLogs,
  showMenu,
  messages,
  isLoading
}) => {
  return (
    <div className="p-4 bg-[#02594C] text-white font-semibold text-lg flex justify-between items-center">
      <img 
        src={HSELogo} 
        alt="HSE Logo" 
        className="w-10 h-10" 
      />
      <span>Chat with us</span>
      <div className="flex space-x-2">
        {/* Debug Button - Shows API logs */}
        <button
          onClick={toggleApiLogs}
          className="bg-[#02594C] text-white hover:bg-[#73E6C2] hover:text-[#212B32] rounded p-1 transition-colors"
          title="Show API Logs">
          🐞
        </button>

        {/* Save as PDF Button - Only show when conversation exists */}
        {!showMenu && messages.length > 0 && (
          <SavePDFButton 
            messages={messages} 
            disabled={isLoading} 
          />
        )}

        {/* Reset Button */}
        <button
          onClick={resetConversation}
          className="bg-[#02594C] text-white hover:bg-[#73E6C2] hover:text-[#212B32] rounded p-1 transition-colors"
          title="Reset conversation">
          <RotateCcw size={16} />
        </button>

        {/* Fullscreen Toggle Button */}
        <button
          onClick={toggleFullScreen}
          className="bg-[#02594C] text-white hover:bg-[#73E6C2] hover:text-[#212B32] rounded p-1 transition-colors"
          title={isFullScreen ? "Exit fullscreen" : "Fullscreen"}>
          {isFullScreen ? <Minimize size={16} /> : <Maximize size={16} />}
        </button>

        {/* Close Button */}
        <button
          onClick={closeChat}
          className="bg-[#02594C] text-white hover:bg-[#73E6C2] hover:text-[#212B32] rounded p-1 transition-colors"
          title="Close chat">
          ✖
        </button>
      </div>
    </div>
  );
};

export default ChatHeader;