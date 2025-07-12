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
#include "ImageUtils.h"
#include <nlohmann/json.hpp>
#include <thread>
#include <boost/algorithm/string.hpp>
#include "source.h"
#include "instagram.h"

using json = nlohmann::json; // Redefines json as one from nlohmann

/* Prototypes */
void youtubeStop();
static bool imageValidCheck(json data, bool& tempDisableCaption, int countattempt, bool& lastCoutWasReturn);


/* Global Variables */
static int TIME_BETWEEN_POSTS, ATTEMPTS_BEFORE_TIMEOUT, countattempt, YT_TIME_BETWEEN_POSTS;
static bool DUPLICATES_ALLOWED, NSFW_ALLOWED, USE_REDDIT_CAPTION, tempDisableCaption, imageValid, ytkeeploop;
static long long USER_ID;
static long http_code;
static std::string TOKEN, SUBREDDITS_RAW, BLACKLIST_RAW, CAPTION_BLACKLIST_RAW, FALLBACK_CAPTION, caption, HASHTAGS, imageURL;
static std::vector<std::string> SUBREDDITS, BLACKLIST, CAPTION_BLACKLIST, usedUrls, media;
static std::string SECRET, ID, YTPOSTMODE, OAUTHTOKEN, REFRESHTOKEN, ytresponse;

