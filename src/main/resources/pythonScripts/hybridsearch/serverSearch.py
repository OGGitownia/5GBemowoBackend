import os
import faiss
import sqlite3
import numpy as np
from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer
import traceback
import requests
import threading
import time
import sys

sys.stdout.reconfigure(encoding='utf-8')

# === CONFIGURATION ===
server_name = sys.argv[1] if len(sys.argv) > 1 else "hybrid_search_server"
port = int(sys.argv[2]) if len(sys.argv) > 2 else 5003
SPRING_BOOT_NOTIFY = f"http://localhost:8080/{server_name}/server-ready"

# === FLASK APP SETUP ===
app = Flask(server_name)

# === MODEL ===
model = SentenceTransformer("BAAI/bge-m3")

# === DATABASE CACHE ===
loaded_faiss = {}
loaded_documents = {}

# === HELPERS ===

def notify_spring_boot():
    print("Notifying Spring Boot that server is ready", flush=True)
    try:
        response = requests.post(SPRING_BOOT_NOTIFY, json={"port": port})
        print(f"Spring Boot responded: {response.status_code} {response.text}", flush=True)
    except Exception as e:
        print(f"Notification error: {e}", flush=True)

def load_hybrid_database(base_path):
    if base_path in loaded_faiss:
        return loaded_faiss[base_path], loaded_documents[base_path]

    index_path = os.path.join(base_path, "hybrid_db.index")
    sqlite_path = os.path.join(base_path, "hybrid_db.sqlite")

    if not os.path.exists(index_path) or not os.path.exists(sqlite_path):
        raise FileNotFoundError("Hybrid database files not found.")

    faiss_index = faiss.read_index(index_path)
    conn = sqlite3.connect(sqlite_path)
    cursor = conn.cursor()
    cursor.execute("SELECT id, sentence FROM documents")
    documents = {row[0]: row[1] for row in cursor.fetchall()}
    conn.close()

    loaded_faiss[base_path] = faiss_index
    loaded_documents[base_path] = documents

    return faiss_index, documents

def text_to_embedding(text, dimension):
    if not text or not isinstance(text, str):
        raise ValueError("Invalid input text.")
    embedding = model.encode(text)
    embedding = np.array(embedding, dtype=np.float32)
    if embedding.shape[0] != dimension:
        raise ValueError(f"Expected embedding dimension {dimension}, got {embedding.shape[0]}")
    return embedding

def search_faiss(base_path, query, top_k=3):
    faiss_index, documents = load_hybrid_database(base_path)
    query_embedding = text_to_embedding(query, faiss_index.d)
    _, indices = faiss_index.search(np.array([query_embedding]), k=top_k)
    results = [documents[i] for i in indices[0] if i in documents]
    return results

# === ROUTES ===

@app.route(f"/{server_name}/process", methods=["POST"])
def process_embedding_request():
    try:
        data = request.get_json()
        query = data.get("query")
        base_path = data.get("basePath")

        if not query or not base_path:
            return jsonify({"error": "Missing 'query' or 'basePath' in request."}), 400

        results = search_faiss(base_path, query)
        return jsonify({"results": results}), 200

    except Exception as e:
        print(traceback.format_exc(), flush=True)
        return jsonify({"error": str(e)}), 500

@app.route(f"/{server_name}/shutdown", methods=["POST"])
def shutdown():
    def shutdown_server():
        os._exit(0)
    threading.Thread(target=shutdown_server).start()
    return jsonify({"message": "Shutting down server"}), 200

@app.route("/status", methods=["GET"])
def status():
    return jsonify({"status": "running"}), 200

# === START ===

def run_server():
    threading.Thread(target=notify_spring_boot).start()
    print(f"Server running on port {port}", flush=True)
    app.run(host="0.0.0.0", port=port)

if __name__ == "__main__":
    run_server()
