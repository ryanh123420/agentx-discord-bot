package com.ryanh.agent_discord_bot.utility;

import com.ryanh.agent_discord_bot.exception.WowUtilsException;
import com.ryanh.agent_discord_bot.exception.WowUtilsException.ErrorCodes;

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

        builder.append("👤 ").append(capitalize(tokens[0]));
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
            return "⌚ <t:" + Instant.parse(importedAt).getEpochSecond() + ":F>";
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

    /**
     * Maps a WoWUtils error code to copy the person who ran the command can act on, without a
     * retry hint. Equivalent to passing {@link WowUtilsException#UNKNOWN_RETRY_AFTER}.
     * @param errorCode Code from the failed call
     * @return Message to show in an error embed
     */
    public static String formatError(ErrorCodes errorCode) {
        return formatError(errorCode, WowUtilsException.UNKNOWN_RETRY_AFTER, WowUtilsException.UNKNOWN_RETRY_AFTER);
    }

    /**
     * Maps a WoWUtils error code to copy the person who ran the command can act on.
     * Deliberately has no default branch: adding a constant to ErrorCodes should break
     * this compile until someone writes copy for it.
     *
     * <p>All user-facing wording lives here, including the rate-limit wait time. The client
     * hands over the raw seconds precisely so this stays the only place that writes prose.
     *
     * @param errorCode Code from the failed call
     * @param retryAfterSeconds Seconds to wait, or {@link WowUtilsException#UNKNOWN_RETRY_AFTER}
     *                          when the response gave no usable hint. Only read for RATE_LIMITED.
     * @param rateLimitReset Timestamp for when the rate limit will lift.
     * @return Message to show in an error embed
     */
    public static String formatError(ErrorCodes errorCode, int retryAfterSeconds, int rateLimitReset) {
        return switch (errorCode) {
            case INVALID_REQUEST -> "Report could not be read, retry with a valid report link or code.";
            case NOT_FOUND -> "The character for the linked report is not on the WoWUtils roster.";
            case PAYLOAD_TOO_LARGE -> "Report is too large for WoWUtils to accept. Re-sim with "
                    + "fewer items,iterations, or profiles.";
            case RATE_LIMITED -> retryAfterSeconds > 0
                    ? "WoWUtils rate limit reached for uploads." + "\n" +
                      "The limit will reset at " + "<t:" + rateLimitReset + ":t>." + "\n"
                      + "Try again in " + formatWait(retryAfterSeconds) + "."
                    : "WoWUtils rate limit reached for uploads. Wait a minute and try again.";
            case IP_THROTTLED -> "Too many uploads right now. Wait a minute and try again.";
            case API_OVERLOADED -> "WoWUtils is busy. Try again in a few minutes.";
            case INTERNAL -> "WoWUtils hit an error on their end. Try again in a few minutes.";
            case CONNECTION_FAILED -> "Couldn't reach WoWUtils. Try again in a few minutes.";
            case INVALID_KEY, GROUP_NOT_SHARED -> "The bot isn't authorised with WoWUtils. Message Toasty "
                    + "to fix, uploading again won't help.";
            case UNKNOWN -> "The upload failed for an unknown reason. Try again, and message Toasty "
                    + "if it keeps happening.";
        };
    }

    /**
     * Renders a wait in whole seconds or whole minutes, rounding minutes up so the advice is
     * never shorter than the real wait.
     */
    private static String formatWait(int seconds) {
        if (seconds < 60) {
            return seconds == 1 ? "1 second" : seconds + " seconds";
        }

        int minutes = (seconds + 59) / 60;

        return minutes == 1 ? "1 minute" : minutes + " minutes";
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