int youtube() {
    try {
        color(6); // Reset cout color to yellow (default)
        ytkeeploop = true;
        /* Load config data */
        INIReader reader("../Config.ini");
        std::string SECRET = reader.Get("Youtube_Settings", "client_secret", "");
        std::string ID = reader.Get("Youtube_Settings", "client_id", "");
        std::string YTPOSTMODE = reader.Get("Youtube_Settings", "post_mode", "manual");
        std::string REFRESHTOKEN = reader.Get("Youtube_Settings", "refresh_token", "");
        std::string CAPTION = reader.Get("Youtube_Settings", "caption", "I didnt set a caption like an idiot :p");
        std::string DESCRIPTION = reader.Get("Youtube_Settings", "description", "I didnt set a description like an idiot :p");
        int YT_TIME_BETWEEN_POSTS = std::stoi(reader.Get("Youtube_Settings", "time_between_posts", "60"));
        const bool DEBUGMODE = reader.GetBoolean("General_Settings", "debug_mode", false);
        boost::to_lower(YTPOSTMODE);
        if (YTPOSTMODE.empty()) YTPOSTMODE = "auto"; // Default to auto if not set
        std::string TOKEN = reader.Get("YouTube_Settings", "api_key", "");
        std::string timeBetweenPostsStr = reader.Get("YouTube_Settings", "time_between_posts", "");
        const int TIME_BETWEEN_POSTS = timeBetweenPostsStr.empty() ? 60 : std::stoi(timeBetweenPostsStr);
        std::string attemptsBeforeTimeoutStr = reader.Get("YouTube_Settings", "attempts_before_timeout", "");
        const int ATTEMPTS_BEFORE_TIMEOUT = attemptsBeforeTimeoutStr.empty() ? 50 : std::stoi(attemptsBeforeTimeoutStr);
        std::string SUBREDDITS_RAW = reader.Get("YouTube_Settings", "subreddits", "");
        if (SUBREDDITS_RAW.empty()) SUBREDDITS_RAW = "memes,meme,comedyheaven"; // Default subreddits if not set
        boost::erase_all(SUBREDDITS_RAW, " ");
        std::vector<std::string> SUBREDDITS = split(SUBREDDITS_RAW, ',');
        boost::to_lower(SUBREDDITS_RAW);
        std::string BLACKLIST_RAW = reader.Get("YouTube_Settings", "blacklist", "");
        if (BLACKLIST_RAW.empty()) BLACKLIST_RAW = "Fuck,Shit,Ass,Bitch,retard,republican,democrat"; // Default if empty
        boost::erase_all(BLACKLIST_RAW, " ");
        boost::to_lower(BLACKLIST_RAW);
        std::vector<std::string> BLACKLIST = split(BLACKLIST_RAW, ',');
        const bool DUPLICATES_ALLOWED = reader.GetBoolean("YouTube_Settings", "duplicates_allowed", false);
        std::string nsfwStr = reader.Get("YouTube_Settings", "nsfw_allowed", "");
        const bool NSFW_ALLOWED = nsfwStr.empty() ? false : reader.GetBoolean("YouTube_Settings", "nsfw_allowed", false);
        std::string captionStr = reader.Get("YouTube_Settings", "use_reddit_caption", "");
        const bool USE_REDDIT_CAPTION = captionStr.empty() ? false : reader.GetBoolean("YouTube_Settings", "use_reddit_caption", false);
        std::string CAPTION_BLACKLIST_RAW = reader.Get("YouTube_Settings", "caption_blacklist", "");
        if (CAPTION_BLACKLIST_RAW.empty()) CAPTION_BLACKLIST_RAW = "Fuck,Shit,Ass,Bitch,retard,republican,democrat"; // Default if empty
        boost::to_lower(CAPTION_BLACKLIST_RAW);
        boost::erase_all(CAPTION_BLACKLIST_RAW, " ");
        std::vector<std::string> CAPTION_BLACKLIST = split(CAPTION_BLACKLIST_RAW, ',');
        std::string FALLBACK_CAPTION = reader.Get("YouTube_Settings", "caption", "");
        std::string HASHTAGS = reader.Get("YouTube_Settings", "hashtags", "");

        std::string video_file;
        usedUrls.clear();
        /* Abort if any required value is default */
        if (SECRET.empty() || ID.empty()) {
            color(4);
            std::cout << "\n\tConfig.ini is missing required Youtube settings. Aborting...\n";
            return 1;
        }
        std::filesystem::path filePath = "../Cache/YT/youtube_used_urls.json"; // Gets used_urls.json size on disk to approximate length
        if (std::filesystem::file_size(filePath) > 100000) {
            std::cout << "\n\tYoutube cache is getting large. You should consider using /clear to clear your old URLS to prevent slowdowns\n";
            lastCoutWasReturn = false;
        }

        int countattempt = 0;
        ytkeeploop = true; // Ensures keeploop isnt false if restarted after /stop

        if (REFRESHTOKEN == "") { // If refresh token is not set, fetch it. Otherwise, run bot like normal
            std::string oauthURL = "https://accounts.google.com/o/oauth2/auth?client_id=" + ID + "&redirect_uri=http://localhost&response_type=code&scope=https://www.googleapis.com/auth/youtube.upload&access_type=offline&prompt=consent";
            std::cout << "\n\n\tBEFORE YOU CAN POST TO YOUTUBE, YOU MUST RETRIEVE YOUR ACCESS TOKEN. ATTEMPTING TO REDIRECT YOU TO THE AUTHENTICATION SITE NOW (OR GO TO " << oauthURL << ")\n";

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
                "&code=" + OAUTHTOKEN +
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
                color(4); // Set color to red
                localtime_s(&tm_obj, &t);
                std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - ERROR RETRIEVING REFRESH TOKEN: " << curl_easy_strerror(res) << std::endl;
            }
            if (DEBUGMODE) {
                std::time_t t = std::time(nullptr); // Get timestamp for output
                std::tm tm_obj;
                localtime_s(&tm_obj, &t);
                color(6); // Reset cout color to yellow (default)
                std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Token retrieval response:\n" << ytresponse << std::endl;
            }

            curl_easy_cleanup(curl);

            json jsonResponse = json::parse(ytresponse); // Parse the JSON response
            std::string refreshToken = jsonResponse["refresh_token"].get<std::string>(); // Janky fix to remove quotes from JSON string
            ytresponse.clear();
            std::cout << "\n\tPLEASE INPUT THE FOLLOWING INTO 'refresh_token' UNDER [YouTube_Settings] IN CONFIG.INI AND RE-RUN:\n\t" << refreshToken;
            return 0;
        }
        while (ytkeeploop) {
            color(6); // Reset cout color to yellow (default)
            std::time_t t = std::time(nullptr); // Get timestamp for output
            std::tm tm_obj;
            localtime_s(&tm_obj, &t);
            ytresponse.clear();

            if (countattempt == 0) { // Only output if on first post attempt
                std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Attempting new post\r";
                lastCoutWasReturn = true;
            }

            if (countattempt >= ATTEMPTS_BEFORE_TIMEOUT) { // Check if stuck in loop
                color(4);
                std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - HIT RETRY LIMIT. WAITING 3 CYCLES BEFORE RETRYING";
                color(6);
                lastCoutWasReturn = false;
                std::this_thread::sleep_for(std::chrono::seconds(TIME_BETWEEN_POSTS * 180));
            }
            countattempt++; // Iterate attempts to post during this cycle

            /* Generate access token */
            CURL* curl = curl_easy_init(); // Initialize curl
            if (!curl) {
                std::time_t t = std::time(nullptr); // Get timestamp for output
                std::tm tm_obj;
                color(4); // Set color to red
                localtime_s(&tm_obj, &t);
                std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - CURL INIT FAILED\n";
                youtubecrash();
                return 1;
            }

            std::string url = "https://oauth2.googleapis.com/token"; // OAuth token URL
            std::string body = "client_id=" + std::string(curl_easy_escape(curl, ID.c_str(), 0)) +
                "&client_secret=" + std::string(curl_easy_escape(curl, SECRET.c_str(), 0)) +
                "&refresh_token=" + REFRESHTOKEN + 
                "&grant_type=refresh_token";

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
                color(4); // Set color to red
                std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - FAILED TO GET ACCESS TOKEN: " << curl_easy_strerror(res) << "\n";
                curl_slist_free_all(token_headers);
                curl_easy_cleanup(curl);
                youtubecrash();
                return 1;
            }
            curl_easy_cleanup(curl); // Cleanup curl session after success
            curl_slist_free_all(token_headers); // Free the headers list

            auto data = nlohmann::json::parse(ytresponse); // Parse response string into JSON object

            if (data.contains("access_token")) {
                OAUTHTOKEN = data["access_token"];
            }
            else {
                std::time_t t = std::time(nullptr); // Get timestamp for output
                std::tm tm_obj;
                localtime_s(&tm_obj, &t);
                color(4); // Set color to red
                std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - NO ACCESS TOKEN FOUND IN RESPONSE:\n\t" << data.dump(2) << "\n";
                youtubecrash();
                return 1;
            }

            ytresponse.clear();
            /* Initiate post */
            if (YTPOSTMODE == "manual") {
                for (const auto& entry : std::filesystem::directory_iterator("../Videos")) { // Log all files in image/video directory
                    media.push_back(entry.path().generic_string());
                }

                if (media.empty()) {
                    color(4); // Set color to red
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - NO VIDEO FILES FOUND IN /Videos\n";
                    youtubecrash();
                    return 1;
                }

                // Randomize selection
                std::srand(static_cast<unsigned int>(std::time(nullptr)));
                int index = std::rand() % media.size();
                video_file = media[index];
            }
			else { // If post mode is auto, use the meme API to get image & convert to video
                /* Open and read used_urls.json */
                std::ifstream inFile("../Cache/YT/youtube_used_urls.json");
                json j;

                if (inFile) {
                    inFile >> j;// Parse JSON content from file
                    for (const auto& item : j) {
                        if (item.is_string()) { // Check if element is a string
                            usedUrls.push_back(item.get<std::string>()); // Convert JSON string to std::string and add to vector
                        }
                    }
                }
                inFile.close();

                json data;
                std::string chosenSubreddit;

                int randIndex = std::rand() % SUBREDDITS.size(); // Generate random index of subreddit
                chosenSubreddit = SUBREDDITS[randIndex];

                std::string apilink = "https://meme-api.com/gimme/" + chosenSubreddit; // Generate subreddit GET request URL

                /* Get post from meme-api*/
                CURL* curl;
                CURLcode res;
                curl_global_init(CURL_GLOBAL_DEFAULT);
                curl = curl_easy_init();
                if (curl) {
                    curl_easy_setopt(curl, CURLOPT_URL, apilink.c_str()); // Get data from meme-api
                    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
                    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &ytresponse);
                    curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);  // Follow redirect to new sites (fix for meme-api deprication)

                    res = curl_easy_perform(curl);

                    if (res != CURLE_OK) {
                        color(4);
                        std::time_t t = std::time(nullptr); // Get timestamp for output
                        std::tm tm_obj;
                        localtime_s(&tm_obj, &t);
                        color(4); // Set color to red
                        std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - CURL GET error: " << curl_easy_strerror(res);
                        std::cerr << "\n\tError details: " << ytresponse << std::endl;
                        lastCoutWasReturn = false;
                        color(6);
                        curl_easy_cleanup(curl);
                        curl_global_cleanup();
                        youtubecrash(); // Call crash function to handle the error
                        return 1;
                    }

                    http_code = 0;
                    curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &http_code); // Get HTTP status code

                    curl_easy_cleanup(curl);
                    curl_global_cleanup();
                    try {
                        data = json::parse(ytresponse);
                    }
                    catch (const std::exception& e) {
                        color(4);
                        std::cerr << "\n\tFailed to parse JSON: " << e.what() << "\n\tRaw response: " << ytresponse;
                        lastCoutWasReturn = false;
                        color(6);
                        youtubecrash(); // Optional: handle gracefully
                        return 1;
                    }
                }
                else {
                    curl_global_cleanup();
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    color(4); // Set color to red
                    std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Failed to initialize CURL";
                    lastCoutWasReturn = false;
                    return 1;
                }
                if (DEBUGMODE) {
                    color(6); // Reset cout color to yellow (default)
                    std::cout << "\n\tHTTP CODE : " << http_code << "\n\tJSON DATA : " << data.dump(1, '\t'); 
                    lastCoutWasReturn = false;
                }
                /* Read JSON data and attempt post*/
                if (http_code == 200) { // Ensure GET success
                    imageURL = data["url"];
                    caption = data["title"];
                    bool nsfw = data["nsfw"];
                    bool tempDisableCaption = false;
					video_file = "../Cache/YT/temp.avi"; // Set video file path
                    if (DEBUGMODE) {
                        color(6); // Reset cout color to yellow (default)
                        std::cout << "\n\tGET success";
                        lastCoutWasReturn = false;
                    }
                    imageValid = imageValidCheck(data, tempDisableCaption, countattempt, lastCoutWasReturn); // Test if image is valid for posting
                    if (imageValid) { // If image is not valid, skip to next iteration
						if (!image_to_video(imageURL)) { // Convert image to video - if failed, set imageValid to false (ImageUtils.h)
                            imageValid = false;
                        }
					}
                }
                else if (http_code == 530) { // If cloudfare error, give notice
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    color(4); // Set color to red
                    std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Failed. Cloudfare HTTP Status Code 530 - The API this program utilizes appears to be under maintenence.\n\tThere is nothing that can be done to fix this but wait. Skipping attempt w/ +6 hour delay...";
                    lastCoutWasReturn = false;
                    std::this_thread::sleep_for(std::chrono::seconds(21600)); // Sleep 6h
                    imageValid = false; // Set imageValid to false to skip current post attempt
                }
                else { // Other error handling
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    color(4); // Set color to red
                    std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - YOUTUBE HTTP GET ERROR " << http_code << ": \n\t" << data;
                    lastCoutWasReturn = false;
                    std::this_thread::sleep_for(std::chrono::seconds(60)); // Sleep to prevent spam
                    imageValid = false; // Set imageValid to false to skip current post attempt
                }

                ytresponse.clear();

                if (!imageValid) { // If image is not valid, skip to next iteration
                    continue;
				}

                if (!lastCoutWasReturn) {
                    std::cout << "\n"; // If last cout was not a return, print newline
                }

                /* Post to youtube */
                std::ifstream infile(video_file); // Check if file exists
                if (!infile.good()) {
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    color(4); // Set color to red
                    std::cerr << "\t" << std::put_time(&tm_obj, "%H:%M") << " - VIDEO FILE NOT FOUND: " << video_file << "\n";
                    youtubecrash();
                    return 1;
                }

                nlohmann::json metadata;
                if (YTPOSTMODE == "manual" || (USE_REDDIT_CAPTION == false || tempDisableCaption == true)) { // Sets caption to either defined or post caption
                    metadata = {
                        {"snippet", {
                            {"title", CAPTION},
                            {"description", DESCRIPTION},
                            {"tags", {"meme", "memes"}},
                            {"categoryId", "24"}  // Entertainment
                        }},
                        {"status", {
                            {"privacyStatus", "public"},
                            {"selfDeclaredMadeForKids", false}
                        }}
                    };
                }
                else { // If USE_REDDIT_CAPTION is true, use reddit caption
                    std::string caption = data["title"];
                    metadata = {
                        {"snippet", {
                            {"title", caption},
                            {"description", DESCRIPTION},
                            {"tags", {"meme", "memes"}},
                            {"categoryId", "24"}  // Entertainment
                        }},
                        {"status", {
                            {"privacyStatus", "public"},
                            {"selfDeclaredMadeForKids", false}
                        }}
                    };
                }

                std::string metadata_str = metadata.dump(); // Convert metadata JSON to string

                curl = curl_easy_init(); // Init curl
                if (!curl) {
                    color(4); // Set color to red
                    std::cerr << "\tCurl init failed\n";
                    youtubecrash();
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
                    color(4); // Set color to red
                    std::cout << "\tNO FILE EXTENSION FOUND FOR: " << video_file << "\n"; // If no extension found
                    youtubecrash();
                    return 1;  // No extension found
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
                curl_easy_setopt(curl, CURLOPT_WRITEDATA, &ytresponse); // Set output string

                res = curl_easy_perform(curl); // Execute the POST request
                if (res != CURLE_OK) {
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    color(4); // Set color to red
                    std::cerr << "\t" << std::put_time(&tm_obj, "%H:%M") << " - UPLOAD FAILED: " << curl_easy_strerror(res) << "\n";
                    curl_easy_cleanup(curl);
                    curl_slist_free_all(headers);
                    youtubecrash();
                    return 1;
                }

                curl_easy_cleanup(curl); // Clean up curl handle
                curl_slist_free_all(headers); // Free header list

                data = nlohmann::json::parse(ytresponse); // Try to parse JSON response
                std::string reason, message;
                if (data.contains("error")) {
                    color(4); // Set color to red
                    if (data["error"].contains("errors") && data["error"]["errors"][0].contains("reason") && data["error"].contains("message")) { // Check if error structure is valid for reading
                        reason = data["error"]["errors"][0]["reason"];
                        message = data["error"]["message"];
                        if (reason == "uploadLimitExceeded" || reason == "rateLimitExceeded" || reason == "quotaExceeded") { // If ratelimit hit
                            std::time_t t = std::time(nullptr); // Get timestamp for output
                            std::tm tm_obj;
                            localtime_s(&tm_obj, &t);
                            color(4); // Set color to red
                            std::cerr << "\t" << std::put_time(&tm_obj, "%H:%M") << " - UPLOAD FAILED: RATELIMITED BY YOUTUBE (INCREASE TIME_BETWEEN_POSTS IN CONFIG.INI)";
                        }
                        else { // General error
                            std::time_t t = std::time(nullptr); // Get timestamp for output
                            std::tm tm_obj;
                            localtime_s(&tm_obj, &t);
                            color(4); // Set color to red
                            std::cerr << "\t" << std::put_time(&tm_obj, "%H:%M") << " - UPLOAD FAILED: " << message << "\n";
                        }
                    }
                    else { // if error structure is not valid print full response (last resort)
                        std::time_t t = std::time(nullptr); // Get timestamp for output
                        std::tm tm_obj;
                        localtime_s(&tm_obj, &t);
                        color(4); // Set color to red
                        std::cerr << "\t" << std::put_time(&tm_obj, "%H:%M") << " - UPLOAD FAILED: " << curl_easy_strerror(res) << "\n";
                        curl_easy_cleanup(curl);
                        curl_slist_free_all(headers);
                    }
                }
                else {
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    if (DEBUGMODE) {
                        std::cout << std::endl << data.dump(4);
                    }
                    color(2); // Set color to green
                    if (YTPOSTMODE == "manual") {
                        std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - " << video_file << " uploaded to YouTube"; // Print success message
                    }
                    else {
						std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - " << imageURL << " uploaded to YouTube";
					}
                }
                lastCoutWasReturn = false;

                /* Begin export of URL to file */
                inFile.open("../Cache/YT/youtube_used_urls.json");
                if (inFile) {
                    if (inFile.peek() == std::ifstream::traits_type::eof()) { // If file is empty, start with empty array
                        j = json::array();
                    }
                    else { // If file has content, parse it
                        inFile >> j;
                        if (!j.is_array()) j = json::array();
                    }
                    inFile.close();
                }
                else { // If file doesn't exist, start with empty array
                    j = json::array();
                }

                j.push_back(imageURL); // Append element to JSON
                usedUrls.push_back(imageURL); // Append element to memory
                std::ofstream outFile("../Cache/YT/youtube_used_urls.json");
                outFile << j.dump(4);
                outFile.close();

                std::this_thread::sleep_for(std::chrono::seconds(YT_TIME_BETWEEN_POSTS * 60)); // Sleep
                countattempt = 0; // Reset number of attempts to post this cycle
            }
        }
        return 0;
    }
    catch (const std::exception& e) { // Error handling
        std::cerr << "\n\tYoutube crashed: " << e.what() << '\n';

        lastCoutWasReturn = false;
        youtubecrash();
        return 1;
    }
    catch (...) {
        std::cerr << "\n\tYoutube crashed with unknown error.\n";

        lastCoutWasReturn = false;
        youtubecrash();
        return 1;
    }
    return 0;
}


