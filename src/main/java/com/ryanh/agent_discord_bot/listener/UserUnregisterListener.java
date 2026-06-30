package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.service.UserService;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
public class UserUnregisterListener extends ListenerAdapter {
    private final UserService userService;

    public UserUnregisterListener(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(event.getName().equals("unregister")) {
            if(userService.checkIfRegistered(event.getUser().getId())) {
                event.reply("Are you sure you want to unregister your BattleTag?")
                        .addComponents(ActionRow.of(
                                Button.primary("unregister-confirm","Confirm"),
                                Button.danger("unregister-cancel", "Cancel")))
                        .setEphemeral(true)
                        .queue();
            }
            else {
                event.reply("You aren't registered!").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if(event.getComponentId().equals("unregister-confirm")) {
            String response = userService.removeUser(event.getUser().getId());
            event.reply(response).setEphemeral(true).queue();
        }
        else if(event.getComponentId().equals("unregister-cancel")) {
            event.reply("BattleTag registration cancelled")
                    .setEphemeral(true).queue();
        }
    }
}
