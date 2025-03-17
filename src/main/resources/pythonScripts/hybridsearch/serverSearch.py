import faiss
import sqlite3
import numpy as np
import os
import traceback
import requests
import sys
import socket
import time
import threading
from flask import Flask, request, jsonify
import sys
from sentence_transformers import SentenceTransformer

sys.stdout.reconfigure(encoding='utf-8')


server_name = sys.argv[1] if len(sys.argv) > 1 else "HybridSearchServer"
port = int(sys.argv[2]) if len(sys.argv) > 2 else 5001
SPRING_BOOT_READY_URL = "http://localhost:8080/{}/server-ready".format(server_name)

app = Flask(server_name)

DB_DIR = "src/main/resources/pythonScripts/hybridsearch/data2"
DB_PATH = os.path.join(DB_DIR, "hybrid_db.sqlite")
FAISS_INDEX_PATH = os.path.join(DB_DIR, "hybrid_db.index")


faiss_index = None
documents = None

def notify_spring_boot():
    time.sleep(5)
    """Powiadamia Spring Boot o gotowości serwera."""
    try:
        response = requests.post(SPRING_BOOT_READY_URL, json={"server_name": server_name, "port": port})
        print(f"Powiadomiono Spring Boot: {server_name} działa na porcie {port}. Status: {response.status_code}")
    except requests.exceptions.RequestException as e:
        print(f"Błąd powiadamiania Spring Boot: {e}")

def load_hybrid_database():
    global faiss_index, documents

    if not os.path.exists(DB_PATH) or not os.path.exists(FAISS_INDEX_PATH):
        print("Błąd: Pliki bazy danych nie istnieją! Uruchom najpierw skrypt tworzący bazę")
        return False

    print("Wczytywanie bazy FAISS i SQLite")

    try:
        faiss_index = faiss.read_index(FAISS_INDEX_PATH)

        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("SELECT id, sentence FROM documents")
        documents = {row[0]: row[1] for row in cursor.fetchall()}
        conn.close()

        print("Baza załadowana poprawnie!")
        return True
    except Exception as e:
        print(f"Błąd podczas wczytywania bazy: {e}")
        print(traceback.format_exc())
        return False


model = SentenceTransformer("BAAI/bge-m3")

def text_to_embedding(text, dimension):

    if not text or not isinstance(text, str):
        raise ValueError("Podany tekst jest pusty lub ma nieprawidłowy format")

    embedding = model.encode(text)
    embedding = np.array(embedding, dtype=np.float32)


    if embedding.shape[0] != dimension:
        raise ValueError(f"Nieprawidłowy wymiar embeddingu: {embedding.shape[0]} zamiast {dimension}")

    return embedding

def search_faiss(query_text, num_results=3):
    print(f"Wyszukiwanie FAISS dla: {query_text} (top-{num_results} wyników)")

    if faiss_index is None or documents is None:
        return {"error": "Baza hybrydowa nie została poprawnie załadowana"}

    query_embedding = text_to_embedding(query_text, faiss_index.d)
    _, indices = faiss_index.search(np.array([query_embedding]), k=num_results)

    results = [documents[i] for i in indices[0] if i in documents]
    return results

@app.route(f'/{server_name}/process', methods=['POST'])
def process_request():
    try:
        print("Odebrano zapytanie do serwera!")

        raw_data = request.data.decode("utf-8").strip()
        print(f"Otrzymany tekst: {raw_data}")

        if not raw_data:
            return jsonify({"error": "Zapytanie nie może być puste"}), 400

        print(f"Wyszukiwanie dla zapytania: '{raw_data}'")
        results = search_faiss(raw_data)

        return jsonify({"results": results}), 200

    except Exception as e:
        print(f"BŁĄD przetwarzania zapytania: {e}")
        print(traceback.format_exc())
        return jsonify({"error": f"Błąd serwera: {e}"}), 500



@app.route(f'/{server_name}/shutdown', methods=['POST'])
def shutdown():
    print(f"Otrzymano żądanie zamknięcia serwera {server_name}")
    func = request.environ.get('werkzeug.server.shutdown')
    if func is None:
        raise RuntimeError("Nie można zamknąć serwera.")
    func()
    return "Serwer zamknięty", 200

if __name__ == '__main__':
    print(f"Uruchamianie serwera {server_name}...")
    if load_hybrid_database():
        print("Baza danych załadowana poprawnie000000000!")
        print(server_name)
    else:
        print("Nie udało się załadować bazy!")

    threading.Thread(target=notify_spring_boot, daemon=True).start()

    app.run(host='0.0.0.0', port=port, debug=False)