void youtubeStop() {
    ytkeeploop = false; // Stop the loop
}

static bool imageValidCheck(json data, bool& tempDisableCaption, int countattempt, bool& wasreturn) { // Ensure image is valid for instagram
    color(4); // Set color to red
    std::string imageURL = data["url"];
    std::string caption = data["title"];
    boost::to_lower(imageURL);
    boost::to_lower(caption);
    bool nsfw = data["nsfw"];
    std::time_t t = std::time(nullptr); // Get timestamp for output
    std::tm tm_obj;
    localtime_s(&tm_obj, &t);
    if (imageURL.find(".gif") != std::string::npos) { // If file is gif, skip
        if (!lastCoutWasReturn) {
            std::cout << "\n";
        }
        else {
            clear();
        }
        std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - Image is GIF - x" << countattempt << " Attempt(s)\r";
        wasreturn = true; // Set true so next cout knows to not print on newline
        return false;
    }

    for (int i = 0; i < CAPTION_BLACKLIST.size(); i++) {
        if (caption.find(CAPTION_BLACKLIST[i]) != std::string::npos) { // Test if reddit title contains blackslisted string. Use defined caption if it does ( DOES NOT FLAG AS INVALID )
            tempDisableCaption = true;
            if (!lastCoutWasReturn) {
                std::cout << "\n";
            }
            else {
                clear();
            }
            std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - Using fallback caption - x" << countattempt << " Attempt(s)\r";
            wasreturn = true; // Set true so next cout knows to not print on newline
        }
    }

    for (int i = 0; i < BLACKLIST.size(); i++) {
        if (caption.find(BLACKLIST[i]) != std::string::npos) { // Test if reddit title contains blackslisted string. Block if it does
            if (!lastCoutWasReturn) {
                std::cout << "\n";
            }
            else {
                clear();
            }
            std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - Caption contains blacklisted string - x" << countattempt << " Attempt(s)\r";
            wasreturn = true; // Set true so next cout knows to not print on newline
            return false;
        }
    }

    for (int i = 0; i < usedUrls.size(); i++) { // Test if URL is duplicate
        if (imageURL == usedUrls[i]) {
            if (!lastCoutWasReturn) {
                std::cout << "\n";
            }
            else {
                clear();
            }
            std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - Duplicate URL - x" << countattempt << " Attempt(s)\r";
            wasreturn = true;
            return false;
        }
    }
    if (NSFW_ALLOWED == false && nsfw == true) { // If NSFW is disabled & post is marked as NSFW, return false
        if (!lastCoutWasReturn) {
            std::cout << "\n";
        }
        else {
            clear();
        }
        std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - Image is marked as NSFW - x" << countattempt << " Attempt(s)\r";
        wasreturn = true;
        return false;
    }

    color(6);

    return true;
}