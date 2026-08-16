package com.ryanh.agent_discord_bot.client;

import com.ryanh.agent_discord_bot.exception.WowUtilsException;
import com.ryanh.agent_discord_bot.exception.WowUtilsException.ErrorCodes;
import com.ryanh.agent_discord_bot.model.DroptimizerResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class WowUtilsClientTest {

    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    WowUtilsClient client = new WowUtilsClient("test-key", builder);

    static final String REPORT_URL = "https://www.raidbots.com/simbot/report/aBcDeF123456";

    private void expectGroupIdLookup() {
        String rootBody = """
                {
                    "group": {
                        "groupId": "g1"
                    }
                }
                """;

        server.expect(requestTo("https://api.wowutils.com/v1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(rootBody, MediaType.APPLICATION_JSON));
    }

    @Test
    void givenSimWithoutWarnings_whenPostDroptimizer_thenReturnSuccessWithNoWarnings() {
        expectGroupIdLookup();

        String postBody = """
                {
                  "characterId": "thrall-tarren-mill",
                  "profileKey": null,
                  "source": "raidbots",
                  "importedAt": "2026-05-16T12:00:00.000Z",
                  "reportUrl": "https://www.raidbots.com/simbot/report/aBcDeF123456",
                  "warnings": []
                }
                """;

        server.expect(requestTo("https://api.wowutils.com/v1/groups/g1/droptimizers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.url").value(REPORT_URL))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(postBody, MediaType.APPLICATION_JSON));

        DroptimizerResponse response =
                client.postDroptimizer(REPORT_URL);

        assertEquals("thrall-tarren-mill", response.characterId());
        assertEquals("2026-05-16T12:00:00.000Z", response.importedAt());
        assertEquals(REPORT_URL, response.reportUrl());
        assertTrue(response.warnings().isEmpty());

        server.verify();
    }

    @Test
    void givenSimWithWarnings_whenPostDroptimizer_thenReturnSuccessWithWarnings() {
        expectGroupIdLookup();

        String postBody = """
                {
                  "characterId": "thrall-tarren-mill",
                  "profileKey": null,
                  "source": "raidbots",
                  "importedAt": "2026-05-16T12:00:00.000Z",
                  "reportUrl": "https://www.raidbots.com/simbot/report/aBcDeF123456",
                  "warnings": [
                    "Sim used 2 targets; group wishlist expects single-target"
                  ]
                }
                """;

        server.expect(requestTo("https://api.wowutils.com/v1/groups/g1/droptimizers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.url").value(REPORT_URL))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(postBody, MediaType.APPLICATION_JSON));

        DroptimizerResponse response =
                client.postDroptimizer(REPORT_URL);

        assertEquals("thrall-tarren-mill", response.characterId());
        assertEquals("2026-05-16T12:00:00.000Z", response.importedAt());
        assertEquals(REPORT_URL, response.reportUrl());
        assertEquals(List.of("Sim used 2 targets; group wishlist expects single-target"), response.warnings());

        server.verify();
    }


    @Test
    void givenBadApiKey_whenPostDroptimizer_thenThrowInvalidKey() {
        expectGroupIdLookup();

        String errorBody = """
                {
                  "error": {
                    "code": "invalid_key",
                    "message": "The provided API key is not valid.",
                    "requestId": "req-123"
                  }
                }
                """;

        expectDroptimizerPost(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody));

        WowUtilsException thrown = assertThrows(WowUtilsException.class,
                () -> client.postDroptimizer(REPORT_URL));

        assertEquals(ErrorCodes.INVALID_KEY, thrown.getErrorCodes());
        // The detail string is diagnostic only, but the requestId is what support would ask for.
        assertTrue(thrown.getMessage().contains("The provided API key is not valid."));
        assertTrue(thrown.getMessage().contains("req-123"));
        assertFalse(thrown.hasRetryAfter());

        server.verify();
    }

    @Test
    void givenUnrecognizedErrorCode_whenPostDroptimizer_thenThrowUnknown() {
        expectGroupIdLookup();

        // A code the API adds after this client was written. It must degrade to UNKNOWN
        // rather than blowing up, so new server-side codes can't break uploads.
        String errorBody = """
                {
                  "error": {
                    "code": "teapot",
                    "message": "I'm a teapot.",
                    "requestId": "req-456"
                  }
                }
                """;

        expectDroptimizerPost(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody));

        WowUtilsException thrown = assertThrows(WowUtilsException.class,
                () -> client.postDroptimizer(REPORT_URL));

        assertEquals(ErrorCodes.UNKNOWN, thrown.getErrorCodes());

        server.verify();
    }

    @Test
    void givenNonJsonErrorBody_whenPostDroptimizer_thenThrowUnknown() {
        expectGroupIdLookup();

        // What a proxy or load balancer returns when it fails before reaching WoWUtils.
        expectDroptimizerPost(withStatus(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.TEXT_HTML)
                .body("<html><body>502 Bad Gateway</body></html>"));

        WowUtilsException thrown = assertThrows(WowUtilsException.class,
                () -> client.postDroptimizer(REPORT_URL));

        assertEquals(ErrorCodes.UNKNOWN, thrown.getErrorCodes());
        assertTrue(thrown.getMessage().contains("no readable error body"));

        server.verify();
    }

    @Test
    void givenEmptyErrorBody_whenPostDroptimizer_thenThrowUnknown() {
        expectGroupIdLookup();

        expectDroptimizerPost(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(""));

        WowUtilsException thrown = assertThrows(WowUtilsException.class,
                () -> client.postDroptimizer(REPORT_URL));

        assertEquals(ErrorCodes.UNKNOWN, thrown.getErrorCodes());
        assertTrue(thrown.getMessage().contains("no readable error body"));

        server.verify();
    }

    @Test
    void givenNullErrorObject_whenPostDroptimizer_thenThrowUnknown() {
        expectGroupIdLookup();

        // Well-formed JSON, but nothing inside the envelope to map.
        expectDroptimizerPost(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\": null}"));

        WowUtilsException thrown = assertThrows(WowUtilsException.class,
                () -> client.postDroptimizer(REPORT_URL));

        assertEquals(ErrorCodes.UNKNOWN, thrown.getErrorCodes());

        server.verify();
    }

    @Test
    void givenRateLimitWithHeaders_whenPostDroptimizer_thenThrowRateLimitedWithWaitTimes() {
        expectGroupIdLookup();

        String errorBody = """
                {
                  "error": {
                    "code": "rate_limited",
                    "message": "Too many requests.",
                    "requestId": "req-789"
                  }
                }
                """;

        expectDroptimizerPost(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Retry-After", "30")
                .header("x-ratelimit-reset", "1747400000")
                .body(errorBody));

        WowUtilsException thrown = assertThrows(WowUtilsException.class,
                () -> client.postDroptimizer(REPORT_URL));

        assertEquals(ErrorCodes.RATE_LIMITED, thrown.getErrorCodes());
        assertEquals(30, thrown.getRetryAfterSeconds());
        assertEquals(1747400000, thrown.getRateLimitReset());
        assertTrue(thrown.hasRetryAfter());

        server.verify();
    }

    @Test
    void givenRateLimitWithoutHeaders_whenPostDroptimizer_thenThrowRateLimitedWithUnknownWaitTimes() {
        expectGroupIdLookup();

        String errorBody = """
                {
                  "error": {
                    "code": "rate_limited",
                    "message": "Too many requests.",
                    "requestId": "req-789"
                  }
                }
                """;

        expectDroptimizerPost(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody));

        WowUtilsException thrown = assertThrows(WowUtilsException.class,
                () -> client.postDroptimizer(REPORT_URL));

        assertEquals(ErrorCodes.RATE_LIMITED, thrown.getErrorCodes());
        assertEquals(WowUtilsException.UNKNOWN_RETRY_AFTER, thrown.getRetryAfterSeconds());
        assertEquals(WowUtilsException.UNKNOWN_RETRY_AFTER, thrown.getRateLimitReset());
        // Drives whether the user is shown a wait time at all.
        assertFalse(thrown.hasRetryAfter());

        server.verify();
    }

    @Test
    void givenRateLimitWithHttpDateRetryAfter_whenPostDroptimizer_thenThrowRateLimitedWithUnknownWaitTimes() {
        expectGroupIdLookup();

        String errorBody = """
                {
                  "error": {
                    "code": "rate_limited",
                    "message": "Too many requests.",
                    "requestId": "req-789"
                  }
                }
                """;

        // Retry-After also permits an HTTP date, which parseTimestamp deliberately does not parse.
        expectDroptimizerPost(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Retry-After", "Wed, 21 Oct 2026 07:28:00 GMT")
                .body(errorBody));

        WowUtilsException thrown = assertThrows(WowUtilsException.class,
                () -> client.postDroptimizer(REPORT_URL));

        assertEquals(ErrorCodes.RATE_LIMITED, thrown.getErrorCodes());
        assertEquals(WowUtilsException.UNKNOWN_RETRY_AFTER, thrown.getRetryAfterSeconds());
        assertFalse(thrown.hasRetryAfter());

        server.verify();
    }

    @Test
    void givenNetworkFailure_whenPostDroptimizer_thenThrowConnectionFailed() {
        expectGroupIdLookup();

        // No response at all, so there is no error code to map.
        expectDroptimizerPost(withException(new IOException("connection reset")));

        WowUtilsException thrown = assertThrows(WowUtilsException.class,
                () -> client.postDroptimizer(REPORT_URL));

        assertEquals(ErrorCodes.CONNECTION_FAILED, thrown.getErrorCodes());
        // The cause is kept rather than flattened, so the stack trace still shows the IOException.
        assertNotNull(thrown.getCause());

        server.verify();
    }

    @Test
    void givenTwoUploads_whenPostDroptimizer_thenGroupIdIsFetchedOnce() {
        String rootBody = """
                {
                    "group": {
                        "groupId": "g1"
                    }
                }
                """;

        // The point of the test: the root lookup is cached, so a second upload must not repeat it.
        server.expect(ExpectedCount.once(), requestTo("https://api.wowutils.com/v1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(rootBody, MediaType.APPLICATION_JSON));

        String postBody = """
                {
                  "characterId": "thrall-tarren-mill",
                  "profileKey": null,
                  "source": "raidbots",
                  "importedAt": "2026-05-16T12:00:00.000Z",
                  "reportUrl": "https://www.raidbots.com/simbot/report/aBcDeF123456",
                  "warnings": []
                }
                """;

        server.expect(ExpectedCount.twice(), requestTo("https://api.wowutils.com/v1/groups/g1/droptimizers"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(postBody, MediaType.APPLICATION_JSON));

        client.postDroptimizer(REPORT_URL);
        client.postDroptimizer(REPORT_URL);

        server.verify();
    }

    /**
     * Registers the droptimizer POST expectation with the matchers every test shares,
     * leaving each test to supply only the response it cares about.
     */
    private void expectDroptimizerPost(ResponseCreator responseCreator) {
        server.expect(requestTo("https://api.wowutils.com/v1/groups/g1/droptimizers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.url").value(REPORT_URL))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(responseCreator);
    }
}