package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.entity.PostOut;
import com.ryanh.agent_discord_bot.service.PostOutService;
import com.ryanh.agent_discord_bot.utility.EmbedUtility;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PostOutListener extends ListenerAdapter {

    private final PostOutService postOutService;
    private final Map<String, List<String>> daySelections = new HashMap<>();
    private final Map<String, List<String>> deleteSelections = new HashMap<>();

    public PostOutListener(PostOutService postOutService) {
        this.postOutService = postOutService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        //Create command
        if(event.getName().equals("postout") && "create".equals(event.getSubcommandName())) {
            event.replyEmbeds(EmbedUtility.info("Create a Post Out",
                                    "When do you need to post out?")
                            .build())
                    .addComponents(
                            ActionRow.of(
                                    StringSelectMenu.create("postout-selectreset")
                                    .addOption("This Reset", "postout-thisreset",
                                            "Week of "
                                            + postOutService.getReset().getMonthValue() + "/"
                                            + postOutService.getReset().getDayOfMonth())
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

            List<PostOut> postOutList = postOutService.getUsersPostOuts(event.getUser().getId());
            if(postOutList.isEmpty()) {
                event.replyEmbeds(EmbedUtility.error("View your Post Outs",
                        "You don't have any post outs created")
                        .build())
                        .queue();
            }
            else {
                List<String> response = postOutService.printListOfPostOuts(postOutList);
                StringBuilder stringBuilder = new StringBuilder();
                for(String s: response) {
                    stringBuilder.append(s).append("\n");
                }

                event.replyEmbeds(EmbedUtility.info("View your Post Outs",
                                        "Here's the list of your post out dates: \n" + stringBuilder)
                                .build())
                        .setEphemeral(true)
                        .queue();
            }
        }

        //Delete command
        else if (event.getName().equals("postout") && "delete".equals(event.getSubcommandName())) {
            List<PostOut> postOutList = postOutService.getUsersPostOuts(event.getUser().getId());
            if(postOutList.isEmpty()) {
                event.replyEmbeds(EmbedUtility.error("Delete Post Outs",
                        "You don't have any post outs created")
                        .build())
                        .queue();
            }
            else {
                StringSelectMenu.Builder menu = StringSelectMenu.create("postout-selectdelete")
                        .setMinValues(1);

                for(PostOut post: postOutList) {
                    menu.addOption(
                            postOutService.printSinglePostOut(post),
                            String.valueOf(post.getId())
                    );
                }
                menu.setMaxValues(menu.getOptions().size());

                event.replyEmbeds(EmbedUtility.info("Delete Post Outs",
                                "Select Post Outs to delete")
                                .build())
                        .setComponents(
                                ActionRow.of(
                                        menu.build()
                                ),
                                ActionRow.of(
                                        Button.primary("postout-delete-confirm", "Confirm"),
                                        Button.danger("postout-delete-cancel", "Cancel")
                                )
                        )
                        .setEphemeral(true)
                        .queue();
            }
        }
    }


    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if(event.getComponentId().equals("postout-selectreset")) {
            String selected = event.getValues().getFirst();
            if(selected.equals("postout-thisreset")) {
                StringSelectMenu.Builder menu = StringSelectMenu.create("postout-thisreset-selectdays")
                        .setMinValues(1);

                for (PostOutService.RaidDay day: postOutService.validMenuOptions()) {
                    menu.addOption(day.label(), day.value(),
                            day.date().getMonth().getValue() + "/" + day.date().getDayOfMonth());
                }
                menu.setMaxValues(menu.getOptions().size());

                event.editMessageEmbeds(EmbedUtility.info("Create a Post Out",
                                "Select which days:").build())
                        .setComponents(
                                ActionRow.of(
                                        menu.build()
                                ),
                                ActionRow.of(
                                        Button.primary("postout-create-confirm", "Confirm"),
                                        Button.danger("postout-create-cancel", "Cancel")
                                )
                        ).queue();
            }
            else if (selected.equals("postout-futurereset")) {
                TextInput dateInput = TextInput.create("dateInput", TextInputStyle.SHORT)
                        .setPlaceholder("Example format: 4/20, 6/7, 6/9")
                        .setRequired(true)
                        .build();

                Modal modal = Modal.create("postout-futurereset-datemodal", "Create a Post Out")
                        .addComponents(Label.of("Enter month/day, separated by commas", dateInput))
                        .build();

                event.replyModal(modal).queue();
            }

        }
        else if(event.getComponentId().equals("postout-thisreset-selectdays")) {
            daySelections.put(event.getUser().getId(), event.getValues());
            event.deferEdit().queue();
        }
        else if(event.getComponentId().equals("postout-selectdelete")) {
            deleteSelections.put(event.getUser().getId(), event.getValues());
            event.deferEdit().queue();
        }

    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if(event.getComponentId().equals("postout-create-cancel")) {
            daySelections.remove(event.getUser().getId());
            event.editMessageEmbeds(EmbedUtility.error("Create a Post Out",
                            "Post Out canceled").build())
                    .setComponents().queue();
        }
        else if(event.getComponentId().equals("postout-create-confirm")) {
            List<String> confirmedDays = daySelections.remove(event.getUser().getId());

            String response = postOutService.insertPostOut(event.getUser().getId(),
                    postOutService.getDatesFromSelect(confirmedDays));

            event.editMessageEmbeds(EmbedUtility.confirm("Create a Post Out", response).build())
                    .setComponents()
                    .queue();
        }
        else if(event.getComponentId().equals("postout-delete-cancel")) {
            deleteSelections.remove(event.getUser().getId());
            event.editMessageEmbeds(EmbedUtility.error("Delete a Post Out",
                            "Delete canceled").build())
                    .setComponents().queue();
        }
        else if(event.getComponentId().equals("postout-delete-confirm")) {
            List<String> confirmedDeleteIds = deleteSelections.remove(event.getUser().getId());

            String response = postOutService.deletePostOut(event.getUser().getId(),
                    confirmedDeleteIds);

            event.editMessageEmbeds(EmbedUtility.confirm("Delete a Post Out", response).build())
                    .setComponents()
                    .queue();
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if(event.getModalId().equals("postout-futurereset-datemodal")) {
            String dateInput = event.getValue("dateInput").getAsString();

            String response = postOutService.insertPostOut(event.getUser().getId(),
                    postOutService.getDatesFromModal(dateInput));

            event.editMessageEmbeds(EmbedUtility.confirm("Create a Post Out", response).build())
                    .setComponents()
                    .queue();
        }
    }
}
