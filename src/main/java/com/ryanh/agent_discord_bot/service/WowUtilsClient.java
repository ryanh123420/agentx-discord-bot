package com.ryanh.agent_discord_bot.service;

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


    private final String apiKey;
    private final String groupId;

    public WowUtilsClient(@Value("${WOW_UTILS_API_KEY}") String apiKey,
                          @Value("${WOW_UTILS_GUILD_ID}") String groupId) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.wowutils.com/v1")
                .build();
        this.apiKey = apiKey;
        this.groupId = groupId;
    }

    public WowUtilsRoster getRoster() {
        return restClient.get()
                .uri("/groups/{groupId}/roster", groupId)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .body(WowUtilsRoster.class);
    }

}
