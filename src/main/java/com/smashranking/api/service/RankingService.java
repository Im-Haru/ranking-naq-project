package com.smashranking.api.service;

import com.smashranking.api.model.PlayerRanking;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RankingService {

    private static final int SMASH_ULTIMATE_ID = 1386;
    private static final double NA_LAT = 44.8378;
    private static final double NA_LNG = -0.5792;
    private static final String NA_RADIUS = "150km";
    private static final List<String> BANNED_PLAYERS = List.of("spectral");

    private final StartGgClient client;

    public RankingService(StartGgClient client) {
        this.client = client;
    }

    private static final String TOURNAMENTS_QUERY = """
        query TournamentsByLocation($perPage: Int!, $coordinates: String!, $radius: String!, $videogameIds: [ID]!, $afterDate: Timestamp!) {
          tournaments(query: {
            perPage: $perPage
            filter: {
              location: {
                distanceFrom: $coordinates
                distance: $radius
              }
              videogameIds: $videogameIds
              afterDate: $afterDate
            }
          }) {
            nodes {
              id
              name
              events(filter: { videogameId: $videogameIds }) {
                id
                name
              }
            }
          }
        }
        """;

    private static final String STANDINGS_QUERY = """
        query EventStandings($eventId: ID!, $page: Int!, $perPage: Int!) {
          event(id: $eventId) {
            standings(query: { page: $page, perPage: $perPage }) {
              nodes {
                placement
                entrant {
                  name
                }
              }
            }
          }
        }
        """;

    public Mono<List<PlayerRanking>> buildRegionalRanking() {
        // Timestamp UNIX : 1704067200L correspond au 1er Janvier 2024 à minuit.
        long startOfSeasonTimestamp = 1704067200L;

        Map<String, Object> tournamentVars = Map.of(
                "perPage", 150,
                "coordinates", NA_LAT + "," + NA_LNG,
                "radius", NA_RADIUS,
                "videogameIds", List.of(String.valueOf(SMASH_ULTIMATE_ID)),
                "afterDate", startOfSeasonTimestamp
        );

        return client.query(TOURNAMENTS_QUERY, tournamentVars)
                .flatMap(this::extractEventIds)
                .flatMap(this::fetchStandingsForAllEvents);
    }

    @SuppressWarnings("unchecked")
    private Mono<List<String>> extractEventIds(Map<String, Object> response) {
        if (response.get("data") == null) {
            System.err.println("Réponse start.gg sans 'data'. Réponse complète : " + response);
            Object errors = response.get("errors");
            return Mono.error(new RuntimeException(
                    "start.gg n'a pas renvoyé de données. Erreurs renvoyées par l'API : " + errors));
        }

        try {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            Map<String, Object> tournaments = (Map<String, Object>) data.get("tournaments");
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) tournaments.get("nodes");
            List<String> eventIds = new ArrayList<>();

            for (Map<String, Object> tournament : nodes) {
                List<Map<String, Object>> events = (List<Map<String, Object>>) tournament.get("events");
                if (events != null) {
                    for (Map<String, Object> event : events) {
                      String eventName = String.valueOf(event.get("name")).toLowerCase();
                      boolean isSingles = eventName.contains("1v1") 
                                      || eventName.contains("singles") 
                                      || eventName.contains("ultimate single")
                                      || eventName.equals("tournoi");
                      
                      boolean isNotTrash = !eventName.contains("ladder") 
                                        && !eventName.contains("liste d'attente")
                                        && !eventName.contains("sf6")
                                        && !eventName.contains("street fighter")
                                        && !eventName.contains("tekken")
                                        && !eventName.contains("t8");

                      if (isSingles && isNotTrash) {
                          eventIds.add(String.valueOf(event.get("id")));
                      }
                  }
                }
            }
            return Mono.just(eventIds);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Erreur en parsant la réponse 'tournaments' de start.gg", e));
        }
    }

    @SuppressWarnings("unchecked")
    private Mono<List<PlayerRanking>> fetchStandingsForAllEvents(List<String> eventIds) {
        Map<String, PlayerRanking> rankingByPlayer = new HashMap<>();

        return reactor.core.publisher.Flux.fromIterable(eventIds)
                .delayElements(java.time.Duration.ofMillis(1500))
                .concatMap(eventId -> client.query(STANDINGS_QUERY, Map.of(
                        "eventId", eventId,
                        "page", 1,
                        "perPage", 64
                )))
                .doOnNext(response -> addStandingsToRanking((Map<String, Object>) response, rankingByPlayer))
                .then(Mono.defer(() -> Mono.just(
                        rankingByPlayer.values().stream()
                                .filter(player -> player.getTournamentsPlayed() >= 3)
                                .sorted(Comparator.comparingInt(PlayerRanking::getTotalPoints).reversed())
                                .collect(Collectors.toList())
                )));
    }

    @SuppressWarnings("unchecked")
    private void addStandingsToRanking(Map<String, Object> response, Map<String, PlayerRanking> rankingByPlayer) {
        try {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            Map<String, Object> event = (Map<String, Object>) data.get("event");
            if (event == null) return;

            Map<String, Object> standings = (Map<String, Object>) event.get("standings");
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) standings.get("nodes");

            for (Map<String, Object> node : nodes) {
                int placement = ((Number) node.get("placement")).intValue();
                Map<String, Object> entrant = (Map<String, Object>) node.get("entrant");
                if (entrant == null) continue;
                
                String rawPlayerName = String.valueOf(entrant.get("name"));
                String finalPlayerName = normalizePlayerName(rawPlayerName);
                
                if (BANNED_PLAYERS.contains(finalPlayerName.toLowerCase())) {
                    continue; 
                }

                rankingByPlayer
                        .computeIfAbsent(finalPlayerName, PlayerRanking::new)
                        .addResult(placement);
            }
        } catch (Exception e) {
            System.err.println("Erreur en parsant les standings d'un event : " + e.getMessage());
        }
    }

    private String normalizePlayerName(String rawName) {
        String cleanName = rawName.trim().toLowerCase();
        if (cleanName.contains("|")) {
            cleanName = cleanName.substring(cleanName.indexOf("|") + 1).trim();
        }
        Map<String, String> aliases = new HashMap<>();
        aliases.put("10 12", "Félix");
        aliases.put("bryan", "CAPTAIN AMERICA");
        aliases.put("maxence", "maxonsse");
        aliases.put("gamercharmant", "CAPTAIN AMERICA");
        aliases.put("leo supremacy", "pathbaulo");

        if (aliases.containsKey(cleanName)) {
            return aliases.get(cleanName);
        }      
        String originalWithoutTeam = rawName.contains("|") ? rawName.substring(rawName.indexOf("|") + 1).trim() : rawName;
        return originalWithoutTeam;
    }
}