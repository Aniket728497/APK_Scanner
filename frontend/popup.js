const BACKEND_URL = "http://localhost:5000/check";

// Get current tab
function getCurrentTabUrl(callback) {
    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        const tab = tabs[0];
        if (!tab || !tab.url) return callback(null, null);

        try {
            const parsed = new URL(tab.url);
            const domain = parsed.hostname.replace(/^www\./, "");
            callback(parsed.href, domain);
        } catch {
            callback(null, null);
        }
    });
}

// Backend call
async function checkWebsiteSafety(fullUrl) {
    const res = await fetch(`${BACKEND_URL}?url=${encodeURIComponent(fullUrl)}`);
    if (!res.ok) throw new Error("Backend error");
    return res.json();
}

// UI update
function displayStatus(status, domain, stats = {}) {
    document.getElementById("checkingState").classList.add("hidden");

    if (status === "safe") {
        document.getElementById("safeState").classList.remove("hidden");
        document.getElementById("safeDomain").textContent = domain;

    } else if (status === "warning") {
        document.getElementById("warningState").classList.remove("hidden");
        document.getElementById("warningDomain").textContent = domain;

    } else if (status === "dangerous") {
        document.getElementById("dangerousState").classList.remove("hidden");
        document.getElementById("dangerousDomain").textContent = domain;

        const box = document.getElementById("duplicateWarning");
        const text = document.getElementById("duplicateText");

        if (stats.duplicate_of) {
            text.textContent = `This website is a duplicate of ${stats.duplicate_of}`;
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

function visitWebsite() {
    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        chrome.tabs.update(tabs[0].id, { active: true }, () => {
            window.close();
        });
    });
}

// ✅ FIXED GO BACK (WORKING VERSION)
function goBack() {
    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        const tab = tabs[0];

        // Try native back first
        chrome.tabs.goBack(tab.id, () => {
            if (!chrome.runtime.lastError) {
                window.close();
                return;
            }

            // Fallback 1: inject history.back()
            chrome.scripting.executeScript({
                target: { tabId: tab.id },
                func: () => {
                    if (window.history.length > 1) {
                        window.history.back();
                        return true;
                    }
                    return false;
                }
            }, (results) => {
                const success = results?.[0]?.result;

                if (success) {
                    window.close();
                } else {
                    // Fallback 2: manual fallback (go to Google or safe page)
                    chrome.tabs.update(tab.id, {
                        url: "https://www.google.com"
                    }, () => {
                        window.close();
                    });
                }
            });
        });
    });
}

// init
document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("visitBtn")?.addEventListener("click", visitWebsite);
    document.getElementById("backBtn")?.addEventListener("click", goBack);

    getCurrentTabUrl(async (url, domain) => {
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