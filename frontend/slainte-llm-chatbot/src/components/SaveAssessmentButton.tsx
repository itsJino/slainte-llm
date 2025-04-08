import React from 'react';
import { Download } from 'lucide-react';
import { generatePDF } from './SavePDFButton';
import { Message } from '@/models/message';

interface SaveAssessmentButtonProps {
  messages: Message[];
  className?: string;
}

const SaveAssessmentButton: React.FC<SaveAssessmentButtonProps> = ({ 
  messages, 
  className = "" 
}) => {
  const handleSave = async () => {
    if (messages.length > 0) {
      await generatePDF(
        messages.filter(msg => !msg.hidden), 
        'Slainte-Health-Assessment.pdf'
      );
    }
  };

  return (
    <button
      onClick={handleSave}
      className={`flex items-center justify-center bg-[#02594C] text-white hover:bg-[#017365] px-4 py-2 rounded-md transition-colors ${className}`}
      disabled={messages.length === 0}
    >
      <Download size={18} className="mr-2" />
      Save Assessment
    </button>
  );
};

export default SaveAssessmentButton;