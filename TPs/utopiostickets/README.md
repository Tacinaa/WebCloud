# UtopiosTickets — Plateforme de billetterie microservices

Architecture microservices deployée sur Kind (Kubernetes local) avec AWS DynamoDB et S3.

## Architecture

| Service | Port local | Rôle |
|---|---|---|
| event-service | 8081 | CRUD événements + réservation de places (DynamoDB) |
| ticket-service | 8082 | Génération de billets PDF/TXT dans S3 + URLs pré-signées |
| booking-service | 8083 | Orchestrateur : coordonne event-service et ticket-service (DynamoDB) |
| report-job | — | CronJob quotidien : export JSON des réservations vers S3 |

## Prérequis

- Docker
- [Kind](https://kind.sigs.k8s.io/) `v0.20+`
- kubectl
- AWS CLI configuré (`aws configure`)
- Java 21 + Maven (pour le build local)

---

## 1. Créer les ressources AWS

```bash
chmod +x scripts/create-aws-resources.sh scripts/delete-aws-resources.sh
./scripts/create-aws-resources.sh
```

Le script affiche les noms des buckets S3 créés. **Mettre à jour `k8s/configmap.yaml`** avec les valeurs réelles :

```yaml
S3_TICKETS_BUCKET: "utopios-tickets-123456789012"
S3_REPORTS_BUCKET: "utopios-reports-123456789012"
```

---

## 2. Créer le cluster Kind

```bash
kind create cluster --name utopios --config k8s/kind-config.yaml
```

---

## 3. Builder les images Docker

Les images event-service et report-job se buildent depuis la racine du projet.  
Les images ticket-service et booking-service se buildent depuis leur répertoire.

```bash
# event-service (contexte = racine)
docker build -f event-service/Dockerfile -t utopios/event-service:latest .

# ticket-service (contexte = ticket-service/)
docker build -t utopios/ticket-service:latest ticket-service/

# booking-service (contexte = booking-service/)
docker build -t utopios/booking-service:latest booking-service/

# report-job (contexte = racine)
docker build -f report-job/Dockerfile -t utopios/report-job:latest .
```

---

## 4. Charger les images dans Kind

```bash
kind load docker-image utopios/event-service:latest   --name utopios
kind load docker-image utopios/ticket-service:latest  --name utopios
kind load docker-image utopios/booking-service:latest --name utopios
kind load docker-image utopios/report-job:latest      --name utopios
```

---

## 5. Déployer sur Kubernetes

```bash
# Namespace
kubectl apply -f k8s/namespace.yaml

# Créer le secret AWS (jamais en clair dans les fichiers)
kubectl create secret generic aws-credentials \
  --namespace utopios \
  --from-literal=AWS_ACCESS_KEY_ID="$AWS_ACCESS_KEY_ID" \
  --from-literal=AWS_SECRET_ACCESS_KEY="$AWS_SECRET_ACCESS_KEY"

# ConfigMap + services
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/event-service.yaml
kubectl apply -f k8s/ticket-service.yaml
kubectl apply -f k8s/booking-service.yaml
kubectl apply -f k8s/report-job.yaml
```

Vérifier que tout est prêt :

```bash
kubectl get pods -n utopios -w
```

---

## 6. Vérification end-to-end

### Créer un événement

```bash
curl -s -X POST http://localhost:8081/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Concert Jazz",
    "location": "Paris",
    "date": "2026-09-15",
    "totalCapacity": 100,
    "unitPrice": 49.90
  }' | jq .
```

### Créer une réservation (flux complet)

```bash
EVENT_ID="<id retourné ci-dessus>"

curl -s -X POST http://localhost:8083/api/bookings \
  -H "Content-Type: application/json" \
  -d "{
    \"customerName\": \"Alice Dupont\",
    \"eventId\": \"$EVENT_ID\",
    \"quantity\": 2
  }" | jq .
```

La réponse contient `ticketDownloadUrl` — une URL pré-signée S3 valable 15 min.

### Télécharger le billet

```bash
curl -L "<ticketDownloadUrl>"
```

### Vérifier les places disponibles

```bash
curl http://localhost:8081/api/events/$EVENT_ID | jq .availableSeats
```

### Rechercher les réservations par client

```bash
curl "http://localhost:8083/api/bookings?customerName=Alice%20Dupont" | jq .
```

### Déclencher le job de rapport manuellement

```bash
kubectl create job --from=cronjob/report-job report-job-manual -n utopios
kubectl logs -n utopios -l job-name=report-job-manual --follow
```

---

## 7. Résilience

Mettre le event-service en échec :

```bash
kubectl scale deployment event-service --replicas=0 -n utopios

# Tentative de réservation → 503 Service Unavailable
curl -s -X POST http://localhost:8083/api/bookings \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Bob","eventId":"xxx","quantity":1}'

kubectl scale deployment event-service --replicas=1 -n utopios
```

---

## 8. Nettoyage

```bash
# Supprimer le cluster Kind
kind delete cluster --name utopios

# Supprimer les ressources AWS
./scripts/delete-aws-resources.sh
```
