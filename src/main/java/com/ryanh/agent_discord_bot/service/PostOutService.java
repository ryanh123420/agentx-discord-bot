package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.config.GuildConfig;
import com.ryanh.agent_discord_bot.entity.PostOut;
import com.ryanh.agent_discord_bot.repository.PostOutRepository;
import com.ryanh.agent_discord_bot.utility.PostOutFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostOutService {

    private final PostOutRepository postOutRepository;
    private final NotificationService notificationService;
    private final GuildConfig guildConfig;
    private final Clock clock;

    @Autowired
    public PostOutService(PostOutRepository postOutRepository,
                          NotificationService notificationService,
                          GuildConfig guildConfig) {
        this.postOutRepository = postOutRepository;
        this.notificationService = notificationService;
        this.guildConfig = guildConfig;
        this.clock = Clock.system(ZoneId.of(guildConfig.getTimezone()));
    }

    //Tests constructor
    public PostOutService(PostOutRepository postOutRepository,
                          NotificationService notificationService,
                          GuildConfig guildConfig,
                          Clock clock) {
        this.postOutRepository = postOutRepository;
        this.notificationService = notificationService;
        this.guildConfig = guildConfig;
        this.clock = clock;
    }

    //Used to help build menu options in the listener
    public record RaidDay(String label, String value, LocalDate date) {}

    public Map<String, List<String>> insertPostOut(String discordId, List<LocalDate> dateList, String note) {
        List<String> added = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock);

        for(LocalDate date: dateList) {
            if(postOutRepository.existsByDiscordIdAndPostDate(discordId, date)) {
                duplicates.add(PostOutFormatter.formatDate(date));
            }
            else {
                PostOut postOut = new PostOut(discordId, date, now);
                postOutRepository.save(postOut);
                added.add(PostOutFormatter.formatDate(date));
            }
        }

        if (!added.isEmpty()) {
            notificationService.sendPostOutCreation(discordId, added, note);
        }

        return Map.of("added", added, "duplicates", duplicates);
    }

    public Map<String, List<String>> deletePostOut(String discordId, List<String> deleteList) {
        List<PostOut> deleted = new ArrayList<>();

        for (String id: deleteList) {
            PostOut postOut = postOutRepository.findById(Integer.parseInt(id))
                    .orElse(null);

            if(postOut != null && postOut.getDiscordId().equals(discordId)) {
                postOutRepository.delete(postOut);
                deleted.add(postOut);
            }
        }

        List<PostOut> remaining = getUsersPostOuts(discordId);

        return Map.of("deleted", printListOfPostOuts(deleted), "remaining", printListOfPostOuts(remaining));
    }

    public Map<String, List<String>> viewPostOuts(String discordId) {
        List<String> thisWeek = new ArrayList<>();
        List<String> futureWeek = new ArrayList<>();

        for(PostOut postOut: getUsersPostOuts(discordId)) {
            if(postOut.getPostDate().isBefore(getNextRaidWeekStartDate().plusWeeks(1))) {
                thisWeek.add(PostOutFormatter.formatDate(postOut));
            }
            else {
                futureWeek.add(PostOutFormatter.formatDate(postOut));
            }
        }

        return Map.of("thisweek", thisWeek, "futureweek", futureWeek);
    }

    /**
     * Returns the start date of the next week, based off if the last raid day and the raid start time
     * has passed for the week.
     * @return Start date of the raid week.
     */
    public LocalDate getNextRaidWeekStartDate() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        List<DayOfWeek> raidDayList = guildConfig.getRaidDays();
        int lastRaidDay = 0;
        for(DayOfWeek day: raidDayList) {
            lastRaidDay = Math.max(lastRaidDay, daysSinceReset(day));
        }

        //If it's the last raid day past the raid start time, then the current raid week is over.
        if(daysSinceReset(now.getDayOfWeek()) == lastRaidDay
                && now.getHour() >= guildConfig.getRaidStartTime()) {
            //Add 1 week to now so we return next week's reset date.
            now = now.plusWeeks(1);
        }
        //If the current raid week is over.
        else if(daysSinceReset(now.getDayOfWeek()) > lastRaidDay) {
            now = now.plusWeeks(1);
        }

        //Returns the reset date (Tuesday for US) of the week that "now" is
        return  now.toLocalDate().with(TemporalAdjusters.previousOrSame(guildConfig.getResetDay()));
    }

    /**
     * Maps the return value of DayOfWeek.getValue() around the daily reset for a region rather than Monday through
     * Sunday. So for US Tuesday = 1 and Monday = 7, EU Wednesday = 1, Tuesday = 7, etc
     * @param day A day Monday through Sunday
     * @return The days value based of the weekly reset
     */
    private int daysSinceReset(DayOfWeek day) {
        return (day.getValue() - guildConfig.getResetDay().getValue() + 7) % 7 + 1;
    }

    /**
     * Provides a list of valid menu options for the "This Reset" drop down by checking if it's past a raid days date
     * and start time. For example, if Tuesday's raid is over, then there's no point putting Tuesday in the menu options.
     * @return List of valid RaidDays to add as menu options
     */
    public List<RaidDay> validMenuOptions() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDate reset = getNextRaidWeekStartDate();
        List<RaidDay> raidDaysList = new ArrayList<>();

        for(int i = 0; i < guildConfig.getRaidDays().size(); i++) {
            DayOfWeek day = guildConfig.getRaidDays().get(i);

            if(isValidMenuOption(day, now)) {
                int offset = daysSinceReset(day) - 1;

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
            int offset = daysSinceReset(day) - 1;

            raidDaysList.add(new RaidDay(day.name().charAt(0)
                    + day.name().substring(1).toLowerCase(),
                    reset.plusDays(offset).getMonthValue() + "/" + reset.plusDays(offset).getDayOfMonth(),
                    reset.plusDays(offset)));
        }

        return raidDaysList;
    }

    private boolean isValidMenuOption(DayOfWeek raidDay, ZonedDateTime now) {
        int lastDay = guildConfig.getRaidDays().stream()
                .mapToInt(this::daysSinceReset)
                .max()
                .orElse(0);
        int todayValue = daysSinceReset(now.getDayOfWeek());
        int raidDayValue = daysSinceReset(raidDay);

        //If the week is over, we want menu options for next week, so return true
        boolean raidWeekOver = todayValue > lastDay
                || (todayValue == lastDay && now.getHour() >= guildConfig.getRaidStartTime());

        if(raidWeekOver) {
            return true;
        }

        if(raidDayValue < todayValue) {
            return false;
        }
        else if(raidDayValue == todayValue && now.getHour() >= guildConfig.getRaidStartTime()) {
            return false;
        }

        return true;
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

    public LocalDate getLastRaidDay() {
        List<DayOfWeek> raidDayList = guildConfig.getRaidDays();
        int lastDay = 0;
        for(DayOfWeek day: raidDayList) {
            lastDay = Math.max(lastDay, day.getValue());
        }

        return LocalDate.now(clock).with(TemporalAdjusters.nextOrSame(DayOfWeek.of(lastDay)));
    }

    private LocalDate parseDate(String date, DateTimeFormatter formatter, int currentYear) {
        MonthDay monthDay = MonthDay.parse(date, formatter);
        LocalDate result = monthDay.atYear(currentYear);

        if(result.isBefore(LocalDate.now(clock))) {
            result = result.plusYears(1);
        }
        return result;
    }

    public List<PostOut> getUsersPostOuts(String discordId) {
        return postOutRepository.findAllByDiscordId(discordId);
    }

    public List<PostOut> getAllPostOuts() {
        return postOutRepository.findAll();
    }

    public List<String> printListOfPostOuts(List<PostOut> postOuts) {
        return postOuts.stream()
                .sorted(Comparator.comparing(PostOut::getPostDate))
                .map(PostOutFormatter::formatDate)
                .toList();
    }

    /**
     * Sends all post outs for the current and next raid reset.
     */
    @Scheduled(cron = "${guild.notification-schedule}", zone = "${guild.timezone}")
    public void postOutReminder() {
        LocalDate now = LocalDate.now(clock);
        if(!now.getDayOfWeek().equals(guildConfig.getResetDay())) {
            return;
        }

        List<PostOut> postOutListThisWeek = postOutRepository.findByPostDateBetween(now, getLastRaidDay());
        List<PostOut> postOutListNextWeek = postOutRepository.findByPostDateBetween(now.plusWeeks(1),
                getLastRaidDay().plusWeeks(1));

        notificationService.sendPostOutReport(postOutListThisWeek, postOutListNextWeek);
    }

    /**
     * Deletes post outs past the current date at midnight each day.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "${guild.timezone}")
    public void cleanPostOuts() {
        LocalDate now = LocalDate.now(clock);
        postOutRepository.deleteByPostDateBefore(now);
    }

}
