#!/usr/bin/env python3

with open(r'C:\Users\ermis\Documents\OpenTron\pyproject.toml', 'rb') as f:
    data = f.read(100)
    print(f"First 100 bytes (hex): {data.hex()}")
    print(f"First 100 bytes (repr): {repr(data)}")
    print(f"As text: {data}")
