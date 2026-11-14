#!/usr/bin/env python3
"""
FLYSTIC Hackathon Local Server (Zero external dependencies, Python 3 + SQLite).
Mirrors backend/src/worker.ts /api/v1/* endpoints so Android & Web apps can use
this Mac as the live server during Hackathon demos.
"""

import base64
import hashlib
import hmac
import json
import os
import re
import secrets
import socket
import sqlite3
import sys
import threading
import time
import urllib.error
import urllib.request
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

PORT = int(os.environ.get("PORT", "8787"))
DB_PATH = os.environ.get(
    "FLYSTIC_DB_PATH",
    os.path.join(os.path.dirname(os.path.abspath(__file__)), ".local_demo.sqlite"),
)
AUTH_SECRET = os.environ.get("AUTH_SECRET", "hackathon-local-auth-secret-2026")
ADMIN_INVITE_CODE = os.environ.get("ADMIN_INVITE_CODE", "HUST-2026")
PASSWORD_ITERATIONS = 16_384
TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
POINT_BASE = {"PET": 10, "HDPE": 10, "PP": 8, "LDPE": 2, "PS": 1}
CLEAN_MULTIPLIER = {"CLEAN": 1.5, "NORMAL": 1.0, "DIRTY": 0.5}
AUTH_MAX_FAILURES = 5
AUTH_FAILURE_WINDOW_MS = 15 * 60_000
AUTH_LOCK_MS = 15 * 60_000

DB_LOCK = threading.Lock()
CONN = sqlite3.connect(DB_PATH, check_same_thread=False)
CONN.row_factory = sqlite3.Row


class HttpError(Exception):
    def __init__(self, status: int, message: str):
        super().__init__(message)
        self.status = status
        self.message = message


def now_ms() -> int:
    return int(time.time() * 1000)


def b64url_encode(raw: bytes) -> str:
    return base64.urlsafe_b64encode(raw).decode("ascii").rstrip("=")


def b64url_decode(text: str) -> bytes:
    pad = "=" * ((4 - len(text) % 4) % 4)
    return base64.urlsafe_b64decode(text + pad)


def digest_b64(value: str) -> str:
    return b64url_encode(hashlib.sha256(value.encode("utf-8")).digest())


def hash_password(password: str, salt: bytes = None):
    if salt is None:
        salt = os.urandom(16)
    dk = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, PASSWORD_ITERATIONS, 32)
    return f"py1${PASSWORD_ITERATIONS}${b64url_encode(dk)}", b64url_encode(salt)


def verify_password(password: str, stored_hash: str, salt_b64: str) -> bool:
    parts = stored_hash.split("$")
    if len(parts) == 3 and parts[0] == "py1":
        iters = int(parts[1])
        salt = b64url_decode(salt_b64)
        dk = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, iters, 32)
        return hmac.compare_digest(b64url_encode(dk), parts[2])
    return False


def sign_session(payload: dict) -> str:
    encoded = b64url_encode(json.dumps(payload, separators=(",", ":")).encode("utf-8"))
    sig = digest_b64(f"{encoded}.{AUTH_SECRET}")
    return f"{encoded}.{sig}"


def verify_session(auth_header: str) -> dict:
    if not auth_header or not auth_header.startswith("Bearer "):
        raise HttpError(401, "Authentication required")
    token = auth_header[7:].strip()
    parts = token.split(".")
    if len(parts) != 2:
        raise HttpError(401, "Invalid session")
    payload_b64, sig = parts
    expected = digest_b64(f"{payload_b64}.{AUTH_SECRET}")
    if not hmac.compare_digest(sig, expected):
        raise HttpError(401, "Invalid session")
    try:
        data = json.loads(b64url_decode(payload_b64).decode("utf-8"))
    except Exception:
        raise HttpError(401, "Invalid session")
    if not data.get("sub") or not data.get("role") or data.get("exp", 0) < now_ms():
        raise HttpError(401, "Session expired")
    return data


def random_token(length: int = 12) -> str:
    return "".join(secrets.choice(TOKEN_ALPHABET) for _ in range(length))


