package com.smashranking.api.controller;

import com.smashranking.api.model.PlayerRanking;
import com.smashranking.api.service.RankingService;
import com.smashranking.api.service.StartGgClient; // Important
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors; 

@RestController
@CrossOrigin
public class RankingController {

    private final RankingService rankingService;
    private final StartGgClient client;

    public RankingController(RankingService rankingService, StartGgClient client) {
        this.rankingService = rankingService;
        this.client = client;
    }

    @GetMapping("/api/ranking")
    public Mono<List<PlayerRanking>> getRegionalRanking() {
        return rankingService.buildRegionalRanking();
    }

    @GetMapping("/api/debug-tournaments")
    public Mono<Map<String, List<String>>> debugTournaments() {
        return client.query(
            """
            query DebugTournaments($perPage: Int!, $coordinates: String!, $radius: String!) {
              tournaments(query: {
                perPage: $perPage
                filter: {
                  location: { distanceFrom: $coordinates, distance: $radius }
                  videogameIds: [1386]
                }
              }) {
                nodes {
                  name
                  events {
                    name
                  }
                }
              }
            }
            """, 
            Map.of("perPage", 30, "coordinates", "44.8378,-0.5792", "radius", "150km")
        ).map(response -> {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            Map<String, Object> tournaments = (Map<String, Object>) data.get("tournaments");
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) tournaments.get("nodes");
            
            Map<String, List<String>> debugMap = new LinkedHashMap<>();
            for (Map<String, Object> t : nodes) {
                List<Map<String, Object>> events = (List<Map<String, Object>>) t.get("events");
                List<String> eventNames = events.stream()
                    .map(e -> String.valueOf(e.get("name")))
                    .collect(Collectors.toList());
                debugMap.put(String.valueOf(t.get("name")), eventNames);
            }
            return debugMap;
        });
    }
}