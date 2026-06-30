# Ranking Smash Ultimate — Nouvelle-Aquitaine

Projet en 2 parties bien séparées :
- **back/** (`src/`) : API Java Spring Boot qui interroge l'API GraphQL de start.gg et expose un endpoint REST `/api/ranking`.
- **front/** (`webpage/`) : site statique HTML/CSS/JS qui fait un `fetch()` vers cette API et affiche le classement.

## 1. Configurer son token start.gg

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

Le plus simple : ouvre `webpage/index.html` directement dans ton navigateur,
ou sers-le avec un petit serveur statique pour éviter les soucis de CORS/file://:

```bash
cd webpage
python3 -m http.server 5500
```

Puis va sur `http://localhost:5500`.


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
└── webpage/
    ├── index.html
    ├── style.css
    └── script.js
```
