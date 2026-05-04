# AppCloud AWS — Architecture 3-Tiers Sécurisée

Application web Python Flask + PostgreSQL conteneurisée sur AWS EC2 avec architecture réseau sécurisée.

## Structure du projet

```
appcloud-aws/
├── backend/                    # EC2 Privé App (10.0.2.0/24)
│   ├── app.py                  # Application Flask (API REST)
│   ├── requirements.txt
│   ├── Dockerfile
│   └── entrypoint.sh
├── web-app/                    # Servi par Flask (templates + static)
│   ├── templates/
│   │   ├── index.html
│   │   ├── articles.html
│   │   └── contact.html
│   └── static/
│       ├── css/style.css
│       └── js/app.js
├── nginx/                      # EC2 Public (10.0.1.0/24)
│   ├── nginx.conf              # Config Nginx sécurisée
│   └── Dockerfile
├── postgres/                   # EC2 Privé DB (10.0.3.0/24)
│   ├── init.sql                # Init BDD + utilisateurs
│   ├── postgresql.conf         # Config PostgreSQL durcie
│   └── Dockerfile
├── security/
│   └── security-groups.json   # Référence SG + NACL
├── docker-compose.yml          # Dev local (tout-en-un)
├── docker-compose.web.yml      # Production EC2-WEB
├── docker-compose.backend.yml  # Production EC2-BACKEND
├── docker-compose.postgres.yml # Production EC2-POSTGRES
├── .env.example
├── .gitignore
```

## Démarrage rapide (local)

```bash
cp .env.example .env
docker-compose up -d --build
# Ouvrir http://localhost
```

## Architecture réseau

| Composant | Sous-réseau | CIDR | Accès Internet |
|---|---|---|---|
| EC2-WEB (Nginx) | Public | 10.0.1.0/24 | ✅ Oui (entrée 80/443) |
| EC2-BACKEND (Flask) | Privé App | 10.0.2.0/24 | ❌ Non |
| EC2-POSTGRES | Privé DB | 10.0.3.0/24 | ❌ Non |

## API Endpoints

| Endpoint | Méthode | Description |
|---|---|---|
| `/` | GET | Page d'accueil |
| `/articles` | GET | Page articles |
| `/contact` | GET | Page contact |
| `/health` | GET | Health check |
| `/api/articles` | GET | Liste des articles |
| `/api/articles` | POST | Créer un article |
| `/api/articles/:id` | PUT | Modifier un article |
| `/api/articles/:id` | DELETE | Archiver un article |
| `/api/contact` | POST | Envoyer un message |
| `/api/stats` | GET | Statistiques |
