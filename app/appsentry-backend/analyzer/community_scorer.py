from scrapers import reddit_scraper, play_store_scraper, xda_scraper
from config import WEIGHT_REDDIT, WEIGHT_PLAY_STORE, WEIGHT_XDA


def compute(package_name: str, app_name: str) -> dict:
    """
    Runs all scrapers. Gracefully degrades when any scraper fails.

    Returns a unified community result:
        {
            "community_score": float,   # 0–100 (trust, NOT risk)
            "play_store_rating": float,
            "install_count": int,
            "flags": list[str],
            "sources_used": list[str],
            "community_available": bool
        }
    """

    # ── Run all scrapers independently ──────────────────────────────────────
    reddit    = _safe_run(reddit_scraper.scrape,      package_name, app_name)
    play      = _safe_run(play_store_scraper.scrape,  package_name)
    xda       = _safe_run(xda_scraper.scrape,         app_name)

    # ── Collect flags from all sources ──────────────────────────────────────
    all_flags = []
    all_flags.extend(reddit.get("flags", []))
    all_flags.extend(play.get("flags", []))
    all_flags.extend(xda.get("flags", []))

    # ── Determine which sources actually returned data ───────────────────────
    sources_used = []
    if reddit.get("available"): sources_used.append("reddit")
    if play.get("available"):   sources_used.append("play_store")
    if xda.get("available"):    sources_used.append("xda")

    # ── Weighted scoring — only count sources that are available ─────────────
    total_weight = 0.0
    weighted_score = 0.0

    if reddit.get("available"):
        weighted_score += reddit["raw_score"] * WEIGHT_REDDIT
        total_weight   += WEIGHT_REDDIT

    if play.get("available"):
        weighted_score += play["raw_score"] * WEIGHT_PLAY_STORE
        total_weight   += WEIGHT_PLAY_STORE

    if xda.get("available"):
        weighted_score += xda["raw_score"] * WEIGHT_XDA
        total_weight   += WEIGHT_XDA

    # ── Fallback: no sources returned data at all ────────────────────────────
    if total_weight == 0:
        return {
            "community_score": 50.0,    # strictly neutral
            "play_store_rating": 0.0,
            "install_count": 0,
            "flags": [],
            "sources_used": [],
            "community_available": False,
        }

    # ── Normalize score to available sources only ────────────────────────────
    final_score = weighted_score / total_weight

    # ── Override: any malware flag tanks the score ───────────────────────────
    danger_flags = [f for f in all_flags if "malware" in f or "danger" in f or "trojan" in f]
    if danger_flags:
        final_score = min(final_score, 20.0)   # hard cap at 20 (very low trust)

    return {
        "community_score": round(final_score, 2),
        "play_store_rating": play.get("rating", 0.0),
        "install_count": play.get("installs", 0),
        "flags": list(set(all_flags)),
        "sources_used": sources_used,
        "community_available": True,
    }


# ── Helpers ──────────────────────────────────────────────────────────────────

def _safe_run(fn, *args) -> dict:
    """
    Calls a scraper function. If it raises for ANY reason,
    returns a neutral unavailable dict — never crashes the pipeline.
    """
    try:
        return fn(*args)
    except Exception as e:
        print(f"[WARN] Scraper {fn.__module__} failed: {e}")
        return {"available": False, "flags": [], "raw_score": 50.0}