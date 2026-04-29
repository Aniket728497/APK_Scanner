// content.js
// Two-layer detection:
//   Layer 1 — Local heuristic (instant). Catches mod/APK/warez sites by
//              domain name and URL patterns. Fires popup immediately.
//   Layer 2 — VirusTotal via Flask backend. Catches everything else.
//              Fires popup only when VT says warning/dangerous.

(function () {
    "use strict";

    const BACKEND_URL = "http://localhost:5000/check";
    const currentURL  = window.location.href;
    const hostname    = window.location.hostname.toLowerCase().replace(/^www\./, "");
    const fullLow     = currentURL.toLowerCase();

    // ── Skip non-http pages (new tab, extensions, devtools, etc.) ────────────
    if (!currentURL.startsWith("http://") && !currentURL.startsWith("https://")) return;

    // ── Skip definitively safe domains — no check needed at all ─────────────
    const SAFE_DOMAINS = [
        "google.com","gmail.com","youtube.com","googleapis.com","gstatic.com",
        "github.com","github.io","githubusercontent.com",
        "microsoft.com","live.com","outlook.com","office.com","bing.com",
        "apple.com","icloud.com",
        "amazon.com","aws.amazon.com",
        "wikipedia.org","wikimedia.org",
        "reddit.com","redd.it",
        "stackoverflow.com","stackexchange.com",
        "openai.com","chatgpt.com","claude.ai","anthropic.com",
        "linkedin.com",
        "twitter.com","x.com",
        "facebook.com","instagram.com","meta.com",
        "netflix.com","spotify.com",
        "cloudflare.com","mozilla.org",
        "discord.com","twitch.tv",
        "paypal.com","stripe.com",
    ];
    if (SAFE_DOMAINS.some(d => hostname === d || hostname.endsWith("." + d))) return;

    // ─────────────────────────────────────────────────────────────────────────
    // LAYER 1 — Local mod/piracy heuristic
    // Mirrors the logic in Downloading.py so both layers agree.
    // ─────────────────────────────────────────────────────────────────────────

    const MOD_DOMAIN_KEYWORDS = [
        "happymod","moddroid","apkpure","apkmirror","apkcombo",
        "apkmonk","apkfab","apkdone","apkfollow","apksfull",
        "apk4fun","apknite","apkadmin","apkresult","apkvision",
        "apkaward","apkdl","apkboat","apkhome","apksmash",
        "apknight","apksum","apkgk","apkplz","apkzip",
        "apkget","apkload","apkgate","apkguard","apkdot",
        "getapk","pureapk","getjar","mob.org",
        "modyolo","liteapks","platinmods","androeed","pdalife",
        "rexdl","revdl","an1.com","sbenny","blackmod",
        "modcombo","apkshub","apkmagic","hiapphere","moddedzone",
        "andropalace","androidoyun","android-1",
        "nulled","warez","crackingpatching","cracksnow",
        "crackedit","warezbb","warezkeeper",
        "thepiratebay","1337x","rarbg","kickasstorrent",
        "torrentdownloads","torrentking","yts.mx",
    ];

    const MOD_DOMAIN_PATTERNS = [
        /apk[-_]?(pure|mirror|combo|monk|fab|done|nite|plz|mod|hack|crack|pro|premium|unlocked)/,
        /mod[-_]?(apk|droid|zone|combo|yolo|menu|game|android)/,
        /(hack|cheat|crack|patch)[-_]?(apk|android|mobile|game|app)/,
        /(free|download)[-_]?apks?/,
        /(nulled|warez|pirat)(ed)?\.(to|io|cc|ws|org|net|site|club|pw)/,
        /(premium|pro|paid)[-_]?(free|unlock|crack|mod)/,
        /unlimited[-_]?(gems|coins|gold|money|credits|lives|energy)/,
        /\bmodded?\b.*(apk|android|game|app)/,
    ];

    const MOD_PATH_PATTERNS = [
        /\.apk(\?|#|$)/,
        /\/(download|dl)\/.*(apk|mod|hack|crack)/,
        /\/(apk|mod|crack|warez|nulled)\//,
        /(mod|hack|cheat|crack|patch|premium|unlocked)[-_]?(apk|android)/,
        /v\d+[\._]\d+.*[-_]mod/,
        /\b(anti[-_]?ban|god[-_]?mode|aimbot|wallhack|unlimited[-_]gems)\b/,
    ];

    function isModSite() {
        // Check domain keywords
        for (const kw of MOD_DOMAIN_KEYWORDS) {
            if (hostname.includes(kw)) return true;
        }
        // Check domain patterns
        for (const re of MOD_DOMAIN_PATTERNS) {
            if (re.test(hostname)) return true;
        }
        // Check URL path patterns
        for (const re of MOD_PATH_PATTERNS) {
            if (re.test(fullLow)) return true;
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LAYER 2 — Backend / VirusTotal
    // ─────────────────────────────────────────────────────────────────────────

    function triggerPopup() {
        chrome.runtime.sendMessage({ type: "OPEN_POPUP_AUTO" });
    }

    // Layer 1: instant local check
    if (isModSite()) {
        console.log("[SafetyChecker] mod/piracy site detected locally:", hostname);
        triggerPopup();
        return; // no need to also hit the backend
    }

    // Layer 2: VT via backend (async, won't block page load)
    fetch(`${BACKEND_URL}?url=${encodeURIComponent(currentURL)}`)
        .then(res => {
            if (!res.ok) throw new Error("HTTP " + res.status);
            return res.json();
        })
        .then(data => {
            console.log("[SafetyChecker] VT result:", data.status, data);
            if (data.status === "warning" || data.status === "dangerous") {
                triggerPopup();
            }
        })
        .catch(err => {
            console.warn("[SafetyChecker] backend unreachable:", err.message);
        });

})();