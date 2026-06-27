#!/usr/bin/env python3
"""Generate Java stub files mirroring Python module structure.

Rules:
- Python files under `src/OpenTron/...` -> Java package `io.OpenTron.<path>`
- Test files under `tests/...` -> `src/test/java/io/OpenTron/tests/...` with JUnit stub
- Other Python files -> `io.OpenTron.external.<path>` under main source
- `__init__.py` -> `package-info.java` with a brief comment
- Other files -> `PascalCase` class with a single stub method
"""
import os
import io
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..'))
OUT_MAIN = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'java'))
OUT_TEST = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'src', 'test', 'java'))

PY_IGNORE_DIRS = {'.git', 'venv', '__pycache__', 'node_modules', 'build'}

def to_package(parts):
    return '.'.join(parts)


def to_classname(name):
    if name.endswith('.py'):
        name = name[:-3]
    parts = [p for p in name.split('_') if p]
    if not parts:
        return 'Module'
    return ''.join(p.capitalize() for p in parts)


def ensure_dir(path):
    os.makedirs(path, exist_ok=True)


def write_file(path, content):
    ensure_dir(os.path.dirname(path))
    with io.open(path, 'w', encoding='utf-8') as f:
        f.write(content)


count = 0
skipped = 0
for dirpath, dirnames, filenames in os.walk(ROOT):
    # skip output tree so we don't recurse into generated files
    if os.path.commonpath([os.path.abspath(dirpath), OUT_MAIN]) == os.path.abspath(OUT_MAIN) or os.path.commonpath([os.path.abspath(dirpath), OUT_TEST]) == os.path.abspath(OUT_TEST):
        continue
    # filter ignored dirs
    dirnames[:] = [d for d in dirnames if d not in PY_IGNORE_DIRS and not d.startswith('.')]
    for fn in filenames:
        if not fn.endswith('.py'):
            continue
        full = os.path.join(dirpath, fn)
        rel = os.path.relpath(full, ROOT).replace('\\', '/')
        # skip generator itself
        if rel.startswith('java/OpenTron-java/tools'):
            skipped += 1
            continue
        # decide target
        if rel.startswith('src/OpenTron/'):
            pkg_parts = ['io', 'OpenTron'] + rel[len('src/OpenTron/'):].split('/')[:-1]
            out_base = OUT_MAIN
        elif rel.startswith('tests/'):
            pkg_parts = ['io', 'OpenTron', 'tests'] + rel[len('tests/'):].split('/')[:-1]
            out_base = OUT_TEST
        else:
            pkg_parts = ['io', 'OpenTron', 'external'] + rel.split('/')[:-1]
            out_base = OUT_MAIN
        # sanitize package parts
        pkg_parts = [p for p in pkg_parts if p and p != '.']
        package = to_package(pkg_parts)
        name = fn
        if fn == '__init__.py':
            # write package-info.java
            target_dir = os.path.join(out_base, *pkg_parts)
            target = os.path.join(target_dir, 'package-info.java')
            content = '/**\n * Auto-generated package-info for %s\n */\npackage %s;\n' % (package, package)
            write_file(target, content)
            count += 1
            continue
        classname = to_classname(fn)
        target_dir = os.path.join(out_base, *pkg_parts)
        target = os.path.join(target_dir, classname + '.java')
        # create a simple class stub
        content = '// Auto-generated stub for %s (source: %s)\npackage %s;\n\npublic class %s {\n    // TODO: implement conversion from Python module: %s\n    public static void __stub() {\n        throw new UnsupportedOperationException("Auto-generated stub");\n    }\n}\n' % (rel, rel, package, classname, rel)
        write_file(target, content)
        count += 1

print('Generated %d stub files (skipped %d).' % (count, skipped))

