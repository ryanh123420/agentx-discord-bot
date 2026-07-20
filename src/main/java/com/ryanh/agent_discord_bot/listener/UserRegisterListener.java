package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.service.MemberService;
import com.ryanh.agent_discord_bot.utility.EmbedUtility;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
public class UserRegisterListener extends ListenerAdapter {

    private final MemberService memberService;

    public UserRegisterListener(MemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(event.getName().equals("register")) {
            String discordId = event.getUser().getId();
            String input = event.getOption("battletag").getAsString();

            if(!input.matches(".+#\\d+")) {
                event.replyEmbeds(EmbedUtility.error(event.getUser(),
                                "Invalid BattleTag format, double check formatting follows: Name#1234")
                                .build())
                        .setEphemeral(true).queue();
            }

            String response = memberService.insertUser(input, discordId);
            event.replyEmbeds(EmbedUtility.confirm(event.getUser(), response).build())
                    .setEphemeral(true)
                    .queue();
        }
    }
}
