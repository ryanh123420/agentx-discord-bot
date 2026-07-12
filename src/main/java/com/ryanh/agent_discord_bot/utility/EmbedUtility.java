package com.ryanh.agent_discord_bot.utility;

import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.time.Instant;

public class EmbedUtility {

    public static EmbedBuilder confirm(String title, String description) {
        return new EmbedBuilder()
                .setColor(Color.decode("#45cf29"))
                .setTitle(title)
                .setDescription("✅ " + description)
                .setTimestamp(Instant.now());
    }

    public static EmbedBuilder error(String title, String description) {
        return new EmbedBuilder()
                .setColor(Color.decode("#c22727"))
                .setTitle(title)
                .setDescription("❌ " + description)
                .setTimestamp(Instant.now());
    }

    public static EmbedBuilder info(String title, String description) {
        return new EmbedBuilder()
                .setColor(Color.decode("#3057cf"))
                .setTitle(title)
                .setDescription("📋 " + description)
                .setTimestamp(Instant.now());
    }

    public static EmbedBuilder postOutWeeklyNotification(String thisWeek, String nextWeek) {
        return new EmbedBuilder()
                .setColor(Color.decode("#620cb3"))
                .setTitle("🗓️ Weekly Post Out Report")
                .setDescription("The following players have made a post out for this week and next week:")
                .addField("This Week:", thisWeek, true)
                .addField("Next Week:", nextWeek, true)
                .setTimestamp(Instant.now());
    }
}
