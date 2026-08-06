package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.model.RootResponse;
import com.ryanh.agent_discord_bot.model.WowUtilsRoster;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Wrapper class for WoWUtils API calls
 */
@Service
public class WowUtilsClient {

    private final RestClient restClient;
    private volatile String groupId;

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

    public WowUtilsRoster getRoster() {
        return restClient.get()
                .uri("/groups/{groupId}/roster", getGroupId())
                .retrieve()
                .body(WowUtilsRoster.class);
    }
}
