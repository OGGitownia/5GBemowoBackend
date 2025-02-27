import faiss
import numpy as np
import sys
import time
from sentence_transformers import SentenceTransformer

# 🔹 Plik bazy FAISS
FAISS_INDEX_PATH = "faiss_index.idx"

# 🔹 Model do embedowania tekstu
embedding_model = SentenceTransformer("all-MiniLM-L6-v2")

# 🔹 Markdown plik z normą
MARKDOWN_PATH = "src/main/resources/norms/36331-e60.md"

# 📌 Funkcja sprawdzająca, czy baza istnieje
def faiss_exists():
    try:
        index = faiss.read_index(FAISS_INDEX_PATH)
        return True
    except:
        return False

# 📌 Funkcja do wczytania i przetwarzania Markdown
def process_markdown():
    with open(MARKDOWN_PATH, "r", encoding="utf-8") as f:
        lines = f.readlines()

    # 🔹 Dzielimy tekst na akapity
    chunks = []
    current_chunk = ""

    for line in lines:
        if line.strip():  # Jeśli linia nie jest pusta, dodajemy do fragmentu
            current_chunk += line.strip() + " "
        else:
            if current_chunk:
                chunks.append(current_chunk.strip())
                current_chunk = ""

    if current_chunk:
        chunks.append(current_chunk)

    return chunks

# 📌 Funkcja tworząca FAISS
def create_faiss():
    if faiss_exists():
        print("✅ Baza FAISS już istnieje.")
        return

    print("📥 Tworzenie FAISS...")

    # 🔹 Pobieramy fragmenty z Markdown
    chunks = process_markdown()
    embeddings = np.array([embedding_model.encode(chunk) for chunk in chunks])

    # 🔹 Tworzymy indeks FAISS
    index = faiss.IndexFlatL2(embeddings.shape[1])
    index.add(embeddings)

    # 🔹 Zapisujemy FAISS do pliku
    faiss.write_index(index, FAISS_INDEX_PATH)

    print("✅ Baza FAISS została utworzona!")

if __name__ == "__main__":
    # Obsługa argumentów
    if len(sys.argv) > 1 and sys.argv[1] == "check":
        if faiss_exists():
            print("EXISTS")
        else:
            print("NOT_FOUND")
    else:
        create_faiss()
