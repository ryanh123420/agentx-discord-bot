package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.entity.PostOut;
import com.ryanh.agent_discord_bot.service.PostOutService;
import com.ryanh.agent_discord_bot.utility.EmbedUtility;
import com.ryanh.agent_discord_bot.utility.PostOutFormatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminListener extends ListenerAdapter {
    private final PostOutService postOutService;

    public AdminListener(PostOutService postOutService) {
        this.postOutService = postOutService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("admin") && event.getSubcommandName().equals("viewpostouts")) {
            EmbedBuilder embed = EmbedUtility.info(event.getUser(), "");
            embed.setTitle("List of all Post Outs:");
            List<PostOut> postOutList = postOutService.getAllPostOuts();

            embed.setDescription(PostOutFormatter.formatPostOutReport(postOutList));

            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        }
    }
}
