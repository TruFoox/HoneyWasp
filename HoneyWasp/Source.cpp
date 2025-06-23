#include <dpp/dpp.h>
#include "INIReader.h"
#include <dpp/unicode_emoji.h>
#include <ctype.h>
#include <boost/algorithm/string.hpp>
#include "Source.h"
#include "instagram.h"
#include "youtube.h"
#include <ctime>
#include <iostream>
#include <windows.h>
#include <vector>
#include <string>
 
/* Function Prototypes*/
void crash();
void send_webhook(std::string& message);
void color(int n);

/* Global Variables */
std::string BOT_TOKEN, WEBHOOK;
int CHANNEL_ID;
dpp::cluster bot;
bool DEBUGMODE = false; // Default to prevent unresolved external error
bool RESTART;

/* Start bot */
int main() {
    try {
        INIReader reader("../Config.ini");
        RESTART = reader.GetBoolean("General_Settings", "restart_on_crash", "false"); // Load restart_on_crash config setting
    }
    catch (const std::exception& e) { // Error handling
        std::cerr << "\n\tBot crashed: " << e.what() << '\n';
        system("pause");
        return 1;
    }
    do {
        try {
            color(6);
            std::cout << R"( 
             @@@@@                        @@@@@@
                 @@@                  @@@
                    @@              @@
                      @@@@@@@@@@@@@@
                    @@ @@        @@ @@
                   @    @@      @@    @
                  @ @@@            @@@ @
                  @ @@@@          @@@@ @
                  @ @@@@@        @@@@@ @
                  @  @@@@@      @@@@@  @
                   @   @@        @@   @
                    @@              @@
                     @@@          @@@
                     @@@@        @@@@
                     @@ @@@    @@@ @@
                        @@ @@@@@@  @@  @@   @@   @@@@   @@   @@  @@@@@ @@   @@ @@       @@  @@@    @@@@@  @@@@@
                        @@     @@      @@   @@  @@  @@  @@@  @@  @@     @@ @@  @@   @   @@  @@@   @@@     @@  @@
                        @@     @@      @@@@@@@ @@    @@ @@@@ @@  @@@@    @@@    @@ @@@ @@  @@ @@   @@@@   @@@@@
                              @@@@     @@   @@  @@  @@  @@ @@@@  @@      @@      @@@@@@@  @@@@@@     @@@  @@
                              @@@@     @@   @@   @@@@   @@   @@  @@@@@  @@        @@ @@   @@   @@ @@@@@   @@  v1.10
                               @@


             -------------------------------------------------------------------------------------------------------------- 

            )"; // Version incremented by 0.1 every day of work
            std::cout << "\n\t" << "Loading Honeywasp";

            /* Load config data */
            INIReader reader("../Config.ini");
            BOT_TOKEN = reader.Get("General_Settings", "discord_bot_token", "");
            std::string AUTOSTART_RAW = reader.Get("General_Settings", "autostart", "none");
            std::vector<std::string> AUTOSTART = split(AUTOSTART_RAW, ','); // Convert into list
            for (std::string& service : AUTOSTART) {
                boost::to_lower(service);
            }
            std::string WEBHOOK = reader.Get("General_Settings", "webhook_url", "Missing");
            bool DEBUGMODE = reader.GetBoolean("General_Settings", "debug_mode", false);

            /* Abort if token is missing */
            if (BOT_TOKEN.empty()) {
                std::cout << "Config.ini is missing bot token. Aborting...";
                system("pause");
                return 1;
            }

            std::time_t t = std::time(nullptr); // Get timestamp
            std::tm tm_obj;
            localtime_s(&tm_obj, &t);
            std::cout << "\n\t" << std::put_time(&tm_obj, "%Y-%m-%d @ %H:%M:%S") << " - Config loaded";
            std::cout << "\n\t" << std::put_time(&tm_obj, "%Y-%m-%d @ %H:%M:%S") << " - Loading discord";
            dpp::cluster bot(BOT_TOKEN);
            std::cout << "\n\t" << "Discord token accepted! Discord client logs:";

            bot.on_log([&DEBUGMODE](const dpp::log_t& event) { // Discord client info output
                if (((dpp::utility::loglevel(event.severity) == "INFO") && event.message.find("Reconnecting shard") == std::string::npos) || (dpp::utility::loglevel(event.severity) == "ERROR") || (DEBUGMODE == true)) { // Only show discord client INFO logs to prevent flooding
                    color(6);
                    std::time_t t = std::time(nullptr); // Get timestamp
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - [" << "DISCORD " << dpp::utility::loglevel(event.severity) << "] " << event.message;
                }
                });



            /* Command events */
            bot.on_slashcommand([&bot](const dpp::slashcommand_t& event) {
                /* Check which command they ran */
                if (event.command.get_command_name() == "stop") { // Stop service
                    std::string service = std::get<std::string>(event.get_parameter("service"));

                    if (service == "instagram") {
                        std::time_t t = std::time(nullptr); // Get timestamp
                        std::tm tm_obj;
                        localtime_s(&tm_obj, &t);
                        std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Stopping Instagram";
                        /* Reply to the command with embed.*/
                        dpp::embed embed = dpp::embed()
                            .set_color(0xFFA500)
                            .set_author("Honeywasp", "https://github.com/TruFoox/HoneyWasp", "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                            .set_thumbnail("https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Instagram_icon.png/960px-Instagram_icon.png")
                            .add_field("Stopping " + service, "");

                        /* Create a message with the content as our new embed. */
                        dpp::message msg(event.command.channel_id, embed);

                        /* Reply to the user with the message, containing our embed. */
                        event.reply(msg);
                        instagramStop();
                    }
                    if (service == "youtube") {
                        std::time_t t = std::time(nullptr); // Get timestamp
                        std::tm tm_obj;
                        localtime_s(&tm_obj, &t);
                        std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Stopping Youtube";
                        /* Reply to the command with embed.*/
                        dpp::embed embed = dpp::embed()
                            .set_color(0xFFA500)
                            .set_author("Honeywasp", "https://github.com/TruFoox/HoneyWasp", "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                            .set_thumbnail("https://images.icon-icons.com/2699/PNG/512/youtube_logo_icon_168737.png")
                            .add_field("Stopping " + service, "");

                        /* Create a message with the content as our new embed. */
                        dpp::message msg(event.command.channel_id, embed);

                        /* Reply to the user with the message, containing our embed. */
                        event.reply(msg);
                        instagramStop();
                    }
                }

                if (event.command.get_command_name() == "clear") { // Clear cache
                    std::string service = std::get<std::string>(event.get_parameter("service"));

                    if (service == "instagram") {
                        std::time_t t = std::time(nullptr); // Get timestamp
                        std::tm tm_obj;
                        localtime_s(&tm_obj, &t);
                        std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Clearing cache";
                        /* Reply to the command with embed.*/
                        dpp::embed embed = dpp::embed()
                            .set_color(0xFFA500)
                            .add_field("Instagram cache cleared", "");

                        /* Create a message with the content as our new embed. */
                        dpp::message msg(event.command.channel_id, embed);

                        /* Reply to the user with the message, containing our embed. */
                        event.reply(msg);
                        instagramClearCache();
                    }
                }
                if (event.command.get_command_name() == "start") { // Start service
                    std::string service = std::get<std::string>(event.get_parameter("service")); // Fetch inputs from commands

                    if (service == "instagram") {
                        /* Reply to the command with embed.*/
                        dpp::embed embed = dpp::embed()
                            .set_color(0xFFA500)
                            .set_title("")
                            .set_author("Honeywasp", "https://github.com/TruFoox/HoneyWasp", "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                            .set_thumbnail("https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Instagram_icon.png/960px-Instagram_icon.png")
                            .add_field(
                                "Starting bot on " + service,
                                "Use /stop to stop"
                            );
                        /* Create a message with the content as our new embed. */
                        dpp::message msg(event.command.channel_id, embed);

                        std::time_t t = std::time(nullptr); // Get timestamp
                        std::tm tm_obj;
                        localtime_s(&tm_obj, &t);
                        std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Starting Instagram";

                        /* Reply to the user with the message, containing our embed. */
                        event.reply(msg);
                        instagram();
                    }

                    if (service == "youtube") {
                        dpp::embed embed = dpp::embed()
                            .set_color(0xFFA500)
                            .set_title("")
                            .set_author("Honeywasp", "https://github.com/TruFoox/HoneyWasp", "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                            .set_thumbnail("https://images.icon-icons.com/2699/PNG/512/youtube_logo_icon_168737.png")
                            .add_field(
                                "Starting bot on " + service,
                                "Use /stop to stop"
                            );
                        /* Create a message with the content as our new embed. */
                        dpp::message msg(event.command.channel_id, embed);

                        /* Reply to the user with the message, containing our embed. */
                        event.reply(msg);
                        std::time_t t = std::time(nullptr); // Get timestampb
                        std::tm tm_obj;
                        localtime_s(&tm_obj, &t);
                        std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Starting Youtube";
                        youtube();
                    }
                }
                });

            /* Start/stop service command definitions */
            bot.on_ready([&bot, AUTOSTART](const dpp::ready_t& event) {
                if (dpp::run_once<struct register_bot_commands>()) {
                    /* Create a new global command on ready event */
                    dpp::slashcommand start_cmd("start", "Start running HoneyWasp", bot.me.id);
                    start_cmd.add_option(
                        dpp::command_option(dpp::co_string, "service", "The service you want to run HoneyWasp on", true)
                        .add_choice(dpp::command_option_choice("Instagram", std::string("instagram")))
                        .add_choice(dpp::command_option_choice("Youtube", std::string("youtube")))
                    );

                    dpp::slashcommand stop_cmd("stop", "Stops the specified Honeywasp service", bot.me.id);
                    stop_cmd.add_option(
                        dpp::command_option(dpp::co_string, "service", "The service you want to stop", true)
                        .add_choice(dpp::command_option_choice("Instagram", std::string("instagram")))
                        .add_choice(dpp::command_option_choice("Youtube", std::string("youtube")))
                    );

                    dpp::slashcommand clear_cmd("clear", "Clear HoneyWasp cache of specific service", bot.me.id);
                    clear_cmd.add_option(
                        dpp::command_option(dpp::co_string, "service", "The service you want to stop", true)
                        .add_choice(dpp::command_option_choice("Instagram", std::string("instagram")))
                        .add_choice(dpp::command_option_choice("Youtube", std::string("youtube")))
                    );

                    /* Register the commands */
                    bot.global_command_create(start_cmd);
                    bot.global_command_create(stop_cmd);
                    bot.global_command_create(clear_cmd);

                    /* Autostart */
                    std::time_t t = std::time(nullptr); // Get timestamp
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    for (const std::string& entry : AUTOSTART) {
                        if (entry == "instagram") {
                            std::cout << "\n\n\tAutostarting Instagram\n";
                            instagram();
                        }
                        else if (entry == "youtube") {
                            std::cout << "\n\n\tAutostarting YouTube\n";
                            youtube();
                        }
                        // Add more cases here as needed
                    }
                }
                });
            bot.start(dpp::st_wait); // Finalize bot start


            return 0;
        }
        catch (const std::exception& e) { // Error handling
            std::cerr << "\n\tBot crashed: " << e.what() << '\n';
            system("pause");
            return 1;
        }
        catch (...) {
            std::cerr << "\n\tBot crashed with unknown error.\n";
            system("pause");
            return 1;
        }
    } while (RESTART);
}

void crash() { // TBA apon crash do...
    // ...
}

void send_webhook(std::string& message) { // Send webhook
    dpp::webhook webhook;
    webhook.channel_id = CHANNEL_ID;
    webhook.name = "HoneyWasp";

    bot.create_webhook(webhook, [](const dpp::confirmation_callback_t& callback) {
        if (callback.is_error()) {
            std::cerr << "Failed to create webhook: " << callback.get_error().message << std::endl;
            return;
        }

        const dpp::webhook& created_webhook = std::get<dpp::webhook>(callback.value);

        dpp::message webhook_message("Hello from the webhook!");
        bot.execute_webhook(created_webhook, webhook_message, true);
        });


}

void color(int n) { // Set cout color
    SetConsoleTextAttribute(GetStdHandle(STD_OUTPUT_HANDLE), n);
}

std::vector<std::string> split(const std::string& str, char delimiter) { // Splits config string into string array
    std::vector<std::string> result;
    std::stringstream ss(str);
    std::string item;
    while (std::getline(ss, item, delimiter)) {
        if (!item.empty()) result.push_back(item);
    }
    return result;
}

std::vector<int> splitInts(const std::string& str, char delimiter) { // Splits config ints into int array
    std::vector<int> result;
    std::stringstream ss(str);
    std::string item;
    while (std::getline(ss, item, delimiter)) {
        try {
            result.push_back(std::stoi(item));
        }
        catch (...) {} // skip invalid numbers
    }
    return result;
}