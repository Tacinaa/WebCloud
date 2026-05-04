#!/bin/bash
set -e
exec > /var/log/user-data.log 2>&1

echo "[$(date)] Début installation Docker..."

# Mise à jour système
dnf update -y

# Installation Docker
dnf install -y docker

# Démarrage et activation au boot
systemctl start docker
systemctl enable docker

# ec2-user dans le groupe docker
usermod -aG docker ec2-user

# Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

echo "[$(date)] Docker installé : $(docker --version)"
echo "[$(date)] Docker Compose  : $(docker-compose --version)"
echo "[$(date)] Installation terminée ✓"