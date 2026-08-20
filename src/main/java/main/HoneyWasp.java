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
import config.*;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import services.*;
import utils.*;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;


/* Main
 * Initializes the Discord bot, registers slash commands
 * Manages interactions with different services such as Instagram and YouTube.
 * Uses Discord to handles user commands for starting, stopping, and clearing service caches.*/
public class HoneyWasp extends ListenerAdapter {
    public static Config config; // Universal config handler for the bot
    public record ServiceData(Supplier<Services> serviceObject, String imageURL, String capsName) {} // Defines data layout of service data

    private static final Map<String, ServiceData> services = Map.of( // List of all services and misc data about them
            "instagram", new ServiceData(Instagram::new, "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Instagram_icon.png/960px-Instagram_icon.png", "Instagram"),
            "youtube", new ServiceData(YouTube::new, "https://images.icon-icons.com/2699/PNG/512/youtube_logo_icon_168737.png", "YouTube"),
            "tiktok", new ServiceData(TikTok::new, "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a6/Tiktok_icon.svg/3840px-Tiktok_icon.svg.png?utm_source=commons.wikimedia.org&utm_campaign=index&utm_content=thumbnail", "TikTok")
    );

    static float currentVersion = 5.1f; // Current version number

    public static Map<String, Services> runningServices = new HashMap<>();
    static Services bot = null;
    static final String iconURL = "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg";
    protected static String BOTTOKEN;
    public static boolean DEBUG_MODE, RESTART, USE_PROXIES; // General config items used by threads
    public static List<String[]> PROXIES;

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

        System.setProperty("org.slf4j.simpleLogger.log.com.fasterxml.jackson", "off"); // Hide jackson error logs before initialization (They are unhelpful and spammy)

        try { // Init config
            config = Config.getInstance();
        } catch (Exception _) {
            Output.print(null, "Config is invalid. Please check JSON formatting (See example config at https://github.com/TruFoox/HoneyWasp/blob/master/example_config.json)", Output.RED, false, false);
            ErrorHandling.exitProgram();
        }
        BOTTOKEN = config.General().getDiscordBotToken();
        DEBUG_MODE = HoneyWasp.config.General().isDebug_mode();
        RESTART = HoneyWasp.config.General().isRestart();
        USE_PROXIES = HoneyWasp.config.General().isProxies_enabled();

        Output.print(null, "HoneyWasp started on " + DateTime.fullTimestamp(), Output.YELLOW, false, false);

        // Initiate proxies
        if (USE_PROXIES) {

            PROXIES = FileIO.readList(null, Paths.get(".", "proxies.txt"), ":");

            if (PROXIES == null || PROXIES.isEmpty()) {
                Output.webhookPrint(null, "Proxies are enabled, but no proxies were provided in ./proxies.txt. Quitting...", Output.RED);
            }

            Output.print(null, "Testing provided proxies", Output.YELLOW, true);

            List<String []> badProxies = new ArrayList<>(); // Stores invalid proxies because java doesn't allow removing an element while iterating over the list

            for (String[] proxy : PROXIES) { // Test each proxy's connectivity
                if (!HTTPSend.testInternet(proxy)) { // Perform connectivity test
                    Output.print(null, "Proxy " + proxy[0] + ":" + proxy[1] + " has been ignored for being invalid", Output.RED, true);
                    badProxies.add(proxy);
                    try {Thread.sleep(1000);} catch (Exception _) {}
                } else {
                    Output.print(null, "Proxy " + proxy[0] + ":" + proxy[1] + " is valid", Output.GREEN, true);
                }
            }

            // Alert user of how many proxies were removed
            if (PROXIES.isEmpty()) { // If all proxies removed
                Output.print(null, "All provided proxies were found to be unable to connect to the internet." +
                        "\n\tPlease replace them or disable proxies under [General_Settings] in config.json", Output.RED);
                ErrorHandling.exitProgram();
            } else if (!badProxies.isEmpty()) { // If at least one proxy is removed
                Output.print(null, badProxies.size() + " proxies found to be invalid:");
                for (String[] proxy : badProxies) {
                    Output.print(null, proxy[0] + ":" + proxy[1]);
                }
            } else {
                Output.print(null, "All proxies found to be valid");
            }
        }

