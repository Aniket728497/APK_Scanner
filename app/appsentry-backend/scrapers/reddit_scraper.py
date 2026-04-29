import requests
from config import REDDIT_SUBREDDITS, REDDIT_MAX_POSTS, MIN_REDDIT_SCORE_TO_TRUST

HEADERS = {"User-Agent": "AppSentry/1.0 (security research tool)"}


def scrape(package_name: str, app_name: str) -> dict:
    """
    Returns:
        {
            "available": bool,
            "mention_count": int,
            "avg_sentiment": float,   # 0.0 (negative) → 1.0 (positive)
            "flags": list[str],
            "raw_score": float        # 0–100 trust score
        }
    """
    mentions = []
    flags = []

    query = _build_query(package_name, app_name)

    for sub in REDDIT_SUBREDDITS:
        try:
            posts = _fetch_subreddit(sub, query)
            mentions.extend(posts)
        except Exception:
            # One subreddit failing is non-fatal
            continue

    if not mentions:
        return _unavailable()

    # Filter low-signal posts
    mentions = [m for m in mentions if m["score"] >= MIN_REDDIT_SCORE_TO_TRUST]

    if not mentions:
        return _unavailable()

    # Detect danger flags from titles/bodies
    for post in mentions:
        text = (post.get("title", "") + " " + post.get("body", "")).lower()
        if any(w in text for w in ["malware", "virus", "spyware", "trojan", "scam", "steal"]):
            flags.append("reddit:malware_mention")
        if any(w in text for w in ["ban", "removed", "fake", "fraud"]):
            flags.append("reddit:fraud_mention")

    avg_sentiment = _avg_sentiment(mentions)
    raw_score = avg_sentiment * 100

    return {
        "available": True,
        "mention_count": len(mentions),
        "avg_sentiment": avg_sentiment,
        "flags": list(set(flags)),
        "raw_score": raw_score,
    }


# ── Internals ─────────────────────────────────────────────────────────────────

def _build_query(package_name: str, app_name: str) -> str:
    # Use app name for readability; fallback to package if name == package
    name = app_name if app_name != package_name else package_name.split(".")[-1]
    return name


def _fetch_subreddit(subreddit: str, query: str) -> list[dict]:
    url = f"https://www.reddit.com/r/{subreddit}/search.json"
    params = {"q": query, "restrict_sr": 1, "sort": "relevance", "limit": REDDIT_MAX_POSTS}
    resp = requests.get(url, headers=HEADERS, params=params, timeout=8)
    resp.raise_for_status()
    data = resp.json()

    posts = []
    for child in data.get("data", {}).get("children", []):
        d = child.get("data", {})
        posts.append({
            "title": d.get("title", ""),
            "body": d.get("selftext", ""),
            "score": d.get("score", 0),
            "upvote_ratio": d.get("upvote_ratio", 0.5),
        })
    return posts


def _avg_sentiment(mentions: list[dict]) -> float:
    if not mentions:
        return 0.5
    # upvote_ratio ∈ [0,1]; weight by score
    total_weight = sum(max(m["score"], 1) for m in mentions)
    weighted_sum = sum(m["upvote_ratio"] * max(m["score"], 1) for m in mentions)
    return weighted_sum / total_weight if total_weight > 0 else 0.5


def _unavailable() -> dict:
    return {
        "available": False,
        "mention_count": 0,
        "avg_sentiment": 0.5,
        "flags": [],
        "raw_score": 50.0,   # neutral fallback
    }