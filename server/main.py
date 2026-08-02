"""
SnapLoad API Server — Python Flask (القسم الخامس — نسخة كاملة محدّثة)
endpoints: /ping, /, /info, /download-url, /formats, /update
"""
import os
import re
import time
import threading
import subprocess
import urllib.request
from flask import Flask, request, jsonify
from flask_cors import CORS
import yt_dlp

app = Flask(__name__)
CORS(app)

# ─── In-memory cache لمدة 5 دقائق ────────────────────────────────────────────
_cache: dict = {}
CACHE_TTL = 300


def cache_get(key: str):
    entry = _cache.get(key)
    if entry and time.time() - entry["ts"] < CACHE_TTL:
        return entry["data"]
    return None


def cache_set(key: str, data):
    _cache[key] = {"data": data, "ts": time.time()}


# ─── User-Agent دوراني ─────────────────────────────────────────────────────────
USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/17.3 Safari/605.1.15",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/123.0.0.0 Safari/537.36",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 Safari/604.1",
]
_ua_index = 0


def next_ua() -> str:
    global _ua_index
    ua = USER_AGENTS[_ua_index % len(USER_AGENTS)]
    _ua_index += 1
    return ua


def ydl_opts(extra: dict = None) -> dict:
    base = {
        "quiet": True,
        "no_warnings": True,
        "socket_timeout": 30,
        "http_headers": {"User-Agent": next_ua()},
        "nocheckcertificate": True,
    }
    if extra:
        base.update(extra)
    return base


def classify_format(f: dict) -> str:
    has_v = f.get("vcodec", "none") not in (None, "none")
    has_a = f.get("acodec", "none") not in (None, "none")
    if has_v and has_a: return "video+audio"
    if has_v: return "video"
    if has_a: return "audio"
    return "unknown"


def format_entry(f: dict) -> dict:
    height = f.get("height") or 0
    quality = f"{height}p" if height else f.get("format_note", f.get("format_id", ""))
    return {
        "format_id": f.get("format_id", ""),
        "quality": quality,
        "ext": f.get("ext", "mp4"),
        "type": classify_format(f),
        "filesize": f.get("filesize") or f.get("filesize_approx"),
        "tbr": f.get("tbr"),
        "url": f.get("url"),
    }


@app.route("/ping", methods=["GET"])
def ping():
    return jsonify({"status": "ok", "message": "SnapLoad API is running 🚀"})


@app.route("/", methods=["GET"])
def index():
    return jsonify({
        "name": "SnapLoad API",
        "version": "1.0.0",
        "endpoints": ["/ping", "/info", "/download-url", "/formats", "/update"],
    })


@app.route("/info", methods=["POST"])
def get_info():
    data = request.get_json(silent=True) or {}
    url = (data.get("url") or "").strip()
    if not url:
        return jsonify({"error": "الرابط مطلوب"}), 400

    cached = cache_get(f"info:{url}")
    if cached:
        return jsonify(cached)

    try:
        with yt_dlp.YoutubeDL(ydl_opts()) as ydl:
            info = ydl.extract_info(url, download=False)

        if not info:
            return jsonify({"error": "لم يتم العثور على الفيديو"}), 404

        if info.get("_type") == "playlist":
            entries = info.get("entries") or []
            if not entries:
                return jsonify({"error": "القائمة فارغة"}), 404
            info = entries[0]

        raw_formats = info.get("formats") or []
        formats = [format_entry(f) for f in raw_formats if classify_format(f) != "unknown"]

        result = {
            "title": info.get("title", ""),
            "thumbnail": info.get("thumbnail", ""),
            "duration": info.get("duration"),
            "uploader": info.get("uploader", info.get("channel", "")),
            "platform": info.get("extractor_key", info.get("extractor", "")),
            "formats": formats,
            "direct_url": info.get("url"),
        }
        cache_set(f"info:{url}", result)
        return jsonify(result)

    except yt_dlp.utils.DownloadError as e:
        msg = str(e)
        if "Private video" in msg:
            return jsonify({"error": "الفيديو خاص"}), 403
        if "not available" in msg:
            return jsonify({"error": "الفيديو غير متاح في منطقتك"}), 451
        return jsonify({"error": f"خطأ: {msg}"}), 400
    except Exception as e:
        return jsonify({"error": f"خطأ داخلي: {str(e)}"}), 500


@app.route("/download-url", methods=["POST"])
def get_download_url():
    data = request.get_json(silent=True) or {}
    url = (data.get("url") or "").strip()
    format_id = (data.get("format_id") or "best").strip()
    if not url:
        return jsonify({"error": "الرابط مطلوب"}), 400

    cache_key = f"dl:{url}:{format_id}"
    cached = cache_get(cache_key)
    if cached:
        return jsonify(cached)

    try:
        with yt_dlp.YoutubeDL(ydl_opts({"format": format_id})) as ydl:
            info = ydl.extract_info(url, download=False)

        if not info:
            return jsonify({"error": "لم يتم العثور على الفيديو"}), 404

        direct_url = info.get("url")
        if not direct_url:
            for f in reversed(info.get("formats") or []):
                if f.get("url"):
                    direct_url = f["url"]
                    break

        if not direct_url:
            return jsonify({"error": "لا يوجد رابط تحميل مباشر"}), 404

        result = {
            "title": info.get("title", ""),
            "ext": info.get("ext", "mp4"),
            "direct_url": direct_url,
            "thumbnail": info.get("thumbnail", ""),
        }
        cache_set(cache_key, result)
        return jsonify(result)

    except yt_dlp.utils.DownloadError as e:
        return jsonify({"error": f"خطأ: {str(e)}"}), 400
    except Exception as e:
        return jsonify({"error": f"خطأ داخلي: {str(e)}"}), 500


@app.route("/formats", methods=["POST"])
def get_formats():
    data = request.get_json(silent=True) or {}
    url = (data.get("url") or "").strip()
    if not url:
        return jsonify({"error": "الرابط مطلوب"}), 400

    cached = cache_get(f"formats:{url}")
    if cached:
        return jsonify(cached)

    try:
        with yt_dlp.YoutubeDL(ydl_opts()) as ydl:
            info = ydl.extract_info(url, download=False)

        formats = [format_entry(f) for f in (info.get("formats") or [])
                   if classify_format(f) != "unknown"]
        result = {"formats": formats, "title": info.get("title", "")}
        cache_set(f"formats:{url}", result)
        return jsonify(result)

    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/update", methods=["POST"])
def update_ytdlp():
    try:
        result = subprocess.run(
            ["pip", "install", "--upgrade", "yt-dlp", "--quiet"],
            capture_output=True, text=True, timeout=120
        )
        if result.returncode == 0:
            _cache.clear()
            ver = subprocess.run(["yt-dlp", "--version"],
                                 capture_output=True, text=True, timeout=10)
            return jsonify({"status": "updated", "version": ver.stdout.strip()})
        return jsonify({"status": "error", "message": result.stderr}), 500
    except subprocess.TimeoutExpired:
        return jsonify({"error": "انتهت مهلة التحديث"}), 504
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ─── Keep-alive ────────────────────────────────────────────────────────────────
def _keep_alive():
    server_url = os.environ.get("RENDER_EXTERNAL_URL", "")
    if not server_url:
        return
    while True:
        try:
            urllib.request.urlopen(f"{server_url}/ping", timeout=10)
        except Exception:
            pass
        time.sleep(240)


def start_keep_alive():
    threading.Thread(target=_keep_alive, daemon=True).start()


if __name__ == "__main__":
    start_keep_alive()
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port, debug=False)
else:
    start_keep_alive()
