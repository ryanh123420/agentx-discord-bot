package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.config.GuildConfig;
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
    private final GuildConfig guildConfig;

    public WishlistListener(WowUtilsClient wowUtilsClient, NotificationService notificationService, GuildConfig guildConfig) {
        this.wowUtilsClient = wowUtilsClient;
        this.notificationService = notificationService;
        this.guildConfig = guildConfig;
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

                MessageEmbed.Field detailsField = new MessageEmbed.Field("Upload Successful:",
                        WishlistFormatter.formatCharacterName(response.characterId()) + "\n"
                                + WishlistFormatter.formatImportedAt(response.importedAt()) + "\n",
                        false
                );
                embed.addField(detailsField);

                if (!response.warnings().isEmpty()) {
                    MessageEmbed.Field warningField = new MessageEmbed.Field(
                            "⚠️ Your report has warnings, you may need to re-sim and fix the following:",
                            WishlistFormatter.formatBulletList(response.warnings()),
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
        else if (event.getName().equals("wishlist") && event.getSubcommandName().equals("validations")) {

            EmbedBuilder embed = EmbedUtility.info(event.getUser(), "");

            embed.addField("Current sim validations:",
                    WishlistFormatter.formatBulletList(guildConfig.getSimValidations()),
                    false);

            event.replyEmbeds(embed.build())
                    .setEphemeral(true)
                    .queue();
        }
    }
}
