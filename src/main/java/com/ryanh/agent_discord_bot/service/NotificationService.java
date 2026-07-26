package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.config.GuildConfig;
import com.ryanh.agent_discord_bot.entity.PostOut;
import com.ryanh.agent_discord_bot.utility.EmbedUtility;
import com.ryanh.agent_discord_bot.utility.PostOutFormatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.springframework.stereotype.Service;

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

    public void sendPostOutCreation(String discordId, List<String> postOutList, String note) {
        EmbedBuilder embed = EmbedUtility.info("Post Out created",
                "<@" + discordId + "> just created a post out:");
        embed.addField("🗓️ Added:", String.join("\n", postOutList),false);

        if(!note.isEmpty()) {
            embed.addField("🗒️ Note:", note, false);
        }

        sendEmbedToChannel(embed.build(), guildConfig.getOfficerChannelId());
    }

    public void sendPostOutReport(List<PostOut> thisWeek, List<PostOut> nextWeek) {
        String thisWeekText = PostOutFormatter.formatPostOutReport(thisWeek);
        String nextWeekText = PostOutFormatter.formatPostOutReport(nextWeek);

        EmbedBuilder embed = EmbedUtility.info("🗓️ Weekly Post Out Report",
                "The following players have made a post out for this week and next week:")
                .addField("This Week:", thisWeekText, true)
                .addField("Next Week:", nextWeekText, true);

        sendEmbedToChannel(embed.build(), guildConfig.getOfficerChannelId());
    }


}
