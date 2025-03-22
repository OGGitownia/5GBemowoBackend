from flask import Flask, request, jsonify
import requests
import sys
import threading
import time

server_name = sys.argv[1] if len(sys.argv) > 1 else "example_server"
port = int(sys.argv[2]) if len(sys.argv) > 2 else 5001

SPRING_BOOT_READY_URL = f"http://localhost:8080/{server_name}/server-ready"

app = Flask(server_name)

def notify_spring_boot():
    time.sleep(2)
    try:
        response = requests.post(SPRING_BOOT_READY_URL, json={"server_name": server_name, "port": port})
        print(f"Spring Boot notified: {server_name} is running on port {port} (status: {response.status_code})", flush=True)
    except requests.exceptions.RequestException as e:
        print(f"Error notifying Spring Boot: {e}", flush=True)

threading.Thread(target=notify_spring_boot, daemon=True).start()

@app.route(f'/{server_name}/process', methods=['POST'])
def process_request():
    return jsonify({"message": "This endpoint has not been implemented yet."}), 200

@app.route(f'/{server_name}/shutdown', methods=['POST'])
def shutdown():
    func = request.environ.get('werkzeug.server.shutdown')
    if func is None:
        raise RuntimeError("Server shutdown function is unavailable.")
    func()
    return "Server shut down", 200

@app.route("/health", methods=["GET"])
def health_check():
    return jsonify({"status": "OK", "server_name": server_name, "port": port})

if __name__ == '__main__':
    print(f"Starting server {server_name} on port {port}", flush=True)
    app.run(host='0.0.0.0', port=port, debug=False)
