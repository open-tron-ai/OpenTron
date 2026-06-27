#!/usr/bin/env python3
import os
import glob

# Find and fix all files with BOM in the .venv directory
venv_dir = r'C:\Users\ermis\Documents\OpenTron\.venv'
fixed_count = 0

for root, dirs, files in os.walk(venv_dir):
    for file in files:
        filepath = os.path.join(root, file)
        try:
            with open(filepath, 'rb') as f:
                data = f.read()
            
            if data.startswith(b'\xef\xbb\xbf'):
                data_without_bom = data[3:]
                with open(filepath, 'wb') as f:
                    f.write(data_without_bom)
                fixed_count += 1
                print(f"Fixed BOM in: {filepath}")
        except Exception as e:
            pass

print(f"Total files fixed: {fixed_count}")
