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