def init_db():
    with DB_LOCK:
        cur = CONN.cursor()
        cur.executescript(
            """
            PRAGMA foreign_keys = ON;

            CREATE TABLE IF NOT EXISTS users (
              id TEXT PRIMARY KEY,
              email TEXT NOT NULL UNIQUE,
              password_hash TEXT NOT NULL,
              password_salt TEXT NOT NULL,
              role TEXT NOT NULL CHECK (role IN ('student', 'admin')),
              name TEXT NOT NULL,
              student_id TEXT,
              faculty TEXT,
              station_id TEXT,
              points INTEGER NOT NULL DEFAULT 0,
              total_kg REAL NOT NULL DEFAULT 0,
              status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'locked', 'suspended')),
              last_login_at INTEGER,
              token_version INTEGER NOT NULL DEFAULT 1,
              created_at INTEGER NOT NULL,
              updated_at INTEGER NOT NULL
            );

            CREATE TABLE IF NOT EXISTS stations (
              id TEXT PRIMARY KEY,
              name TEXT NOT NULL,
              address TEXT NOT NULL,
              latitude REAL,
              longitude REAL,
              capacity_kg REAL NOT NULL CHECK (capacity_kg > 0),
              is_active INTEGER NOT NULL DEFAULT 1,
              created_at INTEGER NOT NULL
            );

            CREATE TABLE IF NOT EXISTS transactions (
              id TEXT PRIMARY KEY,
              token_hash TEXT NOT NULL UNIQUE,
              token_plain TEXT,
              station_id TEXT NOT NULL REFERENCES stations(id),
              admin_id TEXT NOT NULL REFERENCES users(id),
              student_id TEXT REFERENCES users(id),
              items_json TEXT NOT NULL,
              total_quantity REAL NOT NULL CHECK (total_quantity > 0),
              weight_kg REAL NOT NULL CHECK (weight_kg > 0),
              points INTEGER NOT NULL CHECK (points >= 0),
              status TEXT NOT NULL CHECK (status IN ('pending', 'confirmed', 'expired', 'cancelled')),
              created_at INTEGER NOT NULL,
              expires_at INTEGER NOT NULL,
              confirmed_at INTEGER,
              cancelled_at INTEGER
            );

            CREATE TABLE IF NOT EXISTS point_ledger (
              id TEXT PRIMARY KEY,
              user_id TEXT NOT NULL REFERENCES users(id),
              transaction_id TEXT NOT NULL UNIQUE REFERENCES transactions(id),
              points INTEGER NOT NULL,
              created_at INTEGER NOT NULL
            );

            CREATE TABLE IF NOT EXISTS station_transfers (
              id TEXT PRIMARY KEY,
              station_id TEXT NOT NULL REFERENCES stations(id),
              admin_id TEXT NOT NULL REFERENCES users(id),
              weight_kg REAL NOT NULL CHECK (weight_kg > 0),
              created_at INTEGER NOT NULL,
              note TEXT
            );

            CREATE TABLE IF NOT EXISTS audit_events (
              id TEXT PRIMARY KEY,
              actor_id TEXT REFERENCES users(id),
              action TEXT NOT NULL,
              entity_type TEXT NOT NULL,
              entity_id TEXT NOT NULL,
              metadata_json TEXT,
              created_at INTEGER NOT NULL
            );

            CREATE TABLE IF NOT EXISTS auth_throttles (
              throttle_key TEXT PRIMARY KEY,
              email_hint TEXT,
              failure_count INTEGER NOT NULL DEFAULT 0,
              window_started_at INTEGER NOT NULL,
              locked_until INTEGER NOT NULL DEFAULT 0,
              updated_at INTEGER NOT NULL
            );

            CREATE TABLE IF NOT EXISTS access_logs (
              id TEXT PRIMARY KEY,
              user_id TEXT REFERENCES users(id),
              email TEXT NOT NULL,
              role TEXT,
              event_type TEXT NOT NULL,
              outcome TEXT NOT NULL CHECK (outcome IN ('success', 'failed', 'blocked')),
              ip_address TEXT,
              user_agent TEXT,
              detail TEXT,
              created_at INTEGER NOT NULL
            );

            CREATE TABLE IF NOT EXISTS admin_invites (
              id TEXT PRIMARY KEY,
              code_hash TEXT NOT NULL UNIQUE,
              code_preview TEXT NOT NULL,
              label TEXT NOT NULL,
              station_id TEXT NOT NULL REFERENCES stations(id),
              max_uses INTEGER NOT NULL DEFAULT 1,
              used_count INTEGER NOT NULL DEFAULT 0,
              expires_at INTEGER NOT NULL,
              revoked_at INTEGER,
              created_by TEXT REFERENCES users(id),
              created_at INTEGER NOT NULL
            );
            """
        )

        ts = now_ms()
        stations_seed = [
            ("soict-b1", "Trường Công nghệ Thông tin và Truyền thông", "Tòa B1, Đại học Bách khoa Hà Nội", 21.0046, 105.8465, 25.0),
            ("chem-life-c3", "Trường Hóa và Khoa học Sự sống", "Tòa C3, Đại học Bách khoa Hà Nội", 21.0059, 105.8442, 25.0),
            ("mechanical-c7", "Trường Cơ khí", "Tòa C7, Đại học Bách khoa Hà Nội", 21.0064, 105.8435, 25.0),
            ("innovation-center", "Trung tâm Đổi mới Sáng tạo - Đại học Bách khoa Hà Nội", "Tòa nhà C7, Đại học Bách khoa Hà Nội", 21.0068, 105.8431, 25.0),
        ]
        for sid, name, addr, lat, lng, cap in stations_seed:
            cur.execute(
                """INSERT OR IGNORE INTO stations (id, name, address, latitude, longitude, capacity_kg, is_active, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, 1, ?)""",
                (sid, name, addr, lat, lng, cap, ts),
            )

        # Seed demo users only if users table is empty
        user_count = cur.execute("SELECT COUNT(*) FROM users").fetchone()[0]
        if user_count == 0:
            adm_hash, adm_salt = hash_password("admin1234")
            stu_hash, stu_salt = hash_password("student1234")
            users_seed = [
                ("adm-soict-01", "admin@hust.edu.vn", adm_hash, adm_salt, "admin", "Nguyễn Minh Quân", None, None, "soict-b1", 0, 0.0, "active", ts - 120_000, 1),
                ("adm-chem-02", "admin.c3@hust.edu.vn", adm_hash, adm_salt, "admin", "Phạm Lan Hương", None, None, "chem-life-c3", 0, 0.0, "active", ts - 3_600_000, 1),
                ("stu-20225101", "sinhvien@sis.hust.edu.vn", stu_hash, stu_salt, "student", "Nguyễn Thảo Linh", "20225101", "Trường Công nghệ Thông tin và Truyền thông", None, 135, 9.2, "active", ts - 600_000, 1),
                ("stu-20223412", "minh@sis.hust.edu.vn", stu_hash, stu_salt, "student", "Trần Quang Minh", "20223412", "Trường Cơ khí", None, 84, 6.4, "active", ts - 7_200_000, 1),
                ("stu-20231088", "locked@sis.hust.edu.vn", stu_hash, stu_salt, "student", "Lê Hoàng An", "20231088", "Trường Hóa và Khoa học Sự sống", None, 20, 1.5, "locked", ts - 86_400_000, 2),
            ]
            for u in users_seed:
                cur.execute(
                    """INSERT INTO users (id, email, password_hash, password_salt, role, name, student_id, faculty, station_id, points, total_kg, status, last_login_at, token_version, created_at, updated_at)
                       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    (*u, ts - 86_400_000 * 7, ts),
                )

            # Seed sample transactions (confirmed + 1 pending QR)
            tx1_items = json.dumps([{"plasticType": "PET", "cleanliness": "CLEAN", "quantity": 4.2, "points": 63}, {"plasticType": "PP", "cleanliness": "CLEAN", "quantity": 5.0, "points": 60}])
            tx2_items = json.dumps([{"plasticType": "HDPE", "cleanliness": "NORMAL", "quantity": 6.4, "points": 64}, {"plasticType": "PET", "cleanliness": "NORMAL", "quantity": 2.0, "points": 20}])
            tx_pending_items = json.dumps([{"plasticType": "PET", "cleanliness": "CLEAN", "quantity": 1.6, "points": 24}])

            cur.execute(
                """INSERT INTO transactions (id, token_hash, token_plain, station_id, admin_id, student_id, items_json, total_quantity, weight_kg, points, status, created_at, expires_at, confirmed_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'confirmed', ?, ?, ?)""",
                ("tx-demo-01", digest_b64("DEMOQR000001"), "DEMOQR000001", "soict-b1", "adm-soict-01", "stu-20225101", tx1_items, 9.2, 9.2, 123, ts - 7_200_000, ts - 6_900_000, ts - 7_100_000),
            )
            cur.execute(
                """INSERT INTO transactions (id, token_hash, token_plain, station_id, admin_id, student_id, items_json, total_quantity, weight_kg, points, status, created_at, expires_at, confirmed_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'confirmed', ?, ?, ?)""",
                ("tx-demo-02", digest_b64("DEMOQR000002"), "DEMOQR000002", "mechanical-c7", "adm-soict-01", "stu-20223412", tx2_items, 8.4, 8.4, 84, ts - 18_000_000, ts - 17_700_000, ts - 17_900_000),
            )
            cur.execute(
                """INSERT INTO transactions (id, token_hash, token_plain, station_id, admin_id, student_id, items_json, total_quantity, weight_kg, points, status, created_at, expires_at, confirmed_at)
                   VALUES (?, ?, ?, ?, ?, NULL, ?, ?, ?, ?, 'pending', ?, ?, NULL)""",
                ("tx-demo-03", digest_b64("HACKATHONQR1"), "HACKATHONQR1", "soict-b1", "adm-soict-01", tx_pending_items, 1.6, 1.6, 24, ts - 30_000, ts + 25 * 60_000),
            )

            # Seed sample invite codes
            cur.execute(
                """INSERT INTO admin_invites (id, code_hash, code_preview, label, station_id, max_uses, used_count, expires_at, revoked_at, created_by, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?)""",
                ("inv-demo-1", digest_b64("ADM-HUST-B126"), "ADM-HUST-B126", "CTV Trạm B1 - Hackathon", "soict-b1", 10, 1, ts + 7 * 86_400_000, "adm-soict-01", ts - 3_600_000),
            )

            # Seed sample access logs & 1 brute-force lockout
            logs_seed = [
                ("log-seed-1", "adm-soict-01", "admin@hust.edu.vn", "admin", "login_success", "success", "192.168.100.24", "FLYSTIC Android", "Khởi tạo phiên Admin Hackathon", ts - 600_000),
                ("log-seed-2", None, "scanner.bot@mail.test", "student", "login_locked", "blocked", "103.82.14.91", "okhttp/4.12", "Khóa 15 phút sau 5 lần sai mật khẩu", ts - 420_000),
                ("log-seed-3", "stu-20225101", "sinhvien@sis.hust.edu.vn", "student", "qr_confirmed", "success", "192.168.100.45", "FLYSTIC Android", "Xác nhận +123 điểm tại Trạm B1", ts - 300_000),
            ]
            for lg in logs_seed:
                cur.execute(
                    """INSERT INTO access_logs (id, user_id, email, role, event_type, outcome, ip_address, user_agent, detail, created_at)
                       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    lg,
                )

            cur.execute(
                """INSERT INTO auth_throttles (throttle_key, email_hint, failure_count, window_started_at, locked_until, updated_at)
                   VALUES (?, ?, 5, ?, ?, ?)""",
                ("thr-demo-bot", "scanner.bot@mail.test", ts - 500_000, ts + 14 * 60_000, ts - 420_000),
            )

        CONN.commit()


