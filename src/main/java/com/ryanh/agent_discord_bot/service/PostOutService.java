package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.config.GuildConfig;
import com.ryanh.agent_discord_bot.entity.PostOut;
import com.ryanh.agent_discord_bot.repository.PostOutRepository;
import com.ryanh.agent_discord_bot.utility.EmbedUtility;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class PostOutService {

    private final PostOutRepository postOutRepository;
    private final NotificationService notificationService;
    private final GuildConfig guildConfig;

    public PostOutService(PostOutRepository postOutRepository,
                          NotificationService notificationService,
                          GuildConfig guildConfig) {
        this.postOutRepository = postOutRepository;
        this.notificationService = notificationService;
        this.guildConfig = guildConfig;
    }

    public record RaidDay(String label, String value, LocalDate date) {}

    public MessageEmbed insertPostOut(String discordId, List<LocalDate> dateList) {
        List<String> added = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for(LocalDate date: dateList) {
            if(postOutRepository.existsByDiscordIdAndPostDate(discordId, date)) {
                duplicates.add(formatDate(date));
            }
            else {
                PostOut postOut = new PostOut(discordId, date, now, now);
                postOutRepository.save(postOut);
                added.add(formatDate(date));
            }
        }

        EmbedBuilder embed = EmbedUtility.confirm("Create a Post Out", "Post out results:");
        if(!added.isEmpty()) {
            embed.addField("🗓️ Added:", String.join("\n", added), true);
        }
        if (!duplicates.isEmpty()) {
            embed.addField("⚠️ Already exists:", String.join("\n", duplicates), true);
        }

        return embed.build();
    }

    public String deletePostOut(String discordId, List<String> postOutList) {
        for (String id: postOutList) {
            PostOut postOut = postOutRepository.findById(Integer.parseInt(id))
                    .orElse(null);

            if(postOut != null && postOut.getDiscordId().equals(discordId)) {
                postOutRepository.delete(postOut);
            }
        }

        return "Post out deleted!!!!";
    }

    public MessageEmbed viewPostOuts(String discordId) {
        List<String> thisWeek = new ArrayList<>();
        List<String> futureWeek = new ArrayList<>();

        for(PostOut postOut: getUsersPostOuts(discordId)) {

            System.out.println(getReset());
            if(postOut.getPostDate().isBefore(getReset().plusWeeks(1))) {
                thisWeek.add(formatDate(postOut));
            }
            else {
                futureWeek.add(formatDate(postOut));
            }
        }

        EmbedBuilder embed = EmbedUtility.info("View Post Outs", "Here's a list of your post outs:");

        if(!thisWeek.isEmpty()) {
            embed.addField("🗓️ This Week:", String.join("\n", thisWeek), true);
        }
        else {
            embed.addField("🗓️ This Week:", "None", true);
        }
        if (!futureWeek.isEmpty()) {
            embed.addField("🗓️ Later Weeks:", String.join("\n", futureWeek), true);
        }
        else {
            embed.addField("🗓️ Later Weeks:", "None", true);
        }

        return embed.build();
    }

    public LocalDate getReset() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(guildConfig.getTimezone()));
        List<DayOfWeek> raidDayList = guildConfig.getRaidDays();
        int lastDay = 0;
        for(DayOfWeek day: raidDayList) {
            lastDay = Math.max(lastDay, daysSinceReset(day));
        }

        if(daysSinceReset(now.getDayOfWeek()) == lastDay
                && now.getHour() >= guildConfig.getRaidStartTime()) {
            now = now.plusWeeks(1);
        }
        else if(daysSinceReset(now.getDayOfWeek()) > lastDay) {
            now = now.plusWeeks(1);
        }

        return  now.toLocalDate().with(TemporalAdjusters.previousOrSame(guildConfig.getResetDay()));
    }

    /**
     * Return a days value based off the start of the week being on the weekly reset
     * @param day A day Monday through Sunday
     * @return The days value based of the weekly reset (for US weekly reset, Tues = 1 and Mon = 7)
     */
    private int daysSinceReset(DayOfWeek day) {
        return (day.getValue() - guildConfig.getResetDay().getValue() + 7) % 7 + 1;
    }

    /**
     * Checks if the input would be within a valid timeframe to add as a menu option in the PostOutListener
     * @param raidDay Current raid day
     * @return True if before the start time of a raid day, false if after raid day has passed that reset
     */
    private boolean isValidMenuOption(DayOfWeek raidDay) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(guildConfig.getTimezone()));
        int lastDay = guildConfig.getRaidDays().stream()
                .mapToInt(this::daysSinceReset)
                .max()
                .orElse(0);
        int todayValue = daysSinceReset(now.getDayOfWeek());
        int raidDayValue = daysSinceReset(raidDay);

        if(todayValue < lastDay) {
            if(raidDayValue < todayValue) {
                return false;
            }
            else if(raidDayValue == todayValue && now.getHour() > guildConfig.getRaidStartTime()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Provides a list of valid menu options for the Select Reset option on the PostOutListener
     * @return List of valid RaidDays to add as menu options
     */
    public List<RaidDay> validMenuOptions() {
        LocalDate reset = getReset();
        List<RaidDay> raidDaysList = new ArrayList<>();

        for(int i = 0; i < guildConfig.getRaidDays().size(); i++) {
            DayOfWeek day = guildConfig.getRaidDays().get(i);

            if(isValidMenuOption(day)) {
                int offset = daysSinceReset(day) - 1;

                raidDaysList.add(new RaidDay(day.name().charAt(0)
                        + day.name().substring(1).toLowerCase(),
                        day.name().toLowerCase(),
                        reset.plusDays(offset)));
            }
        }

        return raidDaysList;
    }

    public List<LocalDate> getDatesFromSelect(List<String> confirmedDays) {
        LocalDate now = LocalDate.now(ZoneId.of(guildConfig.getTimezone()));

        return confirmedDays.stream()
                .map(s -> DayOfWeek.valueOf(s.toUpperCase()))
                .map(s -> now.with(TemporalAdjusters.nextOrSame(s)))
                .toList();
    }

    public List<LocalDate> getDatesFromModal(String datesInput) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d");
        LocalDateTime now = LocalDateTime.now(ZoneId.of(guildConfig.getTimezone()));
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

        return LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.of(lastDay)));
    }

    private LocalDate parseDate(String date, DateTimeFormatter formatter, int currentYear) {
        MonthDay monthDay = MonthDay.parse(date, formatter);
        LocalDate result = monthDay.atYear(currentYear);

        if(result.isBefore(LocalDate.now(ZoneId.of(guildConfig.getTimezone())))) {
            result = result.plusYears(1);
        }
        return result;
    }

    public List<PostOut> getUsersPostOuts(String discordId) {
        return postOutRepository.findAllByDiscordId(discordId);
    }

    public List<String> printListOfPostOuts(List<PostOut> postOuts) {
        return postOuts.stream()
                .sorted(Comparator.comparing(PostOut::getPostDate))
                .map(this::formatDate)
                .toList();
    }

    public String printSinglePostOut(PostOut postOut) {
        return formatDate(postOut);
    }

    private String formatDate(PostOut postOut) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE M/d/yyyy");
        return postOut.getPostDate().format(formatter);
    }

    private String formatDate(LocalDate postDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE M/d/yyyy");
        return postDate.format(formatter);
    }

    @Scheduled(cron = "${guild.notification-schedule}", zone = "${guild.timezone}")
    public void weeklyPostOutReminder() {
        LocalDate now = LocalDate.now(ZoneId.of(guildConfig.getTimezone()));
        if(!now.getDayOfWeek().equals(guildConfig.getResetDay())) {
            return;
        }

        List<PostOut> postOutListThisWeek = postOutRepository.findByPostDateBetween(now, getLastRaidDay());
        List<PostOut> postOutListNextWeek = postOutRepository.findByPostDateBetween(now.plusWeeks(1),
                getLastRaidDay().plusWeeks(1));

        notificationService.sendPostOutReport(postOutListThisWeek, postOutListNextWeek);
    }

    public void newPostOutNotification() {

    }

}
