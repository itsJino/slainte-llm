import React from 'react';
import { jsPDF } from 'jspdf';
import html2canvas from 'html2canvas';
import { Download } from 'lucide-react';
import { Message } from '@/models/message';

// Function to generate PDF from conversation
export const generatePDF = async (messages: Message[], filename: string = 'conversation.pdf') => {
  if (messages.length === 0) return;

  // Create a temporary div to render the conversation
  const tempDiv = document.createElement('div');
  tempDiv.className = 'pdf-export';
  tempDiv.style.padding = '20px';
  tempDiv.style.width = '600px';
  tempDiv.style.backgroundColor = 'white';
  tempDiv.style.position = 'absolute';
  tempDiv.style.left = '-9999px';

  // Add a header
  const header = document.createElement('div');
  header.innerHTML = `
    <div style="display: flex; align-items: center; margin-bottom: 20px; border-bottom: 2px solid #006354; padding-bottom: 10px;">
      <h1 style="color: #006354; margin: 0; flex-grow: 1;">Slainte Chat Conversation</h1>
      <p style="color: #666; margin: 0;">${new Date().toLocaleString()}</p>
    </div>
  `;
  tempDiv.appendChild(header);

  // Add each message
  messages.forEach((message) => {
    const messageDiv = document.createElement('div');
    messageDiv.style.marginBottom = '15px';
    messageDiv.style.display = 'flex';
    messageDiv.style.flexDirection = 'column';
    
    // Different styling for user vs AI messages
    if (message.role === 'user') {
      messageDiv.innerHTML = `
        <div style="align-self: flex-end; background-color: #f3f3f3; padding: 10px; border-radius: 8px; max-width: 80%;">
          <p style="margin: 0; white-space: pre-wrap;">${message.content}</p>
          <p style="margin: 0; font-size: 10px; color: #666; text-align: right; margin-top: 5px;">${message.timestamp}</p>
        </div>
      `;
    } else {
      messageDiv.innerHTML = `
        <div style="align-self: flex-start; background-color: #006354; color: white; padding: 10px; border-radius: 8px; max-width: 80%;">
          <p style="margin: 0; white-space: pre-wrap;">${message.content}</p>
          <p style="margin: 0; font-size: 10px; color: #eee; margin-top: 5px;">${message.timestamp}</p>
        </div>
      `;
    }
    
    tempDiv.appendChild(messageDiv);
  });

  // Append to document, take screenshot, then remove
  document.body.appendChild(tempDiv);

  try {
    // Create PDF
    const pdf = new jsPDF('p', 'pt', 'a4');
    const canvas = await html2canvas(tempDiv, {
      scale: 2, // Increase scale for better quality
      useCORS: true,
      logging: false
    });

    const imgData = canvas.toDataURL('image/png');
    const imgWidth = 550; // A4 width in points (72 dpi)
    const pageHeight = 842; // A4 height
    const imgHeight = (canvas.height * imgWidth) / canvas.width;
    let heightLeft = imgHeight;
    let position = 20; // Initial position

    // Add first page
    pdf.addImage(imgData, 'PNG', 20, position, imgWidth, imgHeight);
    heightLeft -= pageHeight - 40; // 20px margin top and bottom

    // Add new pages if content overflows
    while (heightLeft > 0) {
      position = heightLeft - imgHeight + 20;
      pdf.addPage();
      pdf.addImage(imgData, 'PNG', 20, position, imgWidth, imgHeight);
      heightLeft -= pageHeight - 40;
    }

    // Save PDF
    pdf.save(filename);
  } catch (error) {
    console.error('Error generating PDF:', error);
  } finally {
    // Clean up
    document.body.removeChild(tempDiv);
  }
};

// Save PDF Button Component
interface SavePDFButtonProps {
  messages: Message[];
  disabled?: boolean;
}

const SavePDFButton: React.FC<SavePDFButtonProps> = ({ messages, disabled = false }) => {
  const handleSave = async () => {
    // Only generate if there are messages to save
    if (messages.length > 0) {
      await generatePDF(messages.filter(msg => !msg.hidden));
    }
  };

  return (
    <button
      onClick={handleSave}
      disabled={disabled || messages.length === 0}
      className={`bg-[#02594C] text-white hover:bg-[#73E6C2] hover:text-[#212B32] rounded p-1 transition-colors ${
        (disabled || messages.length === 0) ? 'opacity-50 cursor-not-allowed' : ''
      }`}
      title="Save conversation as PDF"
    >
      <Download size={16} />
    </button>
  );
};

export default SavePDFButton;