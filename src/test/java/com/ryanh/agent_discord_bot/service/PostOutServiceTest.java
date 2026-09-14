package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.config.GuildConfig;
import com.ryanh.agent_discord_bot.entity.PostOut;
import com.ryanh.agent_discord_bot.repository.PostOutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.*;
import java.util.List;

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
        //Real RaidCalendar, not a mock: it's a pure function of config + clock, so the
        //view tests stay meaningful instead of asserting against a stubbed date.
        RaidCalendar raidCalendar = new RaidCalendar(guildConfig, fixedClock);
        postOutService = new PostOutService(postOutRepository, notificationService, guildConfig,
                raidCalendar, fixedClock);
    }

    @Test
    void givenNewDate_whenInsertPostOut_thenReturnAdded() {
        String discordId = "123";
        LocalDate date = LocalDate.of(2026,7,14);

        //Not in the DB, so it get added
        when(postOutRepository.existsByDiscordIdAndPostDate(discordId, date))
                .thenReturn(false);

        PostOutService.InsertResult result = postOutService.insertPostOut(discordId, List.of(date));

        assertFalse(result.added().isEmpty());
        assertTrue(result.duplicates().isEmpty());
        verify(postOutRepository).save(any());
        //Creation notifications are the listener's job now, so the service must not send one.
        verifyNoInteractions(notificationService);
    }

    @Test
    void givenDuplicateDate_whenInsertPostOut_thenReturnDuplicate() {
        String discordId = "123";
        LocalDate date = LocalDate.of(2026,7,14);

        //Already in the DB, so we add to the dupe list
        when(postOutRepository.existsByDiscordIdAndPostDate(discordId, date))
                .thenReturn(true);

        PostOutService.InsertResult result = postOutService.insertPostOut(discordId, List.of(date));

        assertTrue(result.added().isEmpty());
        assertFalse(result.duplicates().isEmpty());
        verify(postOutRepository, never()).save(any());
    }

    @Test
    void givenEmptyDateList_whenInsertPostOut_thenReturnNothingAdded() {
        String discordId = "123";

        PostOutService.InsertResult result = postOutService.insertPostOut(discordId, List.of());

        assertTrue(result.added().isEmpty());
        assertTrue(result.duplicates().isEmpty());

        verify(postOutRepository, never()).save(any());
    }

    @Test
    void givenDatesToDelete_whenDeletePostOut_returnPostOutsDeleted() {
        String discordId = "123";
        PostOut postOut = new PostOut(discordId, LocalDate.of(2026, 7, 14),
                LocalDateTime.of(2026, 7, 14, 0, 0, 0));

        when(postOutRepository.findAllByIdInAndDiscordId(List.of(1), discordId))
                .thenReturn(List.of(postOut));

        PostOutService.DeleteResult result = postOutService.deletePostOut(discordId, List.of(1));

        assertFalse(result.deleted().isEmpty());
        verify(postOutRepository).deleteAll(List.of(postOut));
    }

    @Test
    void givenEmptyDateList_whenDeletePostOut_returnNothingDeleted() {
        String discordId = "123";

        when(postOutRepository.findAllByIdInAndDiscordId(List.of(), discordId))
                .thenReturn(List.of());

        PostOutService.DeleteResult result = postOutService.deletePostOut(discordId, List.of());

        assertTrue(result.deleted().isEmpty());
        verify(postOutRepository).deleteAll(List.of());
    }

    @Test
    void givenWrongUser_whenDeletePostOut_returnNothingDeleted() {
        //Ownership is enforced by the query, so the post out of another user never
        //comes back. Assert the caller's own id is the one scoping the lookup.
        when(postOutRepository.findAllByIdInAndDiscordId(List.of(1), "123"))
                .thenReturn(List.of());

        PostOutService.DeleteResult result = postOutService.deletePostOut("123", List.of(1));

        assertTrue(result.deleted().isEmpty());
        verify(postOutRepository).findAllByIdInAndDiscordId(List.of(1), "123");
        verify(postOutRepository).deleteAll(List.of());
    }

    @Test
    void givenThisWeekPostOut_whenViewPostOuts_returnPostOuts() {
        String discordId = "123";
        PostOut thisWeekPostOut = new PostOut(discordId, LocalDate.of(2026, 7, 14),
                LocalDateTime.of(2026, 7, 14,0,0,0));

        when(postOutRepository.findAllByDiscordId(discordId)).thenReturn(List.of(thisWeekPostOut));

        PostOutService.PostOutsByWeek result = postOutService.viewPostOuts(discordId);
        assertFalse(result.thisWeek().isEmpty());
        assertTrue(result.futureWeek().isEmpty());
    }

    @Test
    void givenFutureWeekPostOuts_whenViewPostOuts_returnPostOuts() {
        String discordId = "123";
        PostOut futureWeekPostOUt = new PostOut(discordId, LocalDate.of(2026, 7, 28),
                LocalDateTime.of(2026, 7, 28,0,0,0));

        when(postOutRepository.findAllByDiscordId(discordId)).thenReturn(List.of(futureWeekPostOUt));

        PostOutService.PostOutsByWeek result = postOutService.viewPostOuts(discordId);
        assertTrue(result.thisWeek().isEmpty());
        assertFalse(result.futureWeek().isEmpty());
    }

    @Test
    void givenNoPostOuts_whenViewPostOuts_returnNothing() {
        String discordId = "123";

        when(postOutRepository.findAllByDiscordId(discordId)).thenReturn(List.of());

        PostOutService.PostOutsByWeek result = postOutService.viewPostOuts(discordId);
        assertTrue(result.thisWeek().isEmpty());
        assertTrue(result.futureWeek().isEmpty());
    }

    @Test
    void givenThisAndFuturePostOut_whenViewPostOuts_returnPostOuts() {
        PostOut thisWeekPostOut = new PostOut("123", LocalDate.of(2026, 7, 14),
                LocalDateTime.of(2026, 7, 14,0,0,0));
        PostOut futureWeekPostOut = new PostOut("123", LocalDate.of(2026, 7, 28),
                LocalDateTime.of(2026, 7, 28,0,0,0));

        when(postOutRepository.findAllByDiscordId("123"))
                .thenReturn(List.of(thisWeekPostOut, futureWeekPostOut));

        PostOutService.PostOutsByWeek result = postOutService.viewPostOuts("123");

        assertEquals(1, result.thisWeek().size());
        assertEquals(1, result.futureWeek().size());
    }

    @Test
    void givenNoPostOuts_whenViewPostOuts_returnsEmptyLists() {
        when(postOutRepository.findAllByDiscordId("123"))
                .thenReturn(List.of());

        PostOutService.PostOutsByWeek result = postOutService.viewPostOuts("123");

        assertTrue(result.thisWeek().isEmpty());
        assertTrue(result.futureWeek().isEmpty());
    }
}
