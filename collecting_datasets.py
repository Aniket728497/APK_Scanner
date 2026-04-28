import requests
import numpy as np
import pandas as pd

API_KEY = "e9b917b846efe746575400bc55c3b34524011d605a61f9161470fcf960611ea0"
headers = {"x-apikey": API_KEY}

# Example list of URLs to scan
with open("urls.txt") as f:
    urls = [line.strip() for line in f if line.strip()]


data = []

for url in urls:
    # Step 1: Submit URL to VirusTotal
    scan_url = "https://www.virustotal.com/api/v3/urls"
    response = requests.post(scan_url, headers=headers, data={"url": url})
    if response.status_code != 200:
        print(f"Error scanning {url}: {response.text}")
        continue

    url_id = response.json()["data"]["id"]

    # Step 2: Get analysis results
    analysis_url = f"https://www.virustotal.com/api/v3/analyses/{url_id}"
    result = requests.get(analysis_url, headers=headers).json()
    stats = result["data"]["attributes"]["stats"]

    malicious = stats.get("malicious", 0)
    suspicious = stats.get("suspicious", 0)
    harmless = stats.get("harmless", 0)
    url_length = len(url)

    # Step 3: Label (simple rule: >3 malicious = unsafe)
    label = 1 if malicious > 3 else 0

    data.append([malicious, suspicious, harmless, url_length, label])

# Step 4: Save dataset
df = pd.DataFrame(data, columns=["malicious", "suspicious", "harmless", "url_length", "label"])

# Append mode: 'a' = append, header=False prevents writing column names again
df.to_csv("url_dataset.csv", mode="a", header=False, index=False)


print("Dataset saved as url_dataset.csv")
