package bot;

import bot.ai.GPTClient;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

//import net.dv8tion.jda.api.Permission.MESSAGE_MANAGE;

import org.jetbrains.annotations.NotNull;
import storage.StudentDAO;
import util.PdfUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import storage.OpportunityDAO;


import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This class handles message events and command processing in both public and private Discord channels.
 * It also manages the registration flow for student profiles and file upload logic.
 */
public class CommandHandler extends ListenerAdapter {

    private final GPTClient gpt;
    private static final Map<String, Integer> userSteps = new HashMap<>(); // Tracks the registration step per user

    // Begin the registration process for a user
    public static void startRegistrationFor(String userId) {
        userSteps.put(userId, 1);
    }

    public CommandHandler(GPTClient gpt) {
        this.gpt = gpt;
    }

    // Runs when the bot is ready and connected to Discord
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        System.out.println("✅ Bot is online as " + event.getJDA().getSelfUser().getAsTag());
        for (var guild : event.getJDA().getGuilds()) {
            if (guild.getDefaultChannel() instanceof TextChannel channel && channel.canTalk()) {
                channel.sendMessage("👋 **JOBIFY CVUT Bot is now online and ready to help!**")
                        .setActionRow(Button.primary("start", "🚀 Get Started"))
                        .queue();
            }
        }
    }

    // Handles all messages received in public or private channels
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return; // Ignore bot messages

        String userId = event.getAuthor().getId();
        String content = event.getMessage().getContentRaw().trim();

        // Command to check if bot is online
        if (content.equalsIgnoreCase("!status")) {
            event.getChannel().sendMessage("✅ Bot is operational.").queue();
            return;
        }



        // === !clean command ===
        if (content.startsWith("!clean ")) {

            String[] parts = content.split("\\s+");
            if (parts.length != 2) {
                event.getChannel().sendMessage("❗ Usage: `!clean <number>`").queue();
                return;
            }

            int count;
            try {
                count = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("❗ Invalid number.").queue();
                return;
            }

            if (count < 1 || count > 100) {
                event.getChannel().sendMessage("⚠️ Please choose a number between 1 and 100.").queue();
                return;
            }

            event.getChannel().getHistory().retrievePast(count + 1).queue(messages -> {
                event.getChannel().purgeMessages(messages);
                event.getChannel().sendMessage("✅ Deleted " + count + " messages.")
                        .queue(msg -> msg.delete().queueAfter(5, java.util.concurrent.TimeUnit.SECONDS));
            });
            return;
        }


        // Handle private messages (e.g. profile registration and uploading files)
        if (event.isFromType(ChannelType.PRIVATE)) {

            // If user sends a file (resume), handle upload
            if (!event.getMessage().getAttachments().isEmpty()) {
                handlePdfUploadStep(event, userId);
                return;
            }

            // Fetch jobs based on user profile
            if (content.equalsIgnoreCase("!fetch")) {
                try {
                    Map<String, String> profile = StudentDAO.getStudentProfile(userId);

                    if (profile == null || profile.get("Skills") == null || profile.get("Career Interest") == null) {
                        event.getChannel().sendMessage("❗ You need to complete your profile first.").queue();
                        return;
                    }

                    String skills = profile.get("Skills");
                    String interest = profile.get("Career Interest");

                    Set<bot.api.OpportunityClient.Opportunity> results =
                            bot.api.OpportunityClient.searchMultipleKeywords(skills + " " + interest);
                    for (var opp : results) {
                        System.out.println("🔍 Opportunity from API: " + opp.id + " | " + opp.title);
                    }

                    if (results.isEmpty()) {
                        event.getChannel().sendMessage("😢 No opportunities found for your profile.").queue();
                    } else {
                        event.getChannel().sendMessage("🎯 Found " + results.size() + " opportunities for you:").queue();
                        for (var opp : results) {
                            if (!opp.url.isBlank()) {
                                event.getChannel()
                                        .sendMessageEmbeds(opp.toEmbed())
                                        .setActionRow(Button.link(opp.url, "📩 Apply"))
                                        .queue();
                            } else {
                                event.getChannel().sendMessageEmbeds(opp.toEmbed()).queue();
                            }

                            try {
                                if (!storage.OpportunityDAO.existsForUser(opp, userId)) {
                                    storage.OpportunityDAO.insertForUser(opp, userId);
                                    System.out.println("✅ Opportunity saved to DB for " + userId);
                                } else {
                                    System.out.println("🔁 This opportunity already exists for " + userId);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    event.getChannel().sendMessage("❌ Error fetching opportunities: " + e.getMessage()).queue();
                }
                return;
            }

            // Handle !ask command for GPT integration
            if (content.startsWith("!ask ") && gpt != null) {
                String question = content.substring(5).trim();
                event.getChannel().sendTyping().queue();

                userId = event.getAuthor().getId();
                StringBuilder profileInfo = new StringBuilder();
                StringBuilder opportunitiesInfo = new StringBuilder();

                try {
                    // 1. Student profile
                    var profileData = StudentDAO.getStudentProfile(userId);
                    if (profileData != null && !profileData.isEmpty()) {
                        profileInfo.append("📄 Student Profile:\n");
                        profileData.forEach((key, value) -> {
                            if (value != null && !value.isBlank()) {
                                profileInfo.append("- ").append(key).append(": ").append(value).append("\n");
                            }
                        });
                    }

                    // 2. Assigned opportunities
                    var opportunities = OpportunityDAO.getAllForUser(userId);
                    if (opportunities != null && !opportunities.isEmpty()) {
                        opportunitiesInfo.append("📌 Assigned Opportunities:\n");
                        for (var opp : opportunities) {
                            opportunitiesInfo.append(formatOpportunity(opp));
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace(); // Log error, but continue
                }

                // Build the unified prompt
                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content",
                        "You are an AI career assistant in a Discord bot called Jobify CVUT. "
                                + "You help students at FIT ČVUT find the best job opportunities from the opportunities provided. "
                                + "Always be helpful, friendly, and use natural, engaging language. "
                                + "Focus on career guidance, internships, CVs, and job matching based on their profile."
                ));

                StringBuilder fullPrompt = new StringBuilder();
                if (!profileInfo.isEmpty()) {
                    fullPrompt.append("📄 Here is my student profile:\n").append(profileInfo).append("\n");
                }
                if (!opportunitiesInfo.isEmpty()) {
                    fullPrompt.append("📌 These are the job opportunities assigned to me:\n").append(opportunitiesInfo).append("\n");
                }
                fullPrompt.append("💬 My question is: ").append(question);

                messages.add(Map.of("role", "user", "content", fullPrompt.toString()));

                // Debug log
                System.out.println("🧠 Final prompt to GPT:");
                messages.forEach(m -> System.out.println(m.get("role") + " ➜ " + m.get("content")));

                try {
                    String aiReply = gpt.ask(messages, "gpt-3.5-turbo");

                    // 💬 Split response if needed
                    int maxLength = 2000;
                    for (int i = 0; i < aiReply.length(); i += maxLength) {
                        int end = Math.min(aiReply.length(), i + maxLength);
                        event.getChannel().sendMessage(aiReply.substring(i, end)).queue();
                    }

                } catch (IOException e) {
                    event.getChannel().sendMessage("⚠️ OpenAI error: " + e.getMessage()).queue();
                }

                return;
            }








            // Handle step-based registration (step 1: email, step 2: name)
            int step = userSteps.getOrDefault(userId, -1);
            switch (step) {
                case 1 -> {
                    handleEmailStep(event, userId, content);
                    userSteps.put(userId, 2);
                }
                case 2 -> {
                    handleNameStep(event, userId, content);
                    userSteps.remove(userId);
                }
            }
        }
    }

    // Validates and stores email, prompts for name
    public static void handleEmailStep(MessageReceivedEvent event, String userId, String email) {
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            event.getChannel().sendMessage("❗ Invalid email format, please retry.").queue();
            return;
        }
        try {
            StudentDAO.upsertStudent(null, email, null, null, userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        event.getChannel().sendMessage("👤 Please enter your full name.").queue();
    }

    // Stores name and proceeds to skills selection
    public static void handleNameStep(MessageReceivedEvent event, String userId, String name) {
        try {
            StudentDAO.upsertStudent(name, null, null, null, userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        promptSkillsSelection(event);
    }

    // Sends skill selection dropdown
    public static void promptSkillsSelection(MessageReceivedEvent event) {
        StringSelectMenu skillsMenu = StringSelectMenu.create("select_skills")
                .setPlaceholder("💻 Select up to 5 skills")
                .setMaxValues(5)
                .addOption("Java", "java")
                .addOption("Python", "python")
                .addOption("JavaScript", "javascript")
                .addOption("React", "react")
                .addOption("Spring Boot", "spring")
                .addOption("Node.js", "node")
                .addOption("C++", "cpp")
                .addOption("C#", "csharp")
                .addOption("ASP.NET", "aspnet")
                .addOption("SQL", "sql")
                .addOption("Git", "git")
                .addOption("Docker", "docker")
                .addOption("Linux", "linux")
                .addOption("Operating Systems", "os")
                .addOption("Data Science", "data_science")
                .addOption("Machine Learning", "ml")
                .addOption("Deep Learning", "dl")
                .addOption("Recommender Systems", "recommender")
                .addOption("Customer Service", "customer_service")
                .addOption("Security", "security")
                .addOption("Explainability", "explainability")
                .addOption("Software Tool", "software_tool")
                .addOption("Memory", "memory")
                .addOption("Cache Storage", "cache_storage")
                .build();

        event.getChannel()
                .sendMessage("💻 What are your primary skills or technologies?")
                .setActionRow(skillsMenu)
                .queue();
    }


    // Sends position preference dropdown
    public static void promptPositionSelection(MessageReceivedEvent event) {
        StringSelectMenu positionMenu = StringSelectMenu.create("select_position")
                .setPlaceholder("📌 Select up to 5 positions")
                .setMaxValues(5)
                .addOption("Backend", "backend")
                .addOption("Frontend", "frontend")
                .addOption("Full Stack", "fullstack")
                .addOption("Mobile", "mobile")
                .addOption("QA", "qa")
                .addOption("DevOps", "devops")
                .addOption("Data Science", "data")
                .build();
        event.getChannel()
                .sendMessage("🧾 Which type of position are you seeking?")
                .setActionRow(positionMenu)
                .queue();
    }

    // Displays the main action menu (GPT, view, create, match, delete)
    public static void showMainMenu(User user) {

        user.openPrivateChannel().queue(dm -> {
            dm.sendMessage("💼 What would you like to do next?")
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

    }


    // Placeholder for handling a resume description (future enhancement)
    public static void handleResumeDescriptionStep(MessageReceivedEvent event, String userId, String description) {
        try {
            StudentDAO.upsertStudent(null, null, null, null, userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        event.getChannel().sendMessage("📄 Please upload your resume as a PDF file.").queue();
    }

    // Handles resume file upload and extraction
    public void handlePdfUploadStep(MessageReceivedEvent event, String userId) {
        if (event.getMessage().getAttachments().isEmpty()) {
            event.getChannel().sendMessage("❗ Attach a PDF file please.").queue();
            return;
        }

        var attachment = event.getMessage().getAttachments().get(0);
        if (!attachment.getFileName().toLowerCase().endsWith(".pdf")) {
            event.getChannel().sendMessage("❌ Only PDF files are accepted.").queue();
            return;
        }

        File dir = new File("resumes");
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, userId + ".pdf");

        attachment.downloadToFile(out)
                .thenRun(() -> {
                    try {
                        // 📄 Extract text from the uploaded PDF
                        String extractedText = PdfUtils.extractText(out);
                        StudentDAO.updateCvTextByDiscordId(userId, extractedText);
                        System.out.println("✅ Text saved in DB for " + userId);

                        // 🤖 Analyze the CV using GPT
                        if (gpt != null) {
                            // 🎯 Prompt GPT to extract key fields
                            String prompt = """
                        Analyze the following CV and return a JSON object with the following keys:
                        - name (full name)
                        - email (valid email address)
                        - skills (array of skills, that are used in the projects or jobs, for example: JAVA, C)
                        - positions (array of desired job roles like backend, frontend, devops, etc.)

                        CV:
                        --------------------
                        """ + extractedText;

                            List<Map<String, String>> messages = List.of(
                                    Map.of("role", "user", "content", prompt)
                            );

                            String response = gpt.ask(messages, "gpt-3.5-turbo");
                            JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                            String name = json.has("name") && !json.get("name").isJsonNull()
                                    ? json.get("name").getAsString()
                                    : null;

                            String email = json.has("email") && !json.get("email").isJsonNull()
                                    ? json.get("email").getAsString()
                                    : null;

                            String skills = json.has("skills") && json.get("skills").isJsonArray()
                                    ? String.join(", ", toList(json.get("skills").getAsJsonArray()))
                                    : null;

                            String positions = json.has("positions") && json.get("positions").isJsonArray()
                                    ? String.join(", ", toList(json.get("positions").getAsJsonArray()))
                                    : null;


                            StudentDAO.upsertStudent(name, email, skills, positions, userId);
                            System.out.println("✅ Profile updated using AI.");

                            // 📊 Ask GPT for rating and suggestions
                            String ratingPrompt = """
                        You are a career advisor. Read the following CV and evaluate its overall quality.
                        Return a JSON object with two fields:
                        - rating: a number between 1 and 10 (10 = excellent)
                        - feedback: a list of 2–5 suggestions to improve the CV.

                        CV:
                        --------------------
                        """ + extractedText;

                            List<Map<String, String>> ratingMessages = List.of(
                                    Map.of("role", "user", "content", ratingPrompt)
                            );

                            String ratingResponse = gpt.ask(ratingMessages, "gpt-3.5-turbo");
                            JsonObject ratingJson = JsonParser.parseString(ratingResponse).getAsJsonObject();

                            int rating = ratingJson.get("rating").getAsInt();
                            List<String> feedbackList = toList(ratingJson.get("feedback").getAsJsonArray());

                            // 📝 Format and send feedback to the user
                            StringBuilder feedbackMsg = new StringBuilder("📝 **CV Rating: " + rating + "/10**\n");
                            feedbackMsg.append("💡 **Suggestions to improve your CV:**\n");
                            for (String tip : feedbackList) {
                                feedbackMsg.append("- ").append(tip).append("\n");
                            }

                            // ✅ Send the feedback message before the final confirmation
                            event.getChannel().sendMessage(feedbackMsg.toString()).queue();
                        }

                        // 📬 Final confirmation and main menu
                        event.getChannel().sendMessage("✅ PDF resume received and processed.")
                                .queue(msg -> CommandHandler.showMainMenu(event.getAuthor()));


                    } catch (Exception e) {
                        e.printStackTrace();
                        event.getChannel().sendMessage("⚠️ Error processing your CV.")
                                .queue(msg -> CommandHandler.showMainMenu(event.getAuthor()));
                    }
                })
                .exceptionally(ex -> {
                    event.getChannel().sendMessage("❌ Error uploading PDF. Please try again.").queue();
                    return null;
                });
    }



    private static List<String> toList(JsonArray array) {
        List<String> list = new ArrayList<>();
        for (JsonElement el : array) {
            list.add(el.getAsString());
        }
        return list;
    }


    // Formats a single opportunity into a readable format for GPT
    private String formatOpportunity(bot.api.OpportunityClient.Opportunity opp) {
        return String.format("""
        🔹 **Title**: %s
        🏢 **Company**: %s
        💼 **Type**: %s
        📅 **Deadline**: %s
        🏠 **Home Office**: %s
        💰 **Salary**: %s
        🛠 **Tech Req**: %s
        📚 **Formal Req**: %s
        📄 **Description**: %s
        📞 **Contact**: %s

        """,
                opp.title, opp.company, opp.type, opp.deadline,
                opp.homeOffice, opp.wage, opp.techReq, opp.formReq,
                opp.description, opp.contactPerson
        );
    }


}
