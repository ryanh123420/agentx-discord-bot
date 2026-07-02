package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.service.PostOutService;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ViewPostOutListener extends ListenerAdapter {

    private final PostOutService postOutService;

    public ViewPostOutListener(PostOutService postOutService) {
        this.postOutService = postOutService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(event.getName().equals("viewpostouts")) {
            StringBuilder builder = new StringBuilder();
            List<String> response = postOutService.getUsersPostOuts(event.getUser().getId());

            builder.append("Here's the list of your post out dates: \n");

            for(String date: response) {
                builder.append(date.substring(0,1).toUpperCase())
                        .append(date.substring(1).toLowerCase())
                        .append("\n");
            }

            event.reply(builder.toString()).setEphemeral(true).queue();
        }
    }
}
