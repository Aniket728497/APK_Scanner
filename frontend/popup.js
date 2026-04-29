const BACKEND_URL = "http://localhost:5000/check";

// ── Tab resolution ────────────────────────────────────────────────────────────
// Supports two modes:
//   NORMAL: user clicked the extension icon → query active tab
//   AUTO:   background.js opened us with ?tabId=NNN&auto=1
const urlParams  = new URLSearchParams(window.location.search);
const paramTabId = urlParams.get("tabId") ? parseInt(urlParams.get("tabId"), 10) : null;

function getCurrentTabUrl(callback) {
    if (paramTabId) {
        // We were opened automatically — use the exact tab that triggered us
        chrome.tabs.get(paramTabId, (tab) => {
            if (chrome.runtime.lastError || !tab?.url) return callback(null, null);
            try {
                const parsed = new URL(tab.url);
                callback(parsed.href, parsed.hostname.replace(/^www\./, ""), tab.id);
            } catch { callback(null, null); }
        });
    } else {
        // Normal popup click
        chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
            const tab = tabs[0];
            if (!tab?.url) return callback(null, null);
            try {
                const parsed = new URL(tab.url);
                callback(parsed.href, parsed.hostname.replace(/^www\./, ""), tab.id);
            } catch { callback(null, null); }
        });
    }
}

// ── Backend call ──────────────────────────────────────────────────────────────
async function checkWebsiteSafety(fullUrl) {
    const res = await fetch(`${BACKEND_URL}?url=${encodeURIComponent(fullUrl)}`);
    if (!res.ok) throw new Error("Backend error");
    return res.json();
}

// ── UI update ─────────────────────────────────────────────────────────────────
function displayStatus(status, domain, stats = {}) {
    document.getElementById("checkingState").classList.add("hidden");

    if (status === "safe") {
        document.getElementById("safeState").classList.remove("hidden");
        document.getElementById("safeDomain").textContent = domain;

    } else if (status === "warning") {
        document.getElementById("warningState").classList.remove("hidden");
        document.getElementById("warningDomain").textContent = domain;

        // Show specific reason if available
        const rows = document.querySelectorAll("#warningState .detail-item span:last-child");
        if (rows[0]) {
            if (stats.mod_flagged) {
                rows[0].textContent = "Pirated / unofficial APK distribution site";
            } else if (stats.suspicious > 0) {
                rows[0].textContent = `${stats.suspicious} security engine(s) flagged this site`;
            } else {
                rows[0].textContent = "Potential security issues detected";
            }
        }
        if (rows[1]) rows[1].textContent = "Downloads here may contain malware or unwanted software";

    } else if (status === "dangerous") {
        document.getElementById("dangerousState").classList.remove("hidden");
        document.getElementById("dangerousDomain").textContent = domain;

        const box  = document.getElementById("duplicateWarning");
        const text = document.getElementById("duplicateText");
        if (stats.duplicate_of) {
            text.textContent = `This site appears to impersonate ${stats.duplicate_of}`;
            box.classList.remove("hidden");
        } else {
            box.classList.add("hidden");
        }
    }

    updateTimestamp();
}

function updateTimestamp() {
    const now = new Date();
    document.getElementById("timestamp").textContent =
        `${now.getHours()}:${String(now.getMinutes()).padStart(2, "0")}`;
}

// ── Buttons ───────────────────────────────────────────────────────────────────
let resolvedTabId = null;

function visitWebsite() {
    const tid = resolvedTabId || paramTabId;
    if (tid) {
        chrome.tabs.update(tid, { active: true }, () => window.close());
    } else {
        window.close();
    }
}

function goBack() {
    const tid = resolvedTabId || paramTabId;
    if (!tid) { window.close(); return; }

    chrome.tabs.goBack(tid, () => {
        if (!chrome.runtime.lastError) { window.close(); return; }

        chrome.scripting.executeScript({
            target: { tabId: tid },
            func: () => {
                if (window.history.length > 1) { window.history.back(); return true; }
                return false;
            }
        }, (results) => {
            if (results?.[0]?.result) {
                window.close();
            } else {
                chrome.tabs.update(tid, { url: "https://www.google.com" }, () => window.close());
            }
        });
    });
}

// ── Init ──────────────────────────────────────────────────────────────────────
document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("visitBtn")?.addEventListener("click", visitWebsite);
    document.getElementById("backBtn")?.addEventListener("click", goBack);

    getCurrentTabUrl(async (url, domain, tabId) => {
        resolvedTabId = tabId;
        if (!url) return displayStatus("safe", "unknown");

        try {
            const result = await checkWebsiteSafety(url);
            displayStatus(result.status, domain, result);
        } catch (e) {
            console.error(e);
            displayStatus("warning", domain);
        }
    });
});