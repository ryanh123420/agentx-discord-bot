package com.ryanh.agent_discord_bot.exception;

import com.ryanh.agent_discord_bot.exception.WowUtilsException.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ErrorCodes#fromWireValue} is pure, so it is tested directly rather than
 * through a mocked HTTP round trip for each of the nine codes.
 */
class WowUtilsExceptionTest {

    @ParameterizedTest
    @CsvSource({
            "invalid_key,      INVALID_KEY",
            "group_not_shared, GROUP_NOT_SHARED",
            "not_found,        NOT_FOUND",
            "invalid_request,  INVALID_REQUEST",
            "payload_too_large,PAYLOAD_TOO_LARGE",
            "rate_limited,     RATE_LIMITED",
            "ip_throttled,     IP_THROTTLED",
            "api_overloaded,   API_OVERLOADED",
            "internal,         INTERNAL",
            "unknown,          UNKNOWN"
    })
    void givenDocumentedWireValue_whenFromWireValue_thenReturnMatchingCode(String wireValue, ErrorCodes expected) {
        assertEquals(expected, ErrorCodes.fromWireValue(wireValue));
    }

    @ParameterizedTest
    @NullSource
    @CsvSource({"teapot", "INVALID_KEY", "invalid key"})
    void givenUnmappableWireValue_whenFromWireValue_thenReturnUnknown(String wireValue) {
        // Covers null, a code added server-side after this client shipped, and near-misses
        // in casing or separators. All must degrade to UNKNOWN rather than throwing.
        assertEquals(ErrorCodes.UNKNOWN, ErrorCodes.fromWireValue(wireValue));
    }

    @Test
    void givenCodeOnlyConstructor_whenBuildingException_thenMessageIsCodeName() {
        WowUtilsException thrown = new WowUtilsException(ErrorCodes.INTERNAL);

        assertEquals(ErrorCodes.INTERNAL, thrown.getErrorCodes());
        assertEquals("INTERNAL", thrown.getMessage());
    }

    @Test
    void givenNoRetryHint_whenBuildingException_thenRetryAfterIsUnknown() {
        WowUtilsException thrown = new WowUtilsException(ErrorCodes.RATE_LIMITED, "no headers");

        assertEquals(WowUtilsException.UNKNOWN_RETRY_AFTER, thrown.getRetryAfterSeconds());
        assertEquals(WowUtilsException.UNKNOWN_RETRY_AFTER, thrown.getRateLimitReset());
        assertFalse(thrown.hasRetryAfter());
    }

    @Test
    void givenZeroRetryAfter_whenHasRetryAfter_thenReturnFalse() {
        // A wait of zero seconds is not worth showing a user, so it reads as "no hint".
        WowUtilsException thrown = new WowUtilsException(ErrorCodes.RATE_LIMITED, "zero", 0, 0);

        assertEquals(0, thrown.getRetryAfterSeconds());
        assertFalse(thrown.hasRetryAfter());
    }

    @Test
    void givenPositiveRetryAfter_whenHasRetryAfter_thenReturnTrue() {
        WowUtilsException thrown = new WowUtilsException(ErrorCodes.RATE_LIMITED, "wait", 30, 1747400000);

        assertEquals(30, thrown.getRetryAfterSeconds());
        assertEquals(1747400000, thrown.getRateLimitReset());
        assertTrue(thrown.hasRetryAfter());
    }
}
