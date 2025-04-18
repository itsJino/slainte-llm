export interface InfoOption {
  id: string;
  title: string;
  description?: string;
  subOptions?: InfoSubOption[];  // Add this line to fix the error
}

export interface InfoSubOption {
  id: string;
  title: string;
  description?: string;
}

export interface InfoCategory {
  title: string;
  message: string;
  options: InfoOption[];
}

export interface InfoCategories {
  [key: string]: InfoCategory;
}