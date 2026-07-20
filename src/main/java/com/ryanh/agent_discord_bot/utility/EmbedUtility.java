package com.ryanh.agent_discord_bot.utility;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.time.Instant;

public class EmbedUtility {

    public static EmbedBuilder confirm(User user, String description) {
        return new EmbedBuilder()
                .setColor(Color.decode("#45cf29"))
                .setAuthor(user.getEffectiveName(), null, user.getEffectiveAvatarUrl())
                .setDescription(description)
                .setTimestamp(Instant.now());
    }

    public static EmbedBuilder confirm(String user, String description) {
        return new EmbedBuilder()
                .setColor(Color.decode("#45cf29"))
                .setAuthor(user)
                .setDescription(description)
                .setTimestamp(Instant.now());
    }

    public static EmbedBuilder error(User user, String description) {
        return new EmbedBuilder()
                .setColor(Color.decode("#c22727"))
                .setAuthor(user.getEffectiveName(), null, user.getEffectiveAvatarUrl())
                .setDescription(description)
                .setTimestamp(Instant.now());
    }

    public static EmbedBuilder error(String user, String description) {
        return new EmbedBuilder()
                .setColor(Color.decode("#c22727"))
                .setAuthor(user)
                .setDescription(description)
                .setTimestamp(Instant.now());
    }

    public static EmbedBuilder info(User user, String description) {
        return new EmbedBuilder()
                .setColor(Color.decode("#3057cf"))
                .setAuthor(user.getEffectiveName(), null, user.getEffectiveAvatarUrl())
                .setDescription(description)
                .setTimestamp(Instant.now());
    }

    public static EmbedBuilder info(String user, String description) {
        return new EmbedBuilder()
                .setColor(Color.decode("#3057cf"))
                .setAuthor(user)
                .setDescription(description)
                .setTimestamp(Instant.now());
    }
}
