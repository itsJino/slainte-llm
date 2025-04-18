import React from "react";
import HSELogoWhite from '@/assets/images/HSE_bg_logo.png';
import { MessageCircle } from "lucide-react";

interface ChatMenuProps {
  onSelectOption: (option: string) => void;
}

const ChatMenu: React.FC<ChatMenuProps> = ({ onSelectOption }) => {
  const options = [
    {
      id: "general",
      title: "General Information",
    },
    {
      id: "symptoms",
      title: "Symptom Checking",
    },
  ];

  return (
    <div className="flex flex-col h-full">
      {/* Green Header Section */}
      <div className="px-4 bg-[#02594C] text-white text-center pb-16">
        <div className="mx-auto w-25 h-25 bg-white flex items-center justify-center mb-0">
          <img
            src={HSELogoWhite}
            alt="HSE Logo"
            className="max-w-full max-h-full"
          />
        </div>
        <h2 className="text-3xl font-light mb-4">Welcome</h2>
        <p className="text-xl">
          Sláinte is ready to assist
        </p>
      </div>

      {/* Options Section - Positioned to overlap with header */}
      <div className="flex-1 bg-gray-50 px-4 -mt-10">
        {/* Cards */}
        <div className="space-y-4">
          {options.map((option, index) => (
            <div
              key={option.id}
              className={`bg-white shadow p-4 ${index === 0 ? '-mt-8' : ''}`}
            >
              <h3 className="text-lg font-medium mb-2">{option.title}</h3>
              <button
                onClick={() => onSelectOption(option.id)}
                className="flex items-center text-white bg-[#006354] hover:bg-[#73E6C2] hover:text-[#212B32] px-4 py-2 transition-colors"
              >
                <MessageCircle className="mr-2" size={18} />
                Chat with Slainte
              </button>
            </div>
          ))}
        </div>
      </div>

      <div className="bg-white shadow p-4">
        <h3 className="text-xs font-medium mb-0">HSE Live - we're here to help</h3>
        <div className="text-xs text-gray-600 mb-0">
          <p className="text-xs mb-0">Monday to Friday: 8am to 8pm</p>
          <p className="text-xs mb-0">Saturday: 9am to 5pm</p>
          <p className="text-xs mb-0">Sunday: Closed</p>
          <p className="text-xs mb-0 mb-2">Bank holidays: Closed</p>
        </div>
        <p className="text-xs mb-0">Freephone: 1800 700 700</p>
        <p className="text-xs">From outside Ireland: 00 353 1 240 8787</p>
      </div>
    </div>
  );
};

export default ChatMenu;