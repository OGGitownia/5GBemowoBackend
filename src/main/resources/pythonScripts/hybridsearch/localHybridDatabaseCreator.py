import faiss
import sqlite3
import json
import numpy as np
import os

# Stałe ścieżki do plików
JSON_PATH = r"C:\gitRepositories\5GBemowo-Backend\src\main\resources\norms3\embeeded36331-e60.json"
DB_DIR = "data3"
DB_PATH = os.path.join(DB_DIR, "hybrid_db.sqlite")
FAISS_INDEX_PATH = os.path.join(DB_DIR, "hybrid_db.index")


def create_hybrid_database():
    print("Plik do robienia bazy hybrydowej uruchomiony")

    if not os.path.exists(DB_DIR):
        print(f"Katalog '{DB_DIR}' nie istniał")
        os.makedirs(DB_DIR)

    if not os.path.exists(JSON_PATH):
        print(f"Błąd: '{JSON_PATH}' nie istnieje")
        return

    try:
        with open(JSON_PATH, "r", encoding="utf-8") as f:
            data = json.load(f)
    except Exception as e:
        print(f"Błąd : {e}")
        return

    if "fragments" not in data:
        print("Błąd: JSON nie zawiera klucza 'fragments'. Sprawdź strukturę pliku.")
        return

    sentences = []
    embeddings = []

    print("...")

    for entry in data["fragments"]:
        if "content" not in entry or "embeddedContent" not in entry:
            print("Błąd: Brak 'content' albo 'embeddedContent' w JSON")
            return
        sentences.append(entry["content"])
        embeddings.append(entry["embeddedContent"])

    embeddings = np.array(embeddings).astype('float32')

    if len(embeddings) == 0:
        print("Błąd: Brak embeddingów w JSON")
        return

    dimension = embeddings.shape[1]
    print("Liczba zdań: " + str(len(sentences)) + ", Wymiar embeddingów: " + str(dimension))

    # FAISS
    try:
        index = faiss.IndexFlatL2(dimension)
        index.add(embeddings)
        faiss.write_index(index, FAISS_INDEX_PATH)
        print(f"Indeks FAISS zapisany do: {FAISS_INDEX_PATH}")
    except Exception as e:
        print(f"Błąd FAISS: {e}")
        return

    # Tworzenie SQLite
    print("Sprawdzanie bazy SQLite")

    if os.path.exists(DB_PATH):
        print("Usuwanie istniejącej bazy: " + DB_PATH)
        try:
            os.remove(DB_PATH)
        except Exception as e:
            print("Błąd usuwania starej bazy SQLite: " + str({e}))
            return

    try:
        #print("Piastów " + {DB_PATH})
        connectionWithDB = sqlite3.connect(DB_PATH)
        cursor = connectionWithDB.cursor()

        print("Tworzenie tabeli dla bazy sql")
        cursor.execute("CREATE TABLE documents (id INTEGER PRIMARY KEY, sentence TEXT)")

        for idx, sentence in enumerate(sentences):
            cursor.execute("INSERT INTO documents (id, sentence) VALUES (?, ?)", (idx, sentence))

        connectionWithDB.commit()
        connectionWithDB.close()
        print("Baza SQLite zapisana: " + DB_PATH)
    except Exception as e:
        print("Błąd tworzenia bazy SQLite: " + str({e}))
        return

    print("BAZA HYBRYDOWA UTWORZONA")


create_hybrid_database()
