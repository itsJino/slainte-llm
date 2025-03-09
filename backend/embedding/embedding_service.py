from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer

app = Flask(__name__)
model = SentenceTransformer("multi-qa-mpnet-base-dot-v1")

@app.route("/embed", methods=["POST"])
def embed_text():
    data = request.json
    text = data.get("text", "")
    embedding = model.encode([text]).tolist()[0]  # Single vector
    return jsonify({"embedding": embedding})

if __name__ == "__main__":
    app.run(port=5000)
