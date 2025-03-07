import json
import os
import signal
import sys

import faiss
import numpy as np
import requests
from flask import Flask, request, jsonify
from whoosh.fields import Schema, TEXT, ID
from whoosh.index import create_in

sys.stdin.reconfigure(encoding='utf-8')
sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

app = Flask(__name__)

def notify_spring_boot():
    try:
        requests.post("http://localhost:8080/hybrid/server-started")
        print("Wysłano informację do Spring Boot: Serwer Python działa!")
    except requests.exceptions.RequestException as e:
        print(f"Nie udało się wysłać powiadomienia: {e}")

def create_faiss_index(json_path, hybrid_db_path):
    print("run create_faiss_index!")
    with open(json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)["fragments"]

    embedding_dim = len(data[0]["embeddedContent"])  # Zakładamy, że wszystkie embeddingi mają ten sam wymiar
    index = faiss.IndexFlatL2(embedding_dim)

    vectors = []
    texts = []
    for fragment in data:
        vectors.append(fragment["embeddedContent"])
        texts.append(fragment["content"])

    index.add(np.array(vectors, dtype='float32'))
    faiss.write_index(index, os.path.join(hybrid_db_path, "faiss.index"))

    return texts

def create_whoosh_index(hybrid_db_path, texts):
    print("create_whoosh_index")
    schema = Schema(id=ID(stored=True), content=TEXT(stored=True))
    if not os.path.exists(hybrid_db_path):
        os.makedirs(hybrid_db_path)

    ix = create_in(hybrid_db_path, schema)
    writer = ix.writer()
    for i, text in enumerate(texts):
        writer.add_document(id=str(i), content=text)
    writer.commit()

def build_hybrid_database(json_path, hybrid_db_path):
    print("build_hybrid_database", flush=True)
    texts = create_faiss_index(json_path, hybrid_db_path)
    create_whoosh_index(hybrid_db_path, texts)

    try:
        requests.post("http://localhost:8080/hybrid/build-complete", json={"message": "Baza hybrydowa została utworzona!"})
        print("Powiadomienie: budowa bazy hybrydowej zakończona!", flush=True)
    except requests.exceptions.RequestException as e:
        print(f"Nie udało się wysłać powiadomienia: {e}", flush=True)

@app.route('/status', methods=['GET'])
def status():
    return "Serwer Python działa!", 200

@app.route('/handle-queue', methods=['POST'])
def process_item():
    try:
        data = request.get_json()
        if not data or "jsonPath" not in data or "hybridDbPath" not in data:
            return jsonify({"error": "Brakuje wymaganych ścieżek"}), 400

        json_path = data["jsonPath"]
        hybrid_db_path = data["hybridDbPath"]

        if not os.path.isfile(json_path):
            return jsonify({"error": "Plik JSON nie istnieje"}), 400
        if not os.path.isdir(hybrid_db_path):
            os.makedirs(hybrid_db_path)

        # Wykonanie funkcji synchronicznie (bez wątku)
        build_hybrid_database(json_path, hybrid_db_path)

        return jsonify({"message": "Budowa bazy hybrydowej zakończona"}), 200
    except Exception as e:
        return jsonify({"error": f"Błąd serwera: {e}"}), 500

@app.route('/shutdown', methods=['POST'])
def shutdown():
    print("Zatrzymuję serwer Flask...")
    os.kill(os.getpid(), signal.SIGTERM)  # Wysyła sygnał do zamknięcia
    return jsonify({"message": "Serwer Flask zatrzymany"}), 200

if __name__ == '__main__':
    notify_spring_boot()
    app.run(host='0.0.0.0', port=5000)
