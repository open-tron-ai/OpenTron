#!/usr/bin/env python3
import tomllib
import sys

try:
    with open(r'C:\Users\ermis\Documents\OpenTron\pyproject.toml', 'rb') as f:
        data = tomllib.load(f)
    print("✓ File parsed successfully")
    print(f"Project name: {data.get('project', {}).get('name')}")
except Exception as e:
    print(f"✗ Error: {type(e).__name__}: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)
