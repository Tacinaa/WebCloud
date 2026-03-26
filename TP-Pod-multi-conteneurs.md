# TP — Pod multi-conteneurs : Sidecar Writer + Nginx

## Exercice 3 — Accéder à l'application

### Questions
- Je vois afficher "Thu Mar 26 11:26:01 UTC 2026 - Je suis Ã  Ynov Croix"
- Non, la page ne se met pas à jour automatiquement.
- En revanche, le contenu change toutes les 3 secondes côté serveur. Il faut rafraîchir la page manuellement pour observer les mises à jour.
  
## Exercice 4 — Observer les logs
### Questions
- À chaque rafraîchissement, une nouvelle requête HTTP apparaît dans les logs du conteneur nginx.
- -c