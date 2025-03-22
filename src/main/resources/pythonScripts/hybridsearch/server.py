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

# Kodowanie
sys.stdin.reconfigure(encoding='utf-8')
sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

app = Flask(__name__)


def notify_spring_boot():
    try:
        requests.post("http://localhost:8080/hybrid/server-started")
        print("✅ Wysłano informację do Spring Boot: Serwer Python działa!", flush=True)
    except requests.exceptions.RequestException as e:
        print(f"❌ Nie udało się wysłać powiadomienia: {e}", flush=True)


def create_faiss_index(json_path, hybrid_db_path):
    print("📦 Uruchamiam FAISS index...", flush=True)

    with open(json_path, 'r', encoding='utf-8') as f:
        full_data = json.load(f)

    if "fragments" not in full_data:
        raise ValueError("Brak klucza 'fragments' w pliku JSON")

    data = full_data["fragments"]
    if not data:
        raise ValueError("Brak danych wewnątrz 'fragments'")

    vectors = []
    texts = []

    for fragment in data:
        if "embeddedContent" not in fragment or "content" not in fragment:
            print("⚠️ Pominięto fragment bez 'content' lub 'embeddedContent'", flush=True)
            continue
        vectors.append(fragment["embeddedContent"])
        texts.append(fragment["content"])

    if not vectors:
        raise ValueError("Brak embeddingów do zapisania w FAISS")

    vectors_np = np.array(vectors, dtype='float32')
    dimension = vectors_np.shape[1]

    index = faiss.IndexFlatL2(dimension)
    index.add(vectors_np)

    if not os.path.exists(hybrid_db_path):
        os.makedirs(hybrid_db_path)

    index_path = os.path.join(hybrid_db_path, "hybrid_db.index")
    faiss.write_index(index, index_path)

    print(f"✅ Indeks FAISS zapisany do: {index_path}", flush=True)

    return texts


def create_whoosh_index(hybrid_db_path, texts):
    print("📚 Tworzenie indeksu Whoosh...", flush=True)

    schema = Schema(id=ID(stored=True), content=TEXT(stored=True))
    if not os.path.exists(hybrid_db_path):
        os.makedirs(hybrid_db_path)

    ix = create_in(hybrid_db_path, schema)
    writer = ix.writer()

    for i, text in enumerate(texts):
        writer.add_document(id=str(i), content=text)

    writer.commit()

    print("✅ Indeks Whoosh zapisany.", flush=True)


def build_hybrid_database(json_path, hybrid_db_path):
    print("🔧 Rozpoczynam budowę bazy hybrydowej...", flush=True)

    texts = create_faiss_index(json_path, hybrid_db_path)
    create_whoosh_index(hybrid_db_path, texts)

    try:
        requests.post("http://localhost:8080/hybrid/build-complete", json={"message": "Baza hybrydowa została utworzona!"})
        print("✅ Powiadomienie o zakończeniu wysłane do Spring Boot.", flush=True)
    except requests.exceptions.RequestException as e:
        print(f"⚠️ Błąd powiadamiania Spring Boot: {e}", flush=True)


@app.route('/status', methods=['GET'])
def status():
    return "Serwer Python działa!", 200


@app.route('/handle-queue', methods=['POST'])
def process_item():
    try:
        data = request.get_json()
        if not data or "jsonPath" not in data or "hybridDbPath" not in data:
            return jsonify({"error": "Brakuje wymaganych ścieżek: jsonPath, hybridDbPath"}), 400

        json_path = data["jsonPath"]
        hybrid_db_path = data["hybridDbPath"]

        if not os.path.isfile(json_path):
            return jsonify({"error": f"Plik JSON nie istnieje: {json_path}"}), 400

        build_hybrid_database(json_path, hybrid_db_path)

        return jsonify({"message": "Budowa bazy hybrydowej zakończona"}), 200

    except Exception as e:
        print(f"❌ Błąd serwera: {e}", flush=True)
        return jsonify({"error": f"Błąd serwera: {e}"}), 500


@app.route('/shutdown', methods=['POST'])
def shutdown():
    print("🛑 Zatrzymuję serwer Flask...", flush=True)
    os.kill(os.getpid(), signal.SIGTERM)
    return jsonify({"message": "Serwer Flask zatrzymany"}), 200


if __name__ == '__main__':
    notify_spring_boot()
    app.run(host='0.0.0.0', port=5000)
