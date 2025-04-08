import React from 'react';

interface SpinnerProps {
  enhanced?: boolean;
  message?: string;
}

const Spinner: React.FC<SpinnerProps> = ({ enhanced = false, message = "Analyzing symptoms and consulting HSE guidelines..." }) => {
  // Basic spinner for regular loading states
  if (!enhanced) {
    return (
      <div className="flex justify-center items-center py-2">
        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-white"></div>
      </div>
    );
  }

  // Enhanced spinner with text for final assessment
  return (
    <div className="flex flex-col items-center py-3">
      <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-[#73E6C2]"></div>
      <div className="text-center mt-3">
        <p className="text-sm text-[#73E6C2] font-medium">{message}</p>
        <p className="text-xs text-white mt-1 opacity-80">This may take a moment as I search HSE resources</p>
      </div>
    </div>
  );
};

export default Spinner;