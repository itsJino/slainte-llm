import React from 'react';
import '../styles/spinner.css';

const Spinner = () => {
  // This key ensures the animation reflows and properly starts
  const uniqueKey = React.useId();
  
  return (
    <div className="spinner-container">
      {/* Spinner container with a key to ensure animation restart */}
      <div className="spinner-wrapper" key={uniqueKey}>
        {/* Outer circle */}
        <div className="spinner-outer-circle"></div>
        
        {/* Inner circle */}
        <div className="spinner-inner-circle"></div>
        
        {/* Center dot - only in advanced version */}
        <div className="spinner-dot"></div>
      </div>
      
      {/* Loading text - now with white color */}
      <div className="spinner-text text-white">
        Thinking...
      </div>
    </div>
  );
};

export default Spinner;