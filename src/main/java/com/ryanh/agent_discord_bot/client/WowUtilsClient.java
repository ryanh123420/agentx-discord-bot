package com.ryanh.agent_discord_bot.client;

import com.ryanh.agent_discord_bot.exception.WowUtilsException;
import com.ryanh.agent_discord_bot.exception.WowUtilsException.ErrorCodes;
import com.ryanh.agent_discord_bot.model.DroptimizerResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Wrapper class for WoWUtils API calls
 */
@Service
public class WowUtilsClient {

    /** Time until the rate limit is over. */
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    /** Reset time for the rate limit. */
    private static final String RESET_HEADER = "x-ratelimit-reset";

    private final RestClient restClient;
    /** WoWUtils groupId. */
    private volatile String groupId;

    public record RootResponse(Group group) {
        public record Group(String groupId) {}
    }
    public record DroptimizerRequest(String url) {}
    public record DroptimizerResponseError(Error error) {
        public record Error(
            String code,
            String message,
            String requestId
        ) {}
    }

    public WowUtilsClient(@Value("${WOW_UTILS_API_KEY}") String apiKey, RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://api.wowutils.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    private String getGroupId() {
        if (groupId == null) {
            synchronized (this) {
                if (groupId == null) {
                    groupId = restClient.get()
                            .uri("")
                            .retrieve()
                            .body(RootResponse.class)
                            .group()
                            .groupId();
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
            // Declared here, assigned in both branches below, so it's still in scope at the throw.
            ErrorCodes code;
            DroptimizerResponseError body;
            HttpHeaders headers = e.getResponseHeaders();
            try {
                body = e.getResponseBodyAs(DroptimizerResponseError.class);
            } catch (Exception parseFailure) {
                // The response had a body, but it wasn't the JSON we expect (e.g. an HTML error
                // page from a proxy). Nothing useful to read, so treat it as no body at all.
                body = null;
            }

            if (body == null || body.error() == null) {
                code = ErrorCodes.UNKNOWN;
            } else {
                code = ErrorCodes.fromWireValue(body.error().code());
            }

            // Diagnostic text only - the user-facing copy is built from the code by WishlistFormatter.
            String detail = buildDetail(e, body);

            if (code == ErrorCodes.RATE_LIMITED) {
                throw new WowUtilsException(code, detail, parseRetryAfterSeconds(headers), parseRateLimitReset(headers));
            }

            throw new WowUtilsException(code, detail);
        } catch (RestClientException e) {
            // Never reached the API, so there is no code to map. Keep the cause for the stack trace.
            throw new WowUtilsException(ErrorCodes.CONNECTION_FAILED,
                    "Could not connect to WoWUtils: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the developer-facing detail string: the API's own message plus its requestId, which
     * is what WoWUtils support would ask for. Falls back to the status line when there is no body.
     */
    private String buildDetail(RestClientResponseException e, DroptimizerResponseError body) {
        if (body == null || body.error() == null) {
            return "WoWUtils returned " + e.getStatusCode() + " with no readable error body";
        }

        StringBuilder detail = new StringBuilder("WoWUtils returned ")
                .append(e.getStatusCode())
                .append(": ")
                .append(body.error().message());

        if (body.error().requestId() != null && !body.error().requestId().isEmpty()) {
            detail.append(" (requestId: ").append(body.error().requestId()).append(")");
        }

        return detail.toString();
    }

    /**
     * Reads how long to wait before retrying, in seconds.
     *
     * @return Seconds to wait, or {@link WowUtilsException#UNKNOWN_RETRY_AFTER} if unavailable
     */
    private int parseRetryAfterSeconds(HttpHeaders headers) {
        if (headers == null) {
            return WowUtilsException.UNKNOWN_RETRY_AFTER;
        }

        return parseTimestamp(headers.getFirst(RETRY_AFTER_HEADER));
    }

    /**
     * Reads the timestamp of when the rate limit will lift.
     *
     * @return Seconds to wait, or {@link WowUtilsException#UNKNOWN_RETRY_AFTER} if unavailable
     */
    private int parseRateLimitReset(HttpHeaders headers) {
        if (headers == null) {
            return WowUtilsException.UNKNOWN_RETRY_AFTER;
        }

        return parseTimestamp(headers.getFirst(RESET_HEADER));
    }

    private int parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return WowUtilsException.UNKNOWN_RETRY_AFTER;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException notANumber) {
            // Retry-After also allows an HTTP date. Not worth parsing until we see one in the wild.
            return WowUtilsException.UNKNOWN_RETRY_AFTER;
        }
    }
}
