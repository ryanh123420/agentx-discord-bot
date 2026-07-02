package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.entity.PostOut;
import com.ryanh.agent_discord_bot.repository.PostOutRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;

@Service
public class PostOutService {

    private final PostOutRepository postOutRepository;

    public PostOutService(PostOutRepository postOutRepository) {
        this.postOutRepository = postOutRepository;
    }

    public String insertPostOut(String discordId, List<LocalDate> dateList) {

        LocalDateTime now = LocalDateTime.now();

        for(LocalDate date: dateList) {
            PostOut postOut = new PostOut(discordId, date, now, now);
            postOutRepository.save(postOut);
        }

        return "Post out added!!!!!";
    }

    public String deletePostOut() {


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

    public List<LocalDate> getDates(List<String> confirmedDays) {
        LocalDate now = LocalDate.now(ZoneId.of("America/New_York"));

        return confirmedDays.stream()
                .map(s -> DayOfWeek.valueOf(s.toUpperCase()))
                .map(s -> now.with(TemporalAdjusters.nextOrSame(s)))
                .toList();
    }

    public List<LocalDate> getDatesFromString(String datesInput) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d");
        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/New_York"));

        return Arrays.stream(datesInput.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> parseDate(s, formatter, now.getYear()))
                .toList();
    }

    private LocalDate parseDate(String date, DateTimeFormatter formatter, int currentYear) {
        MonthDay monthDay = MonthDay.parse(date, formatter);
        LocalDate result = monthDay.atYear(currentYear);

        if(result.isBefore(LocalDate.now(ZoneId.of("America/New_York")))) {
            result = result.plusYears(1);
        }
        return result;
    }

}
