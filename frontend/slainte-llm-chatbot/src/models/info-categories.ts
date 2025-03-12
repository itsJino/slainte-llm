export interface InfoOption {
    id: string;
    title: string;
  }
  
  export interface InfoCategory {
    title: string;
    message: string;
    options: InfoOption[];
  }
  
  export interface InfoCategories {
    [key: string]: InfoCategory;
  }
  