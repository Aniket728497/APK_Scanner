import requests
from bs4 import BeautifulSoup

HEADERS = {"User-Agent": "Mozilla/5.0 (AppSentry security research)"}
XDA_SEARCH = "https://xdaforums.com/search/?query={query}&type=post"


def scrape(app_name: str) -> dict:
    """
    Best-effort only. Always returns a result dict.
    Returns:
        {
            "available": bool,
            "mention_count": int,
            "flags": list[str],
            "raw_score": float   # 0–100 trust score
        }
    """
    try:
        url = XDA_SEARCH.format(query=requests.utils.quote(app_name))
        resp = requests.get(url, headers=HEADERS, timeout=8)
        resp.raise_for_status()
        return _parse(resp.text)
    except Exception:
        return _unavailable()


# ── Internals ─────────────────────────────────────────────────────────────────

def _parse(html: str) -> dict:
    soup = BeautifulSoup(html, "html.parser")
    results = soup.select(".searchResult, .p-body-pageContent li")

    flags = []
    mention_count = len(results)

    for r in results:
        text = r.get_text(" ", strip=True).lower()
        if any(w in text for w in ["malware", "virus", "scam", "dangerous", "trojan"]):
            flags.append("xda:danger_mention")
            break

    if mention_count == 0:
        return _unavailable()

    # XDA presence is mildly positive (developer community awareness)
    raw_score = 60.0 if not flags else 30.0

    return {
        "available": True,
        "mention_count": mention_count,
        "flags": list(set(flags)),
        "raw_score": raw_score,
    }


def _unavailable() -> dict:
    return {
        "available": False,
        "mention_count": 0,
        "flags": [],
        "raw_score": 50.0,   # neutral, not penalized
    }