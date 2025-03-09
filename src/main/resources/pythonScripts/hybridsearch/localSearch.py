import faiss
import sqlite3
import numpy as np
import os


# Stałe ścieżki do plików
DB_DIR = "data"
DB_PATH = os.path.join(DB_DIR, "hybrid_db.sqlite")
FAISS_INDEX_PATH = os.path.join(DB_DIR, "hybrid_db.index")


def load_hybrid_database():
    """Wczytuje indeksy FAISS i SQLite do pamięci"""
    if not os.path.exists(DB_PATH) or not os.path.exists(FAISS_INDEX_PATH):
        print("Błąd: Pliki bazy danych nie istnieją! Uruchom najpierw skrypt tworzący bazę.")
        return None, None

    print("🔄 Wczytywanie bazy FAISS i SQLite...")

    # Wczytanie FAISS
    faiss_index = faiss.read_index(FAISS_INDEX_PATH)

    # Wczytanie SQLite i pobranie zdań
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT id, sentence FROM documents")
    documents = {row[0]: row[1] for row in cursor.fetchall()}  # Mapowanie ID → Tekst
    conn.close()

    print("✅ Baza załadowana poprawnie!")
    return faiss_index, documents


def text_to_embedding(text, dimension):
    """Symuluje przekształcenie tekstu na embedding. W rzeczywistości powinien być model NLP."""
    np.random.seed(abs(hash(text)) % (2**32))  # Deterministyczne embeddingi dla powtarzalności
    return np.random.rand(dimension).astype("float32")


def search_faiss(query_text, faiss_index, documents):
    """Wykonuje wyszukiwanie w FAISS na podstawie zapytania"""
    print(f"🔍 Wyszukiwanie FAISS dla: {query_text}")

    # Tworzenie embeddingu zapytania
    query_embedding = text_to_embedding(query_text, faiss_index.d)

    # Znalezienie najbardziej podobnych wyników
    _, indices = faiss_index.search(np.array([query_embedding]), k=3)

    results = [documents[i] for i in indices[0] if i in documents]
    return results


def interactive_search():
    """Uruchamia interaktywne wyszukiwanie"""
    faiss_index, documents = load_hybrid_database()
    if faiss_index is None or documents is None:
        return

    while True:
        query_text = input("\n🔍 Wpisz zapytanie (lub 'exit' aby zakończyć): ").strip()
        if query_text.lower() == "exit":
            print("🚪 Zamykanie programu.")
            break

        results = search_faiss(query_text, faiss_index, documents)
        if results:
            print("✅ Wyniki wyszukiwania:")
            for i, result in enumerate(results, start=1):
                print(f"{i}. {result}")
        else:
            print("❌ Brak pasujących wyników.")


# Uruchomienie testowego wyszukiwania
interactive_search()
