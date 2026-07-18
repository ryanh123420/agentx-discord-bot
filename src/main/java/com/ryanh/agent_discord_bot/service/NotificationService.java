package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.config.GuildConfig;
import com.ryanh.agent_discord_bot.entity.PostOut;
import com.ryanh.agent_discord_bot.utility.EmbedUtility;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class NotificationService {
    private final JDA jda;
    private final GuildConfig guildConfig;

    public NotificationService(JDA jda, GuildConfig guildConfig) {
        this.jda = jda;
        this.guildConfig = guildConfig;
    }

    public void sendEmbedToChannel(MessageEmbed embed, String channelId) {
        jda.getTextChannelById(channelId).sendMessageEmbeds(embed).queue();
    }

    public void sendPostOutCreation(String discordId, List<String> postOutList) {
        EmbedBuilder embed = EmbedUtility.info("Post Out created",
                "<@" + discordId + "> just created a post out:");
        embed.addField("🗓️ Added:", String.join("\n", postOutList),true);

        sendEmbedToChannel(embed.build(), guildConfig.getOfficerChannelId());
    }

    public void sendPostOutReport(List<PostOut> thisWeek, List<PostOut> nextWeek) {
        String thisWeekText = formatPostOutReport(thisWeek);
        String nextWeekText = formatPostOutReport(nextWeek);

        EmbedBuilder embed = EmbedUtility.info("🗓️ Weekly Post Out Report",
                "The following players have made a post out for this week and next week:")
                .addField("This Week:", thisWeekText, true)
                .addField("Next Week:", nextWeekText, true);

        sendEmbedToChannel(embed.build(), guildConfig.getOfficerChannelId());
    }

    private String formatPostOutReport(List<PostOut> postOutList) {
        if(postOutList.isEmpty()) {
            return "⭐ There are no post outs! ⭐";
        }
        StringBuilder builder = new StringBuilder();
        Map<String, List<String>> userOutput = new HashMap<>();
        for(PostOut post: postOutList) {
            String discordUser = post.getDiscordId();
            String date = post.getPostDate().format(DateTimeFormatter.ofPattern("EEE M/d"));
            if(!userOutput.containsKey(discordUser)) {
                userOutput.put(discordUser, new ArrayList<>());
            }
            userOutput.get(discordUser).add(date);
        }

        for(String user: userOutput.keySet()) {
            builder.append("👤 <@").append(user).append(">")
                    .append("\n");
            for(String date: userOutput.get(user)) {
                builder.append("🗓️ ")
                        .append(date)
                        .append("\n");
            }
            builder.append("\n");
        }

        return builder.toString();
    }
}
