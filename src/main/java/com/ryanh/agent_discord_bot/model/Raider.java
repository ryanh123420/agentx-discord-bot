package com.ryanh.agent_discord_bot.model;

public record Raider(
        Integer id,
        String discordName,
        String charName,
        Boolean isMain,
        GuildRank rank,
        Role role
) {

}
