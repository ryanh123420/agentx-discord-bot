package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.service.UserService;
import com.ryanh.agent_discord_bot.utility.MessageEmbeds;
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
                event.replyEmbeds(MessageEmbeds.info("Unregister BattleTag",
                                "Are you sure you want to unregister your BattleTag?")
                                .build())
                        .addComponents(ActionRow.of(
                                Button.primary("unregister-confirm","Confirm"),
                                Button.danger("unregister-cancel", "Cancel")))
                        .setEphemeral(true)
                        .queue();
            }
            else {
                event.replyEmbeds(MessageEmbeds.error("Unregister BattleTag",
                        "You aren't registered!")
                                .build())
                        .setEphemeral(true)
                        .queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if(event.getComponentId().equals("unregister-confirm")) {
            String response = userService.removeUser(event.getUser().getId());
            event.replyEmbeds(MessageEmbeds.confirm("Unregister BattleTag", response)
                            .build())
                    .setEphemeral(true).
                    queue();
        }
        else if(event.getComponentId().equals("unregister-cancel")) {
            event.replyEmbeds(MessageEmbeds.error("Unregister BattleTag",
                            "BattleTag registration cancelled")
                            .build())
                    .setEphemeral(true)
                    .queue();
        }
    }
}
