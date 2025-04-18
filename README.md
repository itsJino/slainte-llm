# Slainte LLM-Powered Health Assistant

Slainte is an intelligent chatbot designed to provide accessible health information, using official Health Service Executive (HSE) Ireland resources as its knowledge base. The application utilizes Retrieval Augmented Generation (RAG) to deliver accurate, contextually relevant health information to users.

![Slainte Logo](frontend/slainte-llm-chatbot/src/assets/images/HSE_bg_logo.png)

## 🌟 Features

- **Conversational Health Information**: Provides friendly, accessible health information based on HSE resources
- **Symptom Assessment**: Conducts structured health assessments with personalized guidance
- **RAG Integration**: Retrieves relevant health information from a knowledge base of HSE documents
- **Vector Database Search**: Uses ChromaDB for semantic search of health documents
- **Embeddable Chat Widget**: Can be embedded in other websites with responsive design
- **Export to PDF**: Save conversations and health assessments as PDF documents
- **Fullscreen Mode**: Toggle between compact widget and fullscreen mode
- **Developer Tools**: Debug mode for API logs and request tracing

## 🏗️ Architecture

The application follows a modern microservices architecture:

### Frontend
- React TypeScript application with Tailwind CSS
- Custom hooks for message management, auto-scrolling, and symptom assessment
- Markdown rendering for rich text formatting
- PDF generation capabilities

### Backend
- Spring Boot Java backend
- Python services for embedding generation and vector search
- Ollama integration for running the DeepSeek LLM locally
- ChromaDB for vector database storage and retrieval

## 🛠️ Technology Stack

### Frontend
- React + TypeScript
- Tailwind CSS
- React Markdown
- jsPDF + html2canvas for PDF generation
- Lucide React for icons

### Backend
- Java 17 + Spring Boot
- Python 3.9+
- ChromaDB (Vector Database)
- SentenceTransformers (Embedding Models)
- Ollama (LLM Hosting)
- DeepSeek (LLM Model)

### Tools & Libraries
- PapaParse for CSV parsing
- pdfplumber for PDF text extraction
- Axios for HTTP requests
- RESTful API integration

## 📁 Project Structure

```
slainte/
├── backend/
│   ├── slainte/                 # Spring Boot application
│   │   ├── controller/          # REST endpoints
│   │   ├── service/             # Business logic
│   │   ├── model/               # Data models
│   │   ├── dto/                 # Data transfer objects
│   │   └── config/              # Application configuration
│   ├── chroma/                  # ChromaDB service
│   │   └── chroma_service.py    # Vector DB operations
│   └── embedding/               # Embedding service
│       └── embedding_service.py # Text embedding generation
├── frontend/
│   └── slainte-llm-chatbot/     # React application
│       ├── src/
│       │   ├── components/      # UI components
│       │   ├── hooks/           # Custom React hooks
│       │   ├── models/          # TypeScript interfaces
│       │   ├── lib/             # Utilities
│       │   ├── assets/          # Static assets
│       │   └── styles/          # CSS styles
│       └── public/              # Public assets
└── documents/                   # HSE health information documents
    └── HSE_Condition_Pages/     # PDF documents for knowledge base
```

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Node.js 16+
- Python 3.9+
- Docker (optional, for containerized deployment)
- Ollama (for LLM hosting)

### Setting up the Backend

1. **Start ChromaDB**
   ```bash
   docker run -p 8000:8000 chromadb/chroma
   ```

2. **Setup Embedding Service**
   ```bash
   cd backend/embedding
   pip install -r requirements.txt
   python embedding_service.py
   ```

3. **Index Documents**
   ```bash
   cd backend/chroma
   python chroma_service.py
   ```

4. **Start Ollama with DeepSeek model**
   ```bash
   ollama run deepseek-r1:7b
   ```

5. **Run Spring Boot Application**
   ```bash
   cd backend/slainte
   ./mvnw spring-boot:run
   ```

### Setting up the Frontend

1. **Install dependencies**
   ```bash
   cd frontend/slainte-llm-chatbot
   npm install
   ```

2. **Start development server**
   ```bash
   npm run dev
   ```

3. **Open browser**
   Navigate to `http://localhost:5173`

## 🔄 Conversation Flows

### General Information Flow
Provides conversational health information from HSE resources, with categories including:
- Healthcare Services
- COVID-19 Information
- Schemes and Benefits
- Other Health Services

### Symptom Assessment Flow
Conducts a structured health assessment through a series of questions:
1. Age
2. Gender
3. Smoking status
4. High blood pressure status
5. Diabetes status
6. Main symptoms
7. Symptom severity
8. Symptom duration
9. Medical history
10. Other symptoms

After collecting information, the system provides a comprehensive assessment with personalized recommendations.

## 🔍 RAG Implementation

The Retrieval Augmented Generation system works as follows:

1. User queries are converted to vector embeddings using SentenceTransformers
2. ChromaDB searches for semantically similar content in the health document collection
3. Retrieved content is combined with user query in a prompt to the LLM
4. The LLM generates a response based on the retrieved content and user question
5. Response is formatted and presented to the user with source attribution

## 📄 License

[MIT License](LICENSE)

## 🙏 Acknowledgements

- Health Service Executive (HSE) Ireland for health information
- DeepSeek for the LLM model
- ChromaDB for vector database technology
- SentenceTransformers for embedding models