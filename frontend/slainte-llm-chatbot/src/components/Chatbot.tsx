import { useState, useEffect } from "react";
import ChatHeader from "./ChatHeader";
import ChatInput from "@/components/ChatInput";
import ChatMessage from "@/components/ChatMessage";
import ChatMenu from "@/components/ChatMenu";
import ChatButton from "@/components/ChatButton";
import InfoOptions from "@/components/InfoOptions";
import ApiLogViewer from "./ApiLogViewer";
import useAutoScroll from "@/hooks/useAutoScroll";
import SaveAssessmentButton from "@/components/SaveAssessmentButton";
import EndConversationOptions from "@/components/EndConversationOptions";
import { useChatMessages, formatChatDateTime } from "@/hooks/useChatMessages";
import { useSymptomAssessment } from "@/hooks/useSymptomAssessment";
import { useApiLogs } from "@/hooks/useApiLogs";
import {
  infoCategories,
  SYSTEM_PROMPTS,
  symptomCheckingQuestions
} from "@/lib/prompt-templates"; // Updated import
import { Message } from "@/models/message";

const Chatbot: React.FC = () => {
  // UI state
  const [isOpen, setIsOpen] = useState(false);
  const [isFullScreen, setIsFullScreen] = useState(false);
  const [hasInitialInteraction, setHasInitialInteraction] = useState(false);
  const [conversationId, setConversationId] = useState<string>("");
  const [showMenu, setShowMenu] = useState(true);
  const [showInfoOptions, setShowInfoOptions] = useState(false);
  const [infoCategory, setInfoCategory] = useState<string | null>(null);
  const [currentFlow, setCurrentFlow] = useState<string | null>(null);
  const [generatingFinalAssessment, setGeneratingFinalAssessment] = useState(false);
  const [showSaveOption, setShowSaveOption] = useState(false);
  const [showEndOptions, setShowEndOptions] = useState(false);


  // Custom hooks
  const {
    logs,
    showLogs,
    addLog,
    clearLogs,
    toggleLogs
  } = useApiLogs();

  const {
    messages,
    setMessages,
    newMessage,
    setNewMessage,
    isLoading,
    sendMessage,
    startConversation,
    resetMessages
  } = useChatMessages(conversationId, currentFlow, addLog);

  const {
    assessment,
    resetAssessment,
    advanceAssessment,
    formatAssessmentForPrompt
  } = useSymptomAssessment();

  const messagesEndRef = useAutoScroll();

  // Initialize conversation ID
  useEffect(() => {
    setConversationId(`conv-${Date.now()}`);
  }, []);

  // Toggle functions
  const toggleFullScreen = () => setIsFullScreen(!isFullScreen);
  const closeChat = () => setIsOpen(false);
  const toggleChat = () => setIsOpen(!isOpen);

  // Reset function
  const resetConversation = async () => {
    resetMessages();
    setHasInitialInteraction(false);
    setConversationId(`conv-${Date.now()}`);
    setShowMenu(true);
    setShowInfoOptions(false);
    setInfoCategory(null);
    setCurrentFlow(null);
    resetAssessment();
    setGeneratingFinalAssessment(false);
    setShowSaveOption(false);
  };

  const handleEndConversation = () => {
    setShowEndOptions(true);
  };
  
  const closeEndConversation = () => {
    setShowEndOptions(false);
  };

  // Handle menu selection
  const handleMenuSelect = async (option: string) => {
    setShowMenu(false);
    setHasInitialInteraction(true);

    if (option === "general") {
      // General information flow
      setCurrentFlow("general_information");
      setShowInfoOptions(true);

      // Create a hidden system message that specifies the flow
      const systemMessage: Message = {
        id: `system-${Date.now()}`,
        content: "general_information",
        role: "system",
        loading: false,
        timestamp: formatChatDateTime(),
        error: "",
        hidden: true,
        conversationId: conversationId
      };

      // Create the welcome message
      const welcomeMessage: Message = {
        id: Date.now().toString(),
        content: "🍀 Dia Duit! I'm Slainte, a friendly health advisor. What can I help you with today?",
        role: "ai",
        loading: false,
        timestamp: formatChatDateTime(),
        error: "",
        conversationId: conversationId
      };

      // Add category selection prompt
      const categoryPrompt: Message = {
        id: (Date.now() + 1).toString(),
        content: "What kind of information would you like to know about? Please select a category:",
        role: "ai",
        loading: false,
        timestamp: formatChatDateTime(),
        error: "",
        conversationId: conversationId
      };

      setMessages([systemMessage, welcomeMessage, categoryPrompt]);

    } else if (option === "symptoms") {
      // Modified symptom checking flow
      setCurrentFlow("symptom_checking");
      setShowInfoOptions(false);

      // Reset the symptom assessment tracker
      resetAssessment();

      // Create a hidden system message with the prompt
      const systemMessage: Message = {
        id: `system-${Date.now()}`,
        content: SYSTEM_PROMPTS.symptom_checking, // Updated to use the centralized prompts
        role: "system",
        loading: false,
        timestamp: formatChatDateTime(),
        error: "",
        hidden: true,
        conversationId: conversationId
      };

      // Create the welcome message (first question)
      const welcomeMessage: Message = {
        id: Date.now().toString(),
        content: symptomCheckingQuestions[0],
        role: "ai",
        loading: false,
        timestamp: formatChatDateTime(),
        error: "",
        conversationId: conversationId
      };

      // Set initial messages
      setMessages([systemMessage, welcomeMessage]);
    }
  };

  // Handle info category selection
  const handleCategorySelect = (category: string) => {
    setInfoCategory(category);

    // Add user message showing what they selected
    const categoryNames: { [key: string]: string } = {
      healthcare: "Healthcare Services",
      covid: "COVID-19 Information",
      benefits: "Schemes and Benefits",
      services: "Other Health Services"
    };

    const userMessage: Message = {
      id: Date.now().toString(),
      content: `I'd like to know about ${categoryNames[category]}`,
      role: "user",
      loading: false,
      timestamp: formatChatDateTime(),
      error: "",
      conversationId: conversationId
    };

    // Add AI response with options
    const aiMessage: Message = {
      id: (Date.now() + 1).toString(),
      content: `${infoCategories[category as keyof typeof infoCategories].message}`,
      role: "ai",
      loading: false,
      timestamp: formatChatDateTime(),
      error: "",
      conversationId: conversationId
    };

    // Append to existing messages, maintaining the welcome message at the beginning
    setMessages(prev => [...prev, userMessage, aiMessage]);
  };

  // Handle specific option selection
  const handleOptionSelect = async (optionId: string) => {
    // Add user message showing what they selected
    let optionTitle = "";
    let categoryData = null;

    // Find the option title
    if (infoCategory) {
      categoryData = infoCategories[infoCategory as keyof typeof infoCategories];
      const option = categoryData.options.find(o => o.id === optionId);
      if (option) {
        optionTitle = option.title;
      }
    }

    const userMessage: Message = {
      id: Date.now().toString(),
      content: `I'd like information about ${optionTitle}`,
      role: "user",
      loading: false,
      timestamp: formatChatDateTime(),
      error: "",
      conversationId: conversationId
    };

    // Hide options and continue with conversation
    setShowInfoOptions(false);

    // Add only the user message without creating an AI message placeholder
    // The startConversation function will create the AI message placeholder
    setMessages(prev => [...prev, userMessage]);

    // Generate response based on selected option
    const promptText = `Give me general information on ${optionTitle}`;
    // const promptText = `${optionTitle}`;

    await startConversation(SYSTEM_PROMPTS.general_information, promptText); // Updated to use centralized prompts
  };

  // Custom sendMessage function for symptom checking flow
  const handleSendMessage = async () => {
    if (!newMessage.trim()) return;

    if (currentFlow === "symptom_checking" && !assessment.complete) {
      // Process user's response using symptom assessment system
      const userMessage: Message = {
        id: Date.now().toString(),
        content: newMessage,
        role: "user",
        loading: false,
        timestamp: formatChatDateTime(),
        error: "",
        conversationId: conversationId
      };

      // Add user message to the conversation
      setMessages(prevMessages => [...prevMessages, userMessage]);
      setNewMessage("");

      // Process the user's response and advance the assessment
      const updatedAssessment = advanceAssessment(newMessage);

      // Create placeholder for AI response
      const aiMessage: Message = {
        id: Date.now().toString(),
        content: "",
        role: "ai",
        loading: true,
        timestamp: formatChatDateTime(),
        error: "",
        conversationId: conversationId
      };

      // Add AI message placeholder
      setMessages(prevMessages => [...prevMessages, aiMessage]);

      if (updatedAssessment.complete) {
        // If assessment is complete, generate final assessment using LLM
        try {
          // Set flag for generating final assessment
          setGeneratingFinalAssessment(true);

          // Format a comprehensive prompt with all collected information
          const assessmentPrompt = formatAssessmentForPrompt();

          // Generate final assessment with RAG
          await startConversation(SYSTEM_PROMPTS.symptom_checking, assessmentPrompt); // Updated to use centralized prompts

          // Show save option after assessment is complete
          setShowSaveOption(true);

          // Reset flag when complete
          setGeneratingFinalAssessment(false);
        } catch (error) {
          console.error("Error generating assessment:", error);

          // Update the message with an error
          setMessages(prevMessages =>
            prevMessages.map(msg =>
              msg.id === aiMessage.id
                ? { ...msg, content: "Error retrieving response.", loading: false, error: "Error generating assessment." }
                : msg
            )
          );

          // Reset flag when error occurs
          setGeneratingFinalAssessment(false);
        }
      } else {
        // If assessment is not complete, get the next question from the hardcoded list
        const nextQuestion = symptomCheckingQuestions[updatedAssessment.step];

        // Small delay to simulate thinking
        setTimeout(() => {
          // Update AI message with next question
          setMessages(prevMessages =>
            prevMessages.map(msg =>
              msg.id === aiMessage.id
                ? { ...msg, content: nextQuestion, loading: false }
                : msg
            )
          );
        }, 500);
      }
    } else {
      // For non-symptom checking flows, use the standard sendMessage function
      await sendMessage();
    }
  };

  // Get visible messages (filter out hidden ones)
  const visibleMessages = messages.filter(msg => !msg.hidden && msg.role !== "system");

  // UI rendering
  return (
    <>
      {/* Chat Toggle Button */}
      <ChatButton isOpen={isOpen} onToggle={toggleChat} />

      {/* Chatbot Window */}
      <div
        className={`fixed ${isFullScreen ? "inset-0 w-full h-full" : "bottom-20 right-4 w-80 sm:w-110 h-[700px]"} 
        bg-white shadow-lg border border-gray-300 flex flex-col transition-all duration-300 z-[9999] overflow-hidden ${isOpen ? "translate-y-0 opacity-100" : "translate-y-4 opacity-0 pointer-events-none"
          }`}
      >
        {/* Chat Header */}
        <ChatHeader
          isFullScreen={isFullScreen}
          toggleFullScreen={toggleFullScreen}
          resetConversation={resetConversation}
          closeChat={closeChat}
          toggleApiLogs={toggleLogs}
          showMenu={showMenu}
          messages={visibleMessages}
          isLoading={isLoading}
        />

        {/* Content Area */}
        {showMenu ? (
          /* Main Menu */
          <ChatMenu onSelectOption={handleMenuSelect} />
        ) : (
          /* Chat Interface (either Info Options or Regular Chat) */
          <div className="flex-1 flex flex-col overflow-hidden">
            {showInfoOptions ? (
              /* General Information Options */
              <div className="flex-1 flex flex-col overflow-auto bg-gray-50">
                {/* Message container */}
                <div className="flex-grow overflow-auto p-4" ref={messagesEndRef}>
                  <ChatMessage
                    messages={visibleMessages}
                    isLoading={isLoading || generatingFinalAssessment}
                    isFullScreen={isFullScreen}
                  />
                </div>

                {showEndOptions ? (
                  <EndConversationOptions
                    messages={visibleMessages}
                    onClose={() => {
                      closeEndConversation();
                      closeChat();
                    }}
                  />
                ) : (
                  <div className="border-t border-gray-300 p-0">
                    <div className="flex items-center">
                      <ChatInput
                        isLoading={isLoading}
                        sendMessage={handleSendMessage}
                        newMessage={newMessage}
                        setNewMessage={setNewMessage}
                      />
                      {visibleMessages.length > 1 && (
                        <button
                          onClick={handleEndConversation}
                          className="ml-2 mr-2 text-xs text-[#02594C] hover:underline"
                        >
                          End Conversation
                        </button>
                      )}
                    </div>
                  </div>
                )}

                {/* Options container */}
                <InfoOptions
                  infoCategory={infoCategory}
                  infoCategories={infoCategories}
                  handleCategorySelect={handleCategorySelect}
                  handleOptionSelect={handleOptionSelect}
                />
              </div>
            ) : (
              // Regular chat interface
              <>
                <div className="flex-1 overflow-auto p-2" ref={messagesEndRef}>
                  <ChatMessage
                    messages={visibleMessages}
                    isLoading={isLoading}
                    isFullScreen={isFullScreen}
                  />
                </div>

                {currentFlow === "symptom_checking" && assessment.complete && showSaveOption && (
                  <div className="p-3 bg-gray-50 flex justify-center">
                    <SaveAssessmentButton messages={visibleMessages} />
                  </div>
                )}

                <div className="border-t border-gray-300 p-0">
                  <ChatInput
                    isLoading={isLoading}
                    sendMessage={handleSendMessage}
                    newMessage={newMessage}
                    setNewMessage={setNewMessage}
                  />
                </div>


              </>
            )}
          </div>
        )}

      </div>

      {/* API Logs Modal */}
      {showLogs && (
        <ApiLogViewer logs={logs} onClose={toggleLogs} />
      )}
    </>
  );
};

export default Chatbot;