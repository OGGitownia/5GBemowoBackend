import faiss
import sqlite3
import json
import numpy as np
import os

DB_PATH = "data/hybrid_db.sqlite"
FAISS_INDEX_PATH = "data/hybrid_db.index"

def create_hybrid_database(json_path):
    # Wczytanie JSON
    with open(json_path, "r") as f:
        data = json.load(f)

    sentences = []
    embeddings = []

    for entry in data:
        sentences.append(entry["sentence"])
        embeddings.append(entry["embedding"])

    embeddings = np.array(embeddings).astype('float32')

    # Tworzenie FAISS
    dimension = embeddings.shape[1]
    index = faiss.IndexFlatL2(dimension)
    index.add(embeddings)
    faiss.write_index(index, FAISS_INDEX_PATH)

    # Tworzenie SQLite
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)

    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    cursor.execute("CREATE TABLE documents (id INTEGER PRIMARY KEY, sentence TEXT)")

    for idx, sentence in enumerate(sentences):
        cursor.execute("INSERT INTO documents (id, sentence) VALUES (?, ?)", (idx, sentence))

    conn.commit()
    conn.close()

    print(f"✅ Baza hybrydowa utworzona! FAISS: {FAISS_INDEX_PATH}, SQLite: {DB_PATH}")
