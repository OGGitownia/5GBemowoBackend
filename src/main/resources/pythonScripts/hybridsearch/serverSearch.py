import faiss
import sqlite3
import numpy as np
import os
import json
import traceback
from flask import Flask, request, jsonify
import sys
import requests
import threading
import time

sys.stdout.reconfigure(encoding='utf-8')

# Stałe ścieżki do plików
DB_DIR = "src/main/resources/pythonScripts/hybridsearch/data"
DB_PATH = os.path.join(DB_DIR, "hybrid_db.sqlite")
FAISS_INDEX_PATH = os.path.join(DB_DIR, "hybrid_db.index")
SPRING_BOOT_READY_URL = "http://localhost:8080/hybrid-service/search-server-ready"

app = Flask(__name__)

# Globalne zmienne dla indeksów
faiss_index = None
documents = None


def load_hybrid_database():
    """Wczytuje indeksy FAISS i SQLite do pamięci"""
    global faiss_index, documents


    if not os.path.exists("src/main/resources/pythonScripts/hybridsearch/data/hybrid_db.sqlite"):
        print(DB_PATH, " & ",FAISS_INDEX_PATH)

    if not os.path.exists(DB_PATH) or not os.path.exists(FAISS_INDEX_PATH):
        print("Błąd: Pliki bazy danych nie istnieją! Uruchom najpierw skrypt tworzący bazę.")
        return False

    print("Wczytywanie bazy FAISS i SQLite...")

    try:
        # Wczytanie FAISS
        faiss_index = faiss.read_index(FAISS_INDEX_PATH)

        # Wczytanie SQLite i pobranie zdań
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("SELECT id, sentence FROM documents")
        documents = {row[0]: row[1] for row in cursor.fetchall()}  # Mapowanie ID → Tekst
        conn.close()

        print("Baza załadowana poprawnie! 2")
        return True
    except Exception as e:
        print(f"Błąd podczas wczytywania bazy: {e}")
        print(traceback.format_exc())
        return False


def text_to_embedding(text, dimension):
    """Symuluje przekształcenie tekstu na embedding."""
    np.random.seed(abs(hash(text)) % (2**32))  # Deterministyczne embeddingi dla powtarzalności
    return np.random.rand(dimension).astype("float32")


def search_faiss(query_text, num_results=3):
    """Wykonuje wyszukiwanie w FAISS"""
    print(f"Wyszukiwanie FAISS dla: {query_text} (top-{num_results} wyników)")

    if faiss_index is None or documents is None:
        return {"error": "Baza hybrydowa nie została poprawnie załadowana."}

    query_embedding = text_to_embedding(query_text, faiss_index.d)
    _, indices = faiss_index.search(np.array([query_embedding]), k=num_results)

    results = [documents[i] for i in indices[0] if i in documents]
    return results


@app.route('/search', methods=['POST'])
def search():
    print("Coś przyszło", flush=True)  # Sprawdzenie, czy Flask odbiera zapytanie

    try:
        data = request.get_json()
        print(f"Odebrany JSON: {data}", flush=True)  # Logujemy otrzymane dane

        if not isinstance(data, dict):
            print("BŁĄD: Otrzymano niepoprawny format JSON", flush=True)
            return jsonify({"error": "Niepoprawny format JSON"}), 400

        query_text = data.get("query", "").strip()

        if not query_text:
            print("BŁĄD: Brak zapytania 'query' w JSON", flush=True)
            return jsonify({"error": "Zapytanie nie może być puste"}), 400

        num_results = int(data.get("num_results", 3))

        print(f"Otrzymano zapytanie: {query_text}", flush=True)

        # Sprawdzenie czy FAISS jest załadowany
        if faiss_index is None:
            print("BŁĄD: FAISS Index nie został poprawnie załadowany.", flush=True)
            return jsonify({"error": "FAISS Index nie został załadowany"}), 500

        if documents is None:
            print("BŁĄD: Brak wczytanych dokumentów.", flush=True)
            return jsonify({"error": "Baza dokumentów nie została załadowana"}), 500

        results = search_faiss(query_text, num_results)

        print(f"Wyniki wyszukiwania zwrócone do Spring Boot: {results}", flush=True)
        return jsonify({"results": results}), 200

    except Exception as e:
        print(f"BŁĄD przetwarzania zapytania: {e}", flush=True)
        print(traceback.format_exc(), flush=True)  # Pełne logowanie błędu
        return jsonify({"error": f"Błąd serwera: {e}"}), 500







def notify_spring_boot_ready():
    time.sleep(5)
    try:
        response = requests.post(SPRING_BOOT_READY_URL, timeout=5)
        if response.status_code == 200:
            print("Serwer wyszukiwania powiadomił Spring Boot o gotowości.")
        else:
            print(f"Błąd powiadamiania Spring Boot: {response.status_code}, {response.text}")
    except requests.exceptions.RequestException as e:
        print(f"Błąd połączenia z Spring Boot: {e}")



@app.route('/shutdown', methods=['POST'])
def shutdown():
    print("Otrzymano żądanie zamknięcia siem")
    os._exit(0)


if __name__ == '__main__':
    print("Uruchamianie serwera wyszukiwania...")
    if load_hybrid_database():
        print(" udało się załadować bazy!")

    else:
        print("Nie udało się załadować bazy!")
    threading.Thread(target=notify_spring_boot_ready, daemon=True).start()
    app.run(host='0.0.0.0', port=5001, debug=False)
    print("2")

    print("3")

