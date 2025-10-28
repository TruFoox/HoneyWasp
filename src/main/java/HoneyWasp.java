import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import config.ReadConfig;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import utils.DateTime;
import utils.HTTPSend;
import utils.Output;
import utils.StringToJson;
import services.Instagram;
import services.YouTube;
import java.awt.*;
import java.util.EnumSet;
import java.util.List;


/* Main
 * Initializes the Discord bot, registers slash commands
 * Manages interactions with different services such as Instagram and YouTube.
 * Uses Discord to handles user commands for starting, stopping, and clearing service caches.*/
public class HoneyWasp extends ListenerAdapter {
    public static void main(String[] args) {
        double currentVersion = 3.00; // Current version number

        ReadConfig config = ReadConfig.getInstance(); // Get config

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error"); // Only show JDA logs for errors

        // Print logo
        Output.print("\n" +
                "       @@@@@                      @@@@@@\n" +
                "           @@@                  @@@\n" +
                "              @@              @@\n" +
                "                @@@@@@@@@@@@@@\n" +
                "              @@ @@        @@ @@\n" +
                "             @    @@      @@    @\n" +
                "            @ @@@            @@@ @\n" +
                "            @ @@@@          @@@@ @\n" +
                "            @ @@@@@        @@@@@ @\n" +
                "             @ @@@@@      @@@@@ @\n" +
                "              @  @@        @@  @\n" +
                "               @              @\n" +
                "               @@            @@\n" +
                "               @@@@        @@@@\n" +
                "                @ @@@    @@ @@\n" +
                "                  @@ @@@@@@ @     @@   @@   @@@@   @@   @@ @@@@@ @@   @@ @@       @@   @     @@@@@ @@@@@\n" +
                "                  @@     @@       @@   @@  @@  @@  @@@  @@ @@     @@ @@  @@   @   @@  @@@   @@@    @@  @@\n" +
                "                  @      @@       @@@@@@@ @@    @@ @@@@ @@ @@@@    @@@    @@ @@@ @@  @@ @@   @@@@  @@@@@\n" +
                "                        @@@@      @@   @@  @@  @@  @@ @@@@ @@      @@      @@@@@@@  @@@@@@@    @@@ @@\n" +
                "                        @@@@      @@   @@   @@@@   @@   @@ @@@@@  @@        @@ @@   @@   @@ @@@@@  @@  v" + currentVersion + "\n" +
                "                         @@\n" +
                " \n" +
                "     -------------------------------------------------------------------------------------------------------------\n", Output.YELLOW, false, false);
        Output.print("[SYS] HoneyWasp started on " + DateTime.fullTimestamp(), Output.YELLOW, false, false);

        final String BOTTOKEN = config.getGeneral().getDiscordBotToken().trim();
        final Boolean RESTART = config.getGeneral().isRestart();

        final List<String> AUTOSTART = config.getGeneral().getAutostart();

        JDA jda = null; // Init JDA to null to prevent uninitialized error

        // Login to bot
        try {
            Output.print("[SYS] Logging in to Discord bot...", Output.YELLOW, false, false);
            jda = JDABuilder.createDefault(
                            BOTTOKEN,
                            EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    )
                    .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS) // logging
                    .addEventListeners(new HoneyWasp())
                    .build();

            // Wait until the bot is fully logged in
            jda.awaitReady();

            Output.print("[SYS] Bot connected successfully!", Output.YELLOW, false, false);

        } catch (Exception e) { // Handles login failures and interruptions
            e.printStackTrace();
            Output.print("[SYS] Bot failed to log in. Quitting...");
            Output.exitProgram();
        }

