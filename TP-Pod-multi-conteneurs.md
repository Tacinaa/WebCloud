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

## Exercice 5 — Explorer le volume partagé
- Oui elle affichent le même contenu parce que les 2 conteneurs montent le même volume.
  
## Exercice 6 — Vérifier le réseau partagé
- On ne voit qu’une seule adresse IP car elle correspond au Pod.  
Les conteneurs ne possèdent pas d’IP propre : ils partagent celle du Pod.  
Cela implique qu’ils peuvent communiquer entre eux via localhost.

## Exercice 7 — Modification à chaud
- On voit immédiatement le message aprè srafraichissement.
- 3 secondes plus tard, l'ancien message apparait, étant donné que le conteneur writer continue d'écrire et écrase ainsi la modification manuelle.

## Exercice 8 — Résilience du Pod
- Aucun redémarrage visible n’a été observé dans mon environnement.
- Non, le conteneur nginx a continué à fonctionner normalement.
- La page est restée accessible et nginx a continué à servir le contenu.

## Exercice 9 — Nettoyage
- Le contenu du volume emptyDir est supprimé en même temps que le Pod.