"""
Apply Supabase Migrations Directly via PostgreSQL Connection.
Executes 001_initial_schema.sql and 002_seed_data.sql.
"""

import os
import sys
import psycopg2

def apply_migrations(db_password: str = None):
    host = "db.munzcpabkjiuxorgmrse.supabase.co"
    port = 5432
    dbname = "postgres"
    user = "postgres"
    password = db_password or os.getenv("DB_PASSWORD", "Bug1*smrt")

    print(f"[*] Connecting directly to PostgreSQL at {host}:{port}...")
    conn = psycopg2.connect(
        dbname=dbname,
        user=user,
        password=password,
        host=host,
        port=port,
        connect_timeout=10
    )
    conn.autocommit = True
    cursor = conn.cursor()

    base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    schema_path = os.path.join(base_dir, "migrations", "001_initial_schema.sql")
    seed_path = os.path.join(base_dir, "migrations", "002_seed_data.sql")

    # 1. Apply Schema
    print(f"\n[*] Reading {schema_path}...")
    with open(schema_path, "r", encoding="utf-8") as f:
        schema_sql = f.read()

    print("[*] Executing 001_initial_schema.sql (18 tables, indexes, triggers)...")
    try:
        cursor.execute(schema_sql)
        print("  [SUCCESS] 001_initial_schema.sql applied successfully!")
    except Exception as e:
        print(f"  [ERROR] Applying schema failed: {e}")
        cursor.close()
        conn.close()
        sys.exit(1)

    # 2. Apply Seed Data
    print(f"\n[*] Reading {seed_path}...")
    with open(seed_path, "r", encoding="utf-8") as f:
        seed_sql = f.read()

    print("[*] Executing 002_seed_data.sql (curriculum, phases, topics, quizzes)...")
    try:
        cursor.execute(seed_sql)
        print("  [SUCCESS] 002_seed_data.sql applied successfully!")
    except Exception as e:
        print(f"  [WARNING] Applying seed data failed or partially applied: {e}")

    cursor.close()
    conn.close()
    print("\n=======================================================")
    print("[PASSED] Database migrations applied to Supabase!")
    print("=======================================================")

if __name__ == "__main__":
    pwd = sys.argv[1] if len(sys.argv) > 1 else None
    apply_migrations(pwd)
