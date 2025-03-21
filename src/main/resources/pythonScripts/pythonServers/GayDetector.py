from flask import Flask, request, jsonify
import requests
import socket
import sys

server_name = sys.argv[1] if len(sys.argv) > 1 else "GayDetector"

app = Flask(server_name)

SPRING_BOOT_READY_URL = "http://localhost:8080/hybrid-service/search-server-ready"


def notify_spring_boot(port):

    try:
        response = requests.post(SPRING_BOOT_READY_URL, json={"server_name": server_name, "port": port})
        print(f"Powiadomiono Spring Boot: {server_name} działa na porcie {port}. Status: {response.status_code}")
    except requests.exceptions.RequestException as e:
        print(f"Błąd powiadamiania Spring Boot: {e}")


@app.route(f'/{server_name}/process', methods=['POST'])
def process_request():
    """Przetwarza zapytanie: 'Are you gay?'"""
    data = request.get_json()
    question = data.get("data", "").strip().lower()

    if question == "are you gay?":
        response = "I'm just a Python script, I have no sexuality! 🏳️‍🌈"
    else:
        response = "I only answer the gay question!"

    return jsonify({"response": response})


@app.route(f'/{server_name}/shutdown', methods=['POST'])
def shutdown():
    """Zamyka serwer."""
    print(f"Otrzymano żądanie zamknięcia serwera {server_name}")
    func = request.environ.get('werkzeug.server.shutdown')
    if func is None:
        raise RuntimeError("Nie można zamknąć serwera.")
    func()
    return "Serwer zamknięty", 200


if __name__ == '__main__':
    # Znajdź dostępny port
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(('0.0.0.0', 0))
        actual_port = s.getsockname()[1]

    print(f"Starting {server_name} server on port {actual_port}...")

    # Powiadom Spring Boot o gotowości
    notify_spring_boot(actual_port)

    # Uruchom Flask
    app.run(host='0.0.0.0', port=actual_port, debug=False)
