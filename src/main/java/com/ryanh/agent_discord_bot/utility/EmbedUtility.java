package com.ryanh.agent_discord_bot.utility;

import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.time.Instant;

public class EmbedUtility {

    public static EmbedBuilder confirm(String title, String description) {
        return new EmbedBuilder()
                .setColor(Color.decode("#45cf29"))
                .setAuthor(title)
                .setDescription("✅ " + description)
                .setTimestamp(Instant.now());
    }

    public static EmbedBuilder error(String title, String description) {
        return new EmbedBuilder()
                .setColor(Color.decode("#c22727"))
                .setAuthor(title)
                .setDescription("❌ " + description)
                .setTimestamp(Instant.now());
    }

    public static EmbedBuilder info(String title, String description) {
        return new EmbedBuilder()
                .setColor(Color.decode("#3057cf"))
                .setAuthor(title)
                .setDescription("📋 " + description)
                .setTimestamp(Instant.now());
    }
}
