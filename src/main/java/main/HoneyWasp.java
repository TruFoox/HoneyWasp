package main;

import config.PlatformSettings;
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
import java.util.*;
import java.util.function.Supplier;


/* Main
 * Initializes the Discord bot, registers slash commands
 * Manages interactions with different services such as Instagram and YouTube.
 * Uses Discord to handles user commands for starting, stopping, and clearing service caches.*/
public class HoneyWasp extends ListenerAdapter {
    public static Config config; // Universal config handler for the bot
    public record ServiceData(Supplier<Services> serviceObject, String imageURL, String capsName) {} // Defines data layout of service data

    private static final Map<String, ServiceData> services = Map.of(
            "instagram", new ServiceData(Instagram::new, "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Instagram_icon.png/960px-Instagram_icon.png", "Instagram"),
            "youtube", new ServiceData(YouTube::new, "https://images.icon-icons.com/2699/PNG/512/youtube_logo_icon_168737.png", "YouTube")
    );

    static float currentVersion = 4.3f; // Current version number

    public static Map<String, Services> runningServices = new HashMap<>();
    static Services bot = null;
    static final String iconURL = "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg";
    protected static String BOTTOKEN;
    public static boolean DEBUG_MODE, RESTART; // General config items used by threads


    public static void main(String[] args) {

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
        BOTTOKEN = config.General().getDiscordBotToken();
        DEBUG_MODE = HoneyWasp.config.General().isDebug_mode();
        RESTART = HoneyWasp.config.General().isRestart();

        if (!DEBUG_MODE) { // JDA logging options
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error"); // Only show JDA logs for errors
            System.setProperty("org.slf4j.simpleLogger.log.com.neovisionaries.ws.client", "off"); // Hide network errors
        }

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

        // Login to bot if discord bot token was given, else skip to autostart
        if (BOTTOKEN == null || !BOTTOKEN.isBlank()) {
            JDA jda = null;

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
                Output.print(null, "Discord bot token is invalid. Please verify you copied the correct token from the developer portal" +
                        "\n\tBot will continue to run if autostart enabled");
                ErrorHandling.exitProgram();
            } catch (Exception e) { // Handles login failures and interruptions
                e.printStackTrace();
                Output.print(null, "Bot failed to log in. Quitting...");
                ErrorHandling.exitProgram();
            }

            assert jda != null;


            // Register commands
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
                                    .addOptions(new OptionData(OptionType.STRING, "service", "The service you want to clear the duplicate cache of", true)
                                            .addChoice("All", "all")
                                            .addChoice("Instagram", "instagram")
                                            .addChoice("Youtube", "youtube"))

                    ).queue();
        } else {
            Output.print(null, "No discord bot token supplied. Bot will only be controllable via autostart");
        }

        // Automatic starting of services
        for(String service : services.keySet()) {
            Output.debugPrint(null, "Checking potential autostart token: " + service);
            PlatformSettings serviceSettings = HoneyWasp.config.Platform(service.toLowerCase());

            if (serviceSettings.isAutostart()) {
                bot = services.get(service).serviceObject.get(); // new Instagram, new YouTube, etc
                runningServices.put(service.toLowerCase(), bot);
                bot.start();

                Output.webhookPrint(null, "Autostarting " + services.get(service).capsName, Output.YELLOW);
            }
        }
    } // If no discord token, processing stops here. Otherwise, commands can be invoked

    @Override
    // Slash commands
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.deferReply().queue(hook -> { // Tells discord event has been noticed
        String service = event.getOption("service").getAsString();
        Output.debugPrint(null, "Command /" + event.getName() + " used on service " + service);

        switch (event.getName()) {
            case "start": {
                if (service.equals("all")) {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setColor(new Color(0xFF8307))
                            .setAuthor("Honeywasp", "https://github.com/TruFoox/HoneyWasp", iconURL)
                            .addField("Starting bot on all services", "Use /stop to stop", false);

                    event.getHook().sendMessageEmbeds(embed.build()).queue();

                    for (String name : services.keySet()) {
                        if (runningServices.containsKey(service)) {
                            Output.webhookPrint(null, services.get(name).capsName + " is already running.");
                        } else {
                            bot = services.get(name).serviceObject().get();
                            runningServices.put(name.toLowerCase(), bot);
                            bot.start();
                        }
                    }
                } else {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setColor(new Color(0xFF8307))
                            .setAuthor("Honeywasp", "https://github.com/TruFoox/HoneyWasp", iconURL)
                            .setThumbnail(services.get(service).imageURL)
                            .addField("Starting bot on " + services.get(service).capsName, "Use /stop to stop", false);

                    event.getHook().sendMessageEmbeds(embed.build()).queue();
                    if (runningServices.containsKey(service)) {
                        Output.webhookPrint(null, services.get(service).capsName + " is already running. Stop it first.");
                    } else {
                        bot = services.get(service).serviceObject.get();
                        runningServices.put(service, bot);
                        bot.start();
                    }
                }
                break;
            }
            case "stop": {
                if (service.equals("all")) {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setColor(new Color(0xFF8307))
                            .setAuthor("Honeywasp", "https://github.com/TruFoox/HoneyWasp", iconURL)
                            .setDescription("Attempting to stop all services");

                    event.getHook().sendMessageEmbeds(embed.build()).queue();

                    for (String name : services.keySet()) {
                        if (runningServices.containsKey(name)) {
                            runningServices.get(name).halt();
                        } else {
                            Output.webhookPrint(null, services.get(name).capsName + " is not running.");
                        }
                    }
                } else {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setColor(new Color(0xFF8307))
                            .setAuthor("Honeywasp", "https://github.com/TruFoox/HoneyWasp", iconURL)
                            .setThumbnail(services.get(service).imageURL)
                            .setDescription("Attempting to stop " + services.get(service).capsName);

                    event.getHook().sendMessageEmbeds(embed.build()).queue();

                    if (runningServices.containsKey(service)) {
                        runningServices.get(service).halt();
                    } else {
                        Output.webhookPrint(null, services.get(service).capsName + " is not running");
                    }
                }
                break;
            }
            case "status": {
                if (service.equals("all")) {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setColor(new Color(0xFF8307))
                            .setAuthor("Honeywasp", "https://github.com/TruFoox/HoneyWasp", iconURL)
                            .setTitle("Service Statuses")
                            .addField("Instagram", runningServices.containsKey("instagram") ? "Running" : "Stopped", true)
                            .addField("YouTube", runningServices.containsKey("youtube") ? "Running" : "Stopped", true);
                    event.getHook().sendMessageEmbeds(embed.build()).queue();
                } else {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setColor(new Color(0xFF8307))
                            .setAuthor("Honeywasp", "https://github.com/TruFoox/HoneyWasp", iconURL)
                            .setThumbnail(services.get(service).imageURL)
                            .setTitle(services.get(service).capsName + " Status")
                            .addField("Running", Boolean.toString(runningServices.containsKey(service)), true);
                    event.getHook().sendMessageEmbeds(embed.build()).queue();
                }
                break;
            }
            case "clear": {
                if (service.equals("all")) {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setColor(new Color(0xFF8307))
                            .setAuthor("Honeywasp", "https://github.com/TruFoox/HoneyWasp", iconURL)
                            .setDescription("Attempting to clear all caches");

                    event.getHook().sendMessageEmbeds(embed.build()).queue();

                    for (String name : services.keySet()) {
                        FileIO.clearList(name);
                    }
                } else {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setColor(new Color(0xFF8307))
                            .setAuthor("Honeywasp", "https://github.com/TruFoox/HoneyWasp", iconURL)
                            .setThumbnail(services.get(service).imageURL)
                            .setDescription("Attempting to clear " + services.get(service).capsName + " cache");

                    event.getHook().sendMessageEmbeds(embed.build()).queue();

                    FileIO.clearList(service);
                }
                break;
            }

            default:
                event.reply("Unknown command.").setEphemeral(true).queue();
            }
        });
    }
}
