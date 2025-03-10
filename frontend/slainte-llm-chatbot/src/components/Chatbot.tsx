import { useState, useEffect, useRef } from "react";
import { Maximize, Minimize, RotateCcw, Download } from "lucide-react";
import HSELogo from "@/assets/images/HSE_bg_logo.png";
import HSELongLogo from "@/assets/images/HSE_long_logo.png";
import HSELogoWhite from "@/assets/images/HSE_logo_white.png";
import ChatInput from "@/components/ChatInput";
import ChatMessage from "@/components/ChatMessage";
import ChatButton from "@/components/ChatButton";
import ChatMenu from "@/components/ChatMenu";
import SavePDFButton from "@/components/SavePDFButton";
import useAutoScroll from "@/hooks/useAutoScroll"; 
import { getResponse } from "@/lib/api";
import { Message } from "@/models/message";

// Prompt templates for different flows
const prompts = {
  general_information: `You are Slainte, a friendly health advisor chatbot working for the Health Service Executive (HSE) in Ireland. Your goal is to provide information that might be difficult to find on the HSE website in a more accessible manner.

  - Respond in a friendly, conversational tone using clear, simple language
  - Answer questions about health services, treatments, and conditions based on HSE guidelines
  - Provide information about accessing healthcare services in Ireland
  - Keep responses concise but informative
  - If you don't know something, acknowledge this and suggest where the user might find that information
  - Don't provide medical diagnoses or personalized medical advice
  - Focus on providing factual information from HSE resources
  - Begin all interactions with "🍀 Dia Duit! I'm Slainte, a friendly health advisor. What can I help you with today?"
  - Provide links when appropriate to official HSE resources for further information
  - Use context to guide the conversation and provide relevant information
  - For medical emergencies, always advise contacting emergency services
  - Use bullet points for lists to improve clarity
  - Explain technical terms in simple language
  - Format responses for readability
  - Highlight important points that require attention
  - Don't explain the structure of the chatbot or how it works; focus on providing health information
  
  IMPORTANT:
  - Avoid meta-commentary phrases like "Here's an organized summary" or "This structured approach ensures..."
  - Don't use introductory phrases like "To assist you effectively" or "Certainly!"
  - Never conclude with offers like "Let me know if you need help with specific sections!"
  - Go straight to the relevant information without unnecessary preamble
  - Omit phrases that describe what you're about to do
  - Never summarize your own response at the end
`,

  symptom_checking: `You are Slainte, a friendly health advisor chatbot working for the Health Service Executive (HSE) in Ireland. Your goal is to assist users in getting a general assessment of symptoms they're experiencing.

  You are analyzing comprehensive patient information that has been collected through a structured interview process. The patient has already provided their age, gender, smoking status, relevant medical conditions, and a description of their symptoms, including severity and duration.

  Please provide a thorough but concise assessment that includes:
  1. A summary of the symptoms and health information provided
  2. Potential causes of these symptoms (without making a definitive diagnosis)
  3. Appropriate next steps (self-care suggestions or when to seek medical attention)
  4. Information on where to seek help if needed (GP, emergency services, specific HSE services)

  Use a friendly, reassuring tone while ensuring your advice is factual and based on HSE guidelines. Balance being informative with appropriate caution about limitations of remote symptom assessment.
`
};

// Information Option Component
interface InfoOptionProps {
  title: string;
  onClick: () => void;
  isSelected?: boolean;
}

const InfoOption: React.FC<InfoOptionProps> = ({ title, onClick, isSelected = false }) => {
  return (
    <button
      onClick={onClick}
      className={`text-left w-full p-4 rounded-lg border 
        ${isSelected
          ? "border-[#006354] bg-[#006354] text-white"
          : "border-gray-200 bg-white text-[#006354] hover:border-[#006354]"} 
        transition-colors mb-3 font-medium`}
    >
      {title}
    </button>
  );
};

function formatChatGPTDateTime(): string {
  const now = new Date();
  return new Intl.DateTimeFormat("en-US", {
    weekday: "short",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: true,
  }).format(now);
}

