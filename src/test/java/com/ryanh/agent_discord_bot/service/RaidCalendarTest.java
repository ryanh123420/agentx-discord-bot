package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.config.GuildConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RaidCalendarTest {

    private final GuildConfig guildConfig = new GuildConfig();
    private RaidCalendar raidCalendar;

    @BeforeEach
    void setUp() {
        guildConfig.setRaidDays(List.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY));
        guildConfig.setTimezone("America/New_York");
        guildConfig.setResetDay(DayOfWeek.TUESDAY);
        guildConfig.setRaidStartTime(21);

        raidCalendar = calendarAt(2026, 7, 14, 15);
    }

    /**
     * Builds a RaidCalendar pinned to the given date and hour in the guild's timezone.
     */
    private RaidCalendar calendarAt(int year, int month, int day, int hour) {
        ZoneId zone = ZoneId.of(guildConfig.getTimezone());
        Clock fixedClock = Clock.fixed(
                ZonedDateTime.of(year, month, day, hour, 0, 0, 0, zone).toInstant(),
                zone
        );
        return new RaidCalendar(guildConfig, fixedClock);
    }

    @Test
    void givenTuesdayBeforeRaid_whenValidMenuOptions_returnThreeMenuOptions() {
        raidCalendar = calendarAt(2026, 7, 14, 15);

        List<RaidCalendar.RaidDay> result = raidCalendar.validMenuOptions();

        assertEquals(3, result.size());
    }

    @Test
    void givenTuesdayAfterRaid_whenValidMenuOptions_returnTwoMenuOptions() {
        raidCalendar = calendarAt(2026, 7, 14, 22);

        List<RaidCalendar.RaidDay> result = raidCalendar.validMenuOptions();

        assertEquals(2, result.size());
    }

    @Test
    void givenWednesdayBeforeRaid_whenValidMenuOptions_returnTwoMenuOptions() {
        raidCalendar = calendarAt(2026, 7, 15, 15);

        List<RaidCalendar.RaidDay> result = raidCalendar.validMenuOptions();

        assertEquals(2, result.size());
    }

    @Test
    void givenWednesdayAfterRaid_whenValidMenuOptions_returnOneMenuOptions() {
        raidCalendar = calendarAt(2026, 7, 15, 22);

        List<RaidCalendar.RaidDay> result = raidCalendar.validMenuOptions();

        assertEquals(1, result.size());
    }

    @Test
    void givenThursdayBeforeRaid_whenValidMenuOptions_returnOneMenuOptions() {
        raidCalendar = calendarAt(2026, 7, 16, 10);

        List<RaidCalendar.RaidDay> result = raidCalendar.validMenuOptions();

        assertEquals(1, result.size());
    }

    @Test
    void givenThursdayAfterRaid_whenValidMenuOptions_returnThreeMenuOptions() {
        raidCalendar = calendarAt(2026, 7, 16, 22);

        List<RaidCalendar.RaidDay> result = raidCalendar.validMenuOptions();

        assertEquals(3, result.size());
    }

    @Test
    void givenTuesdayBeforeRaid_whenGetNextRaidWeekStartDate_returnsThisTuesday() {
        raidCalendar = calendarAt(2026, 7, 14, 15);

        assertEquals(LocalDate.of(2026, 7, 14), raidCalendar.getNextRaidWeekStartDate());
    }

    @Test
    void givenThursdayAfterRaid_whenGetNextRaidWeekStartDate_returnsNextTuesday() {
        raidCalendar = calendarAt(2026, 7, 16, 22);

        assertEquals(LocalDate.of(2026, 7, 21), raidCalendar.getNextRaidWeekStartDate());
    }

    @Test
    void givenMonday_whenGetNextRaidWeekStartDate_returnsNextTuesday() {
        raidCalendar = calendarAt(2026, 7, 20, 15);

        assertEquals(LocalDate.of(2026, 7, 21), raidCalendar.getNextRaidWeekStartDate());
    }

    @Test
    void convertDatesFromModal_validInput_returnsDates() {
        List<LocalDate> result = raidCalendar.convertDatesFromModal("7/14, 7/15, 7/16");

        assertEquals(3, result.size());
    }

    @Test
    void convertDatesFromModal_invalidInput_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> raidCalendar.convertDatesFromModal("abc, xyz"));
    }

    @Test
    void convertDatesFromModal_emptyInput_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> raidCalendar.convertDatesFromModal(""));
    }

    @Test
    void givenStandardRaidWeek_whenGetLastRaidDay_returnsThursday() {
        raidCalendar = calendarAt(2026, 7, 14, 12);

        //Raid week starts Tue 7/14, so Thursday is 7/16.
        assertEquals(LocalDate.of(2026, 7, 16), raidCalendar.getLastRaidDay());
    }

    @Test
    void givenRaidDaysCrossingReset_whenGetLastRaidDay_returnsLastDayOfRaidWeek() {
        //Reset is Tuesday, raids are Sunday and Monday. Monday is the last raid day of
        //the raid week even though Sunday has the higher DayOfWeek value.
        guildConfig.setRaidDays(List.of(DayOfWeek.SUNDAY, DayOfWeek.MONDAY));
        raidCalendar = calendarAt(2026, 7, 14, 12);

        //Raid week starts Tue 7/14, so Sunday is 7/19 and Monday is 7/20.
        assertEquals(LocalDate.of(2026, 7, 20), raidCalendar.getLastRaidDay());
    }

    @Test
    void givenTuesdayBeforeRaid_whenValidMenuOptions_returnsThisWeeksDates() {
        raidCalendar = calendarAt(2026, 7, 14, 15);

        List<RaidCalendar.RaidDay> result = raidCalendar.validMenuOptions();

        assertEquals(List.of(LocalDate.of(2026, 7, 14), LocalDate.of(2026, 7, 15),
                        LocalDate.of(2026, 7, 16)),
                result.stream().map(RaidCalendar.RaidDay::date).toList());
        assertEquals(List.of("Tuesday", "Wednesday", "Thursday"),
                result.stream().map(RaidCalendar.RaidDay::label).toList());
    }

    @Test
    void givenThursdayAfterRaid_whenValidMenuOptions_returnsNextWeeksDates() {
        //The raid week has rolled over, so the menu offers next week in full.
        raidCalendar = calendarAt(2026, 7, 16, 22);

        List<LocalDate> dates = raidCalendar.validMenuOptions().stream()
                .map(RaidCalendar.RaidDay::date)
                .toList();

        assertEquals(List.of(LocalDate.of(2026, 7, 21), LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 23)), dates);
    }

    @Test
    void givenTuesday_whenGetNextWeekRaidDays_returnsFollowingWeeksDates() {
        raidCalendar = calendarAt(2026, 7, 14, 15);

        List<LocalDate> dates = raidCalendar.getNextWeekRaidDays().stream()
                .map(RaidCalendar.RaidDay::date)
                .toList();

        assertEquals(List.of(LocalDate.of(2026, 7, 21), LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 23)), dates);
    }

    @Test
    void givenMenuOptionValues_whenConvertDatesFromSelectMenu_roundTripsExactly() {
        raidCalendar = calendarAt(2026, 7, 14, 15);
        List<RaidCalendar.RaidDay> options = raidCalendar.validMenuOptions();

        //The listener uses date.toString() as the option value, so feed that back in.
        List<String> selected = options.stream().map(day -> day.date().toString()).toList();

        assertEquals(options.stream().map(RaidCalendar.RaidDay::date).toList(),
                raidCalendar.convertDatesFromSelectMenu(selected));
    }

    @Test
    void givenRaidWeekCrossingNewYear_whenValidMenuOptions_keepsTheCorrectYear() {
        //The old M/d menu value dropped the year and guessed it back. Tue 12/29/2026
        //starts a raid week whose Thursday falls in 2027.
        raidCalendar = calendarAt(2026, 12, 29, 15);

        List<LocalDate> dates = raidCalendar.validMenuOptions().stream()
                .map(RaidCalendar.RaidDay::date)
                .toList();

        assertEquals(List.of(LocalDate.of(2026, 12, 29), LocalDate.of(2026, 12, 30),
                LocalDate.of(2026, 12, 31)), dates);

        List<String> selected = dates.stream().map(LocalDate::toString).toList();
        assertEquals(dates, raidCalendar.convertDatesFromSelectMenu(selected));
    }
}
