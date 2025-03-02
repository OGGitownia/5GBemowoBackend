import requests
import subprocess
import time
from flask import Flask, request, jsonify
import threading

BASE_URL = "http://localhost:5000"
app = Flask(__name__)
server_ready = False


def start_server():
    global server_process
    server_process = subprocess.Popen(["python", "embedding.py"])
    print("⏳ Czekam na uruchomienie serwera...")


def wait_for_server():
    global server_ready
    while not server_ready:
        try:
            response = requests.get(f"{BASE_URL}/status", timeout=2)
            if response.status_code == 200:
                server_ready = True
                print("✅ Serwer jest gotowy!")
        except requests.exceptions.RequestException:
            time.sleep(1)


@app.route("/notify", methods=["POST"])
def notify():
    """ Odbiera powiadomienie o gotowości serwera """
    global server_ready
    data = request.json
    if data.get("status") == "ready":
        server_ready = True
        print("✅ Serwer embeddingów jest gotowy do pracy.")
    return jsonify({"message": "Received"}), 200


@app.route("/embedding", methods=["POST"])
def embedding():
    """ Otrzymuje embeddingi do przetworzenia """
    data = request.json
    if "text" not in data:
        return jsonify({"error": "Missing 'text' parameter"}), 400

    response = requests.post(f"{BASE_URL}/embed", json={"text": data["text"]})
    return response.json(), response.status_code


@app.route("/shutdown", methods=["POST"])
def shutdown():
    """ Wysyła polecenie wyłączenia serwera Flask """
    response = requests.post(f"{BASE_URL}/shutdown")
    return response.json(), response.status_code


def run_tester():
    app.run(host="0.0.0.0", port=6000, debug=False)


if __name__ == "__main__":
    threading.Thread(target=run_tester, daemon=True).start()
    start_server()
    wait_for_server()

    # Przykładowe użycie
    print("📩 Wysyłam testowy tekst do serwera...")
    embedding_response = requests.post("http://localhost:6000/embedding", json={"text": "Przykładowe zdanie."})
    print("✅ Otrzymane embeddingi:", embedding_response.json())

    print("⏳ Czekam 5 sekund, zanim wyłączę serwer...")
    time.sleep(5)

    print("🛑 Wyłączam serwer...")
    shutdown_response = requests.post("http://localhost:6000/shutdown")
    print("✅ Odpowiedź na shutdown:", shutdown_response.json())

    # Czekamy, aż proces serwera się zakończy
    server_process.wait()
    print("✅ Serwer Flask został zamknięty.")
