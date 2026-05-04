#!/bin/sh
# entrypoint.sh - Initialisation et démarrage du backend

set -e

echo "[INFO] Attente de PostgreSQL sur ${DB_HOST}:${DB_PORT}..."
MAX_RETRIES=30
COUNT=0

while ! python -c "
import psycopg2, sys, os
try:
    psycopg2.connect(
        host=os.environ.get('DB_HOST','postgres'),
        port=os.environ.get('DB_PORT','5432'),
        dbname=os.environ.get('DB_NAME','appdb'),
        user=os.environ.get('DB_USER','appuser'),
        password=os.environ.get('DB_PASS','changeme')
    )
    sys.exit(0)
except Exception as e:
    sys.exit(1)
" 2>/dev/null; do
    COUNT=$((COUNT + 1))
    if [ "$COUNT" -ge "$MAX_RETRIES" ]; then
        echo "[ERROR] PostgreSQL non disponible après $MAX_RETRIES tentatives."
        exit 1
    fi
    echo "[INFO] Tentative $COUNT/$MAX_RETRIES - nouvelle tentative dans 2s..."
    sleep 2
done

echo "[INFO] PostgreSQL disponible. Initialisation de la base..."
python -c "from app import init_db; init_db()"

echo "[INFO] Démarrage de gunicorn..."
exec gunicorn \
    --bind 0.0.0.0:5000 \
    --workers 2 \
    --timeout 60 \
    --access-logfile - \
    --error-logfile - \
    --log-level info \
    app:app
