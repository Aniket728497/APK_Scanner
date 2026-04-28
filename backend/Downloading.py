from flask import Flask, request
import requests

app = Flask(__name__)

API_KEY = "e9b917b846efe746575400bc55c3b34524011d605a61f9161470fcf960611ea0"  # Replace with your actual VirusTotal key
url = "https://github.com/AmruthaHavale"
def check_url(url):
    scan_url = "https://www.virustotal.com/api/v3/urls"
    headers = {"x-apikey": API_KEY}
    response = requests.post(scan_url, headers=headers, data={"url": url})

    if response.status_code == 200:
        url_id = response.json()["data"]["id"]
        analysis_url = f"https://www.virustotal.com/api/v3/analyses/{url_id}"
        result = requests.get(analysis_url, headers=headers).json()
        stats = result["data"]["attributes"]["stats"]
        malicious = stats.get("malicious", 0)

        if malicious > 3:
            return f"⚠️ WARNING: {url} is unsafe! ({malicious} engines flagged it)"
        else:
            return f"✅ {url} is safe. ({malicious} minor flags ignored)"
    else:
        return f"Error submitting URL: {response.text}"

@app.route("/", methods=["GET"])
def home():
    if not url:
        return "Please provide a URL using ?url=<your_url>"
    return check_url(url)

if __name__ == "__main__":
    app.run(debug=True)