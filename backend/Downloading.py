from flask import Flask, request, jsonify
from flask_cors import CORS
import requests
from typosquatting_detection import check_website, load_trusted_domains

app = Flask(__name__)
CORS(app)  # Required so the Chrome extension (different origin) can call this

API_KEY = "e9b917b846efe746575400bc55c3b34524011d605a61f9161470fcf960611ea0"
trusted_domains = load_trusted_domains("top_5_websites.txt")

def check_url(url):
    """Submit a URL to VirusTotal and return a structured safety result."""
    scan_endpoint = "https://www.virustotal.com/api/v3/urls"
    headers = {"x-apikey": API_KEY}

    # Step 1: Submit the URL for scanning
    submit = requests.post(scan_endpoint, headers=headers, data={"url": url})
    if submit.status_code != 200:
        return {
            "status": "error",
            "message": f"VirusTotal submission failed: {submit.text}"
        }

    # Step 2: Fetch the analysis result using the returned ID
    url_id = submit.json()["data"]["id"]
    analysis = requests.get(
        f"https://www.virustotal.com/api/v3/analyses/{url_id}",
        headers=headers
    ).json()

    stats = analysis["data"]["attributes"]["stats"]
    malicious = stats.get("malicious", 0)
    suspicious = stats.get("suspicious", 0)

    # Step 3: Map counts to a simple three-tier status
    if malicious > 3:
        status = "dangerous"
    elif malicious > 0 or suspicious > 0:
        status = "warning"
    else:
        status = "safe"

    return {
        "status": status,
        "malicious": malicious,
        "suspicious": suspicious,
        "harmless": stats.get("harmless", 0),
        "undetected": stats.get("undetected", 0)
    }


@app.route("/check", methods=["GET"])
def check():
    """
    GET /check?url=https://example.com
    Returns JSON: { status, malicious, suspicious, harmless, undetected }
    """
    url = request.args.get("url")
    if not url:
        return jsonify({"status": "error", "message": "Missing ?url= parameter"}), 400

    vt_result = check_url(url)

    typo_result = check_website(url, trusted_domains)

    # If typosquatting detected → override status
    if typo_result["is_duplicate"]:
        vt_result["status"] = "dangerous"
        vt_result["duplicate_of"] = typo_result["matched_domain"]
    else:
        vt_result["duplicate_of"] = None

    return jsonify(vt_result)


if __name__ == "__main__":
    # Run on port 5000 by default; extension calls http://localhost:5000/check
    app.run(debug=True)