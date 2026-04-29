// background.js — MV3 service worker
// content.js calls chrome.runtime.sendMessage({ type: "OPEN_POPUP_AUTO" })
// We open popup.html as a small popup window, passing the tabId so popup.js
// knows which tab to act on when the user clicks Go Back / Continue.

const POPUP_COOLDOWN_MS = 10000; // don't re-open for the same tab within 10 s
const recentlyOpened = new Map(); // tabId → timestamp

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.type !== "OPEN_POPUP_AUTO") return;

    const tabId = sender.tab?.id;
    if (!tabId) return;

    // Deduplicate: ignore if we already opened a popup for this tab recently
    const last = recentlyOpened.get(tabId) || 0;
    if (Date.now() - last < POPUP_COOLDOWN_MS) {
        sendResponse({ ok: false, reason: "cooldown" });
        return;
    }
    recentlyOpened.set(tabId, Date.now());

    const popupUrl = chrome.runtime.getURL("popup.html") + "?tabId=" + tabId + "&auto=1";

    chrome.windows.create({
        url:     popupUrl,
        type:    "popup",
        width:   390,
        height:  580,
        focused: true,
    }, () => sendResponse({ ok: true }));

    return true; // keep message channel open for async sendResponse
});