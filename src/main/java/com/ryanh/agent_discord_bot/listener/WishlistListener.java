package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.exception.WowUtilsException;
import com.ryanh.agent_discord_bot.model.DroptimizerResponse;
import com.ryanh.agent_discord_bot.client.WowUtilsClient;
import com.ryanh.agent_discord_bot.service.NotificationService;
import com.ryanh.agent_discord_bot.utility.EmbedUtility;
import com.ryanh.agent_discord_bot.utility.WishlistFormatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
public class WishlistListener extends ListenerAdapter {

    private final WowUtilsClient wowUtilsClient;
    private final NotificationService notificationService;

    public WishlistListener(WowUtilsClient wowUtilsClient, NotificationService notificationService) {
        this.wowUtilsClient = wowUtilsClient;
        this.notificationService = notificationService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("wishlist") && event.getSubcommandName().equals("upload")) {
            String reportUrl = event.getOption("reporturl").getAsString();

            event.deferReply(true).queue();

            try {
                DroptimizerResponse response = wowUtilsClient.postDroptimizer(reportUrl);

                EmbedBuilder embed = EmbedUtility.info(event.getUser(), "")
                        .setTitle("Report Link")
                        .setUrl(response.reportUrl());

                //Clean sim validations, no warning field in the HTTP response body.
                if (response.warnings().isEmpty()) {
                    MessageEmbed.Field detailsField = new MessageEmbed.Field("Upload Successful:",
                            WishlistFormatter.formatCharacterName(response.characterId()) + "\n"
                                    + WishlistFormatter.formatImportedAt(response.importedAt()) + "\n",
                            false
                    );
                    embed.addField(detailsField);
                }
                else {
                    MessageEmbed.Field warningField = new MessageEmbed.Field(
                            "⚠️ Your report has incorrect sim settings, please re-sim fixing the following:",
                            WishlistFormatter.formatWarnings(response.warnings()),
                            false);

                    embed.addField(warningField);
                }

                event.getHook()
                        .sendMessageEmbeds(embed.build())
                        .queue();

                notificationService.sendWishListUpload(embed, event.getUser().getId());

            } catch (WowUtilsException e) {
                event.getHook()
                        .sendMessageEmbeds(EmbedUtility.error(event.getUser(),
                                WishlistFormatter.formatError(e.getErrorCodes(),
                                        e.getRetryAfterSeconds(), e.getRateLimitReset())).build())
                        .queue();
            }

        }
    }
}
