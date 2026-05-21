"""
Seed script — manda eventos mock a Mixpanel para los 6 BQs.
Uso: python seed_mixpanel.py
Requiere: Python 3.7+ (solo stdlib, sin dependencias externas)
"""

import json
import base64
import urllib.request
import urllib.parse
import random
import time

TOKEN = "687875bcf4d97ec5bd77a1ed4cc6d8c7"
TRACK_URL = "https://api.mixpanel.com/track"

NOW = int(time.time())
DAY = 86400


def send_batch(events: list[dict]):
    encoded = base64.b64encode(json.dumps(events).encode()).decode()
    body = urllib.parse.urlencode({"data": encoded, "verbose": "1"}).encode()
    req = urllib.request.Request(
        TRACK_URL,
        data=body,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    with urllib.request.urlopen(req) as resp:
        result = json.loads(resp.read())
        if result.get("status") != 1:
            print(f"  ERROR: {result}")
        else:
            print(f"  OK: {len(events)} eventos enviados")


def prop(token, distinct_id, t, **kwargs) -> dict:
    return {"token": token, "distinct_id": distinct_id, "time": t, **kwargs}


events = []

# ── BQ1: module_crash ────────────────────────────────────────────────────────
print("BQ1: module_crash")
modules = [
    ("AutoCategorizationService", "NullPointerException in categorize()"),
    ("SyncService", "Timeout connecting to Firestore"),
    ("OcrProcessor", "Failed to parse receipt image"),
    ("NewExpenseScreen", "IllegalStateException: ViewModel not attached"),
    ("GmailNotificationListener", "SecurityException: missing permission"),
]
for _ in range(50):
    mod, err = random.choice(modules)
    uid = f"user_{random.randint(1, 20)}"
    events.append({
        "event": "module_crash",
        "properties": prop(TOKEN, uid, NOW - random.randint(0, 30 * DAY),
                           module_name=mod, error_message=err),
    })

# ── BQ2: days_since_expense ──────────────────────────────────────────────────
print("BQ2: days_since_expense")
for user_id in range(1, 26):
    days = random.choice([0, 1, 1, 2, 3, 5, 7, 10, 14])
    uid = f"user_{user_id}"
    events.append({
        "event": "days_since_expense",
        "properties": prop(TOKEN, uid, NOW - random.randint(0, 30 * DAY),
                           user_id=user_id, days=days, is_inactive=days >= 3),
    })

# ── BQ3: ocr_edit_rate ───────────────────────────────────────────────────────
print("BQ3: ocr_edit_rate")
for _ in range(60):
    uid = f"user_{random.randint(1, 20)}"
    populated = random.randint(2, 5)
    edited = random.randint(0, populated)
    rate = round(edited / populated, 2)
    events.append({
        "event": "ocr_edit_rate",
        "properties": prop(TOKEN, uid, NOW - random.randint(0, 30 * DAY),
                           user_id=int(uid.split("_")[1]),
                           fields_populated=populated,
                           fields_edited=edited,
                           edit_rate=rate),
    })

# ── BQ4: active_hour ─────────────────────────────────────────────────────────
print("BQ4: active_hour")
hour_weights = {
    7: 3, 8: 5, 9: 8, 10: 6, 11: 4,        # mañana
    12: 7, 13: 9, 14: 6, 15: 5, 16: 4,     # tarde
    17: 6, 18: 10, 19: 8, 20: 6, 21: 4,    # noche
    22: 2, 23: 1, 0: 1, 1: 1, 6: 2,        # madrugada
}
hours_pool = [h for h, w in hour_weights.items() for _ in range(w)]
for _ in range(80):
    uid = f"user_{random.randint(1, 20)}"
    hour = random.choice(hours_pool)
    session = (
        "morning" if 6 <= hour <= 11 else
        "afternoon" if 12 <= hour <= 17 else
        "evening" if 18 <= hour <= 22 else
        "night"
    )
    events.append({
        "event": "active_hour",
        "properties": prop(TOKEN, uid, NOW - random.randint(0, 30 * DAY),
                           user_id=int(uid.split("_")[1]),
                           hour=hour, session=session),
    })

# ── BQ5: small_recurring_expenses ───────────────────────────────────────────
print("BQ5: small_recurring_expenses")
for user_id in range(1, 26):
    count = random.randint(1, 8)
    total = round(random.uniform(5_000, 40_000) * count, 2)
    uid = f"user_{user_id}"
    events.append({
        "event": "small_recurring_expenses",
        "properties": prop(TOKEN, uid, NOW - random.randint(0, 30 * DAY),
                           user_id=user_id, count=count, total_amount=total),
    })

# ── BQ6: registration_methods ────────────────────────────────────────────────
print("BQ6: registration_methods")
method_profiles = [
    (40, 5, 2),   (10, 30, 5),  (20, 15, 25), (12, 8, 3),
    (30, 20, 10), (5, 40, 8),   (25, 10, 15), (18, 22, 7),
    (35, 12, 4),  (8, 25, 6),   (22, 18, 30), (15, 10, 2),
    (28, 22, 12), (6, 38, 9),   (20, 8, 18),  (14, 20, 5),
    (45, 8, 3),   (9, 28, 7),   (18, 14, 22), (11, 9, 4),
]
for user_id, (manual, ocr, gpay) in enumerate(method_profiles, start=1):
    uid = f"user_{user_id}"
    counts = {"manual": manual, "ocr": ocr, "google_pay": gpay}
    least = min(counts, key=lambda k: counts[k])
    events.append({
        "event": "registration_methods",
        "properties": prop(TOKEN, uid, NOW - random.randint(0, 30 * DAY),
                           user_id=user_id,
                           manual_count=manual,
                           ocr_count=ocr,
                           google_pay_count=gpay,
                           least_used_method=least),
    })

# ── Envío en lotes de 50 (límite de Mixpanel) ───────────────────────────────
print(f"\nEnviando {len(events)} eventos en total...")
batch_size = 50
for i in range(0, len(events), batch_size):
    send_batch(events[i:i + batch_size])

print("\nListo. Abre Mixpanel > Events para verlos (puede tardar ~1 min en aparecer).")
