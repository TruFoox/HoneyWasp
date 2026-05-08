import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import config.Config;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import services.*;
import utils.*;
import java.awt.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/* Main
 * Initializes the Discord bot, registers slash commands
 * Manages interactions with different services such as Instagram and YouTube.
 * Uses Discord to handles user commands for starting, stopping, and clearing service caches.*/
public class HoneyWasp extends ListenerAdapter {
    static Map<String, Services> services = new HashMap<>();
    static double currentVersion = 4.0; // Current version number
    static Services bot = null;

    public static void main(String[] args) {
        Config config = Config.getInstance(); // Get config

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error"); // Only show JDA logs for errors

        // Print logo
        System.out.print(Output.YELLOW + "\n" +
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
                "     -------------------------------------------------------------------------------------------------------------\n" + Output.RESET);
        if (config == null) {
            Output.print(null, "[ERR] Config is invalid. Please check JSON formatting (See example config at https://github.com/TruFoox/HoneyWasp/blob/master/example_config.json)", Output.RED, false, false);
            ErrorHandling.exitProgram();

            return; // Unnecessary but the compiler whines
        }


        Output.print(null, "HoneyWasp started on " + DateTime.fullTimestamp(), Output.YELLOW, false, false);

        final String BOTTOKEN = config.General().getDiscordBotToken().trim();

        final List<String> AUTOSTART = config.General().getAutostart();

        // Check for new version
        try {
            Output.debugPrint(null, "Checking for new version");
            String responseString = HTTPSend.get(null, "https://api.github.com/repos/trufoox/honeywasp/releases/latest");

            // Fetch latest version, remove "v" (e.g. v4), then parse as double
            double version =  Double.parseDouble(StringToJson.getData(responseString, "tag_name").replace("v", ""));

            if (version > currentVersion) {
                Output.webhookPrint(null, "A new version is available! : v" + version + " (Current : v" + currentVersion + ")\n\tVisit https://github.com/TruFoox/HoneyWasp/releases/latest", Output.GREEN, false);
            }


        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        JDA jda = null; // Init JDA object to prevent uninitialized error

        // Login to bot
        try {
            Output.print(null, "Logging in to Discord bot...", Output.YELLOW, false, false);
            jda = JDABuilder.createDefault(
                            BOTTOKEN,
                            EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    )
                    .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS) // logging
                    .addEventListeners(new HoneyWasp())
                    .build();

            Output.debugPrint(null, "Waiting for JDA to connect");
            // Wait until the bot is fully logged in
            jda.awaitReady();

            Output.print(null, "Bot connected successfully!\n\n", Output.YELLOW, false, false);
        } catch (InvalidTokenException e) {
            Output.print(null, "Discord bot token is invalid. Please verify you copied the full token from the developer portal");
            ErrorHandling.exitProgram();
        } catch (Exception e) { // Handles login failures and interruptions
            e.printStackTrace();
            Output.print(null, "Bot failed to log in. Quitting...");
            ErrorHandling.exitProgram();
        }

        assert jda != null;

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
            Output.debugPrint(null, "Checking autostart token: " + item);


            if (item.equalsIgnoreCase("instagram")) {
                bot = new Instagram();
                services.put("instagram", bot);
            }
            else if (item.equalsIgnoreCase("youtube")) {
                bot = new YouTube();
                services.put("youtube", bot);
            }
            else if (item.equalsIgnoreCase("twitter")) {
                //bot = new Twitter();
                //services.put("twitter", bot);
            }

            if (bot != null) {
                Output.webhookPrint(null, "Autostarting " + item, Output.YELLOW, false);
                new Thread(bot).start();
            }
        }
    }

    @Override
    // Slash commands
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String service = event.getOption("service").getAsString();
        Output.debugPrint(null, "Command /" + event.getName() + " used on service " + service);

        switch (event.getName()) {
            case "start": {
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
                        bot = new Instagram();
                        services.put("instagram", bot);
                        new Thread(bot).start();

                        bot = new YouTube();
                        services.put("youtube", bot);
                        new Thread(bot).start();

                        //bot = new Twitter();
                        services.put("twitter", bot);
                        new Thread(bot).start();

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

                        bot = new Instagram();
                        services.put("instagram", bot);
                        new Thread(bot).start();

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

                        bot = new YouTube();
                        services.put("youtube", bot);
                        new Thread(bot).start();

                        break;
                    }
                    case "twitter": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                                .setThumbnail("https://img.freepik.com/free-vector/new-2023-twitter-logo-x-icon-design_1017-45418.jpg")
                                .addField("Starting bot on " + service, "Use /stop to stop", false);

                        event.replyEmbeds(embed.build()).queue();

                        //bot = new Twitter();
                        services.put("twitter", bot);
                        new Thread(bot).start();

                        break;
                    }
                }
                break;
            }
            case "stop": {
                switch (service) {
                    case "all": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                                .setDescription("Stopping all services");

                        event.replyEmbeds(embed.build()).queue();

                        services.get("instagram").halt();
                        services.get("youtube").halt();
                        services.get("twitter").halt();

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

                        services.get("instagram").halt();

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

                        services.get("youtube").halt();

                        break;
                    }
                    case "twitter": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                                .setThumbnail("https://img.freepik.com/free-vector/new-2023-twitter-logo-x-icon-design_1017-45418.jpg")
                                .setDescription("Stopping " + service);

                        event.replyEmbeds(embed.build()).queue();

                        services.get("twitter").halt();

                        break;
                    }
                }
                break;
            }
            case "clear": {
                switch (service) {
                    case "all": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                                .setDescription("All caches cleared");

                        event.replyEmbeds(embed.build()).queue();

                        services.get("instagram").clear();
                        services.get("youtube").clear();
                        services.get("twitter").clear();

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

                        services.get("instagram").clear();
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

                        services.get("youtube").clear();
                        break;
                    }
                    case "twitter": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                                .setThumbnail("https://img.freepik.com/free-vector/new-2023-twitter-logo-x-icon-design_1017-45418.jpg")
                                .setDescription(service + " cache cleared");

                        event.replyEmbeds(embed.build()).queue();

                        services.get("twitter").clear();
                        break;
                    }
                }
                break;
            }

            default:
                event.reply("Unknown command.").setEphemeral(true).queue();
        }
    }
}
