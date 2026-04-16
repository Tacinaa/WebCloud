FROM python:3.12-slim

LABEL maintainer="Utopios Training"
LABEL description="S3 Manager - Read/Write only (no delete)"

# Sécurité : utilisateur non-root
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Dépendances d'abord (cache Docker optimisé)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Code source
COPY . .

# Permissions
RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 5000

ENV FLASK_ENV=production
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

# Gunicorn en production
CMD ["gunicorn", "--bind", "0.0.0.0:5000", "--workers", "2", "--timeout", "60", "--access-logfile", "-", "app:app"]
