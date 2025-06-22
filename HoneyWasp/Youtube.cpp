#define NOMINMAX
#include <iostream>
#include <curl/curl.h>
#include "INIReader.h"
#include <vector>
#include <stdlib.h>
#include <time.h>
#include <fstream>
#include <filesystem>
#include <sstream>
#include <chrono>
#include <nlohmann/json.hpp>
#include <thread>
#include <boost/algorithm/string.hpp>
#include "source.h"
#include "instagram.h"

using json = nlohmann::json; // redefines json as one from nlohmann

/* Prototypes */
void youtubeStop();

/* Global Variables */
std::string SECRET, ID, YTPOSTMODE, OAUTHTOKEN, REFRESHTOKEN, ytresponse;
std::vector<std::string> media;
long ythttp_code;
bool ytkeeploop;
int YT_TIME_BETWEEN_POSTS;

int youtube() {
    try {
        ytkeeploop = true;

        /* Load config data */
        INIReader reader("../Config.ini");
        std::string SECRET = reader.Get("Youtube_Settings", "client_secret", "");
        std::string ID = reader.Get("Youtube_Settings", "client_id", "");
        int TIME_BETWEEN_POSTS = std::stoi(reader.Get("Instagram_Settings", "time_between_posts", "60"));
        std::string YTPOSTMODE = reader.Get("Youtube_Settings", "post_mode", "manual");
        std::string REFRESHTOKEN = reader.Get("Youtube_Settings", "refresh_token", "");
        std::string CAPTION = reader.Get("Youtube_Settings", "caption", "I didnt set a caption like an idiot :p");
        std::string DESCRIPTION = reader.Get("Youtube_Settings", "description", "I didnt set a description like an idiot :p");
        int YT_TIME_BETWEEN_POSTS = std::stoi(reader.Get("Youtube_Settings", "time_between_posts", "60"));
        boost::to_lower(YTPOSTMODE);

        if (REFRESHTOKEN == "") { // If refresh token is not set, fetch it
            std::string oauthURL = "https://accounts.google.com/o/oauth2/auth?client_id=" + ID + "&redirect_uri=http://localhost&response_type=code&scope=https://www.googleapis.com/auth/youtube.upload&access_type=offline&prompt=consent";
            std::cout << "\n\n\tBEFORE YOU CAN POST, YOU MUST RETRIEVE YOUR ACCESS TOKEN. ATTEMPTING TO REDIRECT YOU TO THE AUTHENTICATION SITE NOW (" << oauthURL << ")\n";

            std::string command = "start \"\" \"" + oauthURL + "\""; // convert to string cmd for Windows
            system(command.c_str()); // Launch OAuth URL in default browser

            std::cout << "\n\tPLEASE INPUT THE AUTHORIZATION CODE YOU RECEIVED AFTER GRANTING ACCESS (SEE https://github.com/TruFoox/HoneyWasp FOR HELP):\n";
            std::cin >> OAUTHTOKEN; // Get the authorization code from user input

            std::string redirect_uri = "http://localhost";

            /* Get refresh token */
            CURL* curl = curl_easy_init();
            std::string postFields =
                "client_id=" + ID +
                "&client_secret=" + SECRET +
                "&code=" + curl_easy_escape(curl, OAUTHTOKEN.c_str(), 0) +
                "&grant_type=authorization_code" +
                "&redirect_uri=" + curl_easy_escape(curl, redirect_uri.c_str(), 0);

            curl_easy_setopt(curl, CURLOPT_URL, "https://oauth2.googleapis.com/token");
            curl_easy_setopt(curl, CURLOPT_POSTFIELDS, postFields.c_str());
            curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
            curl_easy_setopt(curl, CURLOPT_WRITEDATA, &ytresponse);

            CURLcode res = curl_easy_perform(curl);
            if (res != CURLE_OK) {
                std::time_t t = std::time(nullptr); // Get timestamp for output
                std::tm tm_obj;
                localtime_s(&tm_obj, &t);
                std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " -  Error retrieving refresh token: " << curl_easy_strerror(res) << std::endl;
            }
            else if (DEBUGMODE == true) {
                std::time_t t = std::time(nullptr); // Get timestamp for output
                std::tm tm_obj;
                localtime_s(&tm_obj, &t);
                std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " -  Response:\n" << ytresponse << std::endl;
            }

            ytresponse.clear();
            curl_easy_cleanup(curl);

            json jsonResponse = json::parse(ytresponse); // Parse the JSON response
            std::string refreshToken = jsonResponse["refresh_token"].get<std::string>(); // Janky fix to remove quotes from JSON string

            std::cout << "\n\tPLEASE INPUT THE FOLLOWING INTO 'refresh_token' UNDER [Youtube_Settings] IN CONFIG.INI AND RE-RUN:\n" << refreshToken;
            return 0;
        }
        while (ytkeeploop) {
            color(6); // Reset cout color to yellow (default)
            ytresponse.clear();

            /* Generate access token */
            CURL* curl = curl_easy_init(); // Initialize curl
            if (!curl) {
                std::time_t t = std::time(nullptr); // Get timestamp for output
                std::tm tm_obj;
                localtime_s(&tm_obj, &t);
                std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " -  Curl init failed\n";
                return 1;
            }

            std::string url = "https://oauth2.googleapis.com/token"; // OAuth token URL
            std::string body = "client_id=" + std::string(curl_easy_escape(curl, ID.c_str(), 0)) +
                "&client_secret=" + std::string(curl_easy_escape(curl, SECRET.c_str(), 0)) +
                "&refresh_token=" + std::string(curl_easy_escape(curl, REFRESHTOKEN.c_str(), 0)) +
                "&grant_type=refresh_token"; // Prepare POST fields

            struct curl_slist* token_headers = nullptr; // Initialize empty linked list of headers
            token_headers = curl_slist_append(token_headers, "Content-Type: application/x-www-form-urlencoded"); // Set content type header for form data
            curl_easy_setopt(curl, CURLOPT_URL, url.c_str()); // Set the URL for the request
            curl_easy_setopt(curl, CURLOPT_POSTFIELDS, body.c_str()); // Set the POST fields
            curl_easy_setopt(curl, CURLOPT_HTTPHEADER, token_headers); // Set HTTP headers for the request
            curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback); // Set callback function to capture response
            curl_easy_setopt(curl, CURLOPT_WRITEDATA, &ytresponse); // Provide string to store the response

            CURLcode res = curl_easy_perform(curl); // Perform HTTP POST request

            if (res != CURLE_OK) { // Check if request was successful
                std::time_t t = std::time(nullptr); // Get timestamp for output
                std::tm tm_obj;
                localtime_s(&tm_obj, &t);
                std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " -  Failed to get access token: " << curl_easy_strerror(res) << "\n";
                curl_slist_free_all(token_headers);
                curl_easy_cleanup(curl);
                return 1;
            }
            curl_easy_cleanup(curl); // Cleanup curl session after success
            curl_slist_free_all(token_headers); // Free the headers list


            auto json = nlohmann::json::parse(ytresponse); // Parse response string into JSON object

            if (json.contains("access_token")) {
                OAUTHTOKEN = json["access_token"];
            }
            else {
                std::time_t t = std::time(nullptr); // Get timestamp for output
                std::tm tm_obj;
                localtime_s(&tm_obj, &t);
                std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " -  No access token found in response:\n" << json.dump(2) << "\n";
                return 1;
            }

            /* Initiate post */
            if (YTPOSTMODE == "manual") {
                for (const auto& entry : std::filesystem::directory_iterator("../Videos")) { // Log all files in image/video directory
                    media.push_back(entry.path().string());
                }

                if (media.empty()) {
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " -  No video files found in /Videos\n";
                    return 1;
                }

                // Randomize selection
                std::srand(static_cast<unsigned int>(std::time(nullptr)));
                int index = std::rand() % media.size();
                std::string video_file = media[index];


                std::string response;   // Store server response

                // Check if file exists
                std::ifstream infile(video_file);
                if (!infile.good()) {
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " -  Video file not found: " << video_file << "\n";
                    return 1;
                }

                nlohmann::json metadata = { // Create metadata JSON body
                    {"snippet", {
                        {"title", CAPTION},
                        {"description", DESCRIPTION},
                        {"tags", {"meme", "memes", "lol"}},
                        {"categoryId", "24"}  // Entertainment
                    }},
                    {"status", {
                        {"privacyStatus", "public"},
                        {"selfDeclaredMadeForKids", false}
                    }}
                };

                std::string metadata_str = metadata.dump(); // Convert metadata JSON to string

                curl = curl_easy_init(); // Init curl
                if (!curl) {
                    std::cerr << "Curl init failed\n";
                    return 1;
                }

                // Full URL to upload endpoint
                url = "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=multipart&part=snippet,status";

                // Build authorization header
                struct curl_slist* headers = nullptr;
                headers = curl_slist_append(headers, ("Authorization: Bearer " + OAUTHTOKEN).c_str()); // OAuth2 header
                headers = curl_slist_append(headers, "Content-Type: multipart/related; boundary=foo_bar_baz"); // Manual boundary
                headers = curl_slist_append(headers, "Accept: application/json"); // Response type

                // ---- Construct multipart body manually (video + JSON metadata) ----
                std::ifstream video_stream(video_file, std::ios::binary);  // Open video file in binary mode
                std::string video_data((std::istreambuf_iterator<char>(video_stream)), std::istreambuf_iterator<char>()); // Read all video bytes

                size_t pos = video_file.rfind('.');
                if (pos == std::string::npos) {
                    std::cout << "\n\tNo file extension found for: " << video_file << "\n"; // If no extension found
                    return 0;  // No extension found
                }

                std::string multipart_body =
                    "--foo_bar_baz\r\n"
                    "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                    metadata_str + "\r\n" +
                    "--foo_bar_baz\r\n"
                    "Content-Type: video/" + video_file.substr(pos + 1) + "\r\n\r\n" +  // Use file extension for content type
                    video_data + "\r\n" +
                    "--foo_bar_baz--\r\n"; // End boundary

                curl_easy_setopt(curl, CURLOPT_URL, url.c_str()); // Set upload URL
                curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers); // Set headers
                curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE_LARGE, static_cast<curl_off_t>(multipart_body.size()));
                curl_easy_setopt(curl, CURLOPT_POSTFIELDS, multipart_body.c_str()); // Actual body
                curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback); // Capture response
                curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response); // Set output string

                res = curl_easy_perform(curl); // Execute the POST request
                if (res != CURLE_OK) {
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Upload failed: " << curl_easy_strerror(res) << "\n";
                    curl_easy_cleanup(curl);
                    curl_slist_free_all(headers);
                    return 1;
                }

                curl_easy_cleanup(curl); // Clean up curl handle
                curl_slist_free_all(headers); // Free header list

                try {
                    auto json = nlohmann::json::parse(response); // Try to parse JSON response
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    color(2); // Set color to green
                    std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - " << video_file << " SUCCESSFULLY uploaded to YouTube"; // Print video ID
                    if (DEBUGMODE == true) {
                        std::cout << json.dump(4) << "\n";
                    }
                }
                catch (...) {
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Could not parse response:\n" << response << "\n"; // Fallback if parsing fails
                }
                std::this_thread::sleep_for(std::chrono::seconds(YT_TIME_BETWEEN_POSTS * 60)); // Sleep
            }
        } 
        return 0;
    }
    catch (const std::exception& e) { // Error handling
        std::cerr << "\n\tYoutube crashed: " << e.what() << '\n';
        crash();
        return 1;
    }
    catch (...) {
        std::cerr << "\n\tYoutube crashed with unknown error.\n";
        crash();
        return 1;
    }
    return 0;
}


void youtubeStop() {
    ytkeeploop = false; // Stop the loop
}