from flask import Flask, request, jsonify
from analyzer import community_scorer
from cache import file_cache
from config import HOST, PORT, DEBUG, CACHE_TTL_SECONDS

app = Flask(__name__)


@app.route("/analyze", methods=["GET"])
def analyze():
    package_name = request.args.get("package", "").strip()
    app_name     = request.args.get("name", package_name).strip()

    if not package_name:
        return jsonify({"error": "Missing 'package' parameter"}), 400

    # ── Check cache first ─────────────────────────────────────────────────
    cache_key = f"analyze:{package_name}"
    cached = file_cache.get(cache_key, CACHE_TTL_SECONDS)
    if cached:
        cached["from_cache"] = True
        return jsonify(cached)

    # ── Run community analysis ────────────────────────────────────────────
    # All scrapers degrade gracefully — this will ALWAYS return a result
    result = community_scorer.compute(package_name, app_name)
    result["package_name"] = package_name
    result["from_cache"]   = False

    # ── Cache the result ──────────────────────────────────────────────────
    file_cache.set(cache_key, result)

    return jsonify(result)


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(host=HOST, port=PORT, debug=DEBUG)