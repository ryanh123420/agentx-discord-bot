package com.ryanh.agent_discord_bot.model;

import java.util.List;

/**
 * Response from the WoWUtils droptimizer import endpoint.
 * {@code profileKey} is nullable, and {@code warnings} are advisory only —
 * a sim that fails the group's wishlist validation still imports.
 */
public record DroptimizerResponse(
        String characterId,
        String profileKey,
        String source,
        String importedAt,
        String reportUrl,
        List<String> warnings
) {}
