package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.config.GuildConfig;
import com.ryanh.agent_discord_bot.entity.PostOut;
import com.ryanh.agent_discord_bot.repository.PostOutRepository;
import com.ryanh.agent_discord_bot.utility.PostOutFormatter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PostOutService {

    private final PostOutRepository postOutRepository;
    private final NotificationService notificationService;
    private final GuildConfig guildConfig;
    private final RaidCalendar raidCalendar;
    private final Clock clock;

    public PostOutService(PostOutRepository postOutRepository,
                          NotificationService notificationService,
                          GuildConfig guildConfig,
                          RaidCalendar raidCalendar,
                          Clock clock) {
        this.postOutRepository = postOutRepository;
        this.notificationService = notificationService;
        this.guildConfig = guildConfig;
        this.raidCalendar = raidCalendar;
        this.clock = clock;
    }

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

    /**
     * Deletes the given post outs, ignoring any that are already gone or belong to
     * someone else. Scoping the lookup by discordId is what stops a stale menu from
     * deleting another member's post out.
     */
    @Transactional
    public Map<String, List<String>> deletePostOut(String discordId, List<Integer> deleteList) {
        List<PostOut> deleted = postOutRepository.findAllByIdInAndDiscordId(deleteList, discordId);
        postOutRepository.deleteAll(deleted);

        List<PostOut> remaining = getUsersPostOuts(discordId);

        return Map.of("deleted", printListOfPostOuts(deleted), "remaining", printListOfPostOuts(remaining));
    }

    public Map<String, List<String>> viewPostOuts(String discordId) {
        List<String> thisWeek = new ArrayList<>();
        List<String> futureWeek = new ArrayList<>();

        for(PostOut postOut: getUsersPostOuts(discordId)) {
            if(postOut.getPostDate().isBefore(raidCalendar.getNextRaidWeekStartDate().plusWeeks(1))) {
                thisWeek.add(PostOutFormatter.formatDate(postOut));
            }
            else {
                futureWeek.add(PostOutFormatter.formatDate(postOut));
            }
        }

        return Map.of("thisweek", thisWeek, "futureweek", futureWeek);
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

        List<PostOut> postOutListThisWeek = postOutRepository.findByPostDateBetween(now,
                raidCalendar.getLastRaidDay());
        List<PostOut> postOutListNextWeek = postOutRepository.findByPostDateBetween(now.plusWeeks(1),
                raidCalendar.getLastRaidDay().plusWeeks(1));

        notificationService.sendPostOutReport(postOutListThisWeek, postOutListNextWeek);
    }

    /**
     * Deletes post outs past the current date at midnight each day.
     */
    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = "${guild.timezone}")
    public void cleanPostOuts() {
        LocalDate now = LocalDate.now(clock);
        postOutRepository.deleteByPostDateBefore(now);
    }

}
