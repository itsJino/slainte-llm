from flask import Flask, request, jsonify
import requests

app = Flask(__name__)

# Ollama API endpoint for embeddings
OLLAMA_API_URL = "http://localhost:11434/api/embeddings"

def get_embedding(text):
    """Get embeddings using Ollama's nomic-embed-text model"""
    response = requests.post(
        OLLAMA_API_URL,
        json={"model": "nomic-embed-text", "prompt": text}
    )
    
    if response.status_code == 200:
        return response.json()["embedding"]
    else:
        raise Exception(f"Error from Ollama API: {response.text}")

@app.route("/embed", methods=["POST"])
def embed_endpoint():
    data = request.json
    text = data.get("text", "")
    
    if not text:
        return jsonify({"error": "No text provided"}), 400
    
    try:
        embedding = get_embedding(text)
        return jsonify({"embedding": embedding})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run(port=5000)