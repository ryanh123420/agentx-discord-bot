package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.config.GuildConfig;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Raid week date math. Everything here is a pure function of the guild's raid schedule
 * and the current time, with no database involvement.
 */
@Component
public class RaidCalendar {

    private final GuildConfig guildConfig;
    private final Clock clock;

    public RaidCalendar(GuildConfig guildConfig, Clock clock) {
        this.guildConfig = guildConfig;
        this.clock = clock;
    }

    //Used to help build menu options in the listener
    public record RaidDay(String label, String value, LocalDate date) {}

    /**
     * Returns the start date of the next week, based off if the last raid day and the raid start time
     * has passed for the week.
     * @return Start date of the raid week.
     */
    public LocalDate getNextRaidWeekStartDate() {
        return getNextRaidWeekStartDate(ZonedDateTime.now(clock));
    }

    /**
     * Overload taking the current time, so a single operation reads the clock once and
     * can't straddle the raid start hour partway through.
     */
    private LocalDate getNextRaidWeekStartDate(ZonedDateTime now) {
        LocalDate weekStart = now.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(guildConfig.getResetDay()));

        return isRaidWeekOver(now) ? weekStart.plusWeeks(1) : weekStart;
    }

    /**
     * The raid week is over once the last raid day of the week has started.
     */
    private boolean isRaidWeekOver(ZonedDateTime now) {
        return hasStarted(lastRaidDayPosition(), now);
    }

    /**
     * Whether a raid day has already begun, counting a raid as started once its hour arrives.
     * @param position Position in the raid week, as returned by raidWeekPosition
     */
    private boolean hasStarted(int position, ZonedDateTime now) {
        int today = raidWeekPosition(now.getDayOfWeek());

        return position < today
                || (position == today && now.getHour() >= guildConfig.getRaidStartTime());
    }

    /**
     * Position of the last raid day of the week, ordered around the weekly reset.
     */
    private int lastRaidDayPosition() {
        return guildConfig.getRaidDays().stream()
                .mapToInt(this::raidWeekPosition)
                .max()
                .orElse(1);
    }

    /**
     * Maps the return value of DayOfWeek.getValue() around the weekly reset for a region rather than Monday
     * through Sunday. So for US Tuesday = 1 and Monday = 7, EU Wednesday = 1, Tuesday = 7, etc
     * @param day A day Monday through Sunday
     * @return The day's 1-based position in the raid week
     */
    private int raidWeekPosition(DayOfWeek day) {
        return (day.getValue() - guildConfig.getResetDay().getValue() + 7) % 7 + 1;
    }

    /**
     * Provides a list of valid menu options for the "This Reset" drop down by checking if it's past a raid days date
     * and start time. For example, if Tuesday's raid is over, then there's no point putting Tuesday in the menu options.
     * @return List of valid RaidDays to add as menu options
     */
    public List<RaidDay> validMenuOptions() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDate reset = getNextRaidWeekStartDate(now);
        List<RaidDay> raidDaysList = new ArrayList<>();

        for(int i = 0; i < guildConfig.getRaidDays().size(); i++) {
            DayOfWeek day = guildConfig.getRaidDays().get(i);

            if(isValidMenuOption(day, now)) {
                int offset = raidWeekPosition(day) - 1;

                raidDaysList.add(new RaidDay(day.name().charAt(0)
                        + day.name().substring(1).toLowerCase(),
                        reset.plusDays(offset).getMonthValue() + "/" + reset.plusDays(offset).getDayOfMonth(),
                        reset.plusDays(offset)));
            }
        }
        return raidDaysList;
    }

    public List<RaidDay> getNextWeekRaidDays() {
        LocalDate reset = getNextRaidWeekStartDate().plusWeeks(1);
        List<RaidDay> raidDaysList = new ArrayList<>();

        for(int i = 0; i < guildConfig.getRaidDays().size(); i++) {
            DayOfWeek day = guildConfig.getRaidDays().get(i);
            int offset = raidWeekPosition(day) - 1;

            raidDaysList.add(new RaidDay(day.name().charAt(0)
                    + day.name().substring(1).toLowerCase(),
                    reset.plusDays(offset).getMonthValue() + "/" + reset.plusDays(offset).getDayOfMonth(),
                    reset.plusDays(offset)));
        }

        return raidDaysList;
    }

    private boolean isValidMenuOption(DayOfWeek raidDay, ZonedDateTime now) {
        //Once the week is over the menu shows next week, so every day is selectable again.
        return isRaidWeekOver(now) || !hasStarted(raidWeekPosition(raidDay), now);
    }

    public List<LocalDate> convertDatesFromSelectMenu(List<String> confirmedDays) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d");
        LocalDate now = LocalDate.now(clock);
        List<LocalDate> dateList = new ArrayList<>();

        for(String date: confirmedDays) {
            LocalDate formatedDate = parseDate(date,formatter,now.getYear());
            dateList.add(formatedDate);
        }

        return dateList;
    }

    public List<LocalDate> convertDatesFromModal(String datesInput) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d");
        LocalDateTime now = LocalDateTime.now(clock);
        List<LocalDate> dateList = new ArrayList<>();

        for(String date: datesInput.split(",")) {
            String trimmed = date.trim();
            if(trimmed.isEmpty()) {
                continue;
            }
            try {
                LocalDate formatedDate = parseDate(trimmed,formatter,now.getYear());
                dateList.add(formatedDate);
            }
            catch (DateTimeException e) {
                throw new IllegalArgumentException("Invalid date format: \""
                        + trimmed + "\".\n"
                        + "Format should be: M/D separated by commas (spaces are ignored).\n"
                        + "Example: 6/7, 4/20,6/9,1/1");
            }

        }
        if(dateList.isEmpty()) {
            throw new IllegalArgumentException("No valid dates entered.");
        }

        return dateList;
    }

    /**
     * The date of the last raid day in the current raid week. Ordered by raid-week position,
     * not DayOfWeek.getValue(), so a schedule that crosses the weekly reset (Sunday and
     * Monday raids on a Tuesday reset) still puts Monday last.
     * @return Date of the final raid of the raid week
     */
    public LocalDate getLastRaidDay() {
        return getNextRaidWeekStartDate().plusDays(lastRaidDayPosition() - 1);
    }

    private LocalDate parseDate(String date, DateTimeFormatter formatter, int currentYear) {
        MonthDay monthDay = MonthDay.parse(date, formatter);
        LocalDate result = monthDay.atYear(currentYear);

        if(result.isBefore(LocalDate.now(clock))) {
            result = result.plusYears(1);
        }
        return result;
    }
}
