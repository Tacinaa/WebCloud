FROM postgres:16-alpine

# Configuration de sécurité
ENV POSTGRES_DB=appdb
ENV POSTGRES_USER=postgres

# Script d'initialisation
COPY init.sql /docker-entrypoint-initdb.d/01-init.sql

# Config PostgreSQL durcie
COPY postgresql.conf /etc/postgresql/postgresql.conf

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=5 \
    CMD pg_isready -U postgres -d appdb || exit 1

EXPOSE 5432
