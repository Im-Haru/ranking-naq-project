# Smash Ultimate Ranking — Nouvelle-Aquitaine

Project split into 2 clearly separated parts:
- **back/** (`src/`): Java Spring Boot API that queries start.gg's GraphQL API and exposes a REST endpoint `/api/ranking`.
- **front/** (`webpage/`): static HTML/CSS/JS site that does a `fetch()` to this API and displays the rankings.

## 1. Configure your start.gg token

1. Go to https://start.gg/admin/profile/developer and generate a personal token.
2. Launch your backend with the environment variable:
   ```bash
   export STARTGG_TOKEN=your_token_here
   ```
   (or edit `src/main/resources/application.yml` directly, but never commit it with your real token inside).

## 2. Run the backend

```bash
cd smash-na-ranking
mvn spring-boot:run
```

The API will be available at `http://localhost:8080/api/ranking`.

Test it directly in your browser or with curl:
```bash
curl http://localhost:8080/api/ranking
```

## 3. Run the frontend

The simplest way: open `webpage/index.html` directly in your browser,
or serve it with a small static server to avoid CORS/file:// issues:

```bash
cd webpage
python3 -m http.server 5500
```

Then go to `http://localhost:5500`.


## Structure

```
smash-na-ranking/
├── pom.xml
├── src/main/java/com/smashranking/api/
│   ├── SmashRankingApplication.java
│   ├── config/WebConfig.java          # CORS
│   ├── service/StartGgClient.java     # Raw GraphQL calls
│   ├── service/RankingService.java    # Business logic / aggregation
│   ├── model/PlayerRanking.java
│   └── controller/RankingController.java
├── src/main/resources/application.yml
└── webpage/
    ├── index.html
    ├── style.css
    └── script.js
```