        // JDA Logging options
        if (!DEBUG_MODE) {
            System.setProperty("org.slf4j.simpleLogger.log.net.dv8tion.jda", "error"); // Hide non-error JDA logs
            System.setProperty("org.slf4j.simpleLogger.log.net.dv8tion.jda.internal.requests.WebSocketClient", "off"); // Hide all JDA connection failed logs
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
            if (DEBUG_MODE) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            } else {
                Output.print(null, "Failed to check for new version", Output.RED);
            }
        }

        // Login to bot if discord bot token was given, else skip to autostart
        if (BOTTOKEN != null || !BOTTOKEN.isBlank()) {
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
                Output.print(null, "Discord bot token is invalid. Please verify you copied the correct token from the developer portal");
            } catch (Exception e) { // Handles login failures and interruptions
                if (DEBUG_MODE) {
                    e.printStackTrace();
                }
                Output.print(null, "Bot failed to log in to Discord. Check your internet or try again later. Quitting...", Output.RED);
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
                                            .addChoice("YouTube", "youtube")
                                            .addChoice("TikTok", "tiktok")),
                            Commands.slash("stop", "Stops the specified service")
                                    .addOptions(new OptionData(OptionType.STRING, "service", "The service you want to stop", true)
                                            .addChoice("All", "all")
                                            .addChoice("Instagram", "instagram")
                                            .addChoice("YouTube", "youtube")
                                            .addChoice("TikTok", "tiktok")),
                            Commands.slash("status", "Fetch status of specified service")
                                    .addOptions(new OptionData(OptionType.STRING, "service", "The service you want to fetch the status of", true)
                                            .addChoice("All", "all")
                                            .addChoice("Instagram", "instagram")
                                            .addChoice("YouTube", "youtube")
                                            .addChoice("TikTok", "tiktok")),
                            Commands.slash("clear", "Clear cache of specified service")
                                    .addOptions(new OptionData(OptionType.STRING, "service", "The service you want to clear the duplicate cache of", true)
                                            .addChoice("All", "all")
                                            .addChoice("Instagram", "instagram")
                                            .addChoice("YouTube", "youtube")
                                            .addChoice("TikTok", "tiktok"))

                    ).queue();
        } else {
            Output.print(null, "No discord bot token supplied. Headless operation activated");
        }

        // Automatic starting of services
        for(String service : services.keySet()) {
            Output.debugPrint(null, "Checking potential autostart token: " + services.get(service).capsName);
            PlatformSettings serviceSettings = HoneyWasp.config.Platform(service.toLowerCase());

            if (serviceSettings.isAutostart()) {
                bot = services.get(service).serviceObject.get(); // new Instagram, new YouTube, etc
                runningServices.put(service.toLowerCase(), bot);
                bot.start();
            }
        }

        if (BOTTOKEN == null || BOTTOKEN.isBlank()) { // If in headless (No discord) mode, warn user that they need to enable autostart & Quit
            Output.webhookPrint(null, "You need to enable Autostart for least one service in config.json;" +
                    "\nMake sure you have done all the steps for you chosen services found in https://github.com/TruFoox/HoneyWasp#getting-started" +
                    "\n" +
                    "\nThe bot will now close, as it cannot function in headless mode without Autostart. Please enable it, or add a Discord bot token", Output.RED);
            ErrorHandling.exitProgram();
        }
    }

    @Override
    // Slash commands
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.deferReply().queue(hook -> { // Tells discord event has been noticed
        String service = event.getOption("service").getAsString();
        Output.debugPrint(null, "Command /" + event.getName() + " used on service " + service);

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(new Color(0xFF8307))
                .setAuthor("Honeywasp", "https://github.com/TruFoox/HoneyWasp", iconURL);
        switch (event.getName()) {
            case "start": {
                if (service.equals("all")) {
                    embed.addField("Starting bot on all services", "Use /stop to stop", false);

                    event.getHook().sendMessageEmbeds(embed.build()).queue();

                    for (String name : services.keySet()) {
                        if (runningServices.containsKey(name)) {
                            Output.webhookPrint(null, services.get(name).capsName + " is already running.");
                        } else {
                            bot = services.get(name).serviceObject().get();
                            runningServices.put(name.toLowerCase(), bot);
                            bot.start();
                        }
                    }
                } else {
                    embed.setThumbnail(services.get(service).imageURL)
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
                    embed.setDescription("Attempting to stop all services");

                    event.getHook().sendMessageEmbeds(embed.build()).queue();

                    for (String name : services.keySet()) {
                        if (runningServices.containsKey(name)) {
                            runningServices.get(name).halt();
                        } else {
                            Output.webhookPrint(null, services.get(name).capsName + " is not running.");
                        }
                    }
                } else {
                    embed.setDescription("Attempting to stop " + services.get(service).capsName);

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
                    embed.setTitle("Service Statuses");
                    for(String name : services.keySet()) { // Automatically scale list of services
                        String serviceStatus = runningServices.containsKey(name) ? "Running\n" : "Stopped\n";

                        String serviceSleeping;
                        if (serviceStatus.equals("Running\n")) {
                            serviceSleeping = runningServices.get(name).sleeping ? "Sleeping" : "Processing";
                        } else {serviceSleeping = "N/A";}

                        embed.addField(services.get(name).capsName, serviceStatus + serviceSleeping, true);
                    }
                    event.getHook().sendMessageEmbeds(embed.build()).queue();
                } else {
                    String serviceSleeping;
                    if (runningServices.containsKey(service)) {
                        serviceSleeping = Boolean.toString(runningServices.get(service).sleeping);
                    } else {serviceSleeping = "N/A";}

                    embed.setThumbnail(services.get(service).imageURL)
                            .setTitle(services.get(service).capsName + " Status")
                            .addField("Running", Boolean.toString(runningServices.containsKey(service)), true)
                            .addField("Sleeping", serviceSleeping, true);
                    event.getHook().sendMessageEmbeds(embed.build()).queue();
                }
                break;
            }
            case "clear": {
                if (service.equals("all")) {
                    embed.setDescription("Attempting to clear all caches");

                    event.getHook().sendMessageEmbeds(embed.build()).queue();

                    for (String name : services.keySet()) {
                        FileIO.clearList(name);
                    }
                } else {
                    embed.setThumbnail(services.get(service).imageURL)
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
    public static void Redirect(Services service, String url) {
        Output.debugPrint(service, "Attempting redirect");
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) { // Test if browser allows going to URL from here
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}