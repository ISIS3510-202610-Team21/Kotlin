from sqlalchemy import Column, Integer, String, Float, Boolean, DateTime, func
from pydantic import BaseModel
from typing import Optional
from database import Base


# ─── SQLAlchemy Models (tablas en PostgreSQL) ──────────────────────────────────

class Crash(Base):
    __tablename__ = "crashes"
    id = Column(Integer, primary_key=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    module_name = Column(String(100))
    error_message = Column(String(500))
    user_id = Column(Integer, nullable=True)


class DaysSinceExpense(Base):
    __tablename__ = "days_since_expense"
    id = Column(Integer, primary_key=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), nullable=True)
    user_id = Column(Integer, unique=True, index=True)
    days = Column(Integer)
    is_inactive = Column(Boolean)


class OcrEditRate(Base):
    __tablename__ = "ocr_edit_rate"
    id = Column(Integer, primary_key=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    user_id = Column(Integer)
    fields_populated = Column(Integer)
    fields_edited = Column(Integer)
    edit_rate = Column(Float)


class ActiveHour(Base):
    __tablename__ = "active_hours"
    id = Column(Integer, primary_key=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    user_id = Column(Integer)
    hour = Column(Integer)
    session = Column(String(20))


class SmallRecurringExpense(Base):
    __tablename__ = "small_recurring_expenses"
    id = Column(Integer, primary_key=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), nullable=True)
    user_id = Column(Integer, unique=True, index=True)
    count = Column(Integer)
    total_amount = Column(Float)


class RegistrationMethod(Base):
    __tablename__ = "registration_methods"
    id = Column(Integer, primary_key=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), nullable=True)
    user_id = Column(Integer, unique=True, index=True)
    manual_count = Column(Integer)
    ocr_count = Column(Integer)
    google_pay_count = Column(Integer)
    least_used_method = Column(String(50))


# ─── Pydantic Schemas (validación de requests) ────────────────────────────────

class CrashEvent(BaseModel):
    module_name: str
    error_message: str
    user_id: Optional[int] = None


class DaysSinceExpenseEvent(BaseModel):
    user_id: int
    days: int
    is_inactive: bool


class OcrEditRateEvent(BaseModel):
    user_id: int
    fields_populated: int
    fields_edited: int
    edit_rate: float


class ActiveHourEvent(BaseModel):
    user_id: int
    hour: int
    session: str


class SmallRecurringEvent(BaseModel):
    user_id: int
    count: int
    total_amount: float


class RegistrationMethodsEvent(BaseModel):
    user_id: int
    manual_count: int
    ocr_count: int
    google_pay_count: int
    least_used_method: str
