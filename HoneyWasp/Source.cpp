#include <dpp/dpp.h>
#include "INIReader.h"
#include <dpp/unicode_emoji.h>
#include <ctype.h>
#include <boost/algorithm/string.hpp>
#include "Source.h"
#include "instagram.h"
#include <ctime>
#include <iostream>
#include <windows.h>

/* Function Prototypes*/
void crash();
void send_webhook(std::string& message);

/* Global Variables */
std::string BOT_TOKEN, AUTOSTART, WEBHOOK;
int CHANNEL_ID;
bool DEBUG_MODE;
dpp::cluster bot;

/* Start bot */
int main() {
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
                      @@@@     @@   @@   @@@@   @@   @@  @@@@@  @@        @@ @@   @@   @@ @@@@@   @@  v0.06
                       @@


     -------------------------------------------------------------------------------------------------------------- 

    )"; // Version incremented by 0.1 every day of work
    std::cout << "\n\t" << "Loading Honeywasp";

    /* Load config data */
    INIReader reader("../Config.ini");
    std::string BOT_TOKEN = reader.Get("General_Settings", "discord_bot_token", "Missing");
    std::string AUTOSTART = reader.Get("General_Settings", "autostart", "Missing");
    boost::to_lower(AUTOSTART);
    std::string WEBHOOK = reader.Get("General_Settings", "webhook_url", "Missing");
    int CHANNEL_ID = std::stoi(reader.Get("General_Settings", "webhook_channel_id", "0"));
    bool DEBUG_MODE = reader.GetBoolean("General_Settings", "debug_mode", false);


    if (reader.ParseError() < 0 || BOT_TOKEN == "0") { // If on default value/error, abort
        std::cout << "Config.ini failed to load. Aborting...";
        return 1;
    }

    std::time_t t = std::time(nullptr); // Get timestamp
    std::tm tm_obj;
    localtime_s(&tm_obj, &t);
    std::cout << "\n\t" << std::put_time(&tm_obj, "%Y-%m-%d @ %H:%M:%S") << " - Config loaded";
    std::cout << "\n\t" << std::put_time(&tm_obj, "%Y-%m-%d @ %H:%M:%S") << " - Loading discord";
    dpp::cluster bot(BOT_TOKEN);
    std::cout << "\n\t" << "Discord token accepted! Discord client logs:";

    bot.on_log([&DEBUG_MODE](const dpp::log_t& event) { // Discord client info output
        if (((dpp::utility::loglevel(event.severity) == "INFO") && event.message.find("Reconnecting Shard") == std::string::npos) || (dpp::utility::loglevel(event.severity) == "ERROR") || (DEBUG_MODE == true)) { // Only show discord client INFO logs to prevent flooding
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
                    .set_author("Honeywasp", "https://github.com/TruFoox/HoneyWasp", "https://bit.ly/44SZP1F")
                    .set_thumbnail("https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Instagram_icon.png/960px-Instagram_icon.png")
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
                    .set_author("Honeywasp", "https://github.com/TruFoox/HoneyWasp", "https://bit.ly/44SZP1F")
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
                std::time_t t = std::time(nullptr); // Get timestamp
                std::tm tm_obj;
                localtime_s(&tm_obj, &t);
                std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Starting Youtube";
                // ...
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
            if (AUTOSTART == "instagram") {
                std::cout << "\n\n\tAutostarting Instagram\n";
                instagram();
            }
            else if (AUTOSTART == "youtube") {
                std::cout << "\n\n\tAutostarting YouTube\n";
            }
        }
        });
    bot.start(dpp::st_wait); // Finalize bot start


    return 0;
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