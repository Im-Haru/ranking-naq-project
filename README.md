# Ranking Smash Ultimate — Nouvelle-Aquitaine

Projet en 2 parties bien séparées :
- **back/** (`src/`) : API Java Spring Boot qui interroge l'API GraphQL de start.gg et expose un endpoint REST `/api/ranking`.
- **front/** (`frontend/`) : site statique HTML/CSS/JS qui fait un `fetch()` vers cette API et affiche le classement.

## 1. Configurer ton token start.gg

1. Va sur https://start.gg/admin/profile/developer et génère un token personnel.
2. Lance ton back avec la variable d'environnement :
   ```bash
   export STARTGG_TOKEN=ton_token_ici
   ```
   (ou modifie directement `src/main/resources/application.yml`, mais ne le commit jamais avec ton vrai token dedans).

## 2. Lancer le back

```bash
cd smash-na-ranking
mvn spring-boot:run
```

L'API sera disponible sur `http://localhost:8080/api/ranking`.

Teste directement dans le navigateur ou avec curl :
```bash
curl http://localhost:8080/api/ranking
```

## 3. Lancer le front

Le plus simple : ouvre `frontend/index.html` directement dans ton navigateur,
ou sers-le avec un petit serveur statique pour éviter les soucis de CORS/file://:

```bash
cd frontend
python3 -m http.server 5500
```

Puis va sur `http://localhost:5500`.

## 4. Ce qu'il te reste à affiner

- **La requête `TOURNAMENTS_QUERY`** dans `RankingService.java` filtre par distance GPS
  autour de Bordeaux (`distanceFrom` + `distance`). Ajuste `NA_RADIUS` selon ta couverture
  géographique réelle (la Nouvelle-Aquitaine est grande, 150km depuis Bordeaux ne couvre
  pas tout, par ex. le Limousin). Tu peux aussi faire plusieurs requêtes avec plusieurs
  points centraux et fusionner les résultats.
- **La période temporelle** : la query actuelle ne filtre pas par date — ajoute
  `afterDate` / `beforeDate` dans le filter si tu veux un ranking "saison" plutôt que
  "all-time".
- **Pagination** : `tournaments` et `standings` sont paginés côté start.gg (`perPage` max
  est limité, généralement 500 pour standings et moins pour tournaments selon le plan
  d'accès). Si tu as beaucoup de tournois, il faudra boucler sur les pages.
- **Rate limiting** : start.gg limite le nombre de requêtes/seconde. Si tu as beaucoup
  d'events, ajoute un throttle entre tes appels (`Flux.concatMap` avec délai plutôt que
  `Mono.zip` qui part tout en parallèle).
- **Identification des joueurs** : actuellement on regroupe par `entrant.name`, ce qui
  peut créer des doublons si un joueur change de tag. Idéalement utilise plutôt l'ID du
  participant (`entrant.participants[].player.id`) pour un suivi fiable dans le temps.

## Structure

```
smash-na-ranking/
├── pom.xml
├── src/main/java/com/smashranking/api/
│   ├── SmashRankingApplication.java
│   ├── config/WebConfig.java          # CORS
│   ├── service/StartGgClient.java     # Appels GraphQL bruts
│   ├── service/RankingService.java    # Logique métier / agrégation
│   ├── model/PlayerRanking.java
│   └── controller/RankingController.java
├── src/main/resources/application.yml
└── frontend/
    ├── index.html
    ├── style.css
    └── script.js
```
