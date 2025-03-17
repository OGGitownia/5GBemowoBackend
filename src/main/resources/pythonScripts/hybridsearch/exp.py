from llama_cpp import Llama

print("Test wykrywania GPU")
try:
    model = Llama(model_path="C:\\Users\\Pc\\llama.cpp\\models\\Meta-Llama-3.1-8B-Instruct-bf16.gguf", n_gpu_layers=1)
    print("GPU działa!")
except Exception as e:
    print("GPU nie działa!", e)
