import re
import os
import glob
import pdfplumber
import chromadb
from sentence_transformers import SentenceTransformer

# ✅ Path to your knowledge documents (PDFs)
DOCS_DIR = os.path.abspath("../documents/HSE_Condition_Pages")

# ✅ Ensure ChromaDB is running
CHROMADB_HOST = "localhost"
CHROMADB_PORT = 8000

# ✅ Connect to ChromaDB HTTP Server
chroma_client = chromadb.HttpClient(host=CHROMADB_HOST, port=CHROMADB_PORT)
collection_name = "health_assistant"

# ✅ Create the collection (no deletion, just ensure it's there)
collection = chroma_client.get_or_create_collection(name=collection_name)

# ✅ Load a SentenceTransformer embedding model
model = SentenceTransformer("multi-qa-mpnet-base-dot-v1")  # ✅ 768-dimensional embeddings

def clean_text(text):
    """
    Clean extracted text by removing footer/header boilerplate and 
    other irrelevant content that could affect embeddings
    """
    if not text:
        return ""
    
    # Remove HSE Live contact info and social media references
    patterns_to_remove = [
        r"HSE Live - we're here to help.*?Health Service Executive",
        r"Monday to Friday: 8am to 8pm.*?© Health Service Executive",
        r"Freephone: 1800 700 700.*?Disclaimer",
        r"HSE Facebook.*?Emergencies",
        r"Cookie settings.*?Executive"
    ]
    
    # Apply each pattern
    cleaned_text = text
    for pattern in patterns_to_remove:
        cleaned_text = re.sub(pattern, "", cleaned_text, flags=re.DOTALL)
    
    # Remove "Back to Health A to Z" navigation text
    cleaned_text = re.sub(r"Back to Health A to Z", "", cleaned_text)
    
    # Remove page review information
    cleaned_text = re.sub(r"Page last reviewed:.*?Next review due:.*?2024", "", cleaned_text, flags=re.DOTALL)
    
    # Remove funding notice
    cleaned_text = re.sub(r"This project has received funding.*?Number 123\.", "", cleaned_text, flags=re.DOTALL)
    
    # Remove "Menu" text that appears in headers
    cleaned_text = re.sub(r"^Menu\s*", "", cleaned_text)
    
    # Clean up excess whitespace
    cleaned_text = re.sub(r'\n{3,}', '\n\n', cleaned_text)  # Replace 3+ newlines with just 2
    cleaned_text = re.sub(r'\s{2,}', ' ', cleaned_text)     # Replace multiple spaces with single space
    
    return cleaned_text.strip()

def chunk_text(text, chunk_size=512, overlap=50):
    """Splits text into smaller chunks with overlap for better retrieval"""
    if not text:
        return []
        
    chunks = []
    start = 0
    while start < len(text):
        end = min(start + chunk_size, len(text))
        
        # If we're not at the end of the text, try to end the chunk at a sentence
        if end < len(text):
            # Try to find the last sentence ending within the chunk
            last_period = text.rfind('.', start, end)
            if last_period > start + chunk_size // 2:  # Only use if it's not too far back
                end = last_period + 1
        
        chunks.append(text[start:end])
        start += chunk_size - overlap  # ✅ Overlapping chunks for context
    return chunks

def generate_embedding(text):
    """Generate embeddings using SentenceTransformer"""
    if not text.strip():
        return None
    return model.encode(text).tolist()  # ✅ Convert NumPy array to list

