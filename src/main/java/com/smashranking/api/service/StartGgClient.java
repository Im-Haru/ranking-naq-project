package com.smashranking.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class StartGgClient {

    private final WebClient webClient;

    public StartGgClient(
            @Value("${startgg.api-url}") String apiUrl,
            @Value("${startgg.api-token}") String apiToken
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Envoie une requête GraphQL brute à start.gg.
     *
     * @param query     la query/mutation GraphQL (en String)
     * @param variables les variables associées à la query
     * @return le corps JSON de la réponse, sous forme de Map
     */
    public Mono<Map<String, Object>> query(String query, Map<String, Object> variables) {
        Map<String, Object> body = Map.of(
                "query", query,
                "variables", variables
        );

        return webClient.post()
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(m -> (Map<String, Object>) m);
    }
}
