from pathlib import Path

root = Path(__file__).resolve().parent / '..' / 'java' / 'opentron-java' / 'src' / 'main' / 'java'
root = root.resolve()
if not root.exists():
    raise SystemExit(f'Path does not exist: {root}')

files = list(root.rglob('*.java'))
removed = []
for path in files:
    data = path.read_bytes()
    if data.startswith(b'\xef\xbb\xbf'):
        path.write_bytes(data[3:])
        removed.append(path)

print(f'total java files: {len(files)}')
print(f'bom removed: {len(removed)}')
for path in removed:
    print(path)
