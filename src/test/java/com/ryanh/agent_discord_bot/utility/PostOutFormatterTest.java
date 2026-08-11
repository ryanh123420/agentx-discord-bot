package com.ryanh.agent_discord_bot.utility;

import com.ryanh.agent_discord_bot.entity.PostOut;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PostOutFormatterTest {

    private static final int MAX_FIELD_LENGTH = 1024;

    private List<PostOut> postOutsForUsers(int userCount, int datesPerUser) {
        List<PostOut> postOuts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.of(2026, 7, 14, 12, 0);

        for (int user = 0; user < userCount; user++) {
            for (int day = 0; day < datesPerUser; day++) {
                postOuts.add(new PostOut(
                        //Real Discord ids are 18-19 digits, which drives the length of each block.
                        String.valueOf(100000000000000000L + user),
                        LocalDate.of(2026, 7, 14).plusDays(day),
                        now));
            }
        }
        return postOuts;
    }

    @Test
    void givenNoPostOuts_whenFormatPostOutReport_thenReturnSingleFriendlyField() {
        List<String> fields = PostOutFormatter.formatPostOutReport(List.of());

        assertEquals(1, fields.size());
        assertEquals("⭐ There are no post outs! ⭐", fields.getFirst());
    }

    @Test
    void givenSmallGuild_whenFormatPostOutReport_thenReturnOneField() {
        List<String> fields = PostOutFormatter.formatPostOutReport(postOutsForUsers(3, 3));

        assertEquals(1, fields.size());
        assertTrue(fields.getFirst().contains("<@100000000000000000>"));
    }

    @Test
    void givenLargeGuild_whenFormatPostOutReport_thenEveryFieldFitsDiscordsLimit() {
        List<String> fields = PostOutFormatter.formatPostOutReport(postOutsForUsers(25, 3));

        assertTrue(fields.size() > 1, "25 raiders should not fit in a single field");
        for (String field : fields) {
            assertFalse(field.isEmpty(), "Discord rejects blank field values");
            assertTrue(field.length() <= MAX_FIELD_LENGTH,
                    "Field was " + field.length() + " characters, limit is " + MAX_FIELD_LENGTH);
        }
    }

    @Test
    void givenMoreUsersThanFieldsAllow_whenFormatPostOutReport_thenLastFieldNotesTheOverflow() {
        List<String> fields = PostOutFormatter.formatPostOutReport(postOutsForUsers(60, 3));

        assertEquals(2, fields.size());
        assertTrue(fields.getLast().contains("more"),
                "Expected an overflow note, got: " + fields.getLast());
        assertTrue(fields.getLast().length() <= MAX_FIELD_LENGTH);
    }

    @Test
    void givenOneUserWithManyDates_whenFormatPostOutReport_thenBlockIsTruncatedToFit() {
        List<String> fields = PostOutFormatter.formatPostOutReport(postOutsForUsers(1, 200));

        for (String field : fields) {
            assertTrue(field.length() <= MAX_FIELD_LENGTH,
                    "Field was " + field.length() + " characters, limit is " + MAX_FIELD_LENGTH);
        }
    }
}
