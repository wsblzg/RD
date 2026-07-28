import importlib.util
import sys
from pathlib import Path

from fastapi.testclient import TestClient


def test_health(tmp_path, monkeypatch):
    monkeypatch.chdir(tmp_path)
    monkeypatch.setenv("RAG_BUILD_INDEX_ON_STARTUP", "false")
    module_path = Path(__file__).parents[1] / "main.py"
    spec = importlib.util.spec_from_file_location("rag_main", module_path)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)

    response = TestClient(module.app).get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}
