FROM python:3.12-slim

# Sécurité : utilisateur non-root
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

# Dépendances système minimales
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Dépendances Python
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Code source
COPY . .

# Permissions
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 5000

# Health check intégré
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:5000/health || exit 1

# Démarrage avec gunicorn (production-ready)
CMD ["gunicorn", "--bind", "0.0.0.0:5000", "--workers", "2", "--timeout", "60", "--access-logfile", "-", "--error-logfile", "-", "app:app"]
