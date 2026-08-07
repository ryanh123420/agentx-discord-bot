package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.exception.WowUtilsException;
import com.ryanh.agent_discord_bot.service.WowUtilsClient;
import com.ryanh.agent_discord_bot.utility.EmbedUtility;
import com.ryanh.agent_discord_bot.utility.PostOutFormatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WishlistListener extends ListenerAdapter {

    private final WowUtilsClient wowUtilsClient;

    public WishlistListener(WowUtilsClient wowUtilsClient) {
        this.wowUtilsClient = wowUtilsClient;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("wishlist") && event.getSubcommandName().equals("upload")) {
            String discordId = event.getUser().getId();
            String reportUrl = event.getOption("reporturl").getAsString();

            try {
                WowUtilsClient.DroptimizerResponse response = wowUtilsClient.postDroptimizer(reportUrl);

                event.replyEmbeds(EmbedUtility.info(event.getUser(), "Upload successful").build()).queue();
            } catch (WowUtilsException e) {
                event.replyEmbeds(EmbedUtility.error(event.getUser(), e.getMessage()).build()).queue();
            }

        }
    }
}
