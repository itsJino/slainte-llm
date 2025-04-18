import { useState, useCallback } from 'react';

export interface SymptomAssessment {
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

export function useSymptomAssessment() {
  const [assessment, setAssessment] = useState<SymptomAssessment>({
    step: 0,
    otherSymptoms: [],
    complete: false
  });

  // Reset the assessment to its initial state
  const resetAssessment = useCallback(() => {
    setAssessment({
      step: 0,
      otherSymptoms: [],
      complete: false
    });
  }, []);

  // Process the user's response and advance to the next step
  const advanceAssessment = useCallback((userResponse: string) => {
    // Create a copy of the current assessment to update
    const updatedAssessment = { ...assessment };

    // Store the user's response based on the current step
    switch (assessment.step) {
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

    // Update the state with our changes
    setAssessment(updatedAssessment);
    
    // Return the updated assessment (useful for components that need immediate access)
    return updatedAssessment;
  }, [assessment]);

  // Helper function to format symptom assessment data for prompt
  const formatAssessmentForPrompt = useCallback(() => {
    if (!assessment.mainSymptoms) return "";
    
    // Enhanced prompt with RAG instructions for symptom assessment
    return `
Please analyze the following patient information and provide a health assessment:
- Age: ${assessment.age || "Not provided"}
- Gender: ${assessment.gender || "Not provided"}
- Smoking Status: ${assessment.smokingStatus || "Not provided"}
- High Blood Pressure: ${assessment.highBloodPressure || "Not provided"}
- Diabetes: ${assessment.diabetes || "Not provided"}
- Main Symptoms: ${assessment.mainSymptoms}
- Severity: ${assessment.severity || "Not provided"}
- Duration: ${assessment.duration || "Not provided"}
- Medical History: ${assessment.medicalHistory || "Not provided"}
- Other Symptoms: ${assessment.otherSymptoms.join(", ") || "None reported"}

Using the HSE knowledge base, please provide:
- Personalize your response by referring to specific details the user has shared
- Present information in these clear sections without using ### or ** formatting:
  1. "Summary of Your Information" - Bullet points: Age, Gender, Smoking Status, High Blood Pressure, Diabetes, Main Symptoms, Severity, Duration, Medical History, Other Symptoms
  2. "Possible Explanations" - ONLY List 2-3 most relevant potential causes based on the symptoms and HSE guidance
  3. "Suggested Next Steps" - Care recommendations appropriate to severity, citing HSE recommendations
  4. "Where to Get Help" - Relevant HSE services based on symptom severity with specific contact information
  5. "Further Information" - Provide 1-2 specific HSE website links for additional information

At the end of your final assessment, include this exact text:
"For your convenience, you can download a copy of this health assessment as a PDF by clicking the 'Save Assessment' button below. Take care of yourself, and feel free to ask if you have any other questions."
`;
  }, [assessment]);

  return {
    assessment,
    resetAssessment,
    advanceAssessment,
    formatAssessmentForPrompt
  };
}