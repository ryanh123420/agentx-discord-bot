package com.ryanh.agent_discord_bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.DayOfWeek;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "guild")
public class GuildConfig {
    private String timezone;
    private Integer raidStartTime;
    private List<DayOfWeek> raidDays;
    private DayOfWeek resetDay;

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Integer getRaidStartTime() {
        return raidStartTime;
    }

    public void setRaidStartTime(Integer raidStartTime) {
        this.raidStartTime = raidStartTime;
    }

    public List<DayOfWeek> getRaidDays() {
        return raidDays;
    }

    public void setRaidDays(List<DayOfWeek> raidDays) {
        this.raidDays = raidDays;
    }

    public DayOfWeek getResetDay() {
        return resetDay;
    }

    public void setResetDay(DayOfWeek resetDay) {
        this.resetDay = resetDay;
    }
}
