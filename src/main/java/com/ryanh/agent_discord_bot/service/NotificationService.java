package com.ryanh.agent_discord_bot.service;

import com.ryanh.agent_discord_bot.config.GuildConfig;
import com.ryanh.agent_discord_bot.entity.PostOut;
import com.ryanh.agent_discord_bot.utility.EmbedUtility;
import com.ryanh.agent_discord_bot.utility.PostOutFormatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JDA jda;
    private final GuildConfig guildConfig;

    public NotificationService(JDA jda, GuildConfig guildConfig) {
        this.jda = jda;
        this.guildConfig = guildConfig;
    }

    public void sendEmbedToChannel(MessageEmbed embed, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);

        //A bad or uncached channel id shouldn't blow up the caller, which has usually
        //already committed to the database by this point.
        if(channel == null) {
            log.warn("Channel {} not found, embed was not sent.", channelId);
            return;
        }

        channel.sendMessageEmbeds(embed).queue();
    }

    public void sendPostOutCreation(String discordId, List<String> postOutList, String note) {
        EmbedBuilder embed = EmbedUtility.info("Post Out created",
                "<@" + discordId + "> just created a post out:");
        embed.addField("🗓️ Added:", String.join("\n", postOutList),false);

        if(!note.isEmpty()) {
            embed.addField("🗒️ Note:", note, false);
        }

        sendEmbedToChannel(embed.build(), guildConfig.getPostOutChannelId());
    }

    public void sendPostOutReport(List<PostOut> thisWeek, List<PostOut> nextWeek) {
        EmbedBuilder embed = EmbedUtility.info("🗓️ Weekly Post Out Report",
                "The following players have made a post out for this week and next week:");

        addReportSection(embed, "This Week:", PostOutFormatter.formatPostOutReport(thisWeek));
        addReportSection(embed, "Next Week:", PostOutFormatter.formatPostOutReport(nextWeek));

        sendEmbedToChannel(embed.build(), guildConfig.getOfficerChannelId());
    }

    /**
     * Adds a report section, which the formatter may have split over several field values to
     * stay inside Discord's per field character limit.
     */
    private void addReportSection(EmbedBuilder embed, String title, List<String> fieldValues) {
        for(int i = 0; i < fieldValues.size(); i++) {
            embed.addField(i == 0 ? title : title + " (cont.)", fieldValues.get(i), true);
        }
    }


}
