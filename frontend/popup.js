// Simulated website safety database (for frontend demo)
const safeWebsites = [
    'google.com',
    'github.com',
    'stackoverflow.com',
    'twitter.com',
    'linkedin.com',
    'amazon.com',
    'facebook.com',
    'wikipedia.org',
    'youtube.com',
    'reddit.com'
];

const warningWebsites = [
    'bit.ly',
    'tinyurl.com',
    'short.link',
    'unknown-store.com',
    'cheap-deals-now.com'
];

const dangerousWebsites = [
    'malware-site.com',
    'phishing-bank.com',
    'fake-paypal.net',
    'virus-download.ru',
    'scam-lottery.xyz'
];

// Get current tab URL
function getCurrentTabUrl(callback) {
    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        const url = new URL(tabs[0].url);
        const domain = url.hostname.replace('www.', '');
        callback(domain);
    });
}

// Determine website safety status
function checkWebsiteSafety(domain) {
    // Simulate a slight delay for realism
    setTimeout(() => {
        let status = 'safe';

        if (dangerousWebsites.some(site => domain.includes(site))) {
            status = 'dangerous';
        } else if (warningWebsites.some(site => domain.includes(site))) {
            status = 'warning';
        } else if (!safeWebsites.some(site => domain.includes(site))) {
            // Unknown websites get a random status for demo variety
            const random = Math.random();
            if (random < 0.6) {
                status = 'safe';
            } else if (random < 0.85) {
                status = 'warning';
            } else {
                status = 'dangerous';
            }
        }

        displayStatus(status, domain);
    }, 1500);
}

// Display the appropriate status card
function displayStatus(status, domain) {
    // Hide checking state
    document.getElementById('checkingState').classList.add('hidden');

    // Show appropriate status
    if (status === 'safe') {
        document.getElementById('safeState').classList.remove('hidden');
        document.getElementById('safeDomain').textContent = domain;
    } else if (status === 'warning') {
        document.getElementById('warningState').classList.remove('hidden');
        document.getElementById('warningDomain').textContent = domain;
    } else if (status === 'dangerous') {
        document.getElementById('dangerousState').classList.remove('hidden');
        document.getElementById('dangerousDomain').textContent = domain;
    }

    // Update timestamp
    updateTimestamp();
}

// Update timestamp
function updateTimestamp() {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    document.getElementById('timestamp').textContent = `${hours}:${minutes}`;
}

// Visit website button
function visitWebsite() {
    window.close();
}

// Go back button
function goBack() {
    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        chrome.tabs.update(tabs[0].id, { url: 'about:blank' });
        window.close();
    });
}

// Initialize popup when it opens
document.addEventListener('DOMContentLoaded', () => {
    try {
        getCurrentTabUrl((domain) => {
            checkWebsiteSafety(domain);
        });
    } catch (error) {
        // If we can't get the tab URL, show safe state as default
        console.log('Using demo mode');
        displayStatus('safe', 'example.com');
    }
});