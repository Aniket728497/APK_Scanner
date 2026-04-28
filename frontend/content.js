const currentURL = window.location.href;

console.log("Scanning:", currentURL);

fetch("http://localhost:5000/", {
    method: "POST",
    headers: {
        "Content-Type": "application/json"
    },
    body: JSON.stringify({ url: currentURL })
})
.then(res => res.text())
.then(data => {
    console.log("Result:", data);

    if (data.includes("WARNING")) {
        // Show warning banner instead of alert
        const banner = document.createElement("div");
        banner.innerText = "⚠️ This website may be unsafe!";
        banner.style.position = "fixed";
        banner.style.top = "0";
        banner.style.left = "0";
        banner.style.width = "100%";
        banner.style.background = "red";
        banner.style.color = "white";
        banner.style.padding = "10px";
        banner.style.zIndex = "9999";
        banner.style.textAlign = "center";

        document.body.appendChild(banner);
    }
})
.catch(err => console.log("Error:", err));