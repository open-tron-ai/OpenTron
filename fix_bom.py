#!/usr/bin/env python3

# Read the file
with open(r'C:\Users\ermis\Documents\OpenTron\pyproject.toml', 'rb') as f:
    data = f.read()

# Remove UTF-8 BOM if present
if data.startswith(b'\xef\xbb\xbf'):
    data = data[3:]
    print("Removed UTF-8 BOM")
else:
    print("No BOM detected")

# Write back
with open(r'C:\Users\ermis\Documents\OpenTron\pyproject.toml', 'wb') as f:
    f.write(data)

print("File fixed!")
