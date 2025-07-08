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
#include <thread>
#include <string>
 
/* Function Prototypes*/
void instagramcrash();
void youtubecrash();
void send_webhook(std::string& message);
void color(int n);

/* Global Variables */
std::string BOT_TOKEN, WEBHOOK;
int CHANNEL_ID;
dpp::cluster bot;
bool DEBUGMODE; // Default to prevent unresolved external error
bool RESTART;
bool lastCoutWasReturn; // Used to track whether the last cout included a return statement \r to prevent spam

/* Start bot */
int main() {
    try {
        INIReader reader("../Config.ini");
        RESTART = reader.GetBoolean("General_Settings", "restart_on_crash", "false"); // Load restart_on_crash config setting
    }
    catch (const std::exception& e) { // Error handling
        std::cerr << "\nBot crashed: " << e.what();
        system("pause");
        return 1;
    }
	do { // Main loop for restarting bot on crash
        try {
			color(6); // Set color to default (yellow)
            std::cout << R"(
          @@@@@                      @@@@@@
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
                           @@@@     @@   @@   @@@@   @@   @@  @@@@@  @@        @@ @@   @@   @@ @@@@@   @@  v1.14
                            @@


         -------------------------------------------------------------------------------------------------------------

            )"; // Version incremented by 0.1 every day of (a decent amount of) work
            std::time_t t = std::time(nullptr); // Get timestamp
            std::tm tm_obj;
            localtime_s(&tm_obj, &t);
            std::cout << "\n\t" << std::put_time(&tm_obj, "%m/%d/%Y @ %H:%M:%S") << " - Loading Honeywasp";

            /* Load config data */
            INIReader reader("../Config.ini");
            BOT_TOKEN = reader.Get("General_Settings", "discord_bot_token", "");
            std::string AUTOSTART_RAW = reader.Get("General_Settings", "autostart", "none");
            boost::to_lower(AUTOSTART_RAW);
            boost::erase_all(AUTOSTART_RAW, " ");
            std::vector<std::string> AUTOSTART = split(AUTOSTART_RAW, ','); // Convert into list
            std::string WEBHOOK = reader.Get("General_Settings", "webhook_url", "Missing");
            bool DEBUGMODE = reader.GetBoolean("General_Settings", "debug_mode", false);

            /* Abort if token is missing */
            if (BOT_TOKEN.empty()) {
                std::cout << "Config.ini is missing bot token. Aborting...";
                system("pause");
                return 1;
            }

            t = std::time(nullptr); // Get timestamp
            localtime_s(&tm_obj, &t);
            std::cout << "\n\tConfig loaded";
            std::cout << "\n\tLoading discord - Discord client logs:";
            dpp::cluster bot(BOT_TOKEN);

            bot.on_log([&DEBUGMODE](const dpp::log_t& event) { // Discord client info output
                if (((dpp::utility::loglevel(event.severity) == "INFO") && event.message.find("Reconnecting shard") == std::string::npos) || (dpp::utility::loglevel(event.severity) == "ERROR") || (DEBUGMODE == true)) { // Only show discord client INFO logs to prevent flooding
                    color(6);
                    std::time_t t = std::time(nullptr); // Get timestamp
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - [" << "DISCORD " << dpp::utility::loglevel(event.severity) << "] " << event.message;
                    lastCoutWasReturn = false;
                }
                });



            /* Command events */
            bot.on_slashcommand([&bot](const dpp::slashcommand_t& event) {
                /* Check which command they ran */
                if (event.command.get_command_name() == "stop") { // Stop service
                    std::string service = std::get<std::string>(event.get_parameter("service"));
                    color(6);
                    lastCoutWasReturn = false;
                    if (service == "all") {
                        std::time_t t = std::time(nullptr); // Get timestamp
                        std::tm tm_obj;
                        localtime_s(&tm_obj, &t);
                        std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Stopping all services";
                        /* Reply to the command with embed.*/
                        dpp::embed embed = dpp::embed()
                            .set_color(0xFFA500)
                            .set_author("Honeywasp", "https://github.com/TruFoox/HoneyWasp", "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                            .set_thumbnail("https://images.icon-icons.com/2699/PNG/512/youtube_logo_icon_168737.png")
                            .add_field("Stopping all services", "");

                        /* Create a message with the content as our new embed. */
                        dpp::message msg(event.command.channel_id, embed);

                        /* Reply to the user with the message, containing our embed. */
                        event.reply(msg);
                        youtubeStop();
                        instagramStop();
                    }
                    if (service == "instagram") {
                        std::time_t t = std::time(nullptr); // Get timestamp
                        std::tm tm_obj;
                        localtime_s(&tm_obj, &t);
                        std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Stopping Instagram";
                        lastCoutWasReturn = true;
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
                        youtubeStop();
                    }
                }

                if (event.command.get_command_name() == "clear") { // Clear cache
                    color(6);
                    lastCoutWasReturn = false;
                    std::string service = std::get<std::string>(event.get_parameter("service"));

                    if (service == "instagram") {
                        std::time_t t = std::time(nullptr); // Get timestamp
                        std::tm tm_obj;
                        localtime_s(&tm_obj, &t);
                        std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Clearing cache";
                        lastCoutWasReturn = false;
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
                    color(6);
                    lastCoutWasReturn = false;
                    std::string service = std::get<std::string>(event.get_parameter("service")); // Fetch inputs from commands
                    if (service == "all") {
                        /* Reply to the command with embed.*/
                        dpp::embed embed = dpp::embed()
                            .set_color(0xFFA500)
                            .set_title("")
                            .set_author("Honeywasp", "https://github.com/TruFoox/HoneyWasp", "https://i.postimg.cc/gjqQ4CyJ/Untitled248-20250527215650.jpg")
                            .set_thumbnail("https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Instagram_icon.png/960px-Instagram_icon.png")
                            .add_field(
                                "Starting bot on all services",
                                "Use /stop to stop"
                            );
                        /* Create a message with the content as our new embed. */
                        dpp::message msg(event.command.channel_id, embed);

                        std::time_t t = std::time(nullptr); // Get timestamp
                        std::tm tm_obj;
                        localtime_s(&tm_obj, &t);
                        std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Starting Instagram";
                        std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Starting YouTube";
                        std::vector<std::thread> threads;
                        /* Reply to the user with the message, containing our embed. */
                        event.reply(msg);
                        std::cout << "\n\tStarting Instagram";
                        threads.emplace_back(instagram); // Run instagram() on new thread
                        std::cout << "\n\tStarting YouTube";
                        threads.emplace_back(youtube); // Run youtube() on new thread

                        for (std::thread& t : threads) {
                            if (t.joinable()) t.join();
                        }
                    }
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
                lastCoutWasReturn = false;
                if (dpp::run_once<struct register_bot_commands>()) {
                    /* Create a new global command on ready event */
                    dpp::slashcommand start_cmd("start", "Start running HoneyWasp", bot.me.id);
                    start_cmd.add_option(
                        dpp::command_option(dpp::co_string, "service", "The service you want to run HoneyWasp on", true)
                        .add_choice(dpp::command_option_choice("All", std::string("all")))
                        .add_choice(dpp::command_option_choice("Instagram", std::string("instagram")))
                        .add_choice(dpp::command_option_choice("Youtube", std::string("youtube")))
                    );

                    dpp::slashcommand stop_cmd("stop", "Stops the specified Honeywasp service", bot.me.id);
                    stop_cmd.add_option(
                        dpp::command_option(dpp::co_string, "service", "The service you want to stop", true)
                        .add_choice(dpp::command_option_choice("All", std::string("all")))
                        .add_choice(dpp::command_option_choice("Instagram", std::string("instagram")))
                        .add_choice(dpp::command_option_choice("Youtube", std::string("youtube")))
                    );

                    dpp::slashcommand clear_cmd("clear", "Clear HoneyWasp cache of specific service", bot.me.id);
                    clear_cmd.add_option(
                        dpp::command_option(dpp::co_string, "service", "The service you want to stop", true)
                        .add_choice(dpp::command_option_choice("All", std::string("all")))
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
                    std::cout << std::endl;
                    std::vector<std::thread> threads;
                    for (const std::string& entry : AUTOSTART) {
                        if (entry == "instagram") {
                            std::cout << "\n\tAutostarting Instagram";
                            threads.emplace_back(instagram); // Run instagram() on new thread
                        }
                        else if (entry == "youtube") {
                            std::cout << "\n\tAutostarting YouTube";
                            threads.emplace_back(youtube); // Run youtube() on new thread
                        }
                    }
                    std::cout << std::endl;
                    // Join all threads to ensure they complete before program exits
                    for (std::thread& t : threads) {
                        if (t.joinable()) t.join();
                    }
                }
                });
            bot.start(dpp::st_wait); // Finalize bot start


            return 0;
        }
        catch (const std::exception& e) { // Error handling
            std::cerr << "\n\tBot crashed: " << e.what() << '\n';
            if (!RESTART) {
                system("pause");
                return 1;
            }
        }
        catch (...) {
            std::cerr << "\n\tBot crashed with unknown error.\n";
            if (!RESTART) {
                system("pause");
                return 1;
            }
        }
    } while (RESTART);
}

void youtubecrash() { //On crash restart if enabled
    if (RESTART) {
        std::this_thread::sleep_for(std::chrono::seconds(5)); // Sleep 
        youtube(); // Restart youtube
    }
}

void instagramcrash() { // On crash restart if enabled
    if (RESTART) {
        std::this_thread::sleep_for(std::chrono::seconds(5)); // Sleep 
		instagram(); // Restart instagram
    }
}

void send_webhook(std::string& message) { // Send webhook
    try {
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
    catch (const std::exception& e) { // Error handling
        std::cerr << "\n\tWebhook error: " << e.what() << '\n';
    }
    catch (...) {
        std::cerr << "\n\tWebhook error with unknown error.\n";
	}

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