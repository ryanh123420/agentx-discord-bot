package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.service.PostOutService;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PostOutListener extends ListenerAdapter {

    private final PostOutService postOutService;
    private final Map<String, List<String>> daySelections = new HashMap<>();

    public PostOutListener(PostOutService postOutService) {
        this.postOutService = postOutService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(event.getName().equals("postout")) {
            LocalDate reset = getReset();

            event.reply("When do you need to post out?")
                    .addComponents(
                            ActionRow.of(
                                    StringSelectMenu.create("reset")
                                    .addOption("This Reset", "currentReset",
                                            "Week of "
                                            + reset.getMonthValue() + "/"
                                            + reset.getDayOfMonth())
                                    .addOption("Future Date", "futureReset",
                                            "After this reset at a later date.")
                                    .build()),
                            ActionRow.of(
                                    Button.danger("postout-cancel", "Cancel")
                            )
                    )
                    .setEphemeral(true)
                    .queue();
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if(event.getComponentId().equals("reset")) {
            String selected = event.getValues().getFirst();
            if(selected.equals("currentReset")) {
                StringSelectMenu.Builder menu = StringSelectMenu.create("postoutdays")
                        .setMinValues(1);
                LocalDate reset = getReset();

                if(!isRaidDayPassed(DayOfWeek.TUESDAY)) {
                    menu.addOption("Tuesday", "tuesday",
                            reset.getMonthValue() + "/" + reset.getDayOfMonth());
                }
                if(!isRaidDayPassed(DayOfWeek.WEDNESDAY)) {
                    menu.addOption("Wednesday", "wednesday",
                            reset.plusDays(1).getMonthValue()
                                    + "/" + reset.plusDays(1).getDayOfMonth());
                }
                if(!isRaidDayPassed(DayOfWeek.THURSDAY)) {
                    menu.addOption("Thursday", "thursday",
                            reset.plusDays(2).getMonthValue()
                                    + "/" + reset.plusDays(2).getDayOfMonth());
                }
                menu.setMaxValues(menu.getOptions().size());

                event.editMessage("Select which days:")
                        .setComponents(
                                ActionRow.of(
                                        menu.build()
                                ),
                                ActionRow.of(
                                        Button.primary("postout-confirm", "Confirm"),
                                        Button.danger("postout-cancel", "Cancel")
                                )
                        ).queue();
            }
            else if (selected.equals("futureReset")) {
                TextInput dateInput = TextInput.create("dateinput", TextInputStyle.SHORT)
                        .setPlaceholder("Example format: 4/20, 6/7, 6/9")
                        .setRequired(true)
                        .build();

                Modal modal = Modal.create("postout-modal", "Post Out")
                        .addComponents(Label.of("Enter month/day, separated by commas", dateInput))
                        .build();

                event.replyModal(modal).queue();
            }

        }
        else if (event.getComponentId().equals("postoutdays")) {
            daySelections.put(event.getUser().getId(), event.getValues());
            event.deferEdit().queue();
        }

    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if(event.getComponentId().equals("postout-cancel")) {
            daySelections.remove(event.getUser().getId());
            event.editMessage("Post out cancelled.")
                    .setComponents().queue();
        }
        else if(event.getComponentId().equals("postout-confirm")) {
            List<String> confirmedDays = daySelections.remove(event.getUser().getId());
            event.editMessage("Post out created!!!")
                    .setComponents()
                    .queue();
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if(event.getModalId().equals("postout-modal")) {
            String dateInput = event.getValue("dateinput").getAsString();

            //Call PostOutService with the dates

            //placeholder
            event.reply("You did it!").setEphemeral(true).queue();
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

    public boolean isRaidDayPassed(DayOfWeek raidDay) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
        DayOfWeek today = now.getDayOfWeek();

        if(today.getValue() > raidDay.getValue()) {
            return true;
        }
        if(today == raidDay && now.getHour() >= 21) {
            return true;
        }
        return false;
    }
}