def add_document(text, doc_id):
    """Store document chunks in ChromaDB"""
    if not text.strip():
        print(f"⚠️ Skipping empty document: {doc_id}")
        return

    # First clean the text to remove irrelevant content
    cleaned_text = clean_text(text)
    
    if not cleaned_text.strip():
        print(f"⚠️ Document became empty after cleaning: {doc_id}")
        return
        
    # Print sample of cleaned text for verification
    print(f"\n🧹 Cleaned Text Sample from {doc_id}:\n{cleaned_text[:500]}...\n")
    
    chunks = chunk_text(cleaned_text)  # ✅ Split into smaller retrievable chunks
    
    if not chunks:
        print(f"⚠️ No chunks generated for document: {doc_id}")
        return
        
    doc_metadata = {"source": doc_id, "chunk_count": len(chunks)}
    
    for i, chunk in enumerate(chunks):
        chunk_id = f"{doc_id}_chunk_{i}"
        embedding = generate_embedding(chunk)
        
        if embedding:
            # Add metadata about which section this chunk belongs to
            metadata = {
                "source": doc_id,
                "chunk_index": i,
                "total_chunks": len(chunks)
            }
            
            # Try to determine section title from the chunk
            first_line = chunk.split('\n', 1)[0] if '\n' in chunk else ""
            if first_line and len(first_line) < 100:  # Reasonable section title length
                metadata["section"] = first_line
            
            collection.add(
                ids=[chunk_id], 
                embeddings=[embedding], 
                documents=[chunk],
                metadatas=[metadata]
            )

    print(f"✅ Document {doc_id} indexed with {len(chunks)} chunks")

def extract_text_from_pdf(pdf_path):
    """Extracts text from a given PDF file"""
    text = ""
    try:
        with pdfplumber.open(pdf_path) as pdf:
            for page in pdf.pages:
                extracted = page.extract_text()
                if extracted:
                    text += extracted + "\n"
    except Exception as e:
        print(f"❌ Error extracting text from {pdf_path}: {e}")
    return text.strip()

def load_documents():
    """Read all PDF files from the knowledge base directory"""
    print(f"📂 Loading documents from: {DOCS_DIR}")

    pdf_files = glob.glob(os.path.join(DOCS_DIR, "*.pdf"))
    if not pdf_files:
        print("❌ No PDF files found! Check if the directory is correct.")
        return

    for file_path in pdf_files:
        try:
            doc_id = os.path.basename(file_path)
            content = extract_text_from_pdf(file_path)

            if content:
                print(f"\n📄 Processing {doc_id} ({len(content)} characters)")
                add_document(content, doc_id)
            else:
                print(f"⚠️ No content extracted from {doc_id}")

        except Exception as e:
            print(f"❌ Error processing {file_path}: {e}")

    print("\n📊 Checking stored documents in ChromaDB:")
    collection_info = collection.count()
    print(f"Total documents in collection: {collection_info}")
    sample_docs = collection.peek(5)
    print(f"Sample document IDs: {sample_docs['ids'] if 'ids' in sample_docs else 'None'}")

def search_documents(query, top_k=3):
    """Retrieve relevant document chunks using similarity search"""
    if not query.strip():
        return "Please provide a search query."
        
    embedding = generate_embedding(query)
    results = collection.query(query_embeddings=[embedding], n_results=top_k)

    print(f"\n🔍 Searching for: {query}")

    # ✅ Ensure there are results before extracting data
    if not results or "documents" not in results or not results["documents"]:
        return "❌ No relevant information found. Check your indexing!"

    retrieved_chunks = []
    for item in results["documents"]:
        if isinstance(item, list):
            retrieved_chunks.extend(item)  # ✅ Flatten nested lists
        elif isinstance(item, str):
            retrieved_chunks.append(item)

    if retrieved_chunks:
        # Add source information for transparency
        formatted_results = []
        for i, chunk in enumerate(retrieved_chunks[:top_k]):
            # Try to get source information from metadata if available
            source_info = ""
            if "metadatas" in results and i < len(results["metadatas"][0]):
                metadata = results["metadatas"][0][i]
                if metadata and "source" in metadata:
                    source_info = f"\n[Source: {metadata['source']}]"
            
            formatted_results.append(f"{chunk}{source_info}")
            
        merged_text = "\n\n---\n\n".join(formatted_results)
        return merged_text

    return "❌ No relevant information found."

# ✅ Run the script to process documents and test search
if __name__ == "__main__":
    # 🔄 Load and store documents in ChromaDB
    load_documents()

    # 🔎 Test with a sample query
    test_queries = [
        "What are the symptoms of acute cholecystitis?",
        "How is acute cholecystitis diagnosed?",
        "What are the treatments for gallbladder inflammation?"
    ]
    
    for query in test_queries:
        results = search_documents(query)
        print(f"\n🔍 Search Results for '{query}':\n", results)
        print("\n" + "-"*80 + "\n")