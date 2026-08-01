package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.config.GuildConfig;
import com.ryanh.agent_discord_bot.entity.PostOut;
import com.ryanh.agent_discord_bot.repository.PostOutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostOutServiceTest {

    private final PostOutRepository postOutRepository = Mockito.mock(PostOutRepository.class);
    private final NotificationService notificationService = Mockito.mock(NotificationService.class);
    private final GuildConfig guildConfig = new GuildConfig();
    private PostOutService postOutService;

    @BeforeEach
    void setUp() {
        guildConfig.setRaidDays(List.of(DayOfWeek.TUESDAY,DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY));
        guildConfig.setTimezone("America/New_York");
        guildConfig.setResetDay(DayOfWeek.TUESDAY);
        guildConfig.setRaidStartTime(21);

        Clock fixedClock = Clock.fixed(
                ZonedDateTime.of(2026, 7, 14, 15, 0, 0, 0,
                        ZoneId.of(guildConfig.getTimezone())).toInstant(),
                ZoneId.of(guildConfig.getTimezone())
        );
        postOutService = new PostOutService(postOutRepository, notificationService, guildConfig, fixedClock);
    }

    @Test
    void givenNewDate_whenInsertPostOut_thenReturnAdded() {
        String discordId = "123";
        LocalDate date = LocalDate.of(2026,7,14);

        //Not in the DB, so it get added
        when(postOutRepository.existsByDiscordIdAndPostDate(discordId, date))
                .thenReturn(false);

        Map<String, List<String>> result = postOutService.insertPostOut(discordId, List.of(date), "test");

        assertFalse(result.get("added").isEmpty());
        assertTrue(result.get("duplicates").isEmpty());
        verify(notificationService)
                .sendPostOutCreation(eq(discordId), any(), eq("test"));
        verify(postOutRepository).save(any());
    }

    @Test
    void givenDuplicateDate_whenInsertPostOut_thenReturnDuplicate() {
        String discordId = "123";
        LocalDate date = LocalDate.of(2026,7,14);

        //Already in the DB, so we add to the dupe list
        when(postOutRepository.existsByDiscordIdAndPostDate(discordId, date))
                .thenReturn(true);

        Map<String, List<String>> result = postOutService.insertPostOut(discordId, List.of(date), "test");

        assertTrue(result.get("added").isEmpty());
        assertFalse(result.get("duplicates").isEmpty());
        verify(notificationService, never())
                .sendPostOutCreation(any(), any(), any());
        verify(postOutRepository, never()).save(any());
    }

    @Test
    void givenEmptyDateList_whenInsertPostOut_thenReturnNothingAdded() {
        String discordId = "123";

        Map<String, List<String>> result = postOutService.insertPostOut(discordId, List.of(), "test");

        assertTrue(result.get("added").isEmpty());
        assertTrue(result.get("duplicates").isEmpty());

        verify(postOutRepository, never()).save(any());
    }

    @Test
    void givenDatesToDelete_whenDeletePostOut_returnPostOutsDeleted() {
        String discordId = "123";
        List<String> deleteList = new ArrayList<>(List.of("1"));

        PostOut postOut = new PostOut();
        postOut.setDiscordId(discordId);
        postOut.setPostDate(LocalDate.of(2026,7,14));

        when(postOutRepository.findById(1)).thenReturn(Optional.of(postOut));

        Map<String, List<String>> result = postOutService.deletePostOut(discordId, deleteList);

        assertFalse(result.get("deleted").isEmpty());
        verify(postOutRepository).delete(postOut);
    }

    @Test
    void givenEmptyDateList_whenDeletePostOut_returnNothingDeleted() {
        String discordId = "123";

        Map<String, List<String>> result = postOutService.deletePostOut(discordId, List.of());

        assertTrue(result.get("deleted").isEmpty());
        verify(postOutRepository, never()).delete(any());
    }

    @Test
    void givenWrongUser_whenDeletePostOut_returnNothingDeleted() {
        PostOut postOut = new PostOut("456", LocalDate.of(2026, 7, 14),
                LocalDateTime.of(2026, 7, 14,0,0,0));
        when(postOutRepository.findById(1)).thenReturn(Optional.of(postOut));

        //deletePostOut takes the List<String> of database IDs.
        Map<String, List<String>> result = postOutService.deletePostOut("123", List.of("1"));

        assertTrue(result.get("deleted").isEmpty());
        verify(postOutRepository, never()).delete(any());
    }

    @Test
    void givenThisWeekPostOut_whenViewPostOuts_returnPostOuts() {
        String discordId = "123";
        PostOut thisWeekPostOut = new PostOut(discordId, LocalDate.of(2026, 7, 14),
                LocalDateTime.of(2026, 7, 14,0,0,0));

        when(postOutRepository.findAllByDiscordId(discordId)).thenReturn(List.of(thisWeekPostOut));

        Map<String, List<String>> result = postOutService.viewPostOuts(discordId);
        assertFalse(result.get("thisweek").isEmpty());
        assertTrue(result.get("futureweek").isEmpty());
    }

    @Test
    void givenFutureWeekPostOuts_whenViewPostOuts_returnPostOuts() {
        String discordId = "123";
        PostOut futureWeekPostOUt = new PostOut(discordId, LocalDate.of(2026, 7, 28),
                LocalDateTime.of(2026, 7, 28,0,0,0));

        when(postOutRepository.findAllByDiscordId(discordId)).thenReturn(List.of(futureWeekPostOUt));

        Map<String, List<String>> result = postOutService.viewPostOuts(discordId);
        assertTrue(result.get("thisweek").isEmpty());
        assertFalse(result.get("futureweek").isEmpty());
    }

    @Test
    void givenNoPostOuts_whenViewPostOuts_returnNothing() {
        String discordId = "123";

        when(postOutRepository.findAllByDiscordId(discordId)).thenReturn(List.of());

        Map<String, List<String>> result = postOutService.viewPostOuts(discordId);
        assertTrue(result.get("thisweek").isEmpty());
        assertTrue(result.get("futureweek").isEmpty());
    }

    @Test
    void givenThisAndFuturePostOut_whenViewPostOuts_returnPostOuts() {
        PostOut thisWeekPostOut = new PostOut("123", LocalDate.of(2026, 7, 14),
                LocalDateTime.of(2026, 7, 14,0,0,0));
        PostOut futureWeekPostOut = new PostOut("123", LocalDate.of(2026, 7, 28),
                LocalDateTime.of(2026, 7, 28,0,0,0));

        when(postOutRepository.findAllByDiscordId("123"))
                .thenReturn(List.of(thisWeekPostOut, futureWeekPostOut));

        Map<String, List<String>> result = postOutService.viewPostOuts("123");

        assertEquals(1, result.get("thisweek").size());
        assertEquals(1, result.get("futureweek").size());
    }

    @Test
    void givenNoPostOuts_whenViewPostOuts_returnsEmptyLists() {
        when(postOutRepository.findAllByDiscordId("123"))
                .thenReturn(List.of());

        Map<String, List<String>> result = postOutService.viewPostOuts("123");

        assertTrue(result.get("thisweek").isEmpty());
        assertTrue(result.get("futureweek").isEmpty());
    }

    @Test
    void givenTuesdayBeforeRaid_whenValidMenuOptions_returnThreeMenuOptions() {
        ZonedDateTime tuesdayAfternoon = ZonedDateTime.of(2026, 7, 14, 15, 0, 0, 0,
                ZoneId.of(guildConfig.getTimezone()));

        Clock fixedClock = Clock.fixed(tuesdayAfternoon.toInstant(), ZoneId.of(guildConfig.getTimezone()));
        postOutService = new PostOutService(postOutRepository, notificationService, guildConfig, fixedClock);

        List<PostOutService.RaidDay> result = postOutService.validMenuOptions();

        assertEquals(3, result.size());
    }

    @Test
    void givenTuesdayAfterRaid_whenValidMenuOptions_returnTwoMenuOptions() {
        ZonedDateTime tuesdayAfterRaid = ZonedDateTime.of(2026, 7, 14, 22, 0, 0, 0,
                ZoneId.of(guildConfig.getTimezone()));

        Clock fixedClock = Clock.fixed(tuesdayAfterRaid.toInstant(), ZoneId.of(guildConfig.getTimezone()));
        postOutService = new PostOutService(postOutRepository, notificationService, guildConfig, fixedClock);

        List<PostOutService.RaidDay> result = postOutService.validMenuOptions();

        assertEquals(2, result.size());
    }

    @Test
    void givenWednesdayBeforeRaid_whenValidMenuOptions_returnTwoMenuOptions() {
        ZonedDateTime wednesdayAfternoon = ZonedDateTime.of(2026, 7, 15, 15, 0, 0, 0,
                ZoneId.of(guildConfig.getTimezone()));

        Clock fixedClock = Clock.fixed(wednesdayAfternoon.toInstant(), ZoneId.of("America/New_York"));
        postOutService = new PostOutService(postOutRepository, notificationService, guildConfig, fixedClock);

        List<PostOutService.RaidDay> result = postOutService.validMenuOptions();

        assertEquals(2, result.size());
    }

    @Test
    void givenWednesdayAfterRaid_whenValidMenuOptions_returnOneMenuOptions() {
        ZonedDateTime wednesdayAfterRaid = ZonedDateTime.of(2026, 7, 15, 22, 0, 0, 0,
                ZoneId.of(guildConfig.getTimezone()));

        Clock fixedClock = Clock.fixed(wednesdayAfterRaid.toInstant(), ZoneId.of("America/New_York"));
        postOutService = new PostOutService(postOutRepository, notificationService, guildConfig, fixedClock);

        List<PostOutService.RaidDay> result = postOutService.validMenuOptions();

        assertEquals(1, result.size());
    }

    @Test
    void givenThursdayBeforeRaid_whenValidMenuOptions_returnOneMenuOptions() {
        ZonedDateTime thursdayBeforeRaid = ZonedDateTime.of(2026, 7, 16, 10, 0, 0, 0,
                ZoneId.of(guildConfig.getTimezone()));

        Clock fixedClock = Clock.fixed(thursdayBeforeRaid.toInstant(), ZoneId.of(guildConfig.getTimezone()));
        postOutService = new PostOutService(postOutRepository, notificationService, guildConfig, fixedClock);

        List<PostOutService.RaidDay> result = postOutService.validMenuOptions();

        assertEquals(1, result.size());
    }

    @Test
    void givenThursdayAfterRaid_whenValidMenuOptions_returnThreeMenuOptions() {
        ZonedDateTime thursdayAfterRaid = ZonedDateTime.of(2026, 7, 16, 22, 0, 0, 0,
                ZoneId.of(guildConfig.getTimezone()));

        Clock fixedClock = Clock.fixed(thursdayAfterRaid.toInstant(), ZoneId.of(guildConfig.getTimezone()));
        postOutService = new PostOutService(postOutRepository, notificationService, guildConfig, fixedClock);

        List<PostOutService.RaidDay> result = postOutService.validMenuOptions();

        assertEquals(3, result.size());
    }

    @Test
    void givenTuesdayBeforeRaid_whenGetNextRaidWeekStartDate_returnsThisTuesday() {
        ZonedDateTime tuesdayBeforeRaid = ZonedDateTime.of(2026, 7, 14, 15, 0, 0, 0,
                ZoneId.of(guildConfig.getTimezone()));

        Clock fixedClock = Clock.fixed(tuesdayBeforeRaid.toInstant(), ZoneId.of(guildConfig.getTimezone()));
        postOutService = new PostOutService(postOutRepository, notificationService, guildConfig, fixedClock);

        LocalDate expected = LocalDate.of(2026,7,14);
        LocalDate result = postOutService.getNextRaidWeekStartDate();

        assertEquals(expected,result);
    }

    @Test
    void givenThursdayAfterRaid_whenGetNextRaidWeekStartDate_returnsNextTuesday() {
        ZonedDateTime thursdayAfterRaid = ZonedDateTime.of(2026, 7, 16, 22, 0, 0, 0,
                ZoneId.of(guildConfig.getTimezone()));

        Clock fixedClock = Clock.fixed(thursdayAfterRaid.toInstant(), ZoneId.of(guildConfig.getTimezone()));
        postOutService = new PostOutService(postOutRepository, notificationService, guildConfig, fixedClock);

        LocalDate expected = LocalDate.of(2026,7,21);
        LocalDate result = postOutService.getNextRaidWeekStartDate();

        assertEquals(expected,result);
    }

    @Test
    void givenMonday_whenGetNextRaidWeekStartDate_returnsNextTuesday() {
        ZonedDateTime monday = ZonedDateTime.of(2026, 7, 20, 15, 0, 0, 0,
                ZoneId.of(guildConfig.getTimezone()));

        Clock fixedClock = Clock.fixed(monday.toInstant(), ZoneId.of(guildConfig.getTimezone()));
        postOutService = new PostOutService(postOutRepository, notificationService, guildConfig, fixedClock);

        LocalDate expected = LocalDate.of(2026,7,21);
        LocalDate result = postOutService.getNextRaidWeekStartDate();

        assertEquals(expected,result);
    }

    // convertDatesFromModal - valid input
    @Test
    void convertDatesFromModal_validInput_returnsDates() {
        List<LocalDate> result = postOutService.convertDatesFromModal("7/14, 7/15, 7/16");

        assertEquals(3, result.size());
    }

    // convertDatesFromModal - invalid input
    @Test
    void convertDatesFromModal_invalidInput_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> postOutService.convertDatesFromModal("abc, xyz"));
    }

    // convertDatesFromModal - empty input
    @Test
    void convertDatesFromModal_emptyInput_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> postOutService.convertDatesFromModal(""));
    }
}