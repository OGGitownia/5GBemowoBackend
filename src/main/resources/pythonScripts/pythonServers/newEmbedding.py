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

server_name = "embedding-server"
port = 5003
SPRING_BOOT_NOTIFY = "http://localhost:8080/flask/notify"

@app.route(f"/{server_name}/process", methods=["POST"])
def process_embedding_request():
    try:
        data = request.get_json()
        input_file = data.get("inputFile")
        output_file = data.get("outputFile")

        if not input_file or not output_file:
            return jsonify({"error": "Missing 'inputFile' or 'outputFile'"}), 400

        if not os.path.isfile(input_file):
            return jsonify({"error": f"Input file does not exist: {input_file}"}), 400

        with open(input_file, "r", encoding="utf-8") as f:
            json_data = json.load(f)

        if "fragments" not in json_data:
            return jsonify({"error": "'fragments' key not found in input file"}), 400

        for fragment in json_data["fragments"]:
            content = fragment.get("content")
            if content:
                embedding = model.encode(content).tolist()
                fragment["embeddedContent"] = embedding

        with open(output_file, "w", encoding="utf-8") as f_out:
            json.dump(json_data, f_out, ensure_ascii=False, indent=2)

        print(f"Embeddings written to {output_file}", flush=True)
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
    try:
        requests.post(SPRING_BOOT_NOTIFY, json={"status": "ready"})
    except Exception as e:
        print(f"Notification error: {e}", flush=True)

def run_server():
    notify_spring_boot()
    print(f"Embedding server running on port {port}", flush=True)
    app.run(host="0.0.0.0", port=port)

if __name__ == "__main__":
    run_server()
