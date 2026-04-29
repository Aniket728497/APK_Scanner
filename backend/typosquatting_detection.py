import Levenshtein
from urllib.parse import urlparse

def load_trusted_domains(file_path):
    domains = []
    with open(file_path, "r") as f:
        for line in f:
            if "http" in line:
                url = line.strip()
                domain = urlparse(url).netloc.replace("www.", "")
                domains.append(domain)
    return domains


def get_domain(url):
    return urlparse(url).netloc.replace("www.", "")


def is_https(url):
    return url.startswith("https://")


def check_website(url, trusted_domains):
    input_domain = get_domain(url)

    highest_similarity = 0
    best_match = None

    for trusted in trusted_domains:
        similarity = Levenshtein.ratio(input_domain, trusted)

        if similarity > highest_similarity:
            highest_similarity = similarity
            best_match = trusted

    risk_score = 0

    # 🔴 Spoof detection (very important)
    if highest_similarity > 0.85 and input_domain != best_match:
        risk_score += 5

    # 🔴 No HTTPS
    if not is_https(url):
        risk_score += 2

    # 🔴 Suspicious patterns
    if any(x in input_domain for x in ["login", "secure", "verify", "update"]):
        risk_score += 2

    # ✅ Final Decision
    if risk_score >= 5:
        return {
            "is_duplicate": True,
            "matched_domain": best_match
        }
    else:
        return {
            "is_duplicate": False,
            "matched_domain": None
        }


# Run
if __name__ == "__main__":
    trusted_domains = load_trusted_domains("website_list.txt")

    url = input("Enter URL: ")
    print(check_website(url, trusted_domains))