def effective_status(row) -> str:
    if row["status"] == "pending" and now_ms() > row["expires_at"]:
        return "expired"
    return row["status"]


def public_user(cur, user_row) -> dict:
    row = cur.execute(
        """SELECT COALESCE(SUM(CAST(json_extract(metadata_json, '$.points') AS INTEGER)), 0) AS fund_points
           FROM audit_events
           WHERE actor_id = ? AND action = 'points_allocated'
             AND json_extract(metadata_json, '$.destination') = 'fundraising'""",
        (user_row["id"],),
    ).fetchone()
    fund_points = max(0, int(row["fund_points"] or 0)) if row else 0
    total_points = int(user_row["points"] or 0)
    return {
        "id": user_row["id"],
        "email": user_row["email"],
        "role": user_row["role"],
        "name": user_row["name"],
        "studentId": user_row["student_id"],
        "faculty": user_row["faculty"],
        "stationId": user_row["station_id"],
        "points": total_points,
        "rewardPoints": max(0, total_points - fund_points),
        "fundPoints": fund_points,
        "totalKg": float(user_row["total_kg"] or 0.0),
    }


def station_data(cur, include_inactive: bool = False) -> list:
    where = "" if include_inactive else "WHERE s.is_active = 1"
    rows = cur.execute(
        f"""SELECT
              s.id, s.name, s.address, s.latitude, s.longitude, s.capacity_kg, s.is_active,
              COALESCE((SELECT SUM(t.weight_kg) FROM transactions t WHERE t.station_id = s.id AND t.status = 'confirmed'), 0)
              - COALESCE((SELECT SUM(r.weight_kg) FROM station_transfers r WHERE r.station_id = s.id), 0) AS filled_kg
            FROM stations s
            {where}
            ORDER BY s.name"""
    ).fetchall()
    result = []
    for s in rows:
        filled = round(max(0.0, float(s["filled_kg"] or 0.0)), 2)
        cap = float(s["capacity_kg"])
        result.append(
            {
                "id": s["id"],
                "name": s["name"],
                "address": s["address"],
                "latitude": s["latitude"],
                "longitude": s["longitude"],
                "capacityKg": cap,
                "isActive": int(s["is_active"] if s["is_active"] is not None else 1) == 1,
                "filledKg": filled,
                "fillPercent": min(100, round((filled / cap) * 100)) if cap > 0 else 0,
            }
        )
    return result


def transaction_view(row, token_override=None, include_student=True) -> dict:
    tok = token_override if token_override is not None else row["token_plain"]
    res = {
        "id": row["id"],
        "userId": row["student_id"] if include_student else None,
        "userName": row["student_name"] if include_student else None,
        "userFaculty": row["student_faculty"] if include_student else None,
        "adminId": row["admin_id"],
        "stationId": row["station_id"],
        "collectionPoint": row["station_name"],
        "items": json.loads(row["items_json"]),
        "totalQuantity": float(row["total_quantity"]),
        "weightKg": float(row["weight_kg"]),
        "points": int(row["points"]),
        "status": effective_status(row),
        "createdAt": int(row["created_at"]),
        "expiresAt": int(row["expires_at"]),
        "confirmedAt": int(row["confirmed_at"]) if row["confirmed_at"] is not None else None,
    }
    if tok:
        res["token"] = tok
    return res


