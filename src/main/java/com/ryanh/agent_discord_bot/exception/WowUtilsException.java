package com.ryanh.agent_discord_bot.exception;

/**
 * Exception for WoWUtils API errors.
 */
public class WowUtilsException extends RuntimeException {

    /** {@link #getRetryAfterSeconds()} when the response carried no usable retry hint. */
    public static final int UNKNOWN_RETRY_AFTER = -1;

    public enum ErrorCodes{
        INVALID_KEY("invalid_key"),
        GROUP_NOT_SHARED("group_not_shared"),
        NOT_FOUND("not_found"),
        INVALID_REQUEST("invalid_request"),
        PAYLOAD_TOO_LARGE("payload_too_large"),
        RATE_LIMITED("rate_limited"),
        IP_THROTTLED("ip_throttled"),
        API_OVERLOADED("api_overloaded"),
        INTERNAL("internal"),
        UNKNOWN("unknown"),
        // Never sent by the API - used when the request failed before we got any response at all.
        CONNECTION_FAILED("");

        private final String wireValue;

        ErrorCodes(String wireValue) {
            this.wireValue = wireValue;
        }

        public static ErrorCodes fromWireValue(String wireValue) {
            if (wireValue == null) {
                return UNKNOWN;
            }

            for (ErrorCodes code : values()) {
                if (code.wireValue.equals(wireValue)) {
                    return code;
                }
            }

            return UNKNOWN;
        }
    }

    private final ErrorCodes errorCodes;
    private final int retryAfterSeconds;
    private final int rateLimitReset;

    public WowUtilsException(ErrorCodes errorCodes) {
        this(errorCodes, errorCodes.name());
    }

    public WowUtilsException(ErrorCodes errorCodes, String message) {
        super(message);
        this.errorCodes = errorCodes;
        this.retryAfterSeconds = UNKNOWN_RETRY_AFTER;
        this.rateLimitReset = UNKNOWN_RETRY_AFTER;
    }

    /**
     * Used for failures that never reached the API, so the underlying network exception is kept
     * as the cause rather than being flattened into the message.
     */
    public WowUtilsException(ErrorCodes errorCodes, String message, Throwable cause) {
        super(message, cause);
        this.errorCodes = errorCodes;
        this.retryAfterSeconds = UNKNOWN_RETRY_AFTER;
        this.rateLimitReset = UNKNOWN_RETRY_AFTER;
    }

    /**
     * @param retryAfterSeconds How long to wait before retrying, or {@link #UNKNOWN_RETRY_AFTER}
     *                          when the response gave no usable hint.
     */
    public WowUtilsException(ErrorCodes errorCodes, String message, int retryAfterSeconds, int rateLimitReset) {
        super(message);
        this.errorCodes = errorCodes;
        this.retryAfterSeconds = retryAfterSeconds;
        this.rateLimitReset = rateLimitReset;
    }

    public ErrorCodes getErrorCodes() {
        return errorCodes;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public int getRateLimitReset() {
        return rateLimitReset;
    }

    /** True when {@link #getRetryAfterSeconds()} holds a real wait time worth showing a user. */
    public boolean hasRetryAfter() {
        return retryAfterSeconds > 0;
    }
}