        // Check for new version
        try {
            String responseString = HTTPSend.get("https://api.github.com/repos/trufoox/honeywasp/releases/latest");
            String version = StringToJson.getData(responseString, "tag_name");
            version = version.replace("v", "");

            double versionDouble = Double.parseDouble(version);
            if (versionDouble > currentVersion) {
                Output.webhookPrint("[SYS] A new version is available! : v" + versionDouble + " (Current : v" + currentVersion + ")\n\tVisit https://github.com/TruFoox/HoneyWasp/releases/latest", Output.GREEN, false);
            }


        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        // Register commands (global)
        jda.updateCommands()
                .addCommands(
                        Commands.slash("start", "Start running HoneyWasp")
                                .addOptions(new OptionData(OptionType.STRING, "service", "The service you want to run HoneyWasp on", true)
                                        .addChoice("All", "all")
                                        .addChoice("Instagram", "instagram")
                                        .addChoice("Youtube", "youtube")),
                        Commands.slash("stop", "Stops the specified HoneyWasp service")
                                .addOptions(new OptionData(OptionType.STRING, "service", "The service you want to stop", true)
                                        .addChoice("All", "all")
                                        .addChoice("Instagram", "instagram")
                                        .addChoice("Youtube", "youtube")),
                        Commands.slash("clear", "Clear HoneyWasp cache of specific service")
                                .addOptions(new OptionData(OptionType.STRING, "service", "The service you want to stop", true)
                                        .addChoice("All", "all")
                                        .addChoice("Instagram", "instagram")
                                        .addChoice("Youtube", "youtube"))
                ).queue();

        // Automatic starting of services
        for(String item : AUTOSTART) {

            // Instagram
            if (item.toLowerCase().equals("instagram")) {
                Output.webhookPrint("[SYS] Autostarting Instagram", Output.YELLOW, false);

                HoneyWasp main = new HoneyWasp(); // Create call for bot to acknowledge when a thread closes
                Instagram ig = new Instagram(); // Create bot instance
                Thread t = new Thread(ig); // Create thread

                t.start(); // Start bot
            }

            // YouTube
            if (item.toLowerCase().equals("youtube")) {
                Output.webhookPrint("[SYS] Autostarting YouTube", Output.YELLOW, false);

                YouTube yt = new YouTube(); // Create bot instance
                Thread t = new Thread(yt); // Create thread

                t.start(); // Start bot
            }
        }
        Output.print("\n\n", Output.YELLOW, false, false); // Spacing to create distinction between bot running and setup
    }

    @Override
    // Slash commands
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "start": {
                String service = event.getOption("service").getAsString();
                switch (service) {
                    case "all": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                                .addField("Starting bot on all services", "Use /stop to stop", false);

                        event.replyEmbeds(embed.build()).queue();

                        // Threads
                        Thread i  = new Thread(new Instagram()); // Start Instagram
                        Thread y = new Thread(new YouTube()); // Start YouTube
                        i.start();
                        y.start();

                        break;
                    }

                    case "instagram": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                                .setThumbnail("https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Instagram_icon.png/960px-Instagram_icon.png")
                                .addField("Starting bot on " + service, "Use /stop to stop", false);

                        event.replyEmbeds(embed.build()).queue();

                        Thread i = new Thread(new Instagram()); // Start Instagram
                        i.start();

                        break;
                    }

                    case "youtube": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                                .setThumbnail("https://images.icon-icons.com/2699/PNG/512/youtube_logo_icon_168737.png")
                                .addField("Starting bot on " + service, "Use /stop to stop", false);

                        event.replyEmbeds(embed.build()).queue();

                        Thread y = new Thread(new YouTube()); // Start YouTube
                        y.start();

                        break;
                    }
                }
                break;
            }
            case "stop": {
                String service = event.getOption("service").getAsString();
                switch (service) {
                    case "all": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                                .setDescription("Stopping all services");

                        event.replyEmbeds(embed.build()).queue();

                        Instagram.stop();

                        break;
                    }

                    case "instagram": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                                .setThumbnail("https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Instagram_icon.png/960px-Instagram_icon.png")
                                .setDescription("Stopping " + service);

                        event.replyEmbeds(embed.build()).queue();

                        Instagram.stop();

                        break;
                    }

                    case "youtube": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                                .setThumbnail("https://images.icon-icons.com/2699/PNG/512/youtube_logo_icon_168737.png")
                                .setDescription("Stopping " + service);

                        event.replyEmbeds(embed.build()).queue();
                        break;
                    }
                }
                break;
            }
            case "clear": {
                String service = event.getOption("service").getAsString();
                switch (service) {
                    case "all": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                                .setDescription("All caches cleared");

                        event.replyEmbeds(embed.build()).queue();

                        Instagram.clear();

                        break;
                    }

                    case "instagram": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                                .setThumbnail("https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Instagram_icon.png/960px-Instagram_icon.png")
                                .setDescription(service + " cache cleared");

                        event.replyEmbeds(embed.build()).queue();

                        Instagram.clear();

                        break;
                    }

                    case "youtube": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                                .setThumbnail("https://images.icon-icons.com/2699/PNG/512/youtube_logo_icon_168737.png")
                                .setDescription(service + " cache cleared");

                        event.replyEmbeds(embed.build()).queue();
                        break;
                    }
                }
                break;
            }

            default:
                event.reply("Unknown command.").setEphemeral(true).queue();
        }
    }
    public void instagramStopped() {

    }
}
