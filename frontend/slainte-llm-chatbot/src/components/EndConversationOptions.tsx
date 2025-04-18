import React, { useState } from 'react';
import { Download, Mail, Check, X } from 'lucide-react';
import SavePDFButton from './SavePDFButton';
import { Message } from '@/models/message';

interface EndConversationOptionsProps {
  messages: Message[];
  onClose: () => void;
}

const EndConversationOptions: React.FC<EndConversationOptionsProps> = ({ 
  messages, 
  onClose 
}) => {
  const [email, setEmail] = useState('');
  const [showEmailForm, setShowEmailForm] = useState(false);
  const [emailSent, setEmailSent] = useState(false);
  const [emailError, setEmailError] = useState('');
  
  const handleEmailSend = async () => {
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setEmailError('Please enter a valid email address');
      return;
    }
    
    setEmailError('');
    
    try {
      // You would implement this backend endpoint for sending emails
      // For now, we'll just simulate success
      // await fetch('/api/send-conversation-email', {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify({ email, messages: messages.filter(msg => !msg.hidden) })
      // });
      
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      setEmailSent(true);
      setTimeout(() => {
        setShowEmailForm(false);
        setEmailSent(false);
      }, 2000);
    } catch (error) {
      console.error('Error sending email:', error);
      setEmailError('Failed to send email. Please try again.');
    }
  };
  
  return (
    <div className="bg-gray-50 border-t border-gray-200 p-4">
      <div className="text-center mb-3">
        <h3 className="text-lg font-medium text-gray-800">End Conversation</h3>
        <p className="text-sm text-gray-600">Would you like to save this conversation?</p>
      </div>
      
      <div className="flex justify-center space-x-4 mb-4">
        {/* Save as PDF */}
        <button
          onClick={() => {}}
          className="flex items-center justify-center bg-[#02594C] text-white hover:bg-[#017365] px-4 py-2 rounded-md transition-colors"
        >
          <SavePDFButton messages={messages} />
          <span className="ml-2">Save as PDF</span>
        </button>
        
        {/* Send via Email */}
        <button
          onClick={() => setShowEmailForm(true)}
          className="flex items-center justify-center bg-[#02594C] text-white hover:bg-[#017365] px-4 py-2 rounded-md transition-colors"
        >
          <Mail size={18} />
          <span className="ml-2">Email</span>
        </button>
      </div>
      
      {/* Email Form */}
      {showEmailForm && (
        <div className="mt-4 p-3 bg-white rounded-md border border-gray-200">
          <div className="flex items-center mb-2">
            <h4 className="text-sm font-medium flex-grow">Enter your email address:</h4>
            <button 
              onClick={() => {
                setShowEmailForm(false);
                setEmailError('');
              }}
              className="text-gray-400 hover:text-gray-600"
            >
              <X size={16} />
            </button>
          </div>
          
          <div className="flex">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="your.email@example.com"
              className={`flex-grow border ${emailError ? 'border-red-400' : 'border-gray-300'} rounded-l-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#02594C] focus:border-transparent`}
            />
            <button
              onClick={handleEmailSend}
              disabled={emailSent}
              className={`bg-[#02594C] text-white px-4 py-2 rounded-r-md ${
                emailSent ? 'bg-green-600' : 'hover:bg-[#017365]'
              }`}
            >
              {emailSent ? <Check size={18} /> : 'Send'}
            </button>
          </div>
          
          {emailError && (
            <p className="text-xs text-red-500 mt-1">{emailError}</p>
          )}
          
          {emailSent && (
            <p className="text-xs text-green-600 mt-1">Conversation sent to your email!</p>
          )}
        </div>
      )}
      
      {/* Close/Skip Button */}
      <div className="mt-4 text-center">
        <button
          onClick={onClose}
          className="text-sm text-gray-600 hover:text-gray-800 hover:underline"
        >
          Skip and end conversation
        </button>
      </div>
    </div>
  );
};

export default EndConversationOptions;