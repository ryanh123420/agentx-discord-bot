package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.utility.EmbedUtility;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ThreadsListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(event.getName().equals("threads") && "update".equals(event.getSubcommandName())) {

        }
        else if(event.getName().equals("threads") && "manage".equals(event.getSubcommandName())) {
            event.replyEmbeds(EmbedUtility.info(event.getUser(),
                            "Select threads to manage")
                            .build())
                    .addComponents(ActionRow.of(
                            EntitySelectMenu.create("select", EntitySelectMenu.SelectTarget.CHANNEL)
                                    .build()))
                    .queue();
        }


        //event.getGuild().getChannels();
    }

}
