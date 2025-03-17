from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer
import threading
import os
import signal
import sys
import requests

sys.stdout.reconfigure(encoding='utf-8')

app = Flask(__name__)
model = SentenceTransformer("BAAI/bge-m3")  # Wczytaj model

SPRING_BOOT_URL = "http://localhost:8080/embedding-server/status"  # Endpoint Spring Boot do potwierdzenia uruchomienia

@app.route("/embed", methods=["POST"])
def embed():
    data = request.json
    if "text" not in data:
        return jsonify({"error": "Missing 'text' parameter"}), 400

    text = data["text"]
    embedding = model.encode(text).tolist()  # Konwersja na listę floatów
    return jsonify({"embedding": embedding})

@app.route("/status", methods=["GET"])
def status():
    """ Endpoint do potwierdzenia, że serwer embeddingów działa """
    return jsonify({"status": "running"}), 200

@app.route("/shutdown", methods=["POST"])
def shutdown():
    """ Endpoint do zamykania serwera embeddingów """
    def shutdown_server():
        os.kill(os.getpid(), signal.SIGINT)  # Zabija proces serwera

    # Najpierw zwracamy odpowiedź, potem zamykamy serwer
    response = jsonify({"message": "Shutting down embedding server"})
    threading.Thread(target=shutdown_server).start()
    return response, 200


def run_server():
    print("Serwer embeddingów uruchomiony. Powiadamiam Spring Boot...")
    try:
        requests.post("http://localhost:8080/flask/notify", json={"status": "ready"})
    except Exception as e:
        print(f"Błąd podczas powiadamiania Spring Boot: {e}")

    app.run(host="0.0.0.0", port=5000)


if __name__ == "__main__":
    run_server()
