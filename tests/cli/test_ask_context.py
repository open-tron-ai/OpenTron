"""Tests for context injection integration in ``Tron ask``."""

from __future__ import annotations

import importlib

from click.testing import CliRunner

from OpenTron.cli import cli


def test_ask_no_context_flag():
    """The --no-context flag is accepted."""
    result = CliRunner().invoke(cli, ["ask", "--no-context", "--help"])
    # --help should succeed regardless
    assert result.exit_code == 0


def test_ask_has_no_context_option():
    """ask --help lists the --no-context flag."""
    result = CliRunner().invoke(cli, ["ask", "--help"])
    assert result.exit_code == 0
    assert "--no-context" in result.output


def test_get_memory_backend_returns_backend_even_when_empty(
    tmp_path,
    monkeypatch,
):
    """_get_memory_backend returns a backend for an empty DB.

    Retrieval against an empty store is a valid operation — it simply
    returns no hits. Callers must check ``len(results)``; returning
    ``None`` here would conflate "backend unavailable" with "no docs",
    which is the kind of ambiguity that leads to silent grounding
    failures downstream.
    """
    from OpenTron.core.config import TronConfig, MemoryConfig
    from OpenTron.core.registry import MemoryRegistry
    from OpenTron.tools.storage.sqlite import SQLiteMemory

    if not MemoryRegistry.contains("sqlite"):
        MemoryRegistry.register_value("sqlite", SQLiteMemory)

    config = TronConfig()
    config.memory = MemoryConfig(
        db_path=str(tmp_path / "empty.db"),
    )

    mod = importlib.import_module("OpenTron.cli.ask")
    result = mod._get_memory_backend(config)
    assert result is not None
    # An empty backend should still retrieve cleanly (zero hits).
    assert result.retrieve("anything", top_k=3) == []
    if hasattr(result, "close"):
        result.close()


def test_get_memory_backend_returns_backend_with_docs(
    tmp_path,
    monkeypatch,
):
    """_get_memory_backend returns a backend when docs exist."""
    from OpenTron.core.config import TronConfig, MemoryConfig
    from OpenTron.core.registry import MemoryRegistry
    from OpenTron.tools.storage.sqlite import SQLiteMemory

    if not MemoryRegistry.contains("sqlite"):
        MemoryRegistry.register_value("sqlite", SQLiteMemory)

    db_path = str(tmp_path / "test.db")
    config = TronConfig()
    config.memory = MemoryConfig(db_path=db_path)

    # Pre-populate with a document
    backend = SQLiteMemory(db_path=db_path)
    backend.store("test document content")
    backend.close()

    mod = importlib.import_module("OpenTron.cli.ask")
    result = mod._get_memory_backend(config)
    assert result is not None
    if hasattr(result, "close"):
        result.close()

