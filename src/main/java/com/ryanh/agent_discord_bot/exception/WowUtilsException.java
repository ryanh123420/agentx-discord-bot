package com.ryanh.agent_discord_bot.exception;

/**
 * Exception for WoWUtils API errors
 */
public class WowUtilsException extends RuntimeException {
    public WowUtilsException(String message) {
        super(message);
    }
}
