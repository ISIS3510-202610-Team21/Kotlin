"""
Migration: Firestore → PostgreSQL
Backfills BQ2, BQ4, BQ5, BQ6 desde los datos existentes en Firestore.
BQ1 (crashes) y BQ3 (OCR edit rate) arrancan desde cero.

Pasos previos:
  1. Firebase Console → Project Settings → Service Accounts → Generate new private key
  2. Guarda el JSON como analytics_backend/serviceAccountKey.json
  3. Crea la base de datos: createdb spendantt_analytics
  4. Instala deps: pip install -r requirements.txt
  5. Ejecuta: python migration.py
"""
import time
from collections import Counter

import firebase_admin
from firebase_admin import credentials, firestore

from database import SessionLocal, engine
from models import Base, DaysSinceExpense, ActiveHour, SmallRecurringExpense, RegistrationMethod

SMALL_EXPENSE_THRESHOLD = 50_000.0
THREE_MONTHS_MS = 3 * 30 * 24 * 60 * 60 * 1000


def run():
    cred = credentials.Certificate("serviceAccountKey.json")
    firebase_admin.initialize_app(cred)
    fs = firestore.client()

    Base.metadata.create_all(bind=engine)
    db = SessionLocal()

    try:
        expenses_by_user: dict[str, list[dict]] = {}
        for doc in fs.collection("expenses").stream():
            data = doc.to_dict()
            uid = str(data.get("userId") or data.get("firebaseUid") or "unknown")
            expenses_by_user.setdefault(uid, []).append(data)

        total = sum(len(v) for v in expenses_by_user.values())
        print(f"Migrating {total} expenses across {len(expenses_by_user)} users...")

        now_ms = time.time() * 1000

        for uid, expenses in expenses_by_user.items():
            user_id = abs(hash(uid)) % 1_000_000

            # BQ2: días desde último gasto
            dates = [e.get("date") for e in expenses if e.get("date")]
            if dates:
                last_ms = max(float(d) if isinstance(d, (int, float)) else 0 for d in dates)
                days = int((now_ms - last_ms) / (1000 * 60 * 60 * 24))
                db.add(DaysSinceExpense(user_id=user_id, days=days, is_inactive=(days >= 3)))

            # BQ4: hora más activa
            hour_counts: Counter = Counter()
            for e in expenses:
                h = _parse_hour(e.get("time", ""))
                if h is not None:
                    hour_counts[h] += 1
            if hour_counts:
                best_hour = hour_counts.most_common(1)[0][0]
                db.add(ActiveHour(user_id=user_id, hour=best_hour, session=_session(best_hour)))

            # BQ5: gastos pequeños recurrentes (últimos 3 meses)
            cutoff = now_ms - THREE_MONTHS_MS
            small = [
                e for e in expenses
                if e.get("isRecurring")
                and float(e.get("amount", 0)) < SMALL_EXPENSE_THRESHOLD
                and float(e.get("date", 0)) > cutoff
            ]
            if small:
                db.add(SmallRecurringExpense(
                    user_id=user_id,
                    count=len(small),
                    total_amount=sum(float(e.get("amount", 0)) for e in small),
                ))

            # BQ6: métodos de registro
            manual = sum(1 for e in expenses if e.get("source") == "MANUAL")
            ocr = sum(1 for e in expenses if e.get("source") == "OCR")
            gpay = sum(1 for e in expenses if e.get("source") == "GOOGLE_PAY")
            if manual + ocr + gpay > 0:
                least = min(
                    [("manual", manual), ("ocr", ocr), ("google_pay", gpay)],
                    key=lambda x: x[1],
                )[0]
                db.add(RegistrationMethod(
                    user_id=user_id,
                    manual_count=manual,
                    ocr_count=ocr,
                    google_pay_count=gpay,
                    least_used_method=least,
                ))

        db.commit()
        print("Migration completed successfully!")

    except Exception as exc:
        db.rollback()
        print(f"Migration failed: {exc}")
        raise
    finally:
        db.close()


def _parse_hour(time_str: str):
    try:
        upper = time_str.upper()
        is_pm = "PM" in upper
        clean = upper.replace("AM", "").replace("PM", "").strip()
        hour = int(clean.split(":")[0])
        if is_pm and hour != 12:
            hour += 12
        if not is_pm and hour == 12:
            hour = 0
        return hour
    except Exception:
        return None


def _session(hour: int) -> str:
    if 6 <= hour <= 11:
        return "morning"
    if 12 <= hour <= 17:
        return "afternoon"
    if 18 <= hour <= 22:
        return "evening"
    return "night"


if __name__ == "__main__":
    run()
