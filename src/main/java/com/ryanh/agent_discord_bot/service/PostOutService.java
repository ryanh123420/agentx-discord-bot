package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.model.PostOut;
import com.ryanh.agent_discord_bot.repository.PostOutRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

@Service
public class PostOutService {

    private final PostOutRepository postOutRepository;

    public PostOutService(PostOutRepository postOutRepository) {
        this.postOutRepository = postOutRepository;
    }

    public String addPostOut(String discordId) {

        PostOut postOut = new PostOut();


        return "Post out added!!!!!";
    }

    public String cancelPostOut() {


        return "Post out cancelled!!!!";
    }

    public LocalDate getReset() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
        if(now.getDayOfWeek() == DayOfWeek.THURSDAY && now.getHour() >= 21) {
            now = now.plusWeeks(1);
        }
        else if(now.getDayOfWeek().getValue() > DayOfWeek.THURSDAY.getValue()
                || now.getDayOfWeek().getValue() < DayOfWeek.TUESDAY.getValue()) {
            now = now.plusWeeks(1);
        }

        return  now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.TUESDAY));
    }

    public boolean isRaidDayPassed(DayOfWeek raidDay) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
        DayOfWeek today = now.getDayOfWeek();

        if(today.getValue() > raidDay.getValue()) {
            return true;
        }
        else if(today == raidDay && now.getHour() >= 21) {
            return true;
        }
        return false;
    }

}
