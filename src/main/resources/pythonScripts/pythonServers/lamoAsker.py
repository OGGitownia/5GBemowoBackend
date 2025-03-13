from flask import Flask, request, jsonify
import requests
import sys
import threading
import time
from llama_cpp import Llama

sys.stdout.reconfigure(encoding='utf-8')

server_name = sys.argv[1] if len(sys.argv) > 1 else "lamo_asker"
port = int(sys.argv[2]) if len(sys.argv) > 2 else 5002

SPRING_BOOT_READY_URL = f"http://localhost:8080/{server_name}/server-ready"

app = Flask(server_name)


MODEL_PATH = "C:\\Users\\Pc\\llama.cpp\\models\\Meta-Llama-3.1-8B-Instruct-bf16.gguf"

llama_model = None

def init_model():
    global llama_model
    print("Ładowanie modelu lamy")
    llama_model = Llama(
        model_path=MODEL_PATH,
        n_gpu_layers=26,  # Liczba warstw przeniesiona na GPU
        main_gpu=0,  # Użyj pierwszego dostępnego GPU
        tensor_split=[1.0],  # Cały VRAM dostępny dla modelu
        n_batch=512
    )
    threading.Thread(target=notify_spring_boot, daemon=True).start()

threading.Thread(target=init_model, daemon=True).start()



def notify_spring_boot():
    print("POWIAWAMIAM_ZA_5", flush=True)
    time.sleep(5)
    print("POWIAWAMIAM_ZA_0", flush=True)
    try:
        response = requests.post(SPRING_BOOT_READY_URL, json={"server_name": server_name, "port": port})
        print(f"Powiadomiono Spring Boot: {server_name} działa na porcie {port}. Status: {response.status_code}", flush=True)
    except requests.exceptions.RequestException as e:
        print(f"Błąd powiadamiania Spring Boot: {e}")

@app.route(f'/{server_name}/process', methods=['POST'])
def process_request():
    print("Odebrano zapytanie", flush=True)

    try:
        raw_data = request.data.decode("utf-8")
        print(f"Otrzymane surowe dane: {raw_data}", flush=True)

        data = request.get_json()
        print(f"Odebrane dane JSON: {data}", flush=True)

        if not isinstance(data, dict):
            raise ValueError("Odebrano niepoprawny format JSON!")

        context = data.get("context", "").strip()
        question = data.get("question", "").strip()

        print(f"pytanie: {question}", flush=True)
        if not question:
            return jsonify({"error": "Pytanie nie może być puste"}), 400

        response = generate_response(context, question)
        return jsonify({"response": response}), 200

    except Exception as e:
        import traceback
        print("Błąd przetwarzania zapytania:", traceback.format_exc())
        return jsonify({"error": str(e)}), 500


def generate_response(context, question):
    print("context: ", context, flush=True)
    print("questiion: ", question, flush=True)
    print("Próba generowania odp", flush=True)
    if not llama_model:
        return "Model nie jest jeszcze załadowany!"
    print("Model był", flush=True)
    full_prompt = f"Kontekst: {question}\nPytanie: {question}\nOdpowiedź:"

    print("Wysyłanie zapytania do modelu AI", flush=True)
    result = llama_model(full_prompt, max_tokens=200)
    time.sleep(5)
    answer = result["choices"][0]["text"] if "choices" in result else " Błąd generacji odpowiedzi!"

    print(f" Odpowiedź modelu: , flush=True)", flush=True)
    return answer

@app.route(f'/{server_name}/shutdown', methods=['POST'])
def shutdown():
    print(f"Otrzymano żądanie zamknięcia serwera {server_name}")
    func = request.environ.get('werkzeug.server.shutdown')
    if func is None:
        raise RuntimeError("Nie można zamknąć serwera.")
    func()
    return "Serwer zamknięty", 200

if __name__ == '__main__':
    print(f"Uruchamianie serwera {server_name} na porcie {port}")
    app.run(host='0.0.0.0', port=port, debug=False)
