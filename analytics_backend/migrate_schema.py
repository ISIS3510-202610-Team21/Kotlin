"""
Aplica los cambios de schema para el fix de upsert:
- Agrega columna updated_at a days_since_expense, small_recurring_expenses, registration_methods
- Agrega unique constraint en user_id para esas tablas
- Elimina filas duplicadas por user_id (conserva la más reciente por id)

Ejecutar una sola vez: python migrate_schema.py
"""
from sqlalchemy import text
from database import engine

MIGRATIONS = [
    # days_since_expense
    "ALTER TABLE days_since_expense ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ",
    """DELETE FROM days_since_expense d1
       USING days_since_expense d2
       WHERE d1.id < d2.id AND d1.user_id = d2.user_id""",
    "ALTER TABLE days_since_expense DROP CONSTRAINT IF EXISTS days_since_expense_user_id_key",
    "ALTER TABLE days_since_expense ADD CONSTRAINT days_since_expense_user_id_key UNIQUE (user_id)",

    # small_recurring_expenses
    "ALTER TABLE small_recurring_expenses ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ",
    """DELETE FROM small_recurring_expenses d1
       USING small_recurring_expenses d2
       WHERE d1.id < d2.id AND d1.user_id = d2.user_id""",
    "ALTER TABLE small_recurring_expenses DROP CONSTRAINT IF EXISTS small_recurring_expenses_user_id_key",
    "ALTER TABLE small_recurring_expenses ADD CONSTRAINT small_recurring_expenses_user_id_key UNIQUE (user_id)",

    # registration_methods
    "ALTER TABLE registration_methods ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ",
    """DELETE FROM registration_methods d1
       USING registration_methods d2
       WHERE d1.id < d2.id AND d1.user_id = d2.user_id""",
    "ALTER TABLE registration_methods DROP CONSTRAINT IF EXISTS registration_methods_user_id_key",
    "ALTER TABLE registration_methods ADD CONSTRAINT registration_methods_user_id_key UNIQUE (user_id)",
]


def run():
    with engine.begin() as conn:
        for sql in MIGRATIONS:
            sql = sql.strip()
            if sql:
                print(f"Running: {sql[:80].replace(chr(10), ' ')}...")
                conn.execute(text(sql))
    print("Schema migration completed.")


if __name__ == "__main__":
    run()
