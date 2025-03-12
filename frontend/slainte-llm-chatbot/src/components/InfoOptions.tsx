import React from 'react';
import { InfoCategory, InfoCategories } from '@/models/info-categories';

// Single option button component
interface InfoOptionProps {
  title: string;
  onClick: () => void;
  isSelected?: boolean;
}

const InfoOptionButton: React.FC<InfoOptionProps> = ({ title, onClick, isSelected = false }) => {
  return (
    <button
      onClick={onClick}
      className={`text-left w-full p-4 rounded-lg border 
        ${isSelected
          ? "border-[#006354] bg-[#006354] text-white"
          : "border-gray-200 bg-white text-[#006354] hover:border-[#006354]"} 
        transition-colors mb-3 font-medium z-10`}
    >
      {title}
    </button>
  );
};

// Main component props interface
interface InfoOptionsProps {
  infoCategory: string | null;
  infoCategories: InfoCategories;
  handleCategorySelect: (category: string) => void;
  handleOptionSelect: (optionId: string) => void;
}

// Main component
const InfoOptions: React.FC<InfoOptionsProps> = ({
  infoCategory,
  infoCategories,
  handleCategorySelect,
  handleOptionSelect
}) => {
  return (
    <div className="flex-shrink-0 p-4 border-t border-gray-200 bg-gray-50">
      {!infoCategory ? (
        // Display main categories when no category is selected
        <div>
          <h2 className="text-xl font-semibold text-[#006354] mb-4">Information Categories</h2>
          <div className="space-y-2">
            {Object.keys(infoCategories).map((key) => (
              <InfoOptionButton
                key={key}
                title={infoCategories[key].title}
                onClick={() => handleCategorySelect(key)}
              />
            ))}
          </div>
        </div>
      ) : (
        // Display options for selected category
        <div>
          <h2 className="text-xl font-semibold text-[#006354] mb-4">
            {infoCategories[infoCategory].title}
          </h2>
          <div className="space-y-2">
            {infoCategories[infoCategory].options.map(option => (
              <InfoOptionButton
                key={option.id}
                title={option.title}
                onClick={() => handleOptionSelect(option.id)}
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default InfoOptions;