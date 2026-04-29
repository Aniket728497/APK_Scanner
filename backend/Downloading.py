from flask import Flask, request, jsonify
from flask_cors import CORS
import requests
import time
import re
from urllib.parse import urlparse
from typosquatting_detection import check_website, load_trusted_domains

app = Flask(__name__)
CORS(app)

API_KEY = "e9b917b846efe746575400bc55c3b34524011d605a61f9161470fcf960611ea0"
trusted_domains = load_trusted_domains("top_5_websites.txt")


# ─────────────────────────────────────────────────────────────────────────────
# MOD / PIRACY HEURISTIC
# VirusTotal gives clean results for sites like HappyMod, APKPure, etc.
# because they are not technically malware — they are copyright-infringing
# app stores. VT cannot flag them. We must do it ourselves.
# ─────────────────────────────────────────────────────────────────────────────

# Any domain that contains one of these substrings is immediately flagged.
# Ordered longest-match first to avoid partial hits (e.g. "an1" inside "android").
MOD_DOMAIN_KEYWORDS = [
    # ── direct APK stores ──────────────────────────────────────────────────
    "happymod", "moddroid", "apkpure", "apkmirror", "apkcombo",
    "apkmonk",  "apkfab",  "apkdone", "apkfollow", "apksfull",
    "apk4fun",  "apknite", "apkadmin","apkresult", "apkvision",
    "apkaward", "apkdl",   "apkboat", "apkhome",   "apksmash",
    "apknight", "apksum",  "apkgk",   "apkplz",    "apkzip",
    "apkget",   "apkload", "apkgate", "apkguard",  "apkdot",
    "getapk",   "pureapk", "getjar",  "mob.org",
    # ── mod / hack sites ──────────────────────────────────────────────────
    "modyolo",  "liteapks","platinmods","androeed", "pdalife",
    "rexdl",    "revdl",   "an1.com",  "sbenny",   "blackmod",
    "modcombo", "apkshub", "apkmagic", "hiapphere","moddedzone",
    "andropalace","androidoyun","android-1","apkboat",
    # ── warez / nulled / crack ────────────────────────────────────────────
    "nulled",   "warez",   "crackingpatching","cracksnow",
    "crackedit","warezbb", "warezkeeper",
    # ── torrent / piracy ─────────────────────────────────────────────────
    "thepiratebay","1337x","rarbg","kickasstorrent",
    "torrentdownloads","torrentking","yts.mx",
]

# Regex patterns on the full hostname (catches things like "mod-apk-pro.com")
MOD_DOMAIN_PATTERNS = [
    r"apk[-_]?(pure|mirror|combo|monk|fab|done|nite|plz|mod|hack|crack|pro|premium|unlocked)",
    r"mod[-_]?(apk|droid|zone|combo|yolo|menu|game|android)",
    r"(hack|cheat|crack|patch)[-_]?(apk|android|mobile|game|app)",
    r"(free|download)[-_]?apks?",
    r"(nulled|warez|pirat)(ed)?\.(to|io|cc|ws|org|net|site|club|pw)",
    r"(premium|pro|paid)[-_]?(free|unlock|crack|mod)",
    r"unlimited[-_]?(gems|coins|gold|money|credits|lives|energy)",
    r"\bmodded?\b.*(apk|android|game|app)",
]

# Regex patterns on the full URL path (catches direct .apk links etc.)
MOD_PATH_PATTERNS = [
    r"\.apk(\?|#|$)",
    r"/(download|dl)/.*(apk|mod|hack|crack)",
    r"/(apk|mod|crack|warez|nulled)/",
    r"(mod|hack|cheat|crack|patch|premium|unlocked)[-_]?(apk|android)",
    r"v\d+[\._]\d+.*[-_]mod",       # versioned mod: v2.1.3-mod
    r"\b(anti[-_]?ban|god[-_]?mode|aimbot|wallhack|unlimited[-_]gems)\b",
]


