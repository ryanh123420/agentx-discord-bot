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

    public record InsertResult(List<String> added, List<String> duplicates) {}
    public record DeleteResult(List<String> deleted, List<String> remaining) {}
    public record PostOutsByWeek(List<String> thisWeek, List<String> futureWeek) {}

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

    public InsertResult insertPostOut(String discordId, List<LocalDate> dateList) {
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

        return new InsertResult(added, duplicates);
    }

    /**
     * Deletes the given post outs, ignoring any that are already gone or belong to
     * someone else. Scoping the lookup by discordId is what stops a stale menu from
     * deleting another member's post out.
     */
    @Transactional
    public DeleteResult deletePostOut(String discordId, List<Integer> deleteList) {
        List<PostOut> deleted = postOutRepository.findAllByIdInAndDiscordId(deleteList, discordId);
        postOutRepository.deleteAll(deleted);

        List<PostOut> remaining = getUsersPostOuts(discordId);

        return new DeleteResult(PostOutFormatter.formatSorted(deleted), PostOutFormatter.formatSorted(remaining));
    }

    public PostOutsByWeek viewPostOuts(String discordId) {
        List<String> thisWeek = new ArrayList<>();
        List<String> futureWeek = new ArrayList<>();
        LocalDate nextWeekStart = raidCalendar.getNextRaidWeekStartDate().plusWeeks(1);

        for(PostOut postOut: getUsersPostOuts(discordId)) {
            if(postOut.getPostDate().isBefore(nextWeekStart)) {
                thisWeek.add(PostOutFormatter.formatDate(postOut));
            }
            else {
                futureWeek.add(PostOutFormatter.formatDate(postOut));
            }
        }

        return new PostOutsByWeek(thisWeek, futureWeek);
    }

    public List<PostOut> getUsersPostOuts(String discordId) {
        return postOutRepository.findAllByDiscordId(discordId);
    }

    public List<PostOut> getAllPostOuts() {
        return postOutRepository.findAll();
    }

    /**
     * Sends all post outs for the current and next raid reset. The schedule is expected to fire
     * on reset day; the day check guards against a schedule that is changed to fire on other days.
     */
    @Scheduled(cron = "${guild.notification-schedule}", zone = "${guild.timezone}")
    public void postOutReminder() {
        LocalDate now = LocalDate.now(clock);
        if(!now.getDayOfWeek().equals(guildConfig.getResetDay())) {
            return;
        }

        LocalDate lastRaidDay = raidCalendar.getLastRaidDay();
        List<PostOut> postOutListThisWeek = postOutRepository.findByPostDateBetween(now, lastRaidDay);
        List<PostOut> postOutListNextWeek = postOutRepository.findByPostDateBetween(now.plusWeeks(1),
                lastRaidDay.plusWeeks(1));

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
