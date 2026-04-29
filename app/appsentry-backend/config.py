# ── Timing ──────────────────────────────────────────────
CACHE_TTL_SECONDS = 60 * 60 * 6   # 6 hours

# ── Scoring weights (community side only) ───────────────
WEIGHT_REDDIT      = 0.40
WEIGHT_PLAY_STORE  = 0.45
WEIGHT_XDA         = 0.15

# ── Reddit ───────────────────────────────────────────────
REDDIT_SUBREDDITS  = ["androidapps", "Android", "privacy"]
REDDIT_MAX_POSTS   = 10

# ── Thresholds ───────────────────────────────────────────
MIN_REDDIT_SCORE_TO_TRUST = 5     # posts with score < 5 are ignored
PLAY_STORE_GOOD_RATING    = 4.0
PLAY_STORE_MIN_INSTALLS   = 10_000

# ── Server ───────────────────────────────────────────────
HOST = "0.0.0.0"
PORT = 5000
DEBUG = True