package com.ryanh.agent_discord_bot.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WowUtilsRoster(
        List<Member> members
) {
    public record Member(
            String battletag,
            String rank,
            List<Character> characters
    ) {}

    public record Character(
            String name,
            @JsonProperty("class") String charClass,
            String spec
    ) {}
}
