package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.exception.WowUtilsException;
import com.ryanh.agent_discord_bot.model.DroptimizerResponse;
import com.ryanh.agent_discord_bot.service.WowUtilsClient;
import com.ryanh.agent_discord_bot.utility.EmbedUtility;
import com.ryanh.agent_discord_bot.utility.WishlistFormatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
public class WishlistListener extends ListenerAdapter {

    private final WowUtilsClient wowUtilsClient;

    public WishlistListener(WowUtilsClient wowUtilsClient) {
        this.wowUtilsClient = wowUtilsClient;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("wishlist") && event.getSubcommandName().equals("upload")) {
            String reportUrl = event.getOption("reporturl").getAsString();

            event.deferReply(true).queue();

            try {
                DroptimizerResponse response = wowUtilsClient.postDroptimizer(reportUrl);

                EmbedBuilder embed = EmbedUtility.info(event.getUser(), "Upload successful")
                        .setUrl(response.reportUrl())
                        .addField(
                                "Upload details:",
                                WishlistFormatter.formatCharacterName(response.characterId()) + "\n"
                                        + WishlistFormatter.formatImportedAt(response.importedAt()) + "\n"
                                        + WishlistFormatter.formatWarnings(response.warnings()),
                                false
                        );

                event.getHook()
                        .sendMessageEmbeds(embed.build())
                        .queue();

            } catch (WowUtilsException e) {
                event.getHook()
                        .sendMessageEmbeds(EmbedUtility.error(event.getUser(), e.getMessage()).build())
                        .queue();
            }

        }
    }
}
