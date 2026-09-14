package com.ryanh.agent_discord_bot.listener;

import com.ryanh.agent_discord_bot.entity.PostOut;
import com.ryanh.agent_discord_bot.service.NotificationService;
import com.ryanh.agent_discord_bot.service.PostOutService;
import com.ryanh.agent_discord_bot.service.RaidCalendar;
import com.ryanh.agent_discord_bot.utility.EmbedUtility;
import com.ryanh.agent_discord_bot.utility.PostOutFormatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PostOutListener extends ListenerAdapter {

    private final PostOutService postOutService;
    private final RaidCalendar raidCalendar;
    private final NotificationService notificationService;
    private final Map<String, List<String>> daySelections = new ConcurrentHashMap<>();
    private final Map<String, List<String>> deleteSelections = new ConcurrentHashMap<>();

    public PostOutListener(PostOutService postOutService, RaidCalendar raidCalendar,
                           NotificationService notificationService) {
        this.postOutService = postOutService;
        this.raidCalendar = raidCalendar;
        this.notificationService = notificationService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        //Create command
        if(event.getName().equals("postout") && "create".equals(event.getSubcommandName())) {
            event.replyEmbeds(EmbedUtility.info(event.getUser(),
                                    "When do you need to post out?")
                            .build())
                    .addComponents(
                            ActionRow.of(
                                    Button.primary("postout-thisreset", "Week of ("
                                            + raidCalendar.getNextRaidWeekStartDate().getMonthValue() + "/"
                                            + raidCalendar.getNextRaidWeekStartDate().getDayOfMonth() + ")"),
                                    Button.primary("postout-nextreset", "Week of ("
                                            + raidCalendar.getNextRaidWeekStartDate()
                                            .plusWeeks(1).getMonthValue() + "/"
                                            + raidCalendar.getNextRaidWeekStartDate()
                                            .plusWeeks(1).getDayOfMonth() + ")"),
                                    Button.primary("postout-futurereset", "Later Week")
                            ),
                            ActionRow.of(
                                    Button.danger("postout-create-cancel", "Cancel")
                            )
                    )
                    .setEphemeral(true)
                    .queue();
        }

        //View command
        else if(event.getName().equals("postout") && "view".equals(event.getSubcommandName())) {

            List<PostOut> postOutList = postOutService.getUsersPostOuts(event.getUser().getId());
            if(postOutList.isEmpty()) {
                event.replyEmbeds(EmbedUtility.error(event.getUser(),
                        "You don't have any post outs created")
                        .build())
                        .queue();
            }
            else {
                PostOutService.PostOutsByWeek result = postOutService.viewPostOuts(event.getUser().getId());

                EmbedBuilder embed = EmbedUtility.info(event.getUser(), "Here's a list of your post outs:");

                if(!result.thisWeek().isEmpty()) {
                    embed.addField("🗓️ This Week:", String.join("\n", result.thisWeek()), true);
                }
                else {
                    embed.addField("🗓️ This Week:", "None", true);
                }
                if (!result.futureWeek().isEmpty()) {
                    embed.addField("🗓️ Later Weeks:", String.join("\n", result.futureWeek()), true);
                }
                else {
                    embed.addField("🗓️ Later Weeks:", "None", true);
                }

                event.replyEmbeds(embed.build())
                        .setEphemeral(true)
                        .queue();
            }
        }

        //Delete command
        else if (event.getName().equals("postout") && "delete".equals(event.getSubcommandName())) {
            List<PostOut> postOutList = postOutService.getUsersPostOuts(event.getUser().getId());
            if(postOutList.isEmpty()) {
                event.replyEmbeds(EmbedUtility.error(event.getUser(),
                        "You don't have any post outs created")
                        .build())
                        .queue();
            }
            else {
                StringSelectMenu.Builder menu = StringSelectMenu.create("postout-selectdelete")
                        .setMinValues(1);

                for(PostOut post: postOutList) {
                    menu.addOption(
                            PostOutFormatter.formatDate(post),
                            String.valueOf(post.getId())
                    );
                }
                menu.setMaxValues(menu.getOptions().size());

                event.replyEmbeds(EmbedUtility.info(event.getUser(),
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
        //User selected options on the day selection dropdown for "This Reset".
        if(event.getComponentId().equals("postout-selectdays")) {
            daySelections.put(event.getUser().getId(), event.getValues());
            event.deferEdit().queue();
        }
        //User selected options in the delete command dropdown.
        else if(event.getComponentId().equals("postout-selectdelete")) {
            deleteSelections.put(event.getUser().getId(), event.getValues());
            event.deferEdit().queue();
        }

    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        //User clicked "This Reset" button on create command.
        if(event.getComponentId().equals("postout-thisreset")) {
            StringSelectMenu.Builder menu = StringSelectMenu.create("postout-selectdays")
                    .setMinValues(1);

            for (RaidCalendar.RaidDay day: raidCalendar.validMenuOptions()) {
                menu.addOption(day.label(), day.date().toString(),
                        PostOutFormatter.formatShortDate(day.date()));
            }
            menu.setMaxValues(menu.getOptions().size());

            event.editMessageEmbeds(EmbedUtility.info(event.getUser(),
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
        else if(event.getComponentId().equals("postout-nextreset")) {
            StringSelectMenu.Builder menu = StringSelectMenu.create("postout-selectdays")
                    .setMinValues(1);

            for (RaidCalendar.RaidDay day: raidCalendar.getNextWeekRaidDays()) {
                menu.addOption(day.label(), day.date().toString(),
                        PostOutFormatter.formatShortDate(day.date()));
            }
            menu.setMaxValues(menu.getOptions().size());

            event.editMessageEmbeds(EmbedUtility.info(event.getUser(),
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
        //User clicked "Future Reset" button on create command.
        else if (event.getComponentId().equals("postout-futurereset")) {
            TextInput dateInput = TextInput.create("dateInput", TextInputStyle.SHORT)
                    .setPlaceholder("Example format: 4/20, 6/7, 6/9")
                    .setRequired(true)
                    .build();
            TextInput noteInput = TextInput.create("noteInput", TextInputStyle.SHORT)
                    .setPlaceholder("Optional note")
                    .setRequired(false)
                    .build();

            Modal modal = Modal.create("postout-futurereset-datemodal", "Create a Post Out")
                    .addComponents(
                            Label.of("Enter month/day, separated by commas", dateInput),
                            Label.of("Note", noteInput)
                    ).build();

            event.replyModal(modal).queue();
        }
        //User clicked cancel button on create command.
        else if(event.getComponentId().equals("postout-create-cancel")) {
            daySelections.remove(event.getUser().getId());
            event.editMessageEmbeds(EmbedUtility.error(event.getUser(),
                            "Post Out canceled").build())
                    .setComponents().queue();
        }
        //User clicked confirm button on create command.
        else if(event.getComponentId().equals("postout-create-confirm")) {
            List<String> confirmedDays = daySelections.get(event.getUser().getId());

            //User tried to click "Confirm" without selecting any days.
            if (confirmedDays == null || confirmedDays.isEmpty()) {
                event.editMessageEmbeds(EmbedUtility.error(event.getUser(),
                                "Please select at least one day:").build())
                        .queue();
            }
            else {
                event.editMessageEmbeds(EmbedUtility.info(event.getUser(),
                                        "(Optional) Do you want to add a note?")
                                .build())
                        .setComponents(
                                ActionRow.of(
                                        Button.primary("postout-addnote", "Yes"),
                                        Button.primary("postout-skipnote", "Skip")
                                )
                        )
                        .queue();
            }
        }
        //User clicked Add Note button after confirming dates
        else if (event.getComponentId().equals("postout-addnote")) {
            TextInput noteInput = TextInput.create("noteInput", TextInputStyle.SHORT)
                    .setPlaceholder("Leave blank for no note")
                    .setRequired(false)
                    .build();

            Modal modal = Modal.create("postout-notemodal", "Add a note")
                    .addComponents(
                            Label.of("Note", noteInput)
                    ).build();

            event.replyModal(modal).queue();
        }
        //User clicked Skip Note button after confirming dates
        else if(event.getComponentId().equals("postout-skipnote")) {
            List<String> confirmedDays = daySelections.remove(event.getUser().getId());

            //Nothing left to submit, so the button was clicked twice.
            if(confirmedDays == null || confirmedDays.isEmpty()) {
                event.editMessageEmbeds(EmbedUtility.error(event.getUser(),
                                "That post out was already submitted.").build())
                        .setComponents()
                        .queue();
                return;
            }

            PostOutService.InsertResult result = postOutService.insertPostOut(event.getUser().getId(),
                    raidCalendar.convertDatesFromSelectMenu(confirmedDays));

            event.editMessageEmbeds(notifyAndBuildInsertEmbed(event.getUser(), result, ""))
                    .setComponents()
                    .queue();
        }
        //User clicked cancel button on the delete command.
        else if(event.getComponentId().equals("postout-delete-cancel")) {
            deleteSelections.remove(event.getUser().getId());
            event.editMessageEmbeds(EmbedUtility.error(event.getUser(),
                            "❌ Delete canceled").build())
                    .setComponents().queue();
        }
        //User clicked confirm button on the delete command.
        else if(event.getComponentId().equals("postout-delete-confirm")) {
            List<String> confirmedDeleteIds = deleteSelections.remove(event.getUser().getId());

            //User clicked "Confirm" without selecting any post outs, or clicked it twice.
            if(confirmedDeleteIds == null || confirmedDeleteIds.isEmpty()) {
                event.editMessageEmbeds(EmbedUtility.error(event.getUser(),
                                "Please select at least one post out to delete:").build())
                        .queue();
                return;
            }

            //Menu values are the database IDs this listener put there, so they parse cleanly.
            List<Integer> deleteIds = confirmedDeleteIds.stream()
                    .map(Integer::parseInt)
                    .toList();

            PostOutService.DeleteResult result = postOutService.deletePostOut(event.getUser().getId(),
                    deleteIds);

            EmbedBuilder embed = EmbedUtility.confirm(event.getUser(), "Results");

            //Selected post outs can already be gone (cleanup job, stale menu), which would
            //leave this field blank and make JDA reject the embed.
            if(result.deleted().isEmpty()) {
                embed.addField("🗓️ Deleted:", "None", true);
            }
            else {
                embed.addField("🗓️ Deleted:", String.join("\n", result.deleted()), true);
            }
            if(result.remaining().isEmpty()) {
                embed.addField("🗓️ Remaining:", "None", true);
            }
            else {
                embed.addField("🗓️ Remaining:",
                        String.join("\n", result.remaining()), true);
            }

            event.editMessageEmbeds(embed.build())
                    .setComponents()
                    .queue();
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        //User clicked submit on "Future Reset" modal.
        if(event.getModalId().equals("postout-futurereset-datemodal")) {
            String dateInput = event.getValue("dateInput").getAsString();
            String noteInput = event.getValue("noteInput").getAsString();

            try {
                PostOutService.InsertResult result = postOutService.insertPostOut(event.getUser().getId(),
                        raidCalendar.convertDatesFromModal(dateInput));

                event.editMessageEmbeds(notifyAndBuildInsertEmbed(event.getUser(), result, noteInput))
                        .setComponents()
                        .queue();
            }
            catch (IllegalArgumentException e) {
                event.editMessageEmbeds(EmbedUtility.error(event.getUser(), e.getMessage()).build())
                        .queue();
            }

        }
        else if (event.getModalId().equals("postout-notemodal")) {
            String noteInput = event.getValue("noteInput").getAsString();

            List<String> confirmedDays = daySelections.remove(event.getUser().getId());

            //Nothing left to submit, so the modal was submitted twice.
            if(confirmedDays == null || confirmedDays.isEmpty()) {
                event.editMessageEmbeds(EmbedUtility.error(event.getUser(),
                                "That post out was already submitted.").build())
                        .setComponents()
                        .queue();
                return;
            }

            PostOutService.InsertResult result = postOutService.insertPostOut(event.getUser().getId(),
                    raidCalendar.convertDatesFromSelectMenu(confirmedDays));

            event.editMessageEmbeds(notifyAndBuildInsertEmbed(event.getUser(), result, noteInput))
                    .setComponents()
                    .queue();
        }
    }

    /**
     * Shared by all three create paths. Tells the post out channel about any dates that were
     * actually added, then builds the result embed shown to the member.
     */
    private MessageEmbed notifyAndBuildInsertEmbed(User user, PostOutService.InsertResult result, String note) {
        if(!result.added().isEmpty()) {
            notificationService.sendPostOutCreation(user.getId(), result.added(), note);
        }

        EmbedBuilder embed = EmbedUtility.confirm(user, "Post out results:");
        if (!result.added().isEmpty()) {
            embed.addField("🗓️ Added:", String.join("\n", result.added()), true);
        }
        if (!result.duplicates().isEmpty()) {
            embed.addField("⚠️ Already exists:", String.join("\n", result.duplicates()), true);
        }

        return embed.build();
    }
}