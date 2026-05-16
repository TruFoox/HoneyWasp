package main;

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
    public static Config config; // Universal config handler for the bot

    static Map<String, Services> services = new HashMap<>();
    static float currentVersion = 4.2f; // Current version number
    static Services bot = null;
    static final String iconURL = "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg";
    protected static String BOTTOKEN;
    static List<String> AUTOSTART;
    public static boolean DEBUG_MODE, RESTART; // General config items used by threads


    public static void main(String[] args) {
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
        try { // Init config
            config = Config.getInstance(); // Get config
        } catch (Exception _) {
            Output.print(null, "Config is invalid. Please check JSON formatting (See example config at https://github.com/TruFoox/HoneyWasp/blob/master/example_config.json)", Output.RED, false, false);
            ErrorHandling.exitProgram();
        }

        Output.print(null, "HoneyWasp started on " + DateTime.fullTimestamp(), Output.YELLOW, false, false);
        BOTTOKEN = config.General().getDiscordBotToken().trim();
        AUTOSTART = config.General().getAutostart();
        DEBUG_MODE = HoneyWasp.config.General().isDebug_mode();
        RESTART = HoneyWasp.config.General().isRestart();

        // Check for new version
        try {
            Output.debugPrint(null, "Checking for new version");
            String responseString = HTTPSend.get(null, "https://api.github.com/repos/trufoox/honeywasp/releases/latest");

            // Fetch latest version, remove "v" (e.g. v2), then parse as float
            float version =  Float.parseFloat(StringToJson.getData(responseString, "tag_name").replace("v", ""));

            if (version > currentVersion) {
                Output.webhookPrint(null, "A new version is available! : v" + version + " (Current : v" + currentVersion + ")" +
                        "\n\tVisit https://github.com/TruFoox/HoneyWasp/releases/latest", Output.GREEN, false);
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
                    .disableCache(CacheFlag.SOUNDBOARD_SOUNDS)
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
                        Commands.slash("start", "Start running HoneyWasp on a service")
                                .addOptions(new OptionData(OptionType.STRING, "service", "The service you want to run HoneyWasp on", true)
                                        .addChoice("All", "all")
                                        .addChoice("Instagram", "instagram")
                                        .addChoice("Youtube", "youtube")),
                        Commands.slash("stop", "Stops the specified service")
                                .addOptions(new OptionData(OptionType.STRING, "service", "The service you want to stop", true)
                                        .addChoice("All", "all")
                                        .addChoice("Instagram", "instagram")
                                        .addChoice("Youtube", "youtube")),
                        Commands.slash("status", "Fetch status of specified service")
                                .addOptions(new OptionData(OptionType.STRING, "service", "The service you want to fetch the status of", true)
                                        .addChoice("All", "all")
                                        .addChoice("Instagram", "instagram")
                                        .addChoice("Youtube", "youtube")),
                        Commands.slash("clear", "Clear cache of specified service")
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
                bot.start();
                Output.webhookPrint(null, "Autostarting " + bot.name, Output.YELLOW);
            }
        }
    }

    @Override
    // Slash commands
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.deferReply().queue(hook -> { // Tells discord event has been noticed
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
                                        iconURL)
                                .addField("Starting bot on all services", "Use /stop to stop", false);

                        event.getHook().sendMessageEmbeds(embed.build()).queue();

                        // Threads
                        if (services.containsKey("instagram")) {
                            Output.webhookPrint(null, "Instagram is already running.");
                        } else {
                            bot = new Instagram();
                            services.put("instagram", bot);
                            bot.start();
                        }

                        if (services.containsKey("youtube")) {
                            Output.webhookPrint(null, "YouTube is already running.");
                        } else {
                            bot = new YouTube();
                            services.put("youtube", bot);
                            bot.start();
                        }

                        //bot = new Twitter();
                        //services.put("twitter", bot);
                        //bot.start();

                        break;
                    }
                    case "instagram": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        iconURL)
                                .setThumbnail("https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Instagram_icon.png/960px-Instagram_icon.png")
                                .addField("Starting bot on " + service, "Use /stop to stop", false);

                        event.getHook().sendMessageEmbeds(embed.build()).queue();
                        if (services.containsKey("instagram")) {
                            Output.webhookPrint(null, "Instagram is already running. Stop it first.");
                        } else {
                            bot = new Instagram();
                            services.put("instagram", bot);
                            bot.start();
                        }

                        break;
                    }

                    case "youtube": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        iconURL)
                                .setThumbnail("https://images.icon-icons.com/2699/PNG/512/youtube_logo_icon_168737.png")
                                .addField("Starting bot on " + service, "Use /stop to stop", false);

                        event.getHook().sendMessageEmbeds(embed.build()).queue();

                        if (services.containsKey("youtube")) {
                            Output.webhookPrint(null, "YouTube is already running. Stop it first.");
                        } else {
                            bot = new YouTube();
                            services.put("youtube", bot);
                            bot.start();
                        }
                        break;
                    }
                    case "twitter": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        iconURL)
                                .setThumbnail("https://img.freepik.com/free-vector/new-2023-twitter-logo-x-icon-design_1017-45418.jpg")
                                .addField("Starting bot on " + service, "Use /stop to stop", false);

                        event.getHook().sendMessageEmbeds(embed.build()).queue();

                        //bot = new Twitter();
                        services.put("twitter", bot);
                        bot.start();

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
                                        iconURL)
                                .setDescription("Stopping all services");

                        event.getHook().sendMessageEmbeds(embed.build()).queue();

                        if (services.containsKey("instagram")) {
                            services.get("instagram").halt();
                            services.remove("instagram");
                        } else {Output.webhookPrint(null, "Instagram not running");}
                        if (services.containsKey("youtube")) {
                            services.get("youtube").halt();
                            services.remove("youtube");
                        } else {Output.webhookPrint(null, "Youtube not running");}
                        //services.get("twitter").halt();

                        break;
                    }

                    case "instagram": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        iconURL)
                                .setThumbnail("https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Instagram_icon.png/960px-Instagram_icon.png")
                                .setDescription("Stopping " + service);

                        event.getHook().sendMessageEmbeds(embed.build()).queue();

                        if (services.containsKey("instagram")) {
                            services.get("instagram").halt();
                            services.remove("instagram");
                        } else {Output.webhookPrint(null, "Instagram not running");}

                        break;
                    }

                    case "youtube": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        iconURL)
                                .setThumbnail("https://images.icon-icons.com/2699/PNG/512/youtube_logo_icon_168737.png")
                                .setDescription("Stopping " + service);

                        event.getHook().sendMessageEmbeds(embed.build()).queue();

                        if (services.containsKey("youtube")) {
                            services.get("youtube").halt();
                            services.remove("youtube");
                        } else {Output.webhookPrint(null, "Youtube not running");}

                        break;
                    }
                    case "twitter": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        iconURL)
                                .setThumbnail("https://img.freepik.com/free-vector/new-2023-twitter-logo-x-icon-design_1017-45418.jpg")
                                .setDescription("Stopping " + service);

                        event.getHook().sendMessageEmbeds(embed.build()).queue();

                        if (services.containsKey("twitter")) {
                            services.get("twitter").halt();
                            services.remove("twitter");
                        } else {Output.webhookPrint(null, "Twitter not running");}

                        break;
                    }
                }
                break;
            }
            case "status": {
                switch (service) {
                    case "all": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        iconURL)
                                .setTitle("Service Status")
                                .addField("Instagram", services.containsKey("instagram") ? "Running" : "Stopped", true)
                                .addField("YouTube", services.containsKey("youtube") ? "Running" : "Stopped", true);
                        event.getHook().sendMessageEmbeds(embed.build()).queue();

                        break;
                    }

                    case "instagram": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        iconURL)
                                .setTitle("Instagram Status")
                                .addField("Running", Boolean.toString(services.containsKey("instagram")), true);
                        event.getHook().sendMessageEmbeds(embed.build()).queue();

                        break;
                    }

                    case "youtube": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        iconURL)
                                .setTitle("YouTube Status")
                                .addField("Running", Boolean.toString(services.containsKey("youtube")), true);
                        event.getHook().sendMessageEmbeds(embed.build()).queue();

                        break;
                    }
                    case "twitter": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        iconURL)
                                .setTitle("Twitter Status")
                                .addField("Running", Boolean.toString(services.containsKey("twitter")), true);
                        event.getHook().sendMessageEmbeds(embed.build()).queue();

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
                                        iconURL)
                                .setDescription("Attempting to clear all caches");

                        event.getHook().sendMessageEmbeds(embed.build()).queue();

                        FileIO.clearList("Instagram");
                        FileIO.clearList("YouTube");

                        //services.get("twitter").clear();

                        break;
                    }

                    case "instagram": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        iconURL)
                                .setThumbnail("https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Instagram_icon.png/960px-Instagram_icon.png")
                                .setDescription("Attempting to clear " + service + " cache");

                        event.getHook().sendMessageEmbeds(embed.build()).queue();

                        FileIO.clearList("Instagram");

                        break;
                    }

                    case "youtube": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        iconURL)
                                .setThumbnail("https://images.icon-icons.com/2699/PNG/512/youtube_logo_icon_168737.png")
                                .setDescription("Attempting to clear " + service + " cache");

                        event.getHook().sendMessageEmbeds(embed.build()).queue();

                        FileIO.clearList("YouTube");

                        break;
                    }
                    case "twitter": {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setColor(new Color(0xFFA500))
                                .setAuthor("Honeywasp",
                                        "https://github.com/TruFoox/HoneyWasp",
                                        iconURL)
                                .setThumbnail("https://img.freepik.com/free-vector/new-2023-twitter-logo-x-icon-design_1017-45418.jpg")
                                .setDescription("Attempting to clear " + service + " cache");

                        event.getHook().sendMessageEmbeds(embed.build()).queue();

                        FileIO.clearList("Twitter");

                        break;
                    }
                }
                break;
            }

            default:
                event.reply("Unknown command.").setEphemeral(true).queue();
            }
        });
    }
}
