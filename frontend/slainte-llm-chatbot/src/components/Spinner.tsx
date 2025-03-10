import React from 'react';

const Spinner = () => {
  return (
    <div className="flex flex-col items-center justify-center py-2">
      {/* Spinner animation */}
      <div className="relative h-8 w-8">
        {/* Outer circle */}
        <div className="absolute inset-0 rounded-full border-2 border-t-2 border-[#006354] border-t-transparent animate-spin"></div>
        
        {/* Inner circle - slightly delayed */}
        <div className="absolute inset-1 rounded-full border-2 border-t-2 border-[#73E6C2] border-t-transparent animate-spin-slow"></div>
      </div>
      
      {/* Loading text */}
      <div className="text-xs text-[#006354] mt-2 animate-pulse">
        Thinking...
      </div>
    </div>
  );
};

export default Spinner;