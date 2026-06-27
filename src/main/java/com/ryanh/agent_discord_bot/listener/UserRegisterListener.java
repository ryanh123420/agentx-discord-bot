package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.service.UserService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

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

            try {
                userService.insertUser(input, discordId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            event.reply("BattleTag registered: " + input).queue();

        }
    }
}
