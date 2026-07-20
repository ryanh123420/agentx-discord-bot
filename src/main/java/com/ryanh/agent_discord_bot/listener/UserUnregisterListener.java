package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.service.MemberService;
import com.ryanh.agent_discord_bot.utility.EmbedUtility;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
public class UserUnregisterListener extends ListenerAdapter {
    private final MemberService memberService;

    public UserUnregisterListener(MemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(event.getName().equals("unregister")) {
            if(memberService.checkIfRegistered(event.getUser().getId())) {
                event.replyEmbeds(EmbedUtility.info(event.getUser(),
                                "Are you sure you want to unregister your BattleTag?")
                                .build())
                        .addComponents(ActionRow.of(
                                Button.primary("unregister-confirm","Confirm"),
                                Button.danger("unregister-cancel", "Cancel")))
                        .setEphemeral(true)
                        .queue();
            }
            else {
                event.replyEmbeds(EmbedUtility.error(event.getUser(),
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
            String response = memberService.removeUser(event.getUser().getId());
            event.replyEmbeds(EmbedUtility.confirm(event.getUser(), response)
                            .build())
                    .setEphemeral(true).
                    queue();
        }
        else if(event.getComponentId().equals("unregister-cancel")) {
            event.replyEmbeds(EmbedUtility.error(event.getUser(),
                            "BattleTag registration cancelled")
                            .build())
                    .setEphemeral(true)
                    .queue();
        }
    }
}