def check_mod_heuristic(url: str) -> dict:
    """
    Returns {"flagged": True/False, "reason": str}.
    Runs entirely locally — no network call.
    """
    try:
        parsed   = urlparse(url)
        hostname = parsed.netloc.lower().replace("www.", "")
        fullpath = (parsed.path + "?" + parsed.query).lower()
    except Exception:
        return {"flagged": False, "reason": ""}

    # 1. Exact keyword in domain
    for kw in MOD_DOMAIN_KEYWORDS:
        if kw in hostname:
            return {"flagged": True, "reason": f"Known mod/piracy domain keyword: '{kw}'"}

    # 2. Domain pattern
    for pat in MOD_DOMAIN_PATTERNS:
        if re.search(pat, hostname):
            return {"flagged": True, "reason": f"Domain matches mod pattern: {pat}"}

    # 3. URL path pattern
    for pat in MOD_PATH_PATTERNS:
        if re.search(pat, fullpath):
            return {"flagged": True, "reason": f"URL path matches mod pattern: {pat}"}

    return {"flagged": False, "reason": ""}


# ─────────────────────────────────────────────────────────────────────────────
# VIRUSTOTAL CHECK  (still runs for sites that don't match the heuristic)
# ─────────────────────────────────────────────────────────────────────────────

def check_url_vt(url: str) -> dict:
    """Submit URL to VT, poll until completed, return structured result."""
    scan_endpoint = "https://www.virustotal.com/api/v3/urls"
    headers = {"x-apikey": API_KEY}

    try:
        submit = requests.post(scan_endpoint, headers=headers,
                               data={"url": url}, timeout=10)
    except requests.exceptions.RequestException as e:
        return {"status": "unknown", "malicious": 0, "suspicious": 0,
                "harmless": 0, "undetected": 0, "vt_error": str(e)}

    if submit.status_code not in (200, 409):
        return {"status": "unknown", "malicious": 0, "suspicious": 0,
                "harmless": 0, "undetected": 0,
                "vt_error": f"VT HTTP {submit.status_code}"}

    url_id       = submit.json()["data"]["id"]
    analysis_url = f"https://www.virustotal.com/api/v3/analyses/{url_id}"
    stats        = None

    for attempt in range(6):          # poll up to ~12 s
        time.sleep(2)
        try:
            resp = requests.get(analysis_url, headers=headers, timeout=10)
            if resp.status_code != 200:
                continue
            body   = resp.json()
            status = body["data"]["attributes"].get("status", "")
            if status == "completed":
                stats = body["data"]["attributes"]["stats"]
                break
            print(f"[VT] attempt {attempt+1}: status={status}")
        except Exception:
            continue

    if stats is None:
        return {"status": "unknown", "malicious": 0, "suspicious": 0,
                "harmless": 0, "undetected": 0, "vt_error": "timed out"}

    malicious  = stats.get("malicious",  0)
    suspicious = stats.get("suspicious", 0)
    harmless   = stats.get("harmless",   0)
    undetected = stats.get("undetected", 0)

    if malicious >= 5:
        vt_status = "dangerous"
    elif malicious >= 1 or suspicious >= 3:
        vt_status = "warning"
    else:
        vt_status = "safe"

    return {"status": vt_status, "malicious": malicious,
            "suspicious": suspicious, "harmless": harmless,
            "undetected": undetected}


# ─────────────────────────────────────────────────────────────────────────────
# FLASK ROUTE
# ─────────────────────────────────────────────────────────────────────────────

@app.route("/check", methods=["GET"])
def check():
    url = request.args.get("url")
    if not url:
        return jsonify({"status": "error", "message": "Missing ?url= parameter"}), 400

    # ── Layer 1: local mod/piracy heuristic (instant, no network) ────────────
    heuristic = check_mod_heuristic(url)
    if heuristic["flagged"]:
        # Don't bother calling VT — we already know this site is a mod store.
        # Still run typosquatting so duplicate_of is populated if relevant.
        typo = check_website(url, trusted_domains)
        return jsonify({
            "status":       "warning",      # warning not dangerous — user can still proceed
            "malicious":    0,
            "suspicious":   0,
            "harmless":     0,
            "undetected":   0,
            "mod_flagged":  True,
            "mod_reason":   heuristic["reason"],
            "duplicate_of": typo["matched_domain"] if typo["is_duplicate"] else None,
        })

    # ── Layer 2: VirusTotal ───────────────────────────────────────────────────
    vt_result   = check_url_vt(url)
    typo_result = check_website(url, trusted_domains)

    if typo_result["is_duplicate"]:
        vt_result["status"]       = "dangerous"
        vt_result["duplicate_of"] = typo_result["matched_domain"]
    else:
        vt_result["duplicate_of"] = None

    vt_result["mod_flagged"] = False
    vt_result["mod_reason"]  = ""
    return jsonify(vt_result)


if __name__ == "__main__":
    app.run(debug=True)