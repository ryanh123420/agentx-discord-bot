package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.service.PostOutService;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

@Component
public class PostOutListener extends ListenerAdapter {

    private final PostOutService postOutService;

    public PostOutListener(PostOutService postOutService) {
        this.postOutService = postOutService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(event.getName().equals("postout")) {
            LocalDate tuesdayReset = getReset();

            event.reply("When do you need to post out?")
                    .addComponents(ActionRow.of(
                            StringSelectMenu.create("reset")
                                    .addOption("This Reset", "currentReset",
                                            "Week of "
                                            + tuesdayReset.getMonthValue() + "/"
                                            + tuesdayReset.getDayOfMonth())
                                    .addOption("Future Date", "futureReset",
                                            "After this reset at a later date.")
                                    .build()))
                    .setEphemeral(true)
                    .queue();
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if(event.getComponentId().equals("reset")) {
            String selected = event.getValues().getFirst();
            if(selected.equals("currentReset")) {
                //placeholder reply
                event.reply("You posted out for this reset!!!").setEphemeral(true).queue();
            }
            else if (selected.equals("futureReset")) {
                //placeholder reply
                event.reply("You posted out for a future reset!!!").setEphemeral(true).queue();
            }
        }
    }


    public LocalDate getReset() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
        if(now.getDayOfWeek() == DayOfWeek.THURSDAY && now.getHour() >= 21) {
            now = now.plusWeeks(1);
        }
        else if(now.getDayOfWeek().getValue() > DayOfWeek.THURSDAY.getValue()
                || now.getDayOfWeek().getValue() < DayOfWeek.TUESDAY.getValue()) {
            now = now.plusWeeks(1);
        }

        return  now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.TUESDAY));
    }
}
