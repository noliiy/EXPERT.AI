package bot;

import bot.ai.GPTClient;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;

/**
 * This is the main entry point for the Discord bot.
 * It initializes the bot with necessary API keys, registers event listeners,
 * and starts the bot session using the JDA library.
 */
public class BotMain {
    public static void main(String[] args) throws LoginException {
        // 1. Load your Discord bot token from the environment variables
        // This token is necessary for authenticating the bot with the Discord API
        String discordToken = System.getenv("DISCORD_TOKEN");
        if (discordToken == null || discordToken.isEmpty()) {
            System.err.println("❌ DISCORD_TOKEN is not set.");
            return; // Abort if no token is provided
        }

        // 2. Load your OpenAI API key from the environment
        // If you don’t use GPT functionality, this can be left blank
        String openAiKey = System.getenv("OPENAI_API_KEY");
        if (openAiKey == null || openAiKey.isEmpty()) {
            System.err.println("⚠️ OPENAI_API_KEY is not set. GPT features will be disabled.");
        }

        // 3. Create GPTClient only if a valid OpenAI key is present
        GPTClient gptClient = null;
        if (openAiKey != null && !openAiKey.isEmpty()) {
            gptClient = new GPTClient(openAiKey); // This enables GPT-based features
        }

        // 4. Build the JDA Discord client with required configuration
        JDABuilder builder = JDABuilder.createDefault(discordToken)
                // Enable gateway intents for message handling in both DMs and servers
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT
                )
                // Set the activity text shown in Discord as "Listening to !start"
                .setActivity(Activity.listening("!start"));

        // 5. Register your event listeners (handlers for commands and button interactions)
        builder.addEventListeners(
                new CommandHandler(gptClient),   // Handles commands like !start, !ask, etc.
                new InteractionHandler()         // Handles buttons and select menu interactions
        );

        // 6. Login and start the bot
        builder.build();
    }
}
