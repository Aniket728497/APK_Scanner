import requests

# Predefined list of safe app download websites
safe_sites = [
    "https://play.google.com",
    "https://apps.apple.com",
    "https://www.microsoft.com/store",
    "https://f-droid.org",
    "https://www.amazon.com/appstore",
    "https://www.ninite.com",
    "https://filehippo.com",
    "https://www.techspot.com/downloads/",
    "https://www.majorgeeks.com",
    "https://filehorse.com",
    "https://softonic.com",
    "https://sourceforge.net",
    "https://download.cnet.com",
    "https://apkmirror.com",
    "https://apkpure.com",
    "https://uptodown.com",
    "https://getintopc.com",
    "https://portableapps.com",
    "https://snapcraft.io",
    "https://flathub.org",
    # … you can keep extending this list
]

# To reach 100+, you can duplicate categories or pull from feeds
# Example: fetch Alexa/Tranco top sites and filter for app/software keywords

# Save to txt file
with open("app_download_sites.txt", "w") as f:
    for site in safe_sites:
        f.write(site + "\n")

print(f"Saved {len(safe_sites)} app download websites to app_download_sites.txt")
