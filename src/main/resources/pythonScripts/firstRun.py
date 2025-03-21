import subprocess

model_name = "llama3"
question = "What is the purpose of RRCConnectionRequest?"

result = subprocess.run(
    ["ollama", "run", model_name],
    input=question,
    text=True,
    capture_output=True
)

print("Pytanie:", question)
print("Odpowiedź:", result.stdout.strip())

