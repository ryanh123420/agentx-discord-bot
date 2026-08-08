package com.ryanh.agent_discord_bot.utility;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Formats WoWUtils droptimizer responses for display in Discord embeds.
 * Returns plain strings only, no JDA types, so the listener stays in charge of
 * building embeds and these methods stay testable without mocking.
 */
public class WishlistFormatter {

    //Discord rejects any embed field value longer than this.
    private static final int MAX_FIELD_LENGTH = 1024;

    private WishlistFormatter() {
    }

    /**
     * Turns a WoWUtils character slug into display form:
     * "thrall-tarren-mill" -> "Thrall-TarrenMill".
     * The first segment is the character, everything after it is the realm.
     * @param characterId Slug from the response
     * @return Display name, or "" if the slug is missing
     */
    public static String formatCharacterName(String characterId) {
        if(characterId == null || characterId.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        String[] tokens = characterId.split("-");

        builder.append(capitalize(tokens[0]));
        if(tokens.length > 1) {
            builder.append("-");

            for(int i = 1; i < tokens.length; i++) {
                builder.append(capitalize(tokens[i]));
            }
        }

        return builder.toString();
    }

    /**
     * Renders the import time as Discord's relative timestamp markdown, so every viewer
     * sees it in their own timezone.
     * @param importedAt ISO-8601 timestamp from the WoWUtils response
     * @return "<t:epochSeconds:R>", or the raw input if it can't be parsed
     */
    public static String formatImportedAt(String importedAt) {
        if (importedAt == null || importedAt.isEmpty()) {
            return "";
        }

        try {
            return "<t:" + Instant.parse(importedAt).getEpochSecond() + ":R>";
        } catch (DateTimeParseException e) {
            return importedAt;
        }
    }
    /**
     * @param warnings Warnings from the response, possibly empty
     * @return Bulleted warnings within Discord's field limit, or "" if there are none
     */
    public static String formatWarnings(List<String> warnings) {
        if(warnings == null || warnings.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();

        for(String warning: warnings) {
            builder.append("- ").append(warning).append("\n");
        }
        String result = builder.toString();

        return truncate(result);
    }

    private static String truncate(String block) {
        return block.length() <= MAX_FIELD_LENGTH
                ? block
                : block.substring(0, MAX_FIELD_LENGTH - 1) + "…";
    }

    private static String capitalize(String word) {
       if(word == null || word.isEmpty()) {
           return "";
       }

        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }
}
