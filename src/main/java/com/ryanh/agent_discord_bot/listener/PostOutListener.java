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
        //Create command
        if(event.getName().equals("postout") && "create".equals(event.getSubcommandName())) {
            LocalDate reset = postOutService.getReset();

            event.reply("When do you need to post out?")
                    .addComponents(
                            ActionRow.of(
                                    StringSelectMenu.create("postout-selectreset")
                                    .addOption("This Reset", "postout-thisreset",
                                            "Week of "
                                            + reset.getMonthValue() + "/"
                                            + reset.getDayOfMonth())
                                    .addOption("Future Date", "postout-futurereset",
                                            "After this reset at a later date.")
                                    .build()),
                            ActionRow.of(
                                    Button.danger("postout-cancel", "Cancel")
                            )
                    )
                    .setEphemeral(true)
                    .queue();
        }

        //View command
        else if(event.getName().equals("postout") && "view".equals(event.getSubcommandName())) {
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

        //Delete command
        else if (event.getName().equals("postout") && "delete".equals(event.getSubcommandName())) {



            //StringSelectMenu.Builder menu = StringSelectMenu.create("test");

            event.reply("Select Post Outs to delete")
                    .setEphemeral(true)
                    .queue();
        }

        //Edit command
        else if (event.getName().equals("postout") && "edit".equals(event.getSubcommandName())) {

            //placeholder
            event.reply("Select Post Outs to edit")
                    .setEphemeral(true)
                    .queue();
        }
    }


    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if(event.getComponentId().equals("postout-selectreset")) {
            String selected = event.getValues().getFirst();
            if(selected.equals("postout-thisreset")) {
                StringSelectMenu.Builder menu = StringSelectMenu.create("postout-thisreset-selectdays")
                        .setMinValues(1);
                LocalDate reset = postOutService.getReset();

                if(!postOutService.isRaidDayPassed(DayOfWeek.TUESDAY)) {
                    menu.addOption("Tuesday", "tuesday",
                            reset.getMonthValue() + "/" + reset.getDayOfMonth());
                }
                if(!postOutService.isRaidDayPassed(DayOfWeek.WEDNESDAY)) {
                    menu.addOption("Wednesday", "wednesday",
                            reset.plusDays(1).getMonthValue()
                                    + "/" + reset.plusDays(1).getDayOfMonth());
                }
                if(!postOutService.isRaidDayPassed(DayOfWeek.THURSDAY)) {
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
            else if (selected.equals("postout-futurereset")) {
                TextInput dateInput = TextInput.create("dateInput", TextInputStyle.SHORT)
                        .setPlaceholder("Example format: 4/20, 6/7, 6/9")
                        .setRequired(true)
                        .build();

                Modal modal = Modal.create("postout-futurereset-datemodal", "Post Out")
                        .addComponents(Label.of("Enter month/day, separated by commas", dateInput))
                        .build();

                event.replyModal(modal).queue();
            }

        }
        else if (event.getComponentId().equals("postout-thisreset-selectdays")) {
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

            String response = postOutService.insertPostOut(event.getUser().getId(),
                    postOutService.getDates(confirmedDays));

            event.editMessage(response)
                    .setComponents()
                    .queue();
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if(event.getModalId().equals("postout-futurereset-datemodal")) {
            String dateInput = event.getValue("dateInput").getAsString();

            String response = postOutService.insertPostOut(event.getUser().getId(), postOutService.getDatesFromString(dateInput));

            event.editMessage(response)
                    .setComponents()
                    .queue();
        }
    }


}