def log_access(cur, user_id, email, role, event_type, outcome, ip, ua, detail):
    cur.execute(
        """INSERT INTO access_logs (id, user_id, email, role, event_type, outcome, ip_address, user_agent, detail, created_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
        (str(uuid.uuid4()), user_id, email, role, event_type, outcome, ip, (ua or "")[:140], detail, now_ms()),
    )


def log_audit(cur, actor_id, action, entity_type, entity_id, metadata=None):
    cur.execute(
        """INSERT INTO audit_events (id, actor_id, action, entity_type, entity_id, metadata_json, created_at)
           VALUES (?, ?, ?, ?, ?, ?, ?)""",
        (str(uuid.uuid4()), actor_id, action, entity_type, entity_id, json.dumps(metadata) if metadata else None, now_ms()),
    )


def require_current_user(cur, headers):
    session = verify_session(headers.get("authorization", ""))
    user = cur.execute("SELECT * FROM users WHERE id = ?", (session["sub"],)).fetchone()
    if not user:
        raise HttpError(401, "Session user no longer exists")
    if (user["status"] or "active") != "active":
        raise HttpError(403, "Account is locked or suspended")
    if int(session.get("tv", 1)) != int(user["token_version"] or 1):
        raise HttpError(401, "Session token has been revoked")
    return session, user


def calculate_items(items):
    if not isinstance(items, list) or len(items) == 0 or len(items) > 5:
        raise HttpError(400, "A transaction needs 1-5 plastic lines")
    normalized = []
    for item in items:
        ptype = item.get("plasticType")
        clean = item.get("cleanliness")
        qty = item.get("quantity")
        if ptype not in POINT_BASE or clean not in CLEAN_MULTIPLIER:
            raise HttpError(400, "Unsupported plastic type or cleanliness")
        if not isinstance(qty, (int, float)) or qty < 0.01 or qty > 200:
            raise HttpError(400, "Weight must be from 0.01 to 200 kg")
        q = round(float(qty), 2)
        pts = round(q * POINT_BASE[ptype] * CLEAN_MULTIPLIER[clean])
        normalized.append({"plasticType": ptype, "cleanliness": clean, "quantity": q, "points": pts})
    total_q = round(sum(i["quantity"] for i in normalized), 2)
    return {
        "items": normalized,
        "points": sum(i["points"] for i in normalized),
        "totalQuantity": total_q,
        "weightKg": total_q,
    }


def leaderboard_data(cur, period: str) -> list:
    from_ts = 0
    if period == "week":
        from_ts = now_ms() - 7 * 86_400_000
    rows = cur.execute(
        """SELECT u.id AS user_id, u.name, COALESCE(u.faculty, 'Khác') AS faculty,
             COALESCE(SUM(t.points), 0) AS points, COALESCE(SUM(t.weight_kg), 0) AS kg
           FROM users u
           JOIN transactions t ON t.student_id = u.id
           WHERE t.status = 'confirmed' AND t.confirmed_at >= ?
           GROUP BY u.id, u.name, u.faculty
           ORDER BY points DESC, kg DESC
           LIMIT 100""",
        (from_ts,),
    ).fetchall()
    return [
        {
            "userId": r["user_id"],
            "name": r["name"],
            "faculty": r["faculty"],
            "points": int(r["points"]),
            "kg": round(float(r["kg"]), 2),
        }
        for r in rows
    ]


def load_openai_key() -> str:
    env_key = os.environ.get("OPENAI_API_KEY", "").strip()
    if env_key:
        return env_key
    local_props = os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "local.properties"
    )
    if os.path.isfile(local_props):
        try:
            with open(local_props, "r", encoding="utf-8") as f:
                for line in f:
                    if line.startswith("OPENAI_API_KEY="):
                        return line.split("=", 1)[1].strip()
        except Exception:
            pass
    return ""


def get_ai_prompt(locale: str) -> str:
    lang_name = "Vietnamese"
    if locale == "en":
        lang_name = "English"
    elif locale == "ko":
        lang_name = "Korean"

    prompt_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "ai_prompt.txt")
    if os.path.isfile(prompt_path):
        try:
            with open(prompt_path, "r", encoding="utf-8") as f:
                raw = f.read()
            return raw.replace("{LANGUAGE}", lang_name)
        except Exception:
            pass

    return f"""You are VeriFour's zero-retention plastic recycling assistant.
Output strictly JSON:
{{"status": "DETECTED" | "UNCERTAIN" | "UNSUPPORTED", "plastic_code": "PET" | "HDPE" | "PP" | "LDPE" | "PS" | "PVC_OR_OTHER" | "UNKNOWN", "confidence": 0.85, "mascot_message": "...", "prep_tip": "...", "angle_suggestions": [], "packaging_origin_questions": []}}
"""


def detect_plastic_openai(image_b64: str, user_hint: str, locale: str) -> dict:
    api_key = load_openai_key()
    if not api_key:
        raise HttpError(500, "OPENAI_API_KEY is not configured on local server")
    if not image_b64 and not user_hint:
        raise HttpError(400, "Either imageBase64 or userHint is required")

    system_prompt = get_ai_prompt(locale)

    user_text = "Help me identify this plastic item for self-sorting before bringing it to the recycling station."
    if user_hint:
        user_text += f"\nUser clues about packaging, origin, or usage: {user_hint.strip()}"

    content_list = [{"type": "text", "text": user_text}]
    if image_b64:
        clean_b64 = image_b64
        if clean_b64.startswith("data:"):
            clean_b64 = clean_b64.split(",", 1)[-1]
        content_list.append({
            "type": "image_url",
            "image_url": {
                "url": f"data:image/jpeg;base64,{clean_b64}",
                "detail": "high"
            }
        })

    payload = {
        "model": "gpt-4o-mini",
        "store": False,
        "temperature": 0.2,
        "max_tokens": 550,
        "response_format": {"type": "json_object"},
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": content_list}
        ]
    }

    req = urllib.request.Request(
        "https://api.openai.com/v1/chat/completions",
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json"
        },
        method="POST"
    )

    try:
        with urllib.request.urlopen(req, timeout=40) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            raw_content = data["choices"][0]["message"]["content"]
            return json.loads(raw_content)
    except urllib.error.HTTPError as e:
        err_msg = e.read().decode("utf-8", errors="replace")
        raise HttpError(502, f"OpenAI error ({e.code}): {err_msg[:200]}")
    except Exception as e:
        raise HttpError(500, f"AI detect error: {e}")


class FlysticHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, format, *args):
        sys.stdout.write(f"[API] {self.address_string()} - {format % args}\n")
        sys.stdout.flush()

    def send_json(self, body, status=200):
        raw = json.dumps(body, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "authorization, content-type")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.end_headers()
        self.wfile.write(raw)

    def read_json(self) -> dict:
        length = int(self.headers.get("content-length") or 0)
        if length == 0:
            return {}
        raw = self.rfile.read(length).decode("utf-8")
        data = json.loads(raw)
        if not isinstance(data, dict):
            raise HttpError(400, "JSON object expected")
        return data

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Content-Length", "0")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "authorization, content-type")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.end_headers()

    def do_HEAD(self):
        parsed_path = urlparse(self.path).path
        apk_path = os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
            "app",
            "build",
            "outputs",
            "apk",
            "release",
            "app-release.apk",
        )
        if parsed_path in ("/download/app-release.apk", "/download/verifour-release.apk", "/download/flystic-release.apk"):
            if not os.path.isfile(apk_path):
                self.send_response(404)
                self.end_headers()
                return
            size = os.path.getsize(apk_path)
            self.send_response(200)
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Disposition", 'attachment; filename="verifour-hackathon.apk"')
            self.send_header("Content-Length", str(size))
            self.end_headers()
            return
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()

    def do_GET(self):
        parsed_path = urlparse(self.path).path
        apk_path = os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
            "app",
            "build",
            "outputs",
            "apk",
            "release",
            "app-release.apk",
        )
        if parsed_path in ("/", "/download"):
            has_apk = os.path.isfile(apk_path)
            size_mb = f"{os.path.getsize(apk_path) / (1024 * 1024):.1f} MB" if has_apk else "Chưa build"
            html = f"""<!doctype html>
<html lang="vi">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width,initial-scale=1" />
<title>VeriFour Hackathon Demo</title>
<style>
  body {{ font-family: -apple-system, system-ui, sans-serif; background: #f4f7f5; color: #132a1c; margin: 0; padding: 24px; }}
  .card {{ max-width: 460px; margin: 24px auto; background: #fff; border-radius: 18px; padding: 24px; box-shadow: 0 8px 24px rgba(0,0,0,.08); }}
  h1 {{ margin: 0 0 8px; font-size: 22px; }}
  p {{ color: #4f6458; font-size: 14px; line-height: 1.5; }}
  .btn {{ display: block; text-align: center; background: #16a34a; color: #fff; text-decoration: none; font-weight: 700; padding: 14px 18px; border-radius: 12px; margin: 18px 0; }}
  .box {{ background: #f1f5f2; padding: 12px 14px; border-radius: 12px; font-size: 13px; }}
  code {{ background: #e2ebe5; padding: 2px 6px; border-radius: 6px; }}
</style>
</head>
<body>
  <div class="card">
    <h1>VeriFour — Hackathon Demo</h1>
    <p>Server trên máy Mac đang hoạt động. Tải file APK bên dưới để cài trên điện thoại Android cùng Wi-Fi:</p>
    {"<a class='btn' href='/download/app-release.apk'>Tải app-release.apk (" + size_mb + ")</a>" if has_apk else "<p><b>Chưa tìm thấy file APK.</b></p>"}
    <div class="box">
      <div><b>Tài khoản Demo:</b></div>
      <div>• Admin: <code>admin@hust.edu.vn</code> / <code>admin1234</code></div>
      <div>• Sinh viên: <code>sinhvien@sis.hust.edu.vn</code> / <code>student1234</code></div>
      <div>• Mã mời Admin: <code>HUST-2026</code></div>
    </div>
  </div>
</body>
</html>""".encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(html)))
            self.end_headers()
            try:
                self.wfile.write(html)
            except (BrokenPipeError, ConnectionResetError):
                pass
            return

        if parsed_path in ("/download/app-release.apk", "/download/verifour-release.apk", "/download/flystic-release.apk"):
            if not os.path.isfile(apk_path):
                self.send_json({"error": "APK not found"}, 404)
                return
            size = os.path.getsize(apk_path)
            self.send_response(200)
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Disposition", 'attachment; filename="verifour-hackathon.apk"')
            self.send_header("Content-Length", str(size))
            self.end_headers()
            try:
                with open(apk_path, "rb") as f:
                    while True:
                        chunk = f.read(65536)
                        if not chunk:
                            break
                        self.wfile.write(chunk)
            except (BrokenPipeError, ConnectionResetError):
                pass
            return

        self._dispatch("GET")

    def do_POST(self):
        self._dispatch("POST")

    def _dispatch(self, method: str):
        try:
            parsed = urlparse(self.path)
            path = re.sub(r"^/api/v1", "", parsed.path) or "/"
            query = parse_qs(parsed.query)
            client_ip = self.client_address[0]
            ua = self.headers.get("user-agent", "")

            with DB_LOCK:
                cur = CONN.cursor()
                result, status = self.route(method, path, query, cur, client_ip, ua)
                CONN.commit()
            self.send_json(result, status)
        except HttpError as err:
            CONN.commit()
            self.send_json({"error": err.message}, err.status)
        except Exception as err:
            CONN.rollback()
            sys.stderr.write(f"[ERR] {err}\n")
            self.send_json({"error": f"Server error: {err}"}, 500)

    def route(self, method: str, path: str, query: dict, cur, ip: str, ua: str):
        if method == "GET" and path == "/health":
            return {"ok": True, "mode": "hackathon-local-mac", "time": now_ms()}, 200

        if method == "GET" and path == "/stations":
            return station_data(cur, False), 200

        if method == "GET" and path == "/leaderboard":
            period = "week" if query.get("period", ["all"])[0] == "week" else "all"
            return {"period": period, "rows": leaderboard_data(cur, period)}, 200

        if method == "POST" and path == "/ai/detect":
            inp = self.read_json()
            image_b64 = str(inp.get("imageBase64") or "")
            user_hint = str(inp.get("userHint") or "")
            loc = str(inp.get("locale") or "vi").lower()
            return detect_plastic_openai(image_b64, user_hint, loc), 200

        if method == "POST" and path == "/auth/register":
            inp = self.read_json()
            email = str(inp.get("email") or "").strip().lower()
            name = str(inp.get("name") or "").strip()
            password = str(inp.get("password") or "")
            if not email or not name or len(password) < 6:
                raise HttpError(400, "Email, name and password (>= 6 chars) are required")
            if cur.execute("SELECT id FROM users WHERE email = ?", (email,)).fetchone():
                raise HttpError(409, "Email already registered")
            wants_admin = inp.get("role") == "admin"
            role = "admin" if wants_admin else "student"
            station_id = str(inp.get("stationId") or "").strip() if wants_admin else None
            if wants_admin:
                if not station_id:
                    raise HttpError(400, "stationId is required")
                code = str(inp.get("inviteCode") or "").strip()
                inv = cur.execute(
                    "SELECT id, station_id, max_uses, used_count, expires_at, revoked_at FROM admin_invites WHERE code_hash = ?",
                    (digest_b64(code),),
                ).fetchone()
                valid_managed = (
                    inv
                    and inv["revoked_at"] is None
                    and inv["expires_at"] > now_ms()
                    and inv["used_count"] < inv["max_uses"]
                    and inv["station_id"] == station_id
                )
                if code != ADMIN_INVITE_CODE and not valid_managed:
                    log_access(cur, None, email, "admin", "register_admin_denied", "blocked", ip, ua, "Invalid admin invite")
                    raise HttpError(403, f"Invalid admin invite code (Hint: {ADMIN_INVITE_CODE})")
                if valid_managed:
                    cur.execute("UPDATE admin_invites SET used_count = used_count + 1 WHERE id = ?", (inv["id"],))
            pw_hash, pw_salt = hash_password(password)
            ts = now_ms()
            uid = str(uuid.uuid4())
            cur.execute(
                """INSERT INTO users (id, email, password_hash, password_salt, role, name, student_id, faculty, station_id, points, total_kg, status, last_login_at, token_version, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 'active', ?, 1, ?, ?)""",
                (uid, email, pw_hash, pw_salt, role, name, inp.get("studentId"), inp.get("faculty"), station_id, ts, ts, ts),
            )
            log_access(cur, uid, email, role, "register", "success", ip, ua, f"Registered {role}")
            user_row = cur.execute("SELECT * FROM users WHERE id = ?", (uid,)).fetchone()
            token = sign_session({"sub": uid, "role": role, "stationId": station_id, "tv": 1, "exp": ts + 7 * 86_400_000})
            return {"token": token, "user": public_user(cur, user_row)}, 201

        if method == "POST" and path == "/auth/login":
            inp = self.read_json()
            email = str(inp.get("email") or "").strip().lower()
            password = str(inp.get("password") or "")
            thr_key = digest_b64(f"login:{email}")
            thr = cur.execute("SELECT * FROM auth_throttles WHERE throttle_key = ?", (thr_key,)).fetchone()
            if thr and thr["locked_until"] > now_ms():
                log_access(cur, None, email, None, "login_locked", "blocked", ip, ua, "Rate limit lockout")
                raise HttpError(429, "Too many failed attempts. Try again later or unlock in Admin Ops.")
            user_row = cur.execute("SELECT * FROM users WHERE email = ?", (email,)).fetchone()
            if not user_row or not verify_password(password, user_row["password_hash"], user_row["password_salt"]):
                ts = now_ms()
                fails = (thr["failure_count"] + 1) if (thr and thr["window_started_at"] > ts - AUTH_FAILURE_WINDOW_MS) else 1
                locked_until = ts + AUTH_LOCK_MS if fails >= AUTH_MAX_FAILURES else 0
                cur.execute(
                    """INSERT INTO auth_throttles (throttle_key, email_hint, failure_count, window_started_at, locked_until, updated_at)
                       VALUES (?, ?, ?, ?, ?, ?)
                       ON CONFLICT(throttle_key) DO UPDATE SET
                         email_hint = excluded.email_hint,
                         failure_count = excluded.failure_count,
                         window_started_at = CASE WHEN auth_throttles.window_started_at <= ? THEN excluded.window_started_at ELSE auth_throttles.window_started_at END,
                         locked_until = excluded.locked_until,
                         updated_at = excluded.updated_at""",
                    (thr_key, email, fails, ts, locked_until, ts, ts - AUTH_FAILURE_WINDOW_MS),
                )
                log_access(cur, user_row["id"] if user_row else None, email, user_row["role"] if user_row else None, "login_failed", "failed", ip, ua, f"Wrong password ({fails}/{AUTH_MAX_FAILURES})")
                raise HttpError(401, "Email or password is incorrect")
            if (user_row["status"] or "active") != "active":
                log_access(cur, user_row["id"], email, user_row["role"], "login_blocked", "blocked", ip, ua, f"Account status is {user_row['status']}")
                raise HttpError(403, f"Account is {user_row['status']}")
            ts = now_ms()
            cur.execute("DELETE FROM auth_throttles WHERE throttle_key = ?", (thr_key,))
            cur.execute("UPDATE users SET last_login_at = ?, updated_at = ? WHERE id = ?", (ts, ts, user_row["id"]))
            log_access(cur, user_row["id"], email, user_row["role"], "login_success", "success", ip, ua, "Logged in")
            token = sign_session({"sub": user_row["id"], "role": user_row["role"], "stationId": user_row["station_id"], "tv": int(user_row["token_version"] or 1), "exp": ts + 7 * 86_400_000})
            return {"token": token, "user": public_user(cur, user_row)}, 200

        if method == "GET" and path == "/me":
            _, user_row = require_current_user(cur, self.headers)
            return {"user": public_user(cur, user_row)}, 200

        if method == "GET" and path == "/snapshot":
            _, user_row = require_current_user(cur, self.headers)
            if user_row["role"] == "admin":
                tx_rows = cur.execute(
                    """SELECT t.*, s.name AS station_name, u.name AS student_name, u.faculty AS student_faculty
                       FROM transactions t
                       JOIN stations s ON s.id = t.station_id
                       LEFT JOIN users u ON u.id = t.student_id
                       WHERE t.station_id = ?
                       ORDER BY t.created_at DESC LIMIT 250""",
                    (user_row["station_id"],),
                ).fetchall()
                run_rows = cur.execute(
                    """SELECT r.*, s.name AS station_name
                       FROM station_transfers r
                       JOIN stations s ON s.id = r.station_id
                       WHERE r.station_id = ?
                       ORDER BY r.created_at DESC LIMIT 100""",
                    (user_row["station_id"],),
                ).fetchall()
                runs = [
                    {
                        "id": r["id"],
                        "stationId": r["station_id"],
                        "collectionPoint": r["station_name"],
                        "weightKg": float(r["weight_kg"]),
                        "adminId": r["admin_id"],
                        "createdAt": int(r["created_at"]),
                    }
                    for r in run_rows
                ]
            else:
                tx_rows = cur.execute(
                    """SELECT t.*, s.name AS station_name, u.name AS student_name, u.faculty AS student_faculty
                       FROM transactions t
                       JOIN stations s ON s.id = t.station_id
                       LEFT JOIN users u ON u.id = t.student_id
                       WHERE t.student_id = ?
                       ORDER BY t.created_at DESC LIMIT 250""",
                    (user_row["id"],),
                ).fetchall()
                runs = []

            return {
                "user": public_user(cur, user_row),
                "stations": station_data(cur, False),
                "transactions": [transaction_view(t) for t in tx_rows],
                "collectionRuns": runs,
                "demoCampaign": None,
            }, 200

        if method == "POST" and path == "/admin/transactions":
            _, user_row = require_current_user(cur, self.headers)
            if user_row["role"] != "admin" or not user_row["station_id"]:
                raise HttpError(403, "Station admin access required")
            inp = self.read_json()
            calc = calculate_items(inp.get("items"))
            ts = now_ms()
            qr_token = random_token(12)
            tx_id = str(uuid.uuid4())
            expires_at = ts + 5 * 60_000
            cur.execute(
                """INSERT INTO transactions (id, token_hash, token_plain, station_id, admin_id, student_id, items_json, total_quantity, weight_kg, points, status, created_at, expires_at)
                   VALUES (?, ?, ?, ?, ?, NULL, ?, ?, ?, ?, 'pending', ?, ?)""",
                (tx_id, digest_b64(qr_token), qr_token, user_row["station_id"], user_row["id"], json.dumps(calc["items"]), calc["totalQuantity"], calc["weightKg"], calc["points"], ts, expires_at),
            )
            st = cur.execute("SELECT name FROM stations WHERE id = ?", (user_row["station_id"],)).fetchone()
            log_access(cur, user_row["id"], user_row["email"], "admin", "qr_created", "success", ip, ua, f"Created QR {qr_token} (+{calc['points']} pts)")
            return {
                "id": tx_id,
                "token": qr_token,
                "userId": None,
                "userName": None,
                "userFaculty": None,
                "adminId": user_row["id"],
                "stationId": user_row["station_id"],
                "collectionPoint": st["name"] if st else user_row["station_id"],
                **calc,
                "status": "pending",
                "createdAt": ts,
                "expiresAt": expires_at,
                "confirmedAt": None,
            }, 201

        m_cancel = re.match(r"^/admin/transactions/([^/]+)/cancel$", path)
        if method == "POST" and m_cancel:
            _, user_row = require_current_user(cur, self.headers)
            if user_row["role"] != "admin":
                raise HttpError(403, "Admin access required")
            ts = now_ms()
            cur.execute(
                "UPDATE transactions SET status = 'cancelled', cancelled_at = ? WHERE id = ? AND status = 'pending'",
                (ts, m_cancel.group(1)),
            )
            if cur.rowcount != 1:
                raise HttpError(409, "Transaction cannot be cancelled")
            return {"id": m_cancel.group(1), "cancelledAt": ts}, 200

        m_tx = re.match(r"^/transactions/([A-Z0-9]+)(?:/(confirm))?$", path)
        if m_tx:
            qr_token, action = m_tx.group(1), m_tx.group(2)
            _, user_row = require_current_user(cur, self.headers)
            tx = cur.execute(
                """SELECT t.*, s.name AS station_name, u.name AS student_name, u.faculty AS student_faculty
                   FROM transactions t
                   JOIN stations s ON s.id = t.station_id
                   LEFT JOIN users u ON u.id = t.student_id
                   WHERE t.token_hash = ?""",
                (digest_b64(qr_token),),
            ).fetchone()
            if not tx:
                raise HttpError(404, "QR transaction not found")
            eff = effective_status(tx)
            if eff == "expired" and tx["status"] == "pending":
                cur.execute("UPDATE transactions SET status = 'expired' WHERE id = ? AND status = 'pending'", (tx["id"],))
            if method == "GET" and not action:
                can_see = tx["student_id"] == user_row["id"] or (user_row["role"] == "admin" and user_row["station_id"] == tx["station_id"])
                return transaction_view(tx, qr_token, can_see), 200
            if method == "POST" and action == "confirm":
                if user_row["role"] != "student":
                    raise HttpError(403, "Student access required")
                if eff != "pending":
                    raise HttpError(409, f"Transaction is {eff}")
                inp = self.read_json()
                dest = inp.get("destination") or "rewards"
                if dest not in ("rewards", "fundraising"):
                    raise HttpError(400, "Invalid destination")
                ts = now_ms()
                cur.execute(
                    "UPDATE transactions SET status = 'confirmed', student_id = ?, confirmed_at = ? WHERE id = ? AND status = 'pending' AND expires_at > ?",
                    (user_row["id"], ts, tx["id"], ts),
                )
                if cur.rowcount != 1:
                    raise HttpError(409, "Transaction already used or expired")
                cur.execute(
                    "UPDATE users SET points = points + ?, total_kg = total_kg + ?, updated_at = ? WHERE id = ?",
                    (tx["points"], tx["weight_kg"], ts, user_row["id"]),
                )
                cur.execute(
                    "INSERT INTO point_ledger (id, user_id, transaction_id, points, created_at) VALUES (?, ?, ?, ?, ?)",
                    (str(uuid.uuid4()), user_row["id"], tx["id"], tx["points"], ts),
                )
                log_audit(cur, user_row["id"], "points_allocated", "transaction", tx["id"], {"destination": dest, "points": tx["points"]})
                log_access(cur, user_row["id"], user_row["email"], "student", "qr_confirmed", "success", ip, ua, f"Confirmed +{tx['points']} pts ({dest})")
                return {"id": tx["id"], "points": int(tx["points"]), "weightKg": float(tx["weight_kg"]), "destination": dest, "confirmedAt": ts}, 200

        m_transfer = re.match(r"^/admin/stations/([^/]+)/transfers$", path)
        if method == "POST" and m_transfer:
            station_id = m_transfer.group(1)
            _, user_row = require_current_user(cur, self.headers)
            if user_row["role"] != "admin":
                raise HttpError(403, "Admin access required")
            stock = cur.execute(
                """SELECT
                     COALESCE((SELECT SUM(weight_kg) FROM transactions WHERE station_id = ? AND status = 'confirmed'), 0)
                     - COALESCE((SELECT SUM(weight_kg) FROM station_transfers WHERE station_id = ?), 0) AS filled_kg""",
                (station_id, station_id),
            ).fetchone()
            weight_kg = round(max(0.0, float(stock["filled_kg"] or 0.0)), 2)
            if weight_kg <= 0:
                raise HttpError(409, "Station is empty")
            ts = now_ms()
            cur.execute(
                "INSERT INTO station_transfers (id, station_id, admin_id, weight_kg, created_at, note) VALUES (?, ?, ?, ?, ?, NULL)",
                (str(uuid.uuid4()), station_id, user_row["id"], weight_kg, ts),
            )
            log_audit(cur, user_row["id"], "station_transfer", "station", station_id, {"weightKg": weight_kg})
            return {"stationId": station_id, "weightKg": weight_kg, "createdAt": ts}, 201

        # --- Admin Ops Endpoints ---
        if method == "GET" and path == "/admin/ops/overview":
            _, user_row = require_current_user(cur, self.headers)
            if user_row["role"] != "admin":
                raise HttpError(403, "Admin access required")
            ts = now_ms()
            last24 = ts - 86_400_000
            all_stations = station_data(cur, True)
            per_station = {}
            for row in cur.execute(
                """SELECT station_id,
                     SUM(CASE WHEN status = 'pending' AND expires_at > ? THEN 1 ELSE 0 END) AS pending_tokens,
                     SUM(CASE WHEN status = 'confirmed' AND confirmed_at >= ? THEN 1 ELSE 0 END) AS confirmed_today
                   FROM transactions GROUP BY station_id""",
                (ts, last24),
            ).fetchall():
                per_station[row["station_id"]] = row
            for st in all_stations:
                m = per_station.get(st["id"])
                st["pendingTokens"] = int(m["pending_tokens"] or 0) if m else 0
                st["confirmedToday"] = int(m["confirmed_today"] or 0) if m else 0
            return {"stations": all_stations}, 200

        if method == "GET" and path == "/admin/users":
            _, user_row = require_current_user(cur, self.headers)
            if user_row["role"] != "admin":
                raise HttpError(403, "Admin access required")
            rows = cur.execute(
                """SELECT u.*, s.name AS station_name
                   FROM users u LEFT JOIN stations s ON s.id = u.station_id
                   ORDER BY CASE WHEN u.role = 'admin' THEN 0 ELSE 1 END, u.created_at DESC LIMIT 300"""
            ).fetchall()
            return [
                {
                    "id": r["id"],
                    "email": r["email"],
                    "role": r["role"],
                    "name": r["name"],
                    "studentId": r["student_id"],
                    "faculty": r["faculty"],
                    "stationId": r["station_id"],
                    "collectionPoint": r["station_name"],
                    "points": int(r["points"] or 0),
                    "totalKg": float(r["total_kg"] or 0.0),
                    "status": r["status"] or "active",
                    "tokenVersion": int(r["token_version"] or 1),
                    "lastLoginAt": int(r["last_login_at"]) if r["last_login_at"] else None,
                    "createdAt": int(r["created_at"]),
                }
                for r in rows
            ], 200

        if method == "POST" and path == "/admin/users":
            _, actor = require_current_user(cur, self.headers)
            if actor["role"] != "admin":
                raise HttpError(403, "Admin access required")
            inp = self.read_json()
            email = str(inp.get("email") or "").strip().lower()
            name = str(inp.get("name") or "").strip()
            password = str(inp.get("password") or "")
            if not email or not name or len(password) < 8:
                raise HttpError(400, "Valid email, name and password (>= 8 chars) required")
            if cur.execute("SELECT id FROM users WHERE email = ?", (email,)).fetchone():
                raise HttpError(409, "Email already registered")
            role = "admin" if inp.get("role") == "admin" else "student"
            station_id = inp.get("stationId") if role == "admin" else None
            pw_hash, pw_salt = hash_password(password)
            ts = now_ms()
            uid = str(uuid.uuid4())
            cur.execute(
                """INSERT INTO users (id, email, password_hash, password_salt, role, name, student_id, faculty, station_id, points, total_kg, status, token_version, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 'active', 1, ?, ?)""",
                (uid, email, pw_hash, pw_salt, role, name, inp.get("studentId"), inp.get("faculty"), station_id, ts, ts),
            )
            log_access(cur, actor["id"], actor["email"], "admin", "account_created", "success", ip, ua, f"Created {role} {email}")
            return {"id": uid, "email": email, "name": name, "role": role, "stationId": station_id, "status": "active", "tokenVersion": 1, "createdAt": ts}, 201

        m_ustatus = re.match(r"^/admin/users/([^/]+)/status$", path)
        if method == "POST" and m_ustatus:
            target_id = m_ustatus.group(1)
            _, actor = require_current_user(cur, self.headers)
            if actor["role"] != "admin":
                raise HttpError(403, "Admin access required")
            if target_id == actor["id"]:
                raise HttpError(400, "Cannot change your own account status")
            next_status = (self.read_json()).get("status")
            if next_status not in ("active", "locked", "suspended"):
                raise HttpError(400, "Invalid status")
            bump = 1 if next_status != "active" else 0
            ts = now_ms()
            cur.execute("UPDATE users SET status = ?, token_version = token_version + ?, updated_at = ? WHERE id = ?", (next_status, bump, ts, target_id))
            if cur.rowcount != 1:
                raise HttpError(404, "User not found")
            target = cur.execute("SELECT email FROM users WHERE id = ?", (target_id,)).fetchone()
            log_access(cur, actor["id"], actor["email"], "admin", f"account_{next_status}", "success" if next_status == "active" else "blocked", ip, ua, f"{target['email']} -> {next_status}")
            return {"id": target_id, "status": next_status}, 200

        m_urole = re.match(r"^/admin/users/([^/]+)/role$", path)
        if method == "POST" and m_urole:
            target_id = m_urole.group(1)
            _, actor = require_current_user(cur, self.headers)
            if actor["role"] != "admin":
                raise HttpError(403, "Admin access required")
            inp = self.read_json()
            next_role = "admin" if inp.get("role") == "admin" else "student"
            station_id = inp.get("stationId") if next_role == "admin" else None
            ts = now_ms()
            cur.execute("UPDATE users SET role = ?, station_id = ?, updated_at = ? WHERE id = ?", (next_role, station_id, ts, target_id))
            if cur.rowcount != 1:
                raise HttpError(404, "User not found")
            log_access(cur, actor["id"], actor["email"], "admin", "account_role_updated", "success", ip, ua, f"User {target_id} -> {next_role}")
            return {"id": target_id, "status": next_role}, 200

        m_urevoke = re.match(r"^/admin/users/([^/]+)/revoke-tokens$", path)
        if method == "POST" and m_urevoke:
            target_id = m_urevoke.group(1)
            _, actor = require_current_user(cur, self.headers)
            if actor["role"] != "admin":
                raise HttpError(403, "Admin access required")
            ts = now_ms()
            cur.execute("UPDATE users SET token_version = token_version + 1, updated_at = ? WHERE id = ?", (ts, target_id))
            if cur.rowcount != 1:
                raise HttpError(404, "User not found")
            row = cur.execute("SELECT email, token_version FROM users WHERE id = ?", (target_id,)).fetchone()
            log_access(cur, actor["id"], actor["email"], "admin", "token_revoked", "blocked", ip, ua, f"Revoked session tokens for {row['email']} (v{row['token_version']})")
            return {"id": target_id, "tokenVersion": int(row["token_version"])}, 200

        if method == "GET" and path == "/admin/tokens":
            _, actor = require_current_user(cur, self.headers)
            if actor["role"] != "admin":
                raise HttpError(403, "Admin access required")
            tx_rows = cur.execute(
                """SELECT t.*, s.name AS station_name, u.name AS student_name, u.faculty AS student_faculty
                   FROM transactions t
                   JOIN stations s ON s.id = t.station_id
                   LEFT JOIN users u ON u.id = t.student_id
                   ORDER BY t.created_at DESC LIMIT 200"""
            ).fetchall()
            inv_rows = cur.execute(
                """SELECT i.*, s.name AS station_name
                   FROM admin_invites i LEFT JOIN stations s ON s.id = i.station_id
                   ORDER BY i.created_at DESC LIMIT 100"""
            ).fetchall()
            return {
                "qrTransactions": [transaction_view(t) for t in tx_rows],
                "invites": [
                    {
                        "id": inv["id"],
                        "code": inv["code_preview"],
                        "label": inv["label"],
                        "stationId": inv["station_id"],
                        "stationName": inv["station_name"] or inv["station_id"],
                        "maxUses": int(inv["max_uses"]),
                        "usedCount": int(inv["used_count"]),
                        "expiresAt": int(inv["expires_at"]),
                        "isRevoked": inv["revoked_at"] is not None,
                        "createdAt": int(inv["created_at"]),
                    }
                    for inv in inv_rows
                ],
            }, 200

        m_qrev = re.match(r"^/admin/tokens/([^/]+)/revoke$", path)
        if method == "POST" and m_qrev:
            tx_id = m_qrev.group(1)
            _, actor = require_current_user(cur, self.headers)
            if actor["role"] != "admin":
                raise HttpError(403, "Admin access required")
            ts = now_ms()
            cur.execute("UPDATE transactions SET status = 'cancelled', cancelled_at = ? WHERE id = ? AND status = 'pending'", (ts, tx_id))
            if cur.rowcount != 1:
                raise HttpError(409, "Pending QR token not found")
            log_access(cur, actor["id"], actor["email"], "admin", "qr_token_revoked", "blocked", ip, ua, f"Revoked QR {tx_id}")
            return {"id": tx_id, "cancelledAt": ts}, 200

        if method == "POST" and path == "/admin/tokens/expire-stale":
            _, actor = require_current_user(cur, self.headers)
            if actor["role"] != "admin":
                raise HttpError(403, "Admin access required")
            cur.execute("UPDATE transactions SET status = 'expired' WHERE status = 'pending' AND expires_at <= ?", (now_ms(),))
            return {"expiredCount": cur.rowcount}, 200

        if method == "POST" and path == "/admin/invites":
            _, actor = require_current_user(cur, self.headers)
            if actor["role"] != "admin":
                raise HttpError(403, "Admin access required")
            inp = self.read_json()
            label = str(inp.get("label") or "").strip() or "Mã mời Admin"
            station_id = str(inp.get("stationId") or "soict-b1").strip()
            st = cur.execute("SELECT name FROM stations WHERE id = ?", (station_id,)).fetchone()
            if not st:
                raise HttpError(400, "Station not found")
            max_uses = max(1, min(50, int(inp.get("maxUses") or 3)))
            ttl_hours = max(1, min(720, int(inp.get("ttlHours") or 24)))
            ts = now_ms()
            expires_at = ts + ttl_hours * 3_600_000
            raw_code = f"ADM-{random_token(4)}-{random_token(4)}"
            inv_id = str(uuid.uuid4())
            cur.execute(
                """INSERT INTO admin_invites (id, code_hash, code_preview, label, station_id, max_uses, used_count, expires_at, created_by, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, ?)""",
                (inv_id, digest_b64(raw_code), raw_code, label, station_id, max_uses, expires_at, actor["id"], ts),
            )
            log_access(cur, actor["id"], actor["email"], "admin", "invite_created", "success", ip, ua, f"Created invite {raw_code}")
            return {
                "id": inv_id,
                "code": raw_code,
                "label": label,
                "stationId": station_id,
                "stationName": st["name"],
                "maxUses": max_uses,
                "usedCount": 0,
                "expiresAt": expires_at,
                "isRevoked": False,
                "createdAt": ts,
            }, 201

        m_irev = re.match(r"^/admin/invites/([^/]+)/revoke$", path)
        if method == "POST" and m_irev:
            inv_id = m_irev.group(1)
            _, actor = require_current_user(cur, self.headers)
            if actor["role"] != "admin":
                raise HttpError(403, "Admin access required")
            ts = now_ms()
            cur.execute("UPDATE admin_invites SET revoked_at = ? WHERE id = ? AND revoked_at IS NULL", (ts, inv_id))
            if cur.rowcount != 1:
                raise HttpError(404, "Invite not found")
            log_access(cur, actor["id"], actor["email"], "admin", "invite_revoked", "blocked", ip, ua, f"Revoked invite {inv_id}")
            return {"id": inv_id}, 200

        if method == "GET" and path == "/admin/access-logs":
            _, actor = require_current_user(cur, self.headers)
            if actor["role"] != "admin":
                raise HttpError(403, "Admin access required")
            ts = now_ms()
            logs = cur.execute("SELECT * FROM access_logs ORDER BY created_at DESC LIMIT 200").fetchall()
            lockouts = cur.execute("SELECT * FROM auth_throttles WHERE locked_until > ? ORDER BY locked_until DESC LIMIT 50", (ts,)).fetchall()
            return {
                "logs": [
                    {
                        "id": l["id"],
                        "userId": l["user_id"],
                        "email": l["email"],
                        "role": l["role"],
                        "eventType": l["event_type"],
                        "outcome": l["outcome"],
                        "ipAddress": l["ip_address"],
                        "deviceInfo": l["user_agent"],
                        "detail": l["detail"],
                        "createdAt": int(l["created_at"]),
                    }
                    for l in logs
                ],
                "lockouts": [
                    {
                        "throttleKey": lk["throttle_key"],
                        "emailHint": lk["email_hint"] or "Unknown",
                        "failureCount": int(lk["failure_count"]),
                        "lockedUntil": int(lk["locked_until"]),
                        "updatedAt": int(lk["updated_at"]),
                    }
                    for lk in lockouts
                ],
            }, 200

        if method == "POST" and path == "/admin/throttles/clear":
            _, actor = require_current_user(cur, self.headers)
            if actor["role"] != "admin":
                raise HttpError(403, "Admin access required")
            inp = self.read_json()
            thr_key = inp.get("throttleKey")
            if thr_key:
                cur.execute("DELETE FROM auth_throttles WHERE throttle_key = ?", (thr_key,))
            else:
                cur.execute("DELETE FROM auth_throttles WHERE locked_until > 0")
            log_access(cur, actor["id"], actor["email"], "admin", "throttle_cleared", "success", ip, ua, "Unlocked brute-force throttle")
            return {"cleared": True}, 200

        m_toggle = re.match(r"^/admin/stations/([^/]+)/toggle$", path)
        if method == "POST" and m_toggle:
            station_id = m_toggle.group(1)
            _, actor = require_current_user(cur, self.headers)
            if actor["role"] != "admin":
                raise HttpError(403, "Admin access required")
            st = cur.execute("SELECT id, name, is_active FROM stations WHERE id = ?", (station_id,)).fetchone()
            if not st:
                raise HttpError(404, "Station not found")
            next_active = 0 if int(st["is_active"] if st["is_active"] is not None else 1) == 1 else 1
            cur.execute("UPDATE stations SET is_active = ? WHERE id = ?", (next_active, station_id))
            log_access(cur, actor["id"], actor["email"], "admin", "station_resumed" if next_active else "station_paused", "success", ip, ua, f"{st['name']} -> {'active' if next_active else 'paused'}")
            return {"id": station_id, "isActive": next_active == 1}, 200

        raise HttpError(404, "Route not found")


def get_lan_ip() -> str:
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


def main():
    init_db()
    lan_ip = get_lan_ip()
    server = ThreadingHTTPServer(("0.0.0.0", PORT), FlysticHandler)
    print("=" * 60)
    print("  VERIFOUR HACKATHON SERVER IS RUNNING!")
    print("=" * 60)
    print(f"  Server URL (Điện thoại cùng Wi-Fi): http://{lan_ip}:{PORT}")
    print(f"  Server URL (Android Emulator):      http://10.0.2.2:{PORT}")
    print(f"  Database SQLite:                    {DB_PATH}")
    print("-" * 60)
    print("  Tài khoản Demo sẵn sàng:")
    print("    Admin (Trạm B1):   admin@hust.edu.vn       / admin1234")
    print("    Admin (Trạm C3):   admin.c3@hust.edu.vn    / admin1234")
    print("    Sinh viên 1:       sinhvien@sis.hust.edu.vn / student1234")
    print("    Sinh viên 2:       minh@sis.hust.edu.vn     / student1234")
    print("    Tài khoản bị khóa: locked@sis.hust.edu.vn   / student1234")
    print(f"    Mã mời tạo Admin:  {ADMIN_INVITE_CODE}")
    print("=" * 60)
    sys.stdout.flush()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nServer stopped.")


if __name__ == "__main__":
    main()
