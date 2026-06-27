"""Tests for speech configuration."""

from OpenTron.core.config import TronConfig, SpeechConfig


def test_speech_config_defaults():
    cfg = SpeechConfig()
    assert cfg.backend == "auto"
    assert cfg.model == "base"
    assert cfg.language == ""
    assert cfg.device == "auto"
    assert cfg.compute_type == "float16"


def test_Tron_config_has_speech():
    cfg = TronConfig()
    assert hasattr(cfg, "speech")
    assert isinstance(cfg.speech, SpeechConfig)
    assert cfg.speech.backend == "auto"


def test_Tron_system_has_speech_backend():
    """TronSystem has a speech_backend attribute."""
    from OpenTron.system import TronSystem

    assert "speech_backend" in TronSystem.__dataclass_fields__

