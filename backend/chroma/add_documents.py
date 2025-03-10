import re
import os
import glob
import pdfplumber
import chromadb
import argparse
from sentence_transformers import SentenceTransformer

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
        start += chunk_size - overlap  # Overlapping chunks for context
    return chunks

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

def add_documents(input_dir, collection_name="health_assistant", chromadb_host="localhost", chromadb_port=8000, model_name="multi-qa-mpnet-base-dot-v1"):
    """Add new documents to the existing ChromaDB collection"""
    
    print(f"🔄 Connecting to ChromaDB at {chromadb_host}:{chromadb_port}")
    chroma_client = chromadb.HttpClient(host=chromadb_host, port=chromadb_port)
    
    try:
        # Connect to existing collection
        collection = chroma_client.get_collection(name=collection_name)
        print(f"✅ Connected to existing collection: {collection_name}")
    except Exception as e:
        print(f"❌ Error: {e}")
        print(f"Creating new collection: {collection_name}")
        collection = chroma_client.create_collection(name=collection_name)

    # Load the embedding model
    print(f"🧠 Loading embedding model: {model_name}")
    model = SentenceTransformer(model_name)
    
    # Find all PDF files in the input directory and its subdirectories
    pdf_files = []
    for root, dirs, files in os.walk(input_dir):
        for file in files:
            if file.lower().endswith('.pdf'):
                pdf_files.append(os.path.join(root, file))
    
    if not pdf_files:
        print(f"❌ No PDF files found in {input_dir} or its subdirectories")
        return
    
    print(f"📚 Found {len(pdf_files)} PDF files to process")
    
    # Process each PDF file
    for file_path in pdf_files:
        # Create a more descriptive doc_id that includes the subdirectory path
        rel_path = os.path.relpath(file_path, input_dir)
        # Replace directory separators with underscores to create a valid ID
        doc_id = rel_path.replace(os.path.sep, '_')
        print(f"\n📄 Processing {doc_id}")
        
        # Extract text from PDF
        content = extract_text_from_pdf(file_path)
        if not content:
            print(f"⚠️ No content extracted from {doc_id}")
            continue
            
        # Clean the text
        cleaned_text = clean_text(content)
        if not cleaned_text:
            print(f"⚠️ Document became empty after cleaning: {doc_id}")
            continue
            
        # Split text into chunks
        chunks = chunk_text(cleaned_text)
        if not chunks:
            print(f"⚠️ No chunks generated for document: {doc_id}")
            continue
            
        print(f"🧩 Created {len(chunks)} chunks for {doc_id}")
        
        # Add each chunk to the collection
        for i, chunk in enumerate(chunks):
            chunk_id = f"{doc_id}_chunk_{i}"
            
            # Generate embedding
            embedding = model.encode(chunk).tolist()
            
            # Add metadata
            metadata = {
                "source": doc_id,
                "chunk_index": i,
                "total_chunks": len(chunks)
            }
            
            # Try to determine section title from the chunk
            first_line = chunk.split('\n', 1)[0] if '\n' in chunk else ""
            if first_line and len(first_line) < 100:  # Reasonable section title length
                metadata["section"] = first_line
            
            # Add to collection
            collection.add(
                ids=[chunk_id],
                embeddings=[embedding],
                documents=[chunk],
                metadatas=[metadata]
            )
            
        print(f"✅ Successfully added {doc_id} to the database")
    
    # Print collection stats
    collection_count = collection.count()
    print(f"\n📊 Collection now has {collection_count} document chunks")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Add documents to an existing ChromaDB collection")
    parser.add_argument("--input_dir", required=True, help="Directory containing PDF files to add")
    parser.add_argument("--collection", default="health_assistant", help="ChromaDB collection name")
    parser.add_argument("--host", default="localhost", help="ChromaDB host")
    parser.add_argument("--port", default=8000, type=int, help="ChromaDB port")
    parser.add_argument("--model", default="multi-qa-mpnet-base-dot-v1", help="SentenceTransformer model name")
    
    args = parser.parse_args()
    
    add_documents(
        input_dir=args.input_dir,
        collection_name=args.collection,
        chromadb_host=args.host,
        chromadb_port=args.port,
        model_name=args.model
    )