import torch
print(torch.cuda.is_available())  # Powinno zwrócić True
print(torch.cuda.device_count())  # Powinno zwrócić liczbę dostępnych GPU
print(torch.cuda.get_device_name(0))  # Nazwa GPU
