# 📱 APK Scanner & Website Safety Checker

## 🔐 Cyber Crime Protection Tool (PS-Karnataka CB/CID Hackathon Project)

A real-time security system that protects users from:
- Malicious websites
- Phishing / spoofed domains
- Unsafe APK download sources

It combines:
- 🧠 URL spoof detection (Levenshtein similarity)
- 🛡 VirusTotal API scanning
- 🌐 Trusted domain verification
- 🧩 Chrome Extension
- ⚡ Flask backend analysis engine

---

# 🚀 Features

## 🔍 Website Safety Detection
- Scans every visited website in real-time
- Detects:
  - Malware/phishing (VirusTotal)
  - Fake or spoofed domains (e.g., `youtbe.com`)
  - Suspicious URL patterns

---

## 🧠 Spoof / Fake Domain Detection
- Uses Levenshtein distance algorithm
- Compares visited domain with trusted domains list
- Detects:
  - Typosquatting (goog1e.com, faceb00k.com)
  - Brand impersonation
  - Fake login pages

---

## 🛡 VirusTotal Integration
- Sends URL to VirusTotal API
- Returns:
  - Malicious engines count
  - Suspicious flags
  - Safe / unsafe classification

---

## 🌐 Chrome Extension Protection
- Automatically scans every visited website
- Shows warning banner if unsafe
- Popup UI displays detailed security report

---

## 📊 Risk Scoring System
The system evaluates URLs based on:
- HTTPS usage
- Domain similarity
- Suspicious keywords (login, verify, update, secure)
- VirusTotal results

Final output:
- ✅ Safe
- ⚠️ Warning
- 🚨 Dangerous


