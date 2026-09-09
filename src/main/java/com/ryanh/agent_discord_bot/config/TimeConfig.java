package com.ryanh.agent_discord_bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    /**
     * The single time source for the application, fixed to the guild's timezone.
     * Anything that needs "now" injects this rather than calling ZonedDateTime.now()
     * so tests can substitute a Clock.fixed(...).
     */
    @Bean
    public Clock clock(GuildConfig guildConfig) {
        return Clock.system(ZoneId.of(guildConfig.getTimezone()));
    }
}
