from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer
import threading
import os
import signal
import sys
import json
import requests

sys.stdout.reconfigure(encoding='utf-8')


app = Flask(__name__)
model = SentenceTransformer("BAAI/bge-m3")

server_name = sys.argv[1] if len(sys.argv) > 1 else "newEmbeddingServer"
port = int(sys.argv[2]) if len(sys.argv) > 2 else 5003

SPRING_BOOT_NOTIFY = f"http://localhost:8080/{server_name}/server-ready"



@app.route(f"/{server_name}/process", methods=["POST"])
def process_embedding_request():
    print(f"process_embedding_request", flush=True)
    try:
        data = request.get_json()
        input_file = data.get("inputFile")
        output_file = data.get("outputFile")
        print(f"Input file: {input_file}", flush=True)

        if not input_file or not output_file:
            return jsonify({"error": "Missing 'inputFile' or 'outputFile'"}), 400

        if not os.path.isfile(input_file):
            return jsonify({"error": f"Input file does not exist: {input_file}"}), 400


        # Wczytaj plik JSON z chunkami
        with open(input_file, "r", encoding="utf-8") as f:
            json_data = json.load(f)

        lines = json_data.get("chunks", [])
        if not isinstance(lines, list) or not all(isinstance(line, str) for line in lines):
            return jsonify({"error": "'chunks' must be a list of strings in input JSON"}), 400


        total = len(lines)
        if total == 0:
            return jsonify({"error": "Input file is empty"}), 400

        print(f"Rozpoczynam generowanie embeddingów ({total} linii)...", flush=True)

        results = []
        for i, line in enumerate(lines, start=1):
            embedding = model.encode(line).tolist()
            results.append({
                "content": line,
                "embeddedContent": embedding
            })

            # Progres co 10%
            if i % max(1, total // 10) == 0 or i == total:
                print(f"[{i}/{total}] ({round(i / total * 100)}%) przetworzono...", flush=True)

            # Zapis tymczasowy co 100 chunków
            if i % 100 == 0 or i == total:
                with open(output_file, "w", encoding="utf-8") as f_out:
                    json.dump({"embeddedChunks": results}, f_out, ensure_ascii=False, indent=2)

        print(f"Zapisano {i} elementów do pliku tymczasowo ({output_file})", flush=True)

        print(f"Embedding zakończony. Finalnie zapisano {len(results)} elementów do {output_file}", flush=True)
        return jsonify({"message": "Embedding completed", "output": output_file}), 200

    except Exception as e:
        import traceback
        print(traceback.format_exc(), flush=True)
        return jsonify({"error": str(e)}), 500


@app.route(f"/{server_name}/shutdown", methods=["POST"])
def shutdown():
    def shutdown_server():
        os.kill(os.getpid(), signal.SIGINT)
    response = jsonify({"message": "Shutting down server"})
    threading.Thread(target=shutdown_server).start()
    return response, 200

@app.route("/status", methods=["GET"])
def status():
    return jsonify({"status": "running"}), 200

def notify_spring_boot():
    print("Notifying Spring Boot that embedding server is ready...", flush=True)
    print(f"SPRING_BOOT_NOTIFY={SPRING_BOOT_NOTIFY}", flush=True)
    try:
        response = requests.post(SPRING_BOOT_NOTIFY, json={"port": port})
        print(f"Spring Boot responded: {response.status_code} {response.text}", flush=True)
    except Exception as e:
        print(f"Notification error: {e}", flush=True)

def run_server():
    notify_spring_boot()
    print(f"Embedding server running on port {port}", flush=True)
    app.run(host="0.0.0.0", port=port)

if __name__ == "__main__":
    run_server()
