import re
import os
import glob
import pdfplumber
import chromadb
from sentence_transformers import SentenceTransformer

# ✅ Path to your root knowledge documents directory
ROOT_DOCS_DIR = os.path.abspath("../")

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
        r"Cookie settings.*?Executive",
        r"Back to Health A to Z",
        r"Page last reviewed:.*?Next review due:.*?2024",
        r"This project has received funding.*?Number 123\.",
        r"^Menu\s*"
    ]
    
    # Apply each pattern
    cleaned_text = text
    for pattern in patterns_to_remove:
        cleaned_text = re.sub(pattern, "", cleaned_text, flags=re.DOTALL)
    
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

def add_document(text, doc_id, category=None):
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
    print(f"\n🧹 Cleaned Text Sample from {doc_id}:\n{cleaned_text[:300]}...\n")
    
    chunks = chunk_text(cleaned_text)  # ✅ Split into smaller retrievable chunks
    
    if not chunks:
        print(f"⚠️ No chunks generated for document: {doc_id}")
        return
        
    chunk_count = 0
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
            
            # Add category information if available
            if category:
                metadata["category"] = category
            
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
            chunk_count += 1

    print(f"✅ Document {doc_id} indexed with {chunk_count} chunks")
    return chunk_count

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

def load_documents_from_directory(directory_path, category=None):
    """Process all PDF files in a given directory"""
    print(f"📂 Loading documents from: {directory_path}")
    
    pdf_files = glob.glob(os.path.join(directory_path, "*.pdf"))
    
    if not pdf_files:
        print(f"❌ No PDF files found in {directory_path}")
        return 0
    
    total_chunks = 0
    for file_path in pdf_files:
        try:
            # Create a document ID that includes the category path
            relative_path = os.path.relpath(file_path, ROOT_DOCS_DIR)
            doc_id = relative_path.replace("\\", "/")  # Standardize path separators
            
            content = extract_text_from_pdf(file_path)
            
            if content:
                print(f"\n📄 Processing {doc_id} ({len(content)} characters)")
                chunks_added = add_document(content, doc_id, category)
                if chunks_added:
                    total_chunks += chunks_added
            else:
                print(f"⚠️ No content extracted from {doc_id}")
                
        except Exception as e:
            print(f"❌ Error processing {file_path}: {e}")
    
    return total_chunks

def recursively_load_directories(start_dir, category=None):
    """Recursively process all subdirectories containing PDFs"""
    total_chunks = 0
    
    # First process files in the current directory
    dir_chunks = load_documents_from_directory(start_dir, category)
    total_chunks += dir_chunks
    
    # Then recursively process subdirectories
    for item in os.listdir(start_dir):
        item_path = os.path.join(start_dir, item)
        if os.path.isdir(item_path):
            # Build category path for better organization
            sub_category = item if category is None else f"{category}/{item}"
            sub_chunks = recursively_load_directories(item_path, sub_category)
            total_chunks += sub_chunks
    
    return total_chunks

def search_documents(query, top_k=3, category=None):
    """Retrieve relevant document chunks using similarity search"""
    if not query.strip():
        return "Please provide a search query."
        
    embedding = generate_embedding(query)
    
    # Set up filter if category is specified
    filter_dict = None
    if category:
        filter_dict = {"category": {"$eq": category}}
    
    # Execute the query
    results = collection.query(
        query_embeddings=[embedding], 
        n_results=top_k,
        where=filter_dict
    )

    print(f"\n🔍 Searching for: {query}")
    if category:
        print(f"📁 Filtering by category: {category}")

    # Ensure there are results before extracting data
    if not results or "documents" not in results or not results["documents"] or not results["documents"][0]:
        return "❌ No relevant information found."

    formatted_results = []
    for i, chunk in enumerate(results["documents"][0]):
        # Get metadata for this result
        metadata = results["metadatas"][0][i] if "metadatas" in results and i < len(results["metadatas"][0]) else {}
        
        # Format source information
        source_info = ""
        if metadata and "source" in metadata:
            source = metadata["source"]
            category = metadata.get("category", "")
            section = metadata.get("section", "")
            
            source_parts = []
            if category:
                source_parts.append(f"Category: {category}")
            if section:
                source_parts.append(f"Section: {section}")
            source_parts.append(f"Source: {source}")
            
            source_info = f"\n[{' | '.join(source_parts)}]"
        
        formatted_results.append(f"{chunk}{source_info}")
    
    # Join all formatted results
    merged_text = "\n\n---\n\n".join(formatted_results)
    return merged_text

def print_collection_stats():
    """Print statistics about the current collection"""
    try:
        count = collection.count()
        print(f"\n📊 Collection Statistics:")
        print(f"Total documents in collection: {count}")
        
        # Get sample entries
        sample = collection.peek(5)
        if sample and "ids" in sample and sample["ids"]:
            print(f"Sample document IDs: {sample['ids']}")
            
            # Count documents by category
            if "metadatas" in sample and sample["metadatas"]:
                categories = {}
                for metadata_list in sample["metadatas"]:
                    for metadata in metadata_list:
                        if "category" in metadata:
                            cat = metadata["category"]
                            categories[cat] = categories.get(cat, 0) + 1
                
                if categories:
                    print("Categories found in sample:")
                    for cat, count in categories.items():
                        print(f"  - {cat}: {count} documents")
        
    except Exception as e:
        print(f"❌ Error getting collection stats: {e}")

# ✅ Main execution
if __name__ == "__main__":
    # Process the directory structure seen in your screenshot
    services_dir = os.path.join(ROOT_DOCS_DIR, "hse_services_pdf", "eng", "services")
    
    print("🔄 Starting document processing...")
    
    # Process specific directories based on your screenshot
    directories_to_process = [
        {"path": os.path.join(services_dir, "healthcare-in-ireland"), "category": "healthcare-in-ireland"},
        {"path": os.path.join(services_dir, "mhml"), "category": "mhml"},
        {"path": os.path.join(services_dir, "publications"), "category": "publications"},
        {"path": os.path.join(services_dir, "yourhealthservice"), "category": "yourhealthservice"},
        # Add more directories as needed
    ]
    
    total_docs = 0
    for dir_info in directories_to_process:
        print(f"\n📁 Processing directory: {dir_info['path']}")
        chunks = load_documents_from_directory(dir_info["path"], dir_info["category"])
        print(f"Added {chunks} chunks from {dir_info['category']}")
        total_docs += chunks
    
    print(f"\n✅ Processing complete! Added a total of {total_docs} chunks to the collection.")
    
    # Print collection statistics
    print_collection_stats()
    
    # Run some test queries
    test_queries = [
        {"query": "What healthcare services are available in Ireland?", "category": "healthcare-in-ireland"},
        {"query": "How can I access mental health services?", "category": "mhml"},
        {"query": "What are the latest health publications?", "category": None}  # None means search all categories
    ]
    
    print("\n🔍 Testing search functionality:")
    for test in test_queries:
        query = test["query"]
        category = test.get("category")
        
        cat_info = f" in category '{category}'" if category else ""
        print(f"\n🔎 Searching for: '{query}'{cat_info}")
        
        results = search_documents(query, top_k=2, category=category)
        print(f"Results:\n{results}\n")
        print("-" * 80)