// Log entry type
interface ApiLogEntry {
  timestamp: string;
  request: any;
  response?: string;
  error?: string;
}

// New component for displaying API logs
interface ApiLogProps {
  logs: ApiLogEntry[];
  onClose: () => void;
}

const ApiLogViewer: React.FC<ApiLogProps> = ({ logs, onClose }) => {
  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[10000] p-4">
      <div className="bg-white rounded-lg w-full max-w-4xl max-h-[90vh] flex flex-col">
        <div className="p-4 bg-gray-100 flex justify-between items-center border-b">
          <h2 className="text-lg font-bold">API Logs</h2>
          <button
            onClick={onClose}
            className="p-1 hover:bg-gray-200 rounded"
          >
            ✖
          </button>
        </div>
        <div className="flex-1 overflow-auto p-4">
          {logs.length === 0 ? (
            <p className="text-gray-500">No logs yet</p>
          ) : (
            logs.map((log, index) => (
              <div key={index} className="mb-6 border-b pb-4">
                <div className="text-sm text-gray-500 mb-2">{log.timestamp}</div>
                <div className="mb-4">
                  <h3 className="font-semibold mb-2">Request Payload:</h3>
                  <pre className="bg-gray-100 p-3 rounded text-xs overflow-auto max-h-60">
                    {JSON.stringify(log.request, null, 2)}
                  </pre>
                </div>
                {log.response && (
                  <div className="mb-2">
                    <h3 className="font-semibold mb-2">Response:</h3>
                    <pre className="bg-gray-100 p-3 rounded text-xs overflow-auto max-h-60">
                      {log.response}
                    </pre>
                  </div>
                )}
                {log.error && (
                  <div className="mb-2 text-red-500">
                    <h3 className="font-semibold mb-2">Error:</h3>
                    <pre className="bg-red-50 p-3 rounded text-xs overflow-auto max-h-60">
                      {log.error}
                    </pre>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

// Interface for tracking symptom assessment state
interface SymptomAssessment {
  step: number;
  age?: string;
  gender?: string;
  smokingStatus?: string;
  highBloodPressure?: string;
  diabetes?: string;
  mainSymptoms?: string;
  severity?: string;
  duration?: string;
  medicalHistory?: string;
  otherSymptoms: string[];
  complete: boolean;
}

const Chatbot: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [newMessage, setNewMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const [isFullScreen, setIsFullScreen] = useState(false);
  const [hasInitialInteraction, setHasInitialInteraction] = useState(false);
  const [conversationId, setConversationId] = useState<string>("");
  const [showMenu, setShowMenu] = useState(true);
  const [showInfoOptions, setShowInfoOptions] = useState(false);
  const [infoCategory, setInfoCategory] = useState<string | null>(null);
  const [currentFlow, setCurrentFlow] = useState<string | null>(null);
  const [apiLogs, setApiLogs] = useState<ApiLogEntry[]>([]);
  const [showApiLogs, setShowApiLogs] = useState(false);
  const [symptomAssessment, setSymptomAssessment] = useState<SymptomAssessment>({
    step: 0,
    otherSymptoms: [],
    complete: false
  });

  // Define the hardcoded questions for the symptom checking flow
  const symptomCheckingQuestions = [
    "🍀 Dia Duit! I'm Slainte, a friendly health advisor. What can I help you with today? \n\nTo begin Symptom Checking enter: \n\n**Start Assessment**", // 0: Welcome
    "First, may I ask how old you are?", // 1: Age
    "Thank you. What is your gender? (Male, Female, or Other)", // 2: Gender
    "Do you currently smoke, have you smoked in the past, or have you never smoked?", // 3: Smoking
    "Have you been diagnosed with high blood pressure?", // 4: Blood Pressure
    "Have you been diagnosed with diabetes?", // 5: Diabetes
    "Now, could you please describe your main symptoms? Give as much detail as possible.", // 6: Main Symptoms
    "On a scale from mild to severe, how would you rate these symptoms?", // 7: Severity
    "How long have you been experiencing these symptoms?", // 8: Duration
    "Do you have any other medical conditions or relevant history I should know about?", // 9: Medical History
    "Are you experiencing any other symptoms besides what you've already mentioned?", // 10: Other Symptoms
    "Thank you for providing all this information. Let me analyze your symptoms and provide an assessment." // 11: Final - before generating assessment
  ];

  const messagesEndRef = useAutoScroll();

  // Function to add a log entry
  const addApiLog = (logEntry: {
    request: any;
    response?: string;
    error?: string;
  }) => {
    setApiLogs(prevLogs => [
      ...prevLogs,
      {
        ...logEntry,
        timestamp: new Date().toLocaleString()
      }
    ]);
  };

  // Define the information categories and options with condensed format
  const infoCategories = {
    healthcare: {
      title: "Healthcare Services",
      message: "What would you like to know about healthcare services?",
      options: [
        { id: "find-hospital", title: "Find a Hospital" },
        { id: "find-gp", title: "Find a GP" },
        { id: "emergency-care", title: "Urgent and Emergency Care" }
      ]
    },
    covid: {
      title: "COVID-19 Information",
      message: "What information about COVID-19 would you like to know?",
      options: [
        { id: "covid-vaccines", title: "COVID-19 Vaccines" },
        { id: "covid-testing", title: "COVID-19 Testing" },
        { id: "covid-guidance", title: "Current Guidelines" }
      ]
    },
    benefits: {
      title: "Schemes and Benefits",
      message: "Which type of benefit or support would you like information about?",
      options: [
        { id: "healthcare-costs", title: "Healthcare Cost Support" },
        { id: "medical-card", title: "Medical Card" },
        { id: "gp-visit-card", title: "GP Visit Card" }
      ]
    },
    services: {
      title: "Other Health Services",
      message: "What other health services would you like to learn about?",
      options: [
        { id: "mental-health", title: "Mental Health" },
        { id: "screening", title: "Screening and Vaccinations" },
        { id: "elderly-care", title: "Services for Older People" }
      ]
    }
  };

  useEffect(() => {
    setConversationId(`conv-${Date.now()}`);
  }, []);

  const toggleFullScreen = () => {
    setIsFullScreen(!isFullScreen);
  };

  const closeChat = () => {
    setIsOpen(false);
  };

  const resetConversation = async () => {
    setMessages([]);
    setHasInitialInteraction(false);
    setConversationId(`conv-${Date.now()}`);
    setShowMenu(true);
    setShowInfoOptions(false);
    setInfoCategory(null);
    setCurrentFlow(null);
    setSymptomAssessment({
      step: 0,
      otherSymptoms: [],
      complete: false
    });
  };

  const toggleChat = async () => {
    const newOpenState = !isOpen;
    setIsOpen(newOpenState);
  };

  const toggleApiLogs = () => {
    setShowApiLogs(!showApiLogs);
  };

  const clearApiLogs = () => {
    setApiLogs([]);
  };

  // Function to handle the progression of the symptom assessment
  const advanceSymptomAssessment = (userResponse: string) => {
    // Create a copy of the current assessment to update
    const updatedAssessment = { ...symptomAssessment };

    // Store the user's response based on the current step
    switch (symptomAssessment.step) {
      case 0: // Initial - response to "What can I help you with today?"
        // Move to first question
        updatedAssessment.step = 1;
        break;

      case 1: // Age
        updatedAssessment.age = userResponse;
        updatedAssessment.step = 2;
        break;

      case 2: // Gender
        updatedAssessment.gender = userResponse;
        updatedAssessment.step = 3;
        break;

      case 3: // Smoking
        updatedAssessment.smokingStatus = userResponse;
        updatedAssessment.step = 4;
        break;

      case 4: // Blood Pressure
        updatedAssessment.highBloodPressure = userResponse;
        updatedAssessment.step = 5;
        break;

      case 5: // Diabetes
        updatedAssessment.diabetes = userResponse;
        updatedAssessment.step = 6;
        break;

      case 6: // Main Symptoms
        // If we're collecting other symptoms, add to that array
        if (updatedAssessment.mainSymptoms && updatedAssessment.otherSymptoms.length > 0) {
          updatedAssessment.otherSymptoms.push(userResponse);
        } else {
          updatedAssessment.mainSymptoms = userResponse;
        }
        updatedAssessment.step = 7;
        break;

      case 7: // Severity
        // If main symptoms already set, we're collecting info about other symptoms
        if (updatedAssessment.severity && updatedAssessment.otherSymptoms.length > 0) {
          // We don't store severity for other symptoms, but we need to ask about duration
        } else {
          updatedAssessment.severity = userResponse;
        }
        updatedAssessment.step = 8;
        break;

      case 8: // Duration
        // If duration already set, we're collecting info about other symptoms
        if (updatedAssessment.duration && updatedAssessment.otherSymptoms.length > 0) {
          // We don't store duration for other symptoms
          // After collecting info about other symptoms, go back to asking if there are more
          updatedAssessment.step = 10;
        } else {
          updatedAssessment.duration = userResponse;
          updatedAssessment.step = 9;
        }
        break;

      case 9: // Medical History
        updatedAssessment.medicalHistory = userResponse;
        updatedAssessment.step = 10;
        break;

      case 10: // Other Symptoms
        // Check if the user has mentioned additional symptoms
        const hasOtherSymptoms = /yes|yeah|yep|sure|i do/i.test(userResponse.toLowerCase());

        if (hasOtherSymptoms) {
          // If yes, we need to collect those symptoms
          updatedAssessment.step = 6; // Go back to symptom description step
          // We'll use the same steps 6-8 for additional symptoms
        } else {
          // If no other symptoms, the assessment is complete
          updatedAssessment.step = 11; // Final assessment
          updatedAssessment.complete = true;
        }
        break;

      default:
        break;
    }

    setSymptomAssessment(updatedAssessment);
    return updatedAssessment;
  };

  // Handle main menu option selection
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
        timestamp: formatChatGPTDateTime(),
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
        timestamp: formatChatGPTDateTime(),
        error: "",
        conversationId: conversationId
      };

      // Add category selection prompt
      const categoryPrompt: Message = {
        id: (Date.now() + 1).toString(),
        content: "What kind of information would you like to know about? Please select a category:",
        role: "ai",
        loading: false,
        timestamp: formatChatGPTDateTime(),
        error: "",
        conversationId: conversationId
      };

      setMessages([systemMessage, welcomeMessage, categoryPrompt]);

    } else if (option === "symptoms") {
      // Modified symptom checking flow
      setCurrentFlow("symptom_checking");
      setShowInfoOptions(false);

      // Reset the symptom assessment tracker
      setSymptomAssessment({
        step: 0,
        otherSymptoms: [],
        complete: false
      });

      // Create a hidden system message with the prompt
      const systemMessage: Message = {
        id: `system-${Date.now()}`,
        content: prompts.symptom_checking,
        role: "system",
        loading: false,
        timestamp: formatChatGPTDateTime(),
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
        timestamp: formatChatGPTDateTime(),
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
      timestamp: formatChatGPTDateTime(),
      error: "",
      conversationId: conversationId
    };

    // Add AI response with options
    const aiMessage: Message = {
      id: (Date.now() + 1).toString(),
      content: `${infoCategories[category as keyof typeof infoCategories].message}`,
      role: "ai",
      loading: false,
      timestamp: formatChatGPTDateTime(),
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
      timestamp: formatChatGPTDateTime(),
      error: "",
      conversationId: conversationId
    };

    // Hide options and continue with conversation
    setShowInfoOptions(false);

    // Add user message and prepare for AI response
    const aiMessage: Message = {
      id: (Date.now() + 1).toString(),
      content: "",
      role: "ai",
      loading: true,
      timestamp: formatChatGPTDateTime(),
      error: "",
      conversationId: conversationId
    };

    setMessages(prev => [...prev, userMessage, aiMessage]);

    // Generate response based on selected option
    const promptText = `Provide information about ${optionTitle}`;

    try {
      setIsLoading(true);

      // Create a system message with the current flow's prompt
      const systemPrompt = currentFlow ? prompts[currentFlow as keyof typeof prompts] : prompts.general_information;

      // For general information flow, we want to use RAG
      const useRag = currentFlow === "general_information";

      // Prepare messages for API call
      const apiMessages: Message[] = [
        {
          id: `system-${Date.now()}`,
          content: systemPrompt,
          role: "system",
          loading: false,
          timestamp: formatChatGPTDateTime(),
          error: "",
          hidden: true,
          conversationId: conversationId
        },
        {
          id: Date.now().toString(),
          content: promptText,
          role: "user",
          loading: false,
          timestamp: formatChatGPTDateTime(),
          error: "",
          hidden: true,
          conversationId: conversationId
        }
      ];

      // Log request payload
      const requestPayload = {
        messages: apiMessages,
        useRag: useRag
      };

      // Make the actual API call
      let responseData = await getResponse(apiMessages, useRag);

      // Log response
      addApiLog({
        request: requestPayload,
        response: responseData
      });

      // Extract content from the response
      const cleanResponse = responseData
        .replace(/<think>.*?<\/think>/gs, "")
        .replace(/\\u003c.*?\\u003e/g, "")
        .trim();

      // Update state with cleaned AI response
      setMessages((prevMessages) =>
        prevMessages.map((msg) =>
          msg.id === aiMessage.id
            ? { ...msg, content: cleanResponse, loading: false }
            : msg
        )
      );
    } catch (error: unknown) {
      console.error("Error:", error);

      let errorMessage = "An unknown error occurred.";
      if (error instanceof Error) {
        errorMessage = error.message;
      }

      // Log error
      addApiLog({
        request: {
          messages: [
            {
              content: currentFlow ? prompts[currentFlow as keyof typeof prompts] : prompts.general_information,
              role: "system"
            },
            {
              content: promptText,
              role: "user"
            }
          ],
          useRag: currentFlow === "general_information"
        },
        error: errorMessage
      });

      setMessages((prevMessages) =>
        prevMessages.map((msg) =>
          msg.id === aiMessage.id
            ? { ...msg, content: "Error retrieving response.", loading: false, error: errorMessage }
            : msg
        )
      );
    } finally {
      setIsLoading(false);
    }
  };

  // Function for starting the conversation with a specific prompt
  const startConversation = async (promptText: string, aiResponse: string = "") => {
    setIsLoading(true);

    // Create a system message that won't be displayed to the user
    const initialMessage: Message = {
      id: Date.now().toString(),
      content: promptText,
      role: "user",
      loading: false,
      timestamp: formatChatGPTDateTime(),
      error: "",
      hidden: true, // Hide from UI
      conversationId: conversationId
    };

    // Create a placeholder for the AI response that will be shown
    const aiMessage: Message = {
      id: (Date.now() + 1).toString(),
      content: "",
      role: "ai",
      loading: true,
      timestamp: formatChatGPTDateTime(),
      error: "",
      conversationId: conversationId
    };

    // Append the AI message to the existing messages rather than replacing them
    setMessages(prevMessages => [...prevMessages, aiMessage]);

    try {
      // If we have a predefined response, use it
      if (aiResponse) {
        setTimeout(() => {
          setMessages((prevMessages) =>
            prevMessages.map((msg) =>
              msg.id === aiMessage.id
                ? { ...msg, content: aiResponse, loading: false }
                : msg
            )
          );
          setIsLoading(false);
        }, 500); // Simulate a short delay
      } else {
        // For symptom checking, don't use RAG for the guided questions
        const useRag = currentFlow === "general_information";

        // Prepare request payload
        const requestPayload = {
          messages: [initialMessage],
          useRag: useRag
        };

        // Otherwise make the actual API call
        let responseData = await getResponse([initialMessage], useRag);

        // Log request and response
        addApiLog({
          request: requestPayload,
          response: responseData
        });

        // Extract content from the response
        const cleanResponse = responseData
          .replace(/<think>.*?<\/think>/gs, "")
          .replace(/\\u003c.*?\\u003e/g, "")
          .trim();

        // Update state with cleaned AI response
        setMessages((prevMessages) =>
          prevMessages.map((msg) =>
            msg.id === aiMessage.id
              ? { ...msg, content: cleanResponse, loading: false }
              : msg
          )
        );
      }
    } catch (error: unknown) {
      console.error("Error:", error);

      let errorMessage = "An unknown error occurred.";
      if (error instanceof Error) {
        errorMessage = error.message;
      }

      // Log error
      addApiLog({
        request: {
          messages: [initialMessage],
          useRag: currentFlow === "general_information"
        },
        error: errorMessage
      });

      setMessages((prevMessages) =>
        prevMessages.map((msg) =>
          msg.id === aiMessage.id
            ? { ...msg, content: "Error retrieving response.", loading: false, error: errorMessage }
            : msg
        )
      );
    } finally {
      setIsLoading(false);
    }
  };

  const sendMessage = async () => {
    if (!newMessage.trim()) return;
    setIsLoading(true);

    // Create the user message
    const userMessage: Message = {
      id: Date.now().toString(),
      content: newMessage,
      role: "user",
      loading: false,
      timestamp: formatChatGPTDateTime(),
      error: "",
      conversationId: conversationId
    };

    // Add user message to the conversation
    setMessages(prevMessages => [...prevMessages, userMessage]);
    setNewMessage("");

    // Check if we're in symptom checking flow
    if (currentFlow === "symptom_checking") {
      // Process the user's response and advance the flow
      const updatedAssessment = advanceSymptomAssessment(newMessage);

      // Create placeholder for AI response
      const aiMessage: Message = {
        id: Date.now().toString(),
        content: "",
        role: "ai",
        loading: true,
        timestamp: formatChatGPTDateTime(),
        error: "",
        conversationId: conversationId
      };

      // Add AI message placeholder
      setMessages(prevMessages => [...prevMessages, aiMessage]);

      // If assessment is complete, generate final assessment using LLM
      if (updatedAssessment.complete) {
        try {
          // Format a comprehensive prompt with all collected information
          const assessmentPrompt = `
Please analyze the following patient information and provide a health assessment:
- Age: ${updatedAssessment.age}
- Gender: ${updatedAssessment.gender}
- Smoking Status: ${updatedAssessment.smokingStatus}
- High Blood Pressure: ${updatedAssessment.highBloodPressure}
- Diabetes: ${updatedAssessment.diabetes}
- Main Symptoms: ${updatedAssessment.mainSymptoms}
- Severity: ${updatedAssessment.severity}
- Duration: ${updatedAssessment.duration}
- Medical History: ${updatedAssessment.medicalHistory}
- Other Symptoms: ${updatedAssessment.otherSymptoms.join(", ") || "None reported"}

Please provide:
1. A summary of the symptoms and health information
2. Potential causes (without making a definitive diagnosis)
3. Appropriate next steps (self-care or medical attention)
4. Information on where to seek help if needed
`;

          // We'll use RAG for the final assessment
          const systemMessage: Message = {
            id: `system-${Date.now()}`,
            content: prompts.symptom_checking,
            role: "system",
            timestamp: formatChatGPTDateTime(),
            loading: false,
            error: "",
            hidden: true,
            conversationId: conversationId
          };

          const promptMessage: Message = {
            id: Date.now().toString(),
            content: assessmentPrompt,
            role: "user",
            timestamp: formatChatGPTDateTime(),
            loading: false,
            error: "",
            hidden: true,
            conversationId: conversationId
          };

          // Prepare request payload for logging
          const requestPayload = {
            messages: [systemMessage, promptMessage],
            useRag: true  // Use RAG for the final assessment
          };

          // Make API call
          const responseData = await getResponse([systemMessage, promptMessage], true);

          // Log request and response
          addApiLog({
            request: requestPayload,
            response: responseData
          });

          // Clean the response
          const cleanResponse = responseData
            .replace(/<think>.*?<\/think>/gs, "")
            .replace(/\\u003c.*?\\u003e/g, "")
            .trim();

          // Update the AI message with the response
          setMessages(prevMessages =>
            prevMessages.map(msg =>
              msg.id === aiMessage.id
                ? { ...msg, content: cleanResponse, loading: false }
                : msg
            )
          );
        } catch (error) {
          console.error("Error generating assessment:", error);

          let errorMessage = "An error occurred while generating your assessment.";
          if (error instanceof Error) {
            errorMessage = error.message;
          }

          // Update message with error
          setMessages(prevMessages =>
            prevMessages.map(msg =>
              msg.id === aiMessage.id
                ? { ...msg, content: "Error retrieving response.", loading: false, error: errorMessage }
                : msg
            )
          );
        } finally {
          setIsLoading(false);
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
          setIsLoading(false);
        }, 500);
      }
    } else {
      // For non-symptom checking flows, use the original implementation
      // Create placeholder for AI response
      const aiMessage: Message = {
        id: (Date.now() + 1).toString(),
        content: "",
        role: "ai",
        loading: true,
        timestamp: formatChatGPTDateTime(),
        error: "",
        conversationId: conversationId
      };

      // Add AI message placeholder
      setMessages(prevMessages => [...prevMessages, aiMessage]);

      try {
        // Get ALL messages from the current conversation, including system message
        const allConversationMessages = messages.filter(
          msg => msg.conversationId === conversationId
        );

        // Find system message - this contains the full prompt content
        const existingSystemMessage = allConversationMessages.find(
          msg => msg.role === "system" && msg.hidden
        );

        // Get visible messages for context (non-hidden)
        const visibleMessages = allConversationMessages.filter(
          msg => !msg.hidden && msg.role !== "system"
        );

        // Get the system message based on the current flow
        const systemPrompt = currentFlow ? prompts[currentFlow as keyof typeof prompts] : prompts.general_information;

        const systemMessage: Message = existingSystemMessage || {
          id: `system-${Date.now()}`,
          content: systemPrompt,
          role: "system",
          loading: false,
          timestamp: formatChatGPTDateTime(),
          error: "",
          hidden: true,
          conversationId: conversationId
        };

        // Always use RAG for general information flow
        const useRag = currentFlow === "general_information";

        // Create the messages array to send to the API
        const apiMessages: Message[] = [
          systemMessage,
          ...visibleMessages,
          userMessage
        ];

        // Prepare request payload for logging
        const requestPayload = {
          messages: apiMessages,
          useRag: useRag
        };

        // Debug logging
        console.log("Sending messages to API:", apiMessages);

        // Make the API call
        let responseData = await getResponse(apiMessages, useRag);

        // Add log entry
        addApiLog({
          request: requestPayload,
          response: responseData
        });

        // Clean the response
        const cleanResponse = responseData
          .replace(/<think>.*?<\/think>/gs, "")
          .replace(/\\u003c.*?\\u003e/g, "")
          .trim();

        // Update the AI response
        setMessages((prevMessages) =>
          prevMessages.map((msg) =>
            msg.id === aiMessage.id
              ? { ...msg, content: cleanResponse, loading: false }
              : msg
          )
        );
      } catch (error: unknown) {
        console.error("Error:", error);

        let errorMessage = "An unknown error occurred.";
        if (error instanceof Error) {
          errorMessage = error.message;
        }

        // Log error
        const systemPrompt = currentFlow ? prompts[currentFlow as keyof typeof prompts] : prompts.general_information;
        addApiLog({
          request: {
            messages: [
              {
                content: systemPrompt,
                role: "system"
              },
              {
                content: newMessage,
                role: "user"
              }
            ],
            useRag: currentFlow === "general_information"
          },
          error: errorMessage
        });

        setMessages((prevMessages) =>
          prevMessages.map((msg) =>
            msg.id === aiMessage.id
              ? { ...msg, content: "Error retrieving response.", loading: false, error: errorMessage }
              : msg
          )
        );
      } finally {
        setIsLoading(false);
      }
    }
  };

  // Get visible messages (filter out hidden ones)
  const visibleMessages = messages.filter(msg => !msg.hidden && msg.role !== "system");

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
        <div className="p-4 bg-[#02594C] text-white font-semibold text-lg flex justify-between items-center">
          <img
            src={HSELogo}
            alt="HSE Logo"
            className="w-10 h-10"
          />
          <span>Chat with us</span>
          <div className="flex space-x-2">
            {/* Debug Button - Shows API logs (new) */}
            <button
              onClick={toggleApiLogs}
              className="bg-[#02594C] text-white hover:bg-[#73E6C2] hover:text-[#212B32] rounded p-1 transition-colors"
              title="Show API Logs">
              🐞
            </button>

            {/* Save as PDF Button - Only show when conversation exists */}
            {!showMenu && visibleMessages.length > 0 && (
              <SavePDFButton
                messages={visibleMessages}
                disabled={isLoading}
              />
            )}

            {/* Reset Button */}
            <button
              onClick={resetConversation}
              className="bg-[#02594C] text-white hover:bg-[#73E6C2] hover:text-[#212B32] rounded p-1 transition-colors"
              title="Reset conversation">
              <RotateCcw size={16} />
            </button>

            {/* Fullscreen Toggle Button */}
            <button
              onClick={toggleFullScreen}
              className="bg-[#02594C] text-white hover:bg-[#73E6C2] hover:text-[#212B32] rounded p-1 transition-colors"
              title={isFullScreen ? "Exit fullscreen" : "Fullscreen"}>
              {isFullScreen ? <Minimize size={16} /> : <Maximize size={16} />}
            </button>

            {/* Close Button */}
            <button
              onClick={closeChat}
              className="bg-[#02594C] text-white hover:bg-[#73E6C2] hover:text-[#212B32] rounded p-1 transition-colors"
              title="Close chat">
              ✖
            </button>
          </div>
        </div>

        {/* Content Area */}
        {showMenu ? (
          /* Main Menu */
          <ChatMenu onSelectOption={handleMenuSelect} />
        ) : (
          /* Chat Interface (either Info Options or Regular Chat) */
          <div className="flex-1 flex flex-col overflow-hidden">
            {showInfoOptions ? (
              /* General Information Options */
              <div className="flex-1 overflow-auto bg-gray-50" ref={messagesEndRef}>
                {/* Show message content */}
                {visibleMessages.length > 0 && (
                  <div className="p-4">
                    <ChatMessage
                      messages={visibleMessages}
                      isLoading={isLoading}
                    />
                  </div>
                )}

                {/* Show category selection if no category selected */}
                {!infoCategory ? (
                  <div className="p-4 border-t border-gray-200">
                    <h2 className="text-xl font-semibold text-[#006354] mb-4">Information Categories</h2>
                    <div className="space-y-2">
                      <InfoOption
                        title="Healthcare Services"
                        onClick={() => handleCategorySelect('healthcare')}
                      />
                      <InfoOption
                        title="COVID-19 Information"
                        onClick={() => handleCategorySelect('covid')}
                      />
                      <InfoOption
                        title="Schemes and Benefits"
                        onClick={() => handleCategorySelect('benefits')}
                      />
                      <InfoOption
                        title="Other Health Services"
                        onClick={() => handleCategorySelect('services')}
                      />
                    </div>
                  </div>
                ) : (
                  /* Show specific options for selected category */
                  <div className="p-4 border-t border-gray-200">
                    <h2 className="text-xl font-semibold text-[#006354] mb-4">
                      {infoCategories[infoCategory as keyof typeof infoCategories].title}
                    </h2>
                    <div className="space-y-2">
                      {infoCategories[infoCategory as keyof typeof infoCategories].options.map(option => (
                        <InfoOption
                          key={option.id}
                          title={option.title}
                          onClick={() => handleOptionSelect(option.id)}
                        />
                      ))}
                    </div>
                  </div>
                )}
              </div>
            ) : (
              /* Regular Chat Interface */
              <>
                <div className="flex-1 overflow-auto p-2" ref={messagesEndRef}>
                  <ChatMessage
                    messages={visibleMessages}
                    isLoading={isLoading}
                  />
                </div>

                <div className="border-t border-gray-300 p-0">
                  <ChatInput
                    isLoading={isLoading}
                    sendMessage={sendMessage}
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
      {showApiLogs && (
        <ApiLogViewer logs={apiLogs} onClose={() => setShowApiLogs(false)} />
      )}
    </>
  );
};

export default Chatbot;