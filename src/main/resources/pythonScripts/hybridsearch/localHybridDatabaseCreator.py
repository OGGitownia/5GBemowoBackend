import faiss
import sqlite3
import json
import numpy as np
import os

# Stałe ścieżki do plików
JSON_PATH = r"C:\gitRepositories\5GBemowo-Backend\src\main\resources\norms\embeeded36331-e60.json"
DB_DIR = "data"
DB_PATH = os.path.join(DB_DIR, "hybrid_db.sqlite")
FAISS_INDEX_PATH = os.path.join(DB_DIR, "hybrid_db.index")


def create_hybrid_database():
    print("=== Rozpoczynam tworzenie bazy hybrydowej ===")

    # Tworzenie katalogu na pliki, jeśli nie istnieje
    if not os.path.exists(DB_DIR):
        print(f"Katalog '{DB_DIR}' nie istnieje. Tworzenie katalogu...")
        os.makedirs(DB_DIR)

    # Sprawdzenie czy plik JSON istnieje
    if not os.path.exists(JSON_PATH):
        print(f"Błąd: Plik JSON '{JSON_PATH}' nie istnieje")
        return

    print(f"Wczytywanie danych z pliku JSON: {JSON_PATH}")

    try:
        with open(JSON_PATH, "r", encoding="utf-8") as f:
            data = json.load(f)
    except Exception as e:
        print(f"Błąd podczas wczytywania JSON: {e}")
        return

    # Sprawdzanie czy struktura JSON jest poprawna
    if "fragments" not in data:
        print("Błąd: JSON nie zawiera klucza 'fragments'. Sprawdź strukturę pliku.")
        return

    sentences = []
    embeddings = []

    print("Przetwarzanie danych JSON...")

    for entry in data["fragments"]:
        if "content" not in entry or "embeddedContent" not in entry:
            print("Błąd: Brak wymaganych pól ('content' lub 'embeddedContent') w JSON")
            return
        sentences.append(entry["content"])
        embeddings.append(entry["embeddedContent"])

    embeddings = np.array(embeddings).astype('float32')

    if len(embeddings) == 0:
        print("Błąd: Brak embeddingów w pliku JSON")
        return

    dimension = embeddings.shape[1]
    print(f"Liczba zdań: {len(sentences)}, Wymiar embeddingów: {dimension}")

    # Tworzenie FAISS
    print("Tworzenie indeksu FAISS...")
    try:
        index = faiss.IndexFlatL2(dimension)
        index.add(embeddings)
        faiss.write_index(index, FAISS_INDEX_PATH)
        print(f"Indeks FAISS zapisany do: {FAISS_INDEX_PATH}")
    except Exception as e:
        print(f"Błąd podczas tworzenia FAISS: {e}")
        return

    # Tworzenie SQLite
    print("Sprawdzanie bazy SQLite...")

    if os.path.exists(DB_PATH):
        print(f"Usuwanie istniejącej bazy: {DB_PATH}")
        try:
            os.remove(DB_PATH)
        except Exception as e:
            print(f"Błąd podczas usuwania starej bazy SQLite: {e}")
            return

    try:
        print(f"Tworzenie nowej bazy danych SQLite: {DB_PATH}")
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()

        print("Tworzenie tabeli 'documents' w SQLite...")
        cursor.execute("CREATE TABLE documents (id INTEGER PRIMARY KEY, sentence TEXT)")

        print("Wstawianie danych do SQLite...")
        for idx, sentence in enumerate(sentences):
            cursor.execute("INSERT INTO documents (id, sentence) VALUES (?, ?)", (idx, sentence))

        conn.commit()
        conn.close()
        print(f"Baza SQLite zapisana: {DB_PATH}")
    except Exception as e:
        print(f"Błąd podczas tworzenia bazy SQLite: {e}")
        return

    print("=== Proces zakończony pomyślnie ===")


# Uruchomienie funkcji
create_hybrid_database()
