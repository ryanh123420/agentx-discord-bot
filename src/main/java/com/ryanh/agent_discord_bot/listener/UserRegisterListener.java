package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.service.UserService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
public class UserRegisterListener extends ListenerAdapter {

    private final UserService userService;

    public UserRegisterListener(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(event.getName().equals("register")) {
            String discordId = event.getUser().getId();
            String input = event.getOption("battletag").getAsString();

            if(!input.matches(".+#\\d+")) {
                event.reply("Invalid BattleTag format, double check formatting follows: Name#1234")
                        .setEphemeral(true).queue();
            }

            String response = userService.insertUser(input, discordId);
            event.reply(response).setEphemeral(true).queue();
        }
    }
}
