const BACKEND_URL = "http://localhost:5000/check";

// Get the full URL of the active tab and kick off a safety check
function getCurrentTabUrl(callback) {
    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        const tab = tabs[0];
        if (!tab || !tab.url) {
            callback(null, null);
            return;
        }
        try {
            const parsed = new URL(tab.url);
            const domain = parsed.hostname.replace(/^www\./, "");
            callback(parsed.href, domain);
        } catch {
            callback(null, null);
        }
    });
}

// Call the Flask backend and return the safety result
async function checkWebsiteSafety(fullUrl) {
    const response = await fetch(
        `${BACKEND_URL}?url=${encodeURIComponent(fullUrl)}`,
        { method: "GET" }
    );
    if (!response.ok) {
        throw new Error(`Backend returned ${response.status}`);
    }
    return response.json(); // { status, malicious, suspicious, harmless, undetected }
}

// Populate and reveal the right status card
function displayStatus(status, domain, stats = {}) {
    document.getElementById("checkingState").classList.add("hidden");

    if (status === "safe") {
        const card = document.getElementById("safeState");
        document.getElementById("safeDomain").textContent = domain;
        // Enrich the detail rows with real numbers if available
        const items = card.querySelectorAll(".detail-item span:last-child");
        if (items[0]) items[0].textContent = "No threats detected";
        if (items[1]) items[1].textContent = "Secure connection verified";
        if (items[2]) items[2].textContent =
            stats.harmless != null
                ? `${stats.harmless} engines confirm safe`
                : "Trusted domain";
        card.classList.remove("hidden");

    } else if (status === "warning") {
        const card = document.getElementById("warningState");
        document.getElementById("warningDomain").textContent = domain;
        const items = card.querySelectorAll(".detail-item span:last-child");
        if (items[0]) items[0].textContent =
            `${stats.suspicious ?? 0} engine(s) flagged as suspicious`;
        if (items[1]) items[1].textContent = "Verify site authenticity before proceeding";
        card.classList.remove("hidden");

    } else if (status === "dangerous") {
        const card = document.getElementById("dangerousState");
        document.getElementById("dangerousDomain").textContent = domain;
        const items = card.querySelectorAll(".detail-item span:last-child");
        if (items[0]) items[0].textContent =
            `${stats.malicious ?? "Multiple"} engine(s) flagged as malicious`;
        if (items[1]) items[1].textContent = "Do not enter credentials or personal data";
        card.classList.remove("hidden");

    } else {
        // Fallback: treat errors as warning so the user isn't blocked
        document.getElementById("warningDomain").textContent = domain;
        const card = document.getElementById("warningState");
        const items = card.querySelectorAll(".detail-item span:last-child");
        if (items[0]) items[0].textContent = "Could not reach safety service";
        if (items[1]) items[1].textContent = "Proceed with caution";
        card.classList.remove("hidden");
    }

    updateTimestamp();
}

function updateTimestamp() {
    const now = new Date();
    const hh = String(now.getHours()).padStart(2, "0");
    const mm = String(now.getMinutes()).padStart(2, "0");
    document.getElementById("timestamp").textContent = `${hh}:${mm}`;
}

function visitWebsite() {
    window.close();
}

function goBack() {
    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        chrome.tabs.update(tabs[0].id, { url: "about:blank" });
        window.close();
    });
}

// ── Entry point ───────────────────────────────────
document.addEventListener("DOMContentLoaded", () => {
    getCurrentTabUrl(async (fullUrl, domain) => {
        if (!fullUrl || !domain) {
            displayStatus("safe", "unknown site");
            return;
        }

        try {
            const result = await checkWebsiteSafety(fullUrl);
            displayStatus(result.status, domain, result);
        } catch (err) {
            console.error("Safety check failed:", err);
            // Show warning (not safe) so the user is aware something went wrong
            displayStatus("error", domain);
        }
    });
});
