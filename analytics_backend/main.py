from fastapi import FastAPI, Depends
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from datetime import datetime

from database import get_db, engine, Base
import models

Base.metadata.create_all(bind=engine)

app = FastAPI(title="SpendAntt Analytics API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health():
    return {"status": "ok", "timestamp": datetime.utcnow().isoformat()}


# ── BQ1: crashes por módulo ────────────────────────────────────────────────────
@app.post("/events/crash")
def log_crash(payload: models.CrashEvent, db: Session = Depends(get_db)):
    db.add(models.Crash(
        module_name=payload.module_name,
        error_message=payload.error_message,
        user_id=payload.user_id,
    ))
    db.commit()
    return {"ok": True}


# ── BQ2: días desde último gasto (upsert por user_id) ────────────────────────
@app.post("/events/days-since-expense")
def log_days_since(payload: models.DaysSinceExpenseEvent, db: Session = Depends(get_db)):
    row = db.query(models.DaysSinceExpense).filter_by(user_id=payload.user_id).first()
    if row:
        row.days = payload.days
        row.is_inactive = payload.is_inactive
        row.updated_at = datetime.utcnow()
    else:
        db.add(models.DaysSinceExpense(
            user_id=payload.user_id,
            days=payload.days,
            is_inactive=payload.is_inactive,
        ))
    db.commit()
    return {"ok": True}


# ── BQ3: OCR edit rate ────────────────────────────────────────────────────────
@app.post("/events/ocr-edit-rate")
def log_ocr_rate(payload: models.OcrEditRateEvent, db: Session = Depends(get_db)):
    db.add(models.OcrEditRate(
        user_id=payload.user_id,
        fields_populated=payload.fields_populated,
        fields_edited=payload.fields_edited,
        edit_rate=payload.edit_rate,
    ))
    db.commit()
    return {"ok": True}


# ── BQ4: hora más activa (evento por sesión) ──────────────────────────────────
@app.post("/events/active-hour")
def log_active_hour(payload: models.ActiveHourEvent, db: Session = Depends(get_db)):
    db.add(models.ActiveHour(
        user_id=payload.user_id,
        hour=payload.hour,
        session=payload.session,
    ))
    db.commit()
    return {"ok": True}


# ── BQ5: gastos pequeños recurrentes (upsert por user_id) ────────────────────
@app.post("/events/small-recurring")
def log_small_recurring(payload: models.SmallRecurringEvent, db: Session = Depends(get_db)):
    row = db.query(models.SmallRecurringExpense).filter_by(user_id=payload.user_id).first()
    if row:
        row.count = payload.count
        row.total_amount = payload.total_amount
        row.updated_at = datetime.utcnow()
    else:
        db.add(models.SmallRecurringExpense(
            user_id=payload.user_id,
            count=payload.count,
            total_amount=payload.total_amount,
        ))
    db.commit()
    return {"ok": True}


# ── BQ6: métodos de registro (upsert por user_id) ────────────────────────────
@app.post("/events/registration-methods")
def log_methods(payload: models.RegistrationMethodsEvent, db: Session = Depends(get_db)):
    row = db.query(models.RegistrationMethod).filter_by(user_id=payload.user_id).first()
    if row:
        row.manual_count = payload.manual_count
        row.ocr_count = payload.ocr_count
        row.google_pay_count = payload.google_pay_count
        row.least_used_method = payload.least_used_method
        row.updated_at = datetime.utcnow()
    else:
        db.add(models.RegistrationMethod(
            user_id=payload.user_id,
            manual_count=payload.manual_count,
            ocr_count=payload.ocr_count,
            google_pay_count=payload.google_pay_count,
            least_used_method=payload.least_used_method,
        ))
    db.commit()
    return {"ok": True}
