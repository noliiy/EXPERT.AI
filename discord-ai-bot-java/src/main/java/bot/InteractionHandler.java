package bot;

import bot.api.OpportunityClient;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;
import storage.FeedbackDAO;
import storage.OpportunityDAO;
import storage.StudentDAO;
import net.dv8tion.jda.api.EmbedBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

import static net.dv8tion.jda.api.interactions.components.text.TextInputStyle.PARAGRAPH;



/**
 * Handles all interactions from Discord UI components such as buttons and select menus.
 * This includes profile actions, matching jobs, and chatbot prompts.
 */
public class InteractionHandler extends ListenerAdapter {

    /**
     * Responds to button clicks based on their component ID.
     * Each button triggers a different workflow depending on its ID.
     */


    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("feedback_modal")) {
            String feedbackText = event.getValue("feedback_input").getAsString();
            long userId = event.getUser().getIdLong();

            String discordId = event.getUser().getId();

            FeedbackDAO dao = new FeedbackDAO();
            try {
                dao.insertFeedback(feedbackText, userId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Yıldızları gönder
            event.getUser().openPrivateChannel().queue(channel -> {
                channel.sendMessage("Thanks for your feedback! Please rate us:")
                        .addActionRow(
                                Button.secondary("star_1", "⭐"),
                                Button.secondary("star_2", "⭐⭐"),
                                Button.secondary("star_3", "⭐⭐⭐"),
                                Button.secondary("star_4", "⭐⭐⭐⭐"),
                                Button.secondary("star_5", "⭐⭐⭐⭐⭐")
                        ).queue();
            });

            event.reply("✅ Your feedback has been received!").setEphemeral(true).queue();
        }
    }




    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();              // Unique identifier of the button clicked
        String userId = event.getUser().getId();         // Discord user ID

        String componentId = event.getComponentId();

        if(componentId.startsWith("star_")) {
            int stars = Integer.parseInt(componentId.split("_")[1]);
            String userID = event.getUser().getId();

            FeedbackDAO dao = new FeedbackDAO();
            dao.updateStarsByDiscordId(userID, stars);


            event.reply("⭐ Thanks! Your rating has been saved.").setEphemeral(true).queue(msg -> CommandHandler.showMainMenu(event.getUser()));

        }


        switch (id) {
            case "start" -> {
                // Show main menu after the user clicks the start button
                event.reply("📬 Check your DMs to continue.")
                        .setEphemeral(true)
                        .queue(success -> CommandHandler.showMainMenu(event.getUser()));
            }

            case "delete_profile" -> {
                // Delete user profile and all related opportunities
                event.deferReply(true).queue();
                try {
                    // 🗑️ First, delete all opportunities linked to this user
                    OpportunityDAO.deleteAllForUser(userId);
                    // 👤 Then, delete the user profile
                    boolean deleted = StudentDAO.deleteProfileByDiscordId(userId);
                    if (deleted) {
                        event.getHook().sendMessage("✅ Your profile has been successfully deleted.")
                                .queue(msg -> CommandHandler.showMainMenu(event.getUser()));
                    } else {
                        event.getHook().sendMessage("⚠️ No profile was found to delete.")
                                .queue(msg -> CommandHandler.showMainMenu(event.getUser()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    event.getHook().sendMessage("❌ An error occurred while trying to delete your profile.")
                            .queue(msg -> CommandHandler.showMainMenu(event.getUser()));
                }
            }
            case "feedback" -> {
                Modal feedbackModal = Modal.create("feedback_modal", "📝 Bot Feedback")
                        .addActionRow(
                                TextInput.create("feedback_input", "Your thoughts", PARAGRAPH)
                                        .setPlaceholder("Tell us what you think about the bot...")
                                        .setRequired(true)
                                        .build()
                        )
                        .build();
                event.replyModal(feedbackModal).queue();
            }





            case "gpt_ask" -> {
                // Prompt user to type a GPT question with contextual explanation and usage conditions
                event.reply("""
        🤖 **Welcome to Jobify CVUT – your personal AI career assistant!**
        
        You can ask me questions using `!ask <your question>`.
        I’ll use your saved **profile** and **matched opportunities** to guide you.

        ✅ Make sure you’ve already:
        • Completed your profile (Name, Email, Skills, Career Interest)
        • Clicked the **🎯 Match Me** button to find suitable job offers

        Once you’ve done that, I can:
        • 🔍 Recommend the best-fit job from your saved opportunities  
        • 🧠 Suggest skills to improve based on your goals  
        • 📄 Help you improve your CV and job applications  
        • ❓ Answer anything about internships, tech roles, or FIT ČVUT career tips

        _📌 Best prompt for accurate job matching:_  
        `!ask Based on my profile and the opportunities below, please recommend the one that fits me best`

        _💡 Example:_  
        `!ask Which opportunity suits my backend experience more?`
        """).setEphemeral(true)
                        .queue(msg -> CommandHandler.showMainMenu(event.getUser()));
            }


            case "view_profile" -> {
                event.deferReply(true).queue();
                try {
                    var data = StudentDAO.getStudentProfile(userId);
                    if (data == null || data.isEmpty()) {
                        event.getHook().sendMessage("⚠️ You don't have a profile yet. Select 'Create Profile' to start.")
                                .queue(msg -> CommandHandler.showMainMenu(event.getUser()));
                    } else {
                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setTitle("👤 Your Profile");
                        embed.setColor(0x5865F2); // Discord blurple

                        if (data.get("Name") != null)
                            embed.addField("🧑 Name", data.get("Name"), false);
                        if (data.get("Email") != null)
                            embed.addField("📧 Email", data.get("Email"), false);
                        if (data.get("Skills") != null)
                            embed.addField("🛠️ Skills", data.get("Skills"), false);
                        if (data.get("Career Interest") != null)
                            embed.addField("🎯 Career Interests", data.get("Career Interest"), false);

                        event.getHook().sendMessageEmbeds(embed.build())
                                .queue(msg -> CommandHandler.showMainMenu(event.getUser()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    event.getHook().sendMessage("❌ Error retrieving profile.")
                            .queue(msg -> CommandHandler.showMainMenu(event.getUser()));
                }
            }

            case "create_profile" -> {
                // Ask if the user has a resume (CV)
                event.reply("\uD83D\uDCC4 Do you have a resume (CV)?")
                        .setEphemeral(true)
                        .setActionRow(
                                Button.success("cv_yes", "✅ Yes"),
                                Button.danger("cv_no", "❌ No")
                        ).queue();
            }

            case "cv_yes" -> {
                // Ask the user to upload a PDF
                event.reply("\uD83D\uDCC4 Please upload your resume as a PDF.")
                        .setEphemeral(true).queue();
            }

            case "cv_no" -> {
                // Initialize profile and start registration (email step)
                event.deferReply(true).queue();
                try {
                    StudentDAO.upsertStudent(null, null, null, null, userId);
                    CommandHandler.startRegistrationFor(userId);
                    event.getHook().sendMessage("\uD83D\uDCE7 Please enter your email address.").queue();
                } catch (Exception e) {
                    e.printStackTrace();
                    event.getHook().sendMessage("❌ Failed to initialize profile setup.").queue();
                }
            }

            case "match_jobs" -> {
                // Match job opportunities based on profile data
                event.deferReply(true).queue();
                try {
                    var profile = StudentDAO.getStudentProfile(userId);

                    if (profile == null || profile.get("Skills") == null || profile.get("Career Interest") == null) {
                        event.getHook().sendMessage("❗ You need to complete your profile first.")
                                .queue(msg -> CommandHandler.showMainMenu(event.getUser()));
                        return;
                    }

                    String skills = profile.get("Skills");
                    String interest = profile.get("Career Interest");

                    Set<OpportunityClient.Opportunity> results =
                            OpportunityClient.searchMultipleKeywords(skills + " " + interest);

                    if (results.isEmpty()) {
                        event.getHook().sendMessage("😢 No opportunities found for your profile.")
                                .queue(msg -> CommandHandler.showMainMenu(event.getUser()));
                    } else {
                        event.getHook().sendMessage("🎯 Found " + results.size() + " opportunities for you:")
                                .queue(msg -> {
                                    for (var opp : results) {
                                        try {
                                            if (!OpportunityDAO.existsForUser(opp, userId)) {
                                                OpportunityDAO.insertForUser(opp, userId);

                                            }
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }

                                        if (!opp.url.isBlank()) {
                                            event.getChannel().sendMessageEmbeds(opp.toEmbed())
                                                    .addActionRow(Button.link(opp.url, "📩 Apply"))
                                                    .queue();
                                        } else {
                                            event.getChannel().sendMessageEmbeds(opp.toEmbed()).queue();
                                        }
                                    }
                                    // Show menu after listing jobs
                                    CommandHandler.showMainMenu(event.getUser());
                                });
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    event.getHook().sendMessage("❌ Error matching opportunities: " + e.getMessage())
                            .queue(msg -> CommandHandler.showMainMenu(event.getUser()));
                }
            }

            // Default case for unknown buttons
            default -> event.reply("\u26A0\uFE0F Unrecognized button.")
                    .setEphemeral(true).queue();
        }
    }

    /**
     * Handles dropdown (select menu) interaction events from the user.
     */
    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String userId = event.getUser().getId();

        switch (event.getComponentId()) {
            case "select_skills" -> {
                // Store selected skills to user profile
                List<String> values = event.getValues();
                String skills = String.join(", ", values);
                try {
                    StudentDAO.upsertStudent(null, null, skills, null, userId);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                event.reply("✅ Skills saved.").setEphemeral(true).queue();

                // Prompt for position selection
                StringSelectMenu posMenu = StringSelectMenu.create("select_position")
                        .setPlaceholder("\uD83D\uDCCC Choose your preferred position")
                        .setMaxValues(5)
                        .addOption("Backend", "backend")
                        .addOption("Frontend", "frontend")
                        .addOption("Full Stack", "fullstack")
                        .addOption("Mobile", "mobile")
                        .addOption("QA", "qa")
                        .addOption("DevOps", "devops")
                        .addOption("Data Science", "data")
                        .build();

                event.getChannel().sendMessage("\uD83D\uDCDD What type of position are you looking for?")
                        .setActionRow(posMenu)
                        .queue();
            }

            case "select_position" -> {
                event.deferReply(true).queue();

                List<String> selectedPositions = event.getValues();
                String joined = String.join(", ", selectedPositions);

                try {
                    StudentDAO.upsertStudent(null, null, null, joined, userId); // Save positions

                    event.getHook().sendMessage("✅ Positions saved: " + joined).queue(msg -> {

                        // Crear un pequeño delay antes de abrir el DM
                        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                        scheduler.schedule(() -> {
                            event.getUser().openPrivateChannel().queue(dm -> {
                                dm.sendMessage("✅ Your profile has been saved! What would you like to do next?")
                                        .addActionRow(
                                                Button.primary("gpt_ask", "🤖 Ask GPT"),
                                                Button.primary("view_profile", "👤 View Profile"),
                                                Button.success("create_profile", "📝 Create Profile")
                                        )
                                        .addActionRow(
                                                Button.secondary("match_jobs", "🎯 Match Me"),
                                                Button.danger("delete_profile", "🗑️ Delete Profile"),
                                                Button.primary("feedback","⭐ Feedback")

                                        )
                                        .queue();
                            });
                            scheduler.shutdown(); // Cerramos el scheduler después de usarlo
                        }, 1500, TimeUnit.MILLISECONDS);

                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    event.getHook().sendMessage("❌ Error saving positions. Please try again.").queue();
                }
            }



            // Default case for unknown select menus
            default -> event.reply("⚠️ Unknown select menu.")
                    .setEphemeral(true)
                    .queue();
        }
    }
}
