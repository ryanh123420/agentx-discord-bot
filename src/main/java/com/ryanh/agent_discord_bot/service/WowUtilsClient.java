package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.exception.WowUtilsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/**
 * Wrapper class for WoWUtils API calls
 */
@Service
public class WowUtilsClient {
    private final RestClient restClient;
    private volatile String groupId;

    public record RootResponse(String groupId) {}
    public record DroptimizerRequest(String url) {}
    public record DroptimizerResponse(
            String characterId,
            String profileKey,
            String source,
            String importedAt,
            String reportUrl,
            List<String> warnings
    ) {}

    public WowUtilsClient(@Value("${WOW_UTILS_API_KEY}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.wowutils.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    private String getGroupId() {
        if (groupId == null) {
            synchronized (this) {
                if (groupId == null) {
                    groupId = restClient.get().uri("").retrieve()
                            .body(RootResponse.class).groupId();
                }
            }
        }
        return groupId;
    }

    public DroptimizerResponse postDroptimizer(String url) {
        try {
            return restClient.post()
                    .uri("/groups/{groupId}/droptimizers", getGroupId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DroptimizerRequest(url))
                    .retrieve()
                    .body(DroptimizerResponse.class);
        } catch (RestClientResponseException e) {
            throw new WowUtilsException("Failed to upload report: " + e.getMessage());
        } catch (RestClientException e) {
            throw new WowUtilsException("Could not connect to WoWUtils: " + e.getMessage());
        }
    }
}
