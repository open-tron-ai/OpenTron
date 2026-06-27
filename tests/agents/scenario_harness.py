"""Shared harness dataclasses for agent lifecycle scenario tests."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from OpenTron.agents.executor import AgentExecutor
from OpenTron.agents.manager import AgentManager
from OpenTron.agents.scheduler import AgentScheduler
from OpenTron.core.events import EventBus
from tests.agents.fake_engine import FakeEngine


@dataclass(slots=True)
class FakeSystem:
    """Lightweight stand-in for TronSystem — just engine + model."""

    engine: FakeEngine
    model: str = "fake-model"
    memory_backend: Any = None
    channel_backend: Any = None
    tools: list = field(default_factory=list)
    config: Any = None
    session_store: Any = None


@dataclass(slots=True)
class ScenarioHarness:
    """All components needed for an agent lifecycle scenario test."""

    manager: AgentManager
    executor: AgentExecutor
    scheduler: AgentScheduler
    bus: EventBus
    engine: FakeEngine
    system: FakeSystem
    db_path: str

