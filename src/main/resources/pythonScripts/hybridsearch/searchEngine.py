import json
import os
import sys
import faiss
import numpy as np
import requests
from flask import Flask, request, jsonify
from whoosh.index import open_dir
from whoosh.qparser import QueryParser

# Ustawienia
HYBRID_DB_PATH = "C:/gitRepositories/5GBemowo-Backend/src/main/resources/hybrid"

SPRING_BOOT_URL = "http://localhost:8080/hybrid-service/search-server-ready"

app = Flask(__name__)

# Globalne zmienne do indeksów
faiss_index = None
whoosh_index = None
texts = []

def load_hybrid_database():
    """Ładuje indeksy FAISS i Whoosh do pamięci"""
    global faiss_index, whoosh_index, texts

    try:
        print("🔄 Ładowanie bazy FAISS...", flush=True)
        faiss_index = faiss.read_index(os.path.join(HYBRID_DB_PATH, "faiss.index"))

        print("🔄 Ładowanie bazy Whoosh...", flush=True)
        whoosh_index = open_dir(HYBRID_DB_PATH)

        # Wczytaj teksty, aby mapować wyniki FAISS do oryginalnych fragmentów
        with open(os.path.join(HYBRID_DB_PATH, "data.json"), "r", encoding="utf-8") as f:
            texts = [fragment["content"] for fragment in json.load(f)["fragments"]]

        print("✅ Baza hybrydowa załadowana!", flush=True)
        notify_spring_boot()

    except Exception as e:
        print(f"❌ Błąd ładowania bazy: {e}", flush=True)

def notify_spring_boot():
    """Wysyła informację do Spring Boot, że serwer jest gotowy"""
    try:
        requests.post(SPRING_BOOT_URL)
        print("✅ Wysłano powiadomienie do Spring Boot: Serwer wyszukiwania jest gotowy!", flush=True)
    except requests.exceptions.RequestException as e:
        print(f"❌ Błąd wysyłania powiadomienia do Spring Boot: {e}", flush=True)

@app.route('/status', methods=['GET'])
def status():
    """Sprawdza status serwera"""
    return jsonify({"status": "OK", "message": "Serwer wyszukiwania działa!"}), 200

@app.route('/search', methods=['POST'])
def search():
    """Obsługuje zapytania wysyłane przez Spring Boot"""
    try:
        data = request.get_json()
        query_text = data.get("query", "").strip()

        if not query_text:
            return jsonify({"error": "Zapytanie nie może być puste"}), 400

        print(f"🔍 Otrzymano zapytanie: {query_text}", flush=True)

        faiss_results = search_faiss(query_text)
        whoosh_results = search_whoosh(query_text)

        combined_results = {
            "faiss": faiss_results,
            "whoosh": whoosh_results
        }

        print(f"✅ Wyniki wyszukiwania zwrócone do Spring Boot!", flush=True)
        return jsonify(combined_results), 200

    except Exception as e:
        print(f"❌ Błąd przetwarzania zapytania: {e}", flush=True)
        return jsonify({"error": f"Błąd serwera: {e}"}), 500

def search_faiss(query_text):
    """Symuluje wyszukiwanie w FAISS"""
    try:
        print("🔄 Wyszukiwanie w FAISS...", flush=True)

        # 🔧 Tutaj powinieneś zamienić na prawdziwy embedding query_text
        fake_embedding = np.random.rand(faiss_index.d).astype("float32")

        _, indices = faiss_index.search(np.array([fake_embedding]), k=3)  # Znajdujemy 3 najbardziej podobne wyniki

        results = [texts[i] for i in indices[0] if i >= 0]
        print(f"✅ FAISS zwrócił {len(results)} wyników", flush=True)
        return results

    except Exception as e:
        print(f"❌ Błąd FAISS: {e}", flush=True)
        return []

def search_whoosh(query_text):
    """Wyszukuje w indeksie Whoosh"""
    try:
        print("🔄 Wyszukiwanie w Whoosh...", flush=True)
        with whoosh_index.searcher() as searcher:
            parser = QueryParser("content", whoosh_index.schema)
            query = parser.parse(query_text)

            results = [hit["content"] for hit in searcher.search(query, limit=3)]
            print(f"✅ Whoosh zwrócił {len(results)} wyników", flush=True)
            return results

    except Exception as e:
        print(f"❌ Błąd Whoosh: {e}", flush=True)
        return []

if __name__ == '__main__':
    print("🚀 Uruchamianie serwera wyszukiwania...", flush=True)
    load_hybrid_database()
    app.run(host='0.0.0.0', port=5001)
