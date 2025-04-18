// src/components/InfoOptions.tsx
import React, { useState } from 'react';
import { InfoCategory, InfoCategories, InfoOption } from '@/models/info-categories';

// Single option button component
interface InfoOptionButtonProps {
  title: string;
  description?: string;
  onClick: () => void;
  isSelected?: boolean;
  hasSubOptions?: boolean;
}

const InfoOptionButton: React.FC<InfoOptionButtonProps> = ({ 
  title, 
  description, 
  onClick, 
  isSelected = false,
  hasSubOptions = false
}) => {
  return (
    <button
      onClick={onClick}
      className={`text-left w-full p-3 rounded-md border 
        ${isSelected
          ? "border-[#006354] bg-[#006354] text-white"
          : "border-gray-200 bg-white text-[#006354] hover:border-[#006354]"} 
        transition-colors mb-2 font-medium z-10`}
    >
      <div className="flex justify-between items-center">
        <div>
          <div className="text-sm font-medium">{title}</div>
          {description && (
            <p className="text-xs mt-0.5 font-normal text-gray-500 line-clamp-1">{description}</p>
          )}
        </div>
        <span className="text-xs ml-1">→</span>
      </div>
    </button>
  );
};

// Main component props interface
interface InfoOptionsProps {
  infoCategory: string | null;
  infoCategories: InfoCategories;
  handleCategorySelect: (category: string) => void;
  handleOptionSelect: (optionId: string) => void;
  handleSubOptionSelect: (optionId: string, subOptionId: string) => void;
}

// Main component
const InfoOptions: React.FC<InfoOptionsProps> = ({
  infoCategory,
  infoCategories,
  handleCategorySelect,
  handleOptionSelect,
  handleSubOptionSelect
}) => {
  const [selectedOption, setSelectedOption] = useState<string | null>(null);

  // Back button handler
  const handleBack = () => {
    setSelectedOption(null);
  };

  return (
    <div className="flex-shrink-0 p-2 border-t border-gray-200 bg-gray-50">
      {!infoCategory ? (
        // Display main categories when no category is selected
        <div>
          <h2 className="text-base font-semibold text-[#006354] mb-2">Information Categories</h2>
          <div className="grid grid-cols-2 gap-2">
            {Object.keys(infoCategories).map((key) => (
              <InfoOptionButton
                key={key}
                title={infoCategories[key].title}
                onClick={() => handleCategorySelect(key)}
              />
            ))}
          </div>
        </div>
      ) : selectedOption ? (
        // Display suboptions for the selected option
        <div>
          <button
            onClick={handleBack}
            className="text-white bg-[#02A78B] hover:bg-[#029277] px-3 py-1.5 rounded text-sm mb-2 flex items-center w-auto"
          >
            <span>← Back to {infoCategories[infoCategory].title}</span>
          </button>
          
          <h2 className="text-base font-semibold text-[#006354] mb-2">
            {infoCategories[infoCategory].options.find(o => o.id === selectedOption)?.title}
          </h2>
          
          <div className="grid grid-cols-2 gap-2 max-h-48 overflow-y-auto">
            {infoCategories[infoCategory].options
              .find(o => o.id === selectedOption)
              ?.subOptions?.map(subOption => (
                <InfoOptionButton
                  key={subOption.id}
                  title={subOption.title}
                  description={subOption.description}
                  onClick={() => handleSubOptionSelect(selectedOption, subOption.id)}
                />
              ))}
          </div>
        </div>
      ) : (
        // Display options for selected category
        <div>
          <button
            onClick={() => handleCategorySelect("")}
            className="text-white bg-[#02A78B] hover:bg-[#029277] px-3 py-1.5 rounded text-sm mb-2 flex items-center w-auto"
          >
            <span>← Back to Schemes and Benefits</span>
          </button>
          
          <h2 className="text-base font-semibold text-[#006354] mb-2">
            {infoCategories[infoCategory].title}
          </h2>
          
          <div className="grid grid-cols-2 gap-2 max-h-48 overflow-y-auto">
            {infoCategories[infoCategory].options.map(option => (
              <InfoOptionButton
                key={option.id}
                title={option.title}
                description={option.description}
                hasSubOptions={!!(option.subOptions && option.subOptions.length > 0)}
                onClick={() => {
                  if (option.subOptions && option.subOptions.length > 0) {
                    setSelectedOption(option.id);
                  } else {
                    handleOptionSelect(option.id);
                  }
                }}
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default InfoOptions;