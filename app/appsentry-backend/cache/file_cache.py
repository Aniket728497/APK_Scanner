import json
import os
import time

CACHE_DIR = os.path.join(os.path.dirname(__file__), "_store")


def _path(key: str) -> str:
    os.makedirs(CACHE_DIR, exist_ok=True)
    safe = key.replace("/", "_").replace(".", "_")
    return os.path.join(CACHE_DIR, safe + ".json")


def get(key: str, ttl: int) -> dict | None:
    p = _path(key)
    if not os.path.exists(p):
        return None
    try:
        with open(p, "r") as f:
            entry = json.load(f)
        if time.time() - entry["ts"] > ttl:
            os.remove(p)
            return None
        return entry["data"]
    except Exception:
        return None


def set(key: str, data: dict) -> None:
    p = _path(key)
    try:
        with open(p, "w") as f:
            json.dump({"ts": time.time(), "data": data}, f)
    except Exception:
        pass