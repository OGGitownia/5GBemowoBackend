from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer
import threading
import os
import signal
import sys
import json
import requests
import faiss
import sqlite3
import numpy as np
from flask import request, jsonify


sys.stdout.reconfigure(encoding='utf-8')

app = Flask(__name__)

server_name = sys.argv[1] if len(sys.argv) > 1 else "serverTemplete "
port = int(sys.argv[2]) if len(sys.argv) > 2 else 5003

SPRING_BOOT_NOTIFY = f"http://localhost:8080/{server_name}/server-ready"





@app.route(f"/{server_name}/process", methods=["POST"])
def process_embedding_request():
    print("process_embedding_request", flush=True)
    try:
        data = request.get_json()
        print(f"Dane wejściowe: {data}", flush=True)

        input_path = data.get("inputPath")
        output_path = data.get("outputPath")

        if not input_path or not output_path:
            return jsonify({"error": "Brakuje pola 'inputPath' lub 'outputPath'"}), 400

        if not os.path.exists(input_path):
            return jsonify({"error": f"Plik wejściowy nie istnieje: {input_path}"}), 400

        if not os.path.exists(output_path):
            print(f"Tworzę katalog: {output_path}", flush=True)
            os.makedirs(output_path)

        # Wczytaj dane – TERAZ lista obiektów, nie dict
        with open(input_path, "r", encoding="utf-8") as f:
            fragments = json.load(f)

        if not isinstance(fragments, list):
            return jsonify({"error": "Plik JSON nie jest listą fragmentów"}), 400

        sentences = []
        embeddings = []

        for entry in fragments:
            if "content" not in entry or "embeddedContent" not in entry:
                return jsonify({"error": "Niektóre fragmenty nie mają 'content' lub 'embeddedContent'"}), 400
            sentences.append(entry["content"])
            embeddings.append(entry["embeddedContent"])

        embeddings = np.array(embeddings).astype('float32')
        if embeddings.size == 0:
            return jsonify({"error": "Brak embeddingów"}), 400

        dimension = embeddings.shape[1]
        print(f"Liczba zdań: {len(sentences)}, Wymiar: {dimension}", flush=True)

        # FAISS
        index = faiss.IndexFlatL2(dimension)
        index.add(embeddings)
        index_path = os.path.join(output_path, "hybrid_db.index")
        faiss.write_index(index, index_path)
        print(f"Indeks FAISS zapisany: {index_path}", flush=True)

        # SQLite
        db_path = os.path.join(output_path, "hybrid_db.sqlite")
        if os.path.exists(db_path):
            os.remove(db_path)

        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        cursor.execute("CREATE TABLE documents (id INTEGER PRIMARY KEY, sentence TEXT)")

        for idx, sentence in enumerate(sentences):
            cursor.execute("INSERT INTO documents (id, sentence) VALUES (?, ?)", (idx, sentence))

        conn.commit()
        conn.close()
        print(f"Baza SQLite zapisana: {db_path}", flush=True)

        print("✅ Baza hybrydowa utworzona", flush=True)
        return jsonify({"message": "Hybrid database created successfully"}), 200

    except Exception as e:
        import traceback
        print("❌ Błąd podczas tworzenia bazy:", flush=True)
        print(traceback.format_exc(), flush=True)
        return jsonify({"error": str(e)}), 500




@app.route(f"/{server_name}/shutdown", methods=["POST"])
def shutdown():
    def shutdown_server():
        os.kill(os.getpid(), signal.SIGINT)
    response = jsonify({"message": "Shutting down server"})
    threading.Thread(target=shutdown_server).start()
    return response, 200

@app.route("/status", methods=["GET"])
def status():
    return jsonify({"status": "running"}), 200

def notify_spring_boot():
    print("Notifying Spring Boot that server is ready", flush=True)
    print(f"SPRING_BOOT_NOTIFY={SPRING_BOOT_NOTIFY}", flush=True)
    try:
        response = requests.post(SPRING_BOOT_NOTIFY, json={"port": port})
        print(f"Spring Boot responded: {response.status_code} {response.text}", flush=True)
    except Exception as e:
        print(f"Notification error: {e}", flush=True)

def run_server():
    notify_spring_boot()
    print(f"Server running on port {port}", flush=True)
    app.run(host="0.0.0.0", port=port)

if __name__ == "__main__":
    run_server()
