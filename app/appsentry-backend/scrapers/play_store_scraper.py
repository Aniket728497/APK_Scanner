from google_play_scraper import app as gps_app, exceptions as gps_exc
from config import PLAY_STORE_GOOD_RATING, PLAY_STORE_MIN_INSTALLS


def scrape(package_name: str) -> dict:
    """
    Returns:
        {
            "available": bool,
            "rating": float,
            "installs": int,
            "developer": str,
            "flags": list[str],
            "raw_score": float   # 0–100 trust score
        }
    """
    try:
        result = gps_app(package_name, lang="en", country="us")
    except (gps_exc.NotFoundError, Exception):
        return _unavailable()

    rating   = result.get("score") or 0.0
    installs = _parse_installs(result.get("installs") or result.get("minInstalls") or 0)
    developer = result.get("developer", "Unknown")
    flags = []

    if rating > 0 and rating < 2.5:
        flags.append("play:low_rating")
    if installs < 1_000 and installs > 0:
        flags.append("play:very_low_installs")

    raw_score = _compute_score(rating, installs)

    return {
        "available": True,
        "rating": rating,
        "installs": installs,
        "developer": developer,
        "flags": flags,
        "raw_score": raw_score,
    }


# ── Internals ─────────────────────────────────────────────────────────────────

def _parse_installs(value) -> int:
    if isinstance(value, int):
        return value
    if isinstance(value, str):
        cleaned = value.replace("+", "").replace(",", "").strip()
        try:
            return int(cleaned)
        except ValueError:
            return 0
    return 0


def _compute_score(rating: float, installs: int) -> float:
    """Map Play Store signals → 0–100 trust score."""
    if rating == 0 and installs == 0:
        return 50.0   # not on Play Store — neutral, not penalized here

    rating_score = (rating / 5.0) * 100     # 0–100
    install_score = min(100, (installs / PLAY_STORE_MIN_INSTALLS) * 50)  # caps at 100K installs

    return (rating_score * 0.7) + (install_score * 0.3)


def _unavailable() -> dict:
    return {
        "available": False,
        "rating": 0.0,
        "installs": 0,
        "developer": "Unknown",
        "flags": [],
        "raw_score": 50.0,    # neutral fallback — absence ≠ dangerous
    }