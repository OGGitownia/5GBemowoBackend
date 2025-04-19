from flask import Flask, request, jsonify
import requests
import sys
import threading
import time
from langchain.prompts import ChatPromptTemplate
from langchain_ollama import OllamaLLM

sys.stdout.reconfigure(encoding='utf-8')

# Parametry serwera
server_name = sys.argv[1] if len(sys.argv) > 1 else "lamo_asker"
port = int(sys.argv[2]) if len(sys.argv) > 2 else 5002

SPRING_BOOT_READY_URL = f"http://localhost:8080/{server_name}/server-ready"

app = Flask(server_name)


model = OllamaLLM(model="llama3")


PROMPT_TEMPLATE = """
Answer the question based only on the following context:

{context}

---

Answer the question: {question}
"""

def notify_spring_boot():
    """ Powiadomienie Spring Boot o gotowości serwera. """
    print("POWIADAMIAM ZA 5s...", flush=True)
    time.sleep(2)
    print("POWIADAMIAM TERAZ!", flush=True)

    try:
        response = requests.post(SPRING_BOOT_READY_URL, json={"server_name": server_name, "port": port})
        print(f"Powiadomiono Spring Boot: {server_name} działa na porcie {port}. Status: {response.status_code}", flush=True)
    except requests.exceptions.RequestException as e:
        print(f"Błąd powiadamiania Spring Boot: {e}", flush=True)

threading.Thread(target=notify_spring_boot, daemon=True).start()


@app.route(f'/{server_name}/process', methods=['POST'])
def process_request():
    print("Odebrano zapytanie...", flush=True)

    try:
        data = request.get_json()
        if not isinstance(data, dict):
            raise ValueError("Niepoprawny format JSON!")

        context = data.get("context", "").strip()
        question = data.get("question", "").strip()

        if not question:
            return jsonify({"error": "Pytanie nie może być puste"}), 400

        response = generate_response(context, question)
        return jsonify({"response": response}), 200

    except Exception as e:
        import traceback
        print("Błąd przetwarzania zapytania:", traceback.format_exc(), flush=True)
        return jsonify({"error": str(e)}), 500


def generate_response(context, question):
    """ Generowanie odpowiedzi za pomocą LangChain + Ollama """
    prompt_template = ChatPromptTemplate.from_template(PROMPT_TEMPLATE)
    prompt = prompt_template.format(context=context, question=question)
    print(f"{context}", flush=True)
    print(f"{question}", flush=True)
    print(f"{prompt}", flush=True)

    print(f"Wysyłanie zapytania do Ollama:\n{prompt}", flush=True)

    response_text = model.invoke(prompt)

    print(f"Odpowiedź modelu:\n{response_text}", flush=True)

    return response_text


@app.route(f'/{server_name}/shutdown', methods=['POST'])
def shutdown():
    """ Zamknięcie serwera Flask. """
    print(f"Zamykanie serwera {server_name}", flush=True)
    func = request.environ.get('werkzeug.server.shutdown')
    if func is None:
        raise RuntimeError("Nie można zamknąć serwera.")
    func()
    return "Serwer zamknięty", 200


@app.route("/health", methods=["GET"])
def health_check():
    """ Endpoint sprawdzający stan serwera. """
    return jsonify({"status": "OK", "server_name": server_name, "port": port})


if __name__ == '__main__':
    print(f"Uruchamianie serwera {server_name} na porcie {port}", flush=True)
    app.run(host='0.0.0.0', port=port, debug=False)
