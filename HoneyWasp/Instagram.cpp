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
#include "ImageUtils.h"
#include "instagram.h"

using json = nlohmann::json; // Redefines json as one from nlohmann


/* Prototypes */
size_t WriteCallback(void* contents, size_t size, size_t nmemb, void* userp);
json HTTP_Post(const std::string& base_url, long long& http_code, const std::map<std::string, std::string>& params = {});
void instagramStop();
bool imageValidCheck(json data, bool& tempDisableCaption, int countattempt, bool& lastCoutWasReturn);


/* Global variables */
static int TIME_BETWEEN_POSTS, ATTEMPTS_BEFORE_TIMEOUT, countattempt;
static long long USER_ID, http_code;
static bool DUPLICATES_ALLOWED, NSFW_ALLOWED, USE_REDDIT_CAPTION, keeploop, tempDisableCaption, imageValid;
static std::string TOKEN, FALLBACK_CAPTION, caption, tempstring, HASHTAGS, POSTMODE, mediaURL, fileDir, redditURL;
static std::vector<std::string> SUBREDDITS, BLACKLIST, CAPTION_BLACKLIST, usedUrls, media;

/* Starts sending API calls to post to instagram */
int instagram() {
    try {
		std::this_thread::sleep_for(std::chrono::seconds(1)); // Desynchronize with YouTube (Prevents outputting at the same time) 

        /* Load config data */
        INIReader reader("../Config.ini");

        std::string TOKEN = reader.Get("Instagram_Settings", "api_key", "");
        std::string POSTMODE = reader.Get("Instagram_Settings", "post_mode", "");
        if (POSTMODE.empty()) POSTMODE = "auto"; // Default to auto if not set
        boost::to_lower(POSTMODE);
        std::string timeBetweenPostsStr = reader.Get("Instagram_Settings", "time_between_posts", "");
        const int TIME_BETWEEN_POSTS = timeBetweenPostsStr.empty() ? 60 : std::stoi(timeBetweenPostsStr);
        std::string attemptsBeforeTimeoutStr = reader.Get("Instagram_Settings", "attempts_before_timeout", "");
        const int ATTEMPTS_BEFORE_TIMEOUT = attemptsBeforeTimeoutStr.empty() ? 50 : std::stoi(attemptsBeforeTimeoutStr);
        std::string SUBREDDITS_RAW = reader.Get("Instagram_Settings", "subreddits", "");
        if (SUBREDDITS_RAW.empty()) SUBREDDITS_RAW = "memes,meme,comedyheaven"; // Default subreddits if not set
        boost::erase_all(SUBREDDITS_RAW, " ");
        boost::to_lower(SUBREDDITS_RAW);
        std::vector<std::string> SUBREDDITS = split(SUBREDDITS_RAW, ',');
        std::string FORMAT = reader.Get("Instagram_Settings", "format", "video");
        boost::to_lower(FORMAT);
        std::string BLACKLIST_RAW = reader.Get("Instagram_Settings", "blacklist", "");
        if (BLACKLIST_RAW.empty()) BLACKLIST_RAW = "Fuck,Shit,Ass,Bitch,retard,republican,democrat"; // Default if empty
        boost::erase_all(BLACKLIST_RAW, " ");
        boost::to_lower(BLACKLIST_RAW);
        std::vector<std::string> BLACKLIST = split(BLACKLIST_RAW, ',');
        std::string duplicatesStr = reader.Get("Instagram_Settings", "duplicates_allowed", "");
        const bool DUPLICATES_ALLOWED = duplicatesStr.empty() ? false : reader.GetBoolean("Instagram_Settings", "duplicates_allowed", false);
        std::string nsfwStr = reader.Get("Instagram_Settings", "nsfw_allowed", "");
        const bool NSFW_ALLOWED = nsfwStr.empty() ? false : reader.GetBoolean("Instagram_Settings", "nsfw_allowed", false);
        std::string captionStr = reader.Get("Instagram_Settings", "use_reddit_caption", "");
        const bool USE_REDDIT_CAPTION = captionStr.empty() ? false : reader.GetBoolean("Instagram_Settings", "use_reddit_caption", false);
        std::string CAPTION_BLACKLIST_RAW = reader.Get("Instagram_Settings", "caption_blacklist", "");
        if (CAPTION_BLACKLIST_RAW.empty()) CAPTION_BLACKLIST_RAW = "Fuck,Shit,Ass,Bitch,retard,republican,democrat"; // Default if empty
        boost::to_lower(CAPTION_BLACKLIST_RAW);
        boost::erase_all(CAPTION_BLACKLIST_RAW, " ");
        std::vector<std::string> CAPTION_BLACKLIST = split(CAPTION_BLACKLIST_RAW, ',');
        std::string FALLBACK_CAPTION = reader.Get("Instagram_Settings", "caption", "");
        std::string HASHTAGS = reader.Get("Instagram_Settings", "hashtags", "");

        int countattempt = 0;
        keeploop = true; // Ensures keeploop isnt false if restarted after /stop
        imageValid = true;

        /* Abort if any required value is default */
        if (TOKEN.empty() || (POSTMODE == "auto" && SUBREDDITS_RAW.empty())) {
            color(4);
            std::cout << "\n\tConfig.ini is missing required Instagram settings. Aborting...\n";
            lastCoutWasReturn = false;
            return 1;
        }

        /* Get User ID */
        CURL* curl;
        CURLcode res;
        std::string response;

        std::string apilink = "https://graph.facebook.com/v19.0/me/accounts?access_token=" + TOKEN; // Get Facebook page ID

        curl = curl_easy_init();
        if (curl) {
            curl_easy_setopt(curl, CURLOPT_URL, apilink.c_str());
            curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
            curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response);

            res = curl_easy_perform(curl);

            if (res != CURLE_OK) {
                std::cerr << "\n\tRequest failed: " << curl_easy_strerror(res) << std::endl;
                curl_easy_cleanup(curl);
                return 1;
            }

            std::string pageID;
            try {
                auto jsonResp = json::parse(response);
                if (!jsonResp.contains("data") || jsonResp["data"].empty()) {
                    std::cerr << "\n\tToken valid, but no Facebook Pages returned.\n";
                    std::cout << response;
                    curl_easy_cleanup(curl);
                    return 1;
                }

                pageID = jsonResp["data"][0]["id"].get<std::string>();
            }
            catch (...) {
                std::cerr << "\n\tFailed to parse Facebook Page ID from response.\n";
                std::cout << response;
                curl_easy_cleanup(curl);
                return 1;
            }

            response.clear();

            apilink = "https://graph.facebook.com/v19.0/" + pageID + "?fields=instagram_business_account&access_token=" + TOKEN; // Get Instagram ID from Page
            curl_easy_setopt(curl, CURLOPT_URL, apilink.c_str());
            curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response);

            res = curl_easy_perform(curl);

            if (res != CURLE_OK) {
                std::cerr << "\n\tRequest failed: " << curl_easy_strerror(res) << std::endl;
                curl_easy_cleanup(curl);
                return 1;
            }

            try {
                auto jsonResp = json::parse(response);

                if (!jsonResp.contains("instagram_business_account")) {
                    std::cerr << "\n\tToken valid, but no linked Instagram Business Account found.\n";
                    std::cout << response;
                    curl_easy_cleanup(curl);
                    return 1;
                }

                std::string igID = jsonResp["instagram_business_account"]["id"].get<std::string>();
                USER_ID = std::stoll(igID);
            }
            catch (...) {
                std::cerr << "\n\tFailed to parse Instagram ID from response.\n";
                std::cout << response;
                curl_easy_cleanup(curl);
                return 1;
            }

            curl_easy_cleanup(curl);
        }


        if (POSTMODE == "auto") {
            /* Open and read used_urls.json */
            std::ifstream inFile("../Cache/INST/instagram_used_urls.json");
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

            std::filesystem::path filePath = "../Cache/INST/instagram_used_urls.json"; // Gets used_urls.json size on disk to approximate length
            if (std::filesystem::file_size(filePath) > 100000) {
                std::cout << "\n\tInstagram cache is getting large. You should consider using /clear to clear your old URLS to prevent slowdowns\n";
                lastCoutWasReturn = false;
                lastCoutWasReturn = false;
            }
        }
        else {
            if (FORMAT == "image") {
                /* Open and read \Images for manual images */
                for (const auto& entry : std::filesystem::directory_iterator("../Images")) { // Log all files in image/video directory
                    media.push_back(entry.path().generic_string());
                }

                if (media.empty()) {
                    color(4); // Set color to red
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - NO IMAGES FILES FOUND IN /Images\n";
                    lastCoutWasReturn = false;
                    instagramcrash();
                    return 1;
                }
            }
            else {
                for (const auto& entry : std::filesystem::directory_iterator("../Videos")) { // Log all files in image/video directory
                    media.push_back(entry.path().generic_string());
                }

                if (media.empty()) {
                    color(4); // Set color to red
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - NO VIDEO FILES FOUND IN /Videos\n";
                    lastCoutWasReturn = false;
                    instagramcrash();
                    return 1;
                }
            }
            // Randomize selection
            std::srand(static_cast<unsigned int>(std::time(nullptr)));
            int index = std::rand() % media.size();
            fileDir = media[index];
        }

        /* Start instagram bot */
        while (keeploop) { // Loops as long as /stop isnt used
            color(6); // Reset cout color to yellow (default)
            std::time_t t = std::time(nullptr); // Get timestamp for output
            std::tm tm_obj;
            localtime_s(&tm_obj, &t);
            imageValid = true;

            if (countattempt == 0) { // Only output if on first post attempt
                if (!lastCoutWasReturn) {
                    std::cout << "\n"; // If last cout was not a return, print newline
                }
                else {
                    clear();
                }

                std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Attempting new post\r";
                lastCoutWasReturn = true;
            }

            if (countattempt >= ATTEMPTS_BEFORE_TIMEOUT) { // Check if stuck in loop
                color(4);
                std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - HIT RETRY LIMIT. WAITING 3 CYCLES BEFORE RETRYING";
                color(6);
                lastCoutWasReturn = false;
                std::this_thread::sleep_for(std::chrono::seconds(TIME_BETWEEN_POSTS * 180));
            }
            countattempt++; // Iterate attempts to post during this cycle
            /* Change behavior based on chosen post mode */
            json data;
            std::string chosenSubreddit;
            if (POSTMODE == "auto") { // If POSTMODE is auto, use the meme API

                int randIndex = std::rand() % SUBREDDITS.size(); // Generate random index of subreddit
                chosenSubreddit = SUBREDDITS[randIndex];

                apilink = "https://meme-api.com/gimme/" + chosenSubreddit; // Generate subreddit GET request URL

                /* Get post from meme-api*/
                CURL* curl;
                CURLcode res;
                std::string instaresponse;
                curl_global_init(CURL_GLOBAL_DEFAULT);
                curl = curl_easy_init();
                if (curl) {
                    curl_easy_setopt(curl, CURLOPT_URL, apilink.c_str()); // Get data from meme-api
                    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
                    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &instaresponse);
                    curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);  // Follow redirect to new sites (fix for meme-api deprication)

                    res = curl_easy_perform(curl);

                    if (res != CURLE_OK) {
                        color(4);
                        std::time_t t = std::time(nullptr); // Get timestamp for output
                        std::tm tm_obj;
                        localtime_s(&tm_obj, &t);
                        color(4); // Set color to red
                        std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - CURL GET error: " << curl_easy_strerror(res);
                        std::cerr << "\n\tError details: " << instaresponse << std::endl;
                        lastCoutWasReturn = false;
                        color(6);
                        curl_easy_cleanup(curl);
                        curl_global_cleanup();
                        instagramcrash(); // Call crash function to handle the error
                        return 1;
                    }

                    http_code = 0;
                    curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &http_code); // Get HTTP status code

                    curl_easy_cleanup(curl);
                    curl_global_cleanup();
                    try {
                        data = json::parse(instaresponse);
                    }
                    catch (const std::exception& e) {
                        color(4);
                        std::cerr << "\n\tFailed to parse JSON: " << e.what() << "\n\tRaw response: " << instaresponse;
                        lastCoutWasReturn = false;
                        color(6);
                        instagramcrash(); // Optional: handle gracefully
                        return 1;
                    }
                }
                else {
                    curl_global_cleanup();
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    color(4); // Set color to red
                    std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Failed to initialize CURL";
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
                    mediaURL = data["url"];
					redditURL = mediaURL; // Save reddit URL for later use
                    caption = data["title"];
                    bool nsfw = data["nsfw"];
                    bool tempDisableCaption = false;

                    if (DEBUGMODE) {
                        color(6); // Reset cout color to yellow (default)
                        std::cout << "\n\tGET success";
                        lastCoutWasReturn = false;
                    }
                    imageValid = imageValidCheck(data, tempDisableCaption, countattempt, lastCoutWasReturn); // Test if image is valid for posting

                }
                else if (http_code == 530) { // If cloudfare error, give notice
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    color(4); // Set color to red
                    std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Failed. Cloudfare HTTP Status Code 530 - The API this program utilizes appears to be under maintenence.\n\tThere is nothing that can be done to fix this but wait. Skipping attempt w/ +6 hour delay...";
                    lastCoutWasReturn = false;
                    std::this_thread::sleep_for(std::chrono::seconds(21600)); // Sleep 6h
                    imageValid = false; // Set imageValid to false to skip current post attempt
                }
                else { // Other error handling
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    color(4); // Set color to red
                    std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] -  HTTP GET ERROR " << http_code << ": \n\t" << data;
                    lastCoutWasReturn = false;
                    std::this_thread::sleep_for(std::chrono::seconds(60)); // Sleep to prevent spam
                    imageValid = false; // Set imageValid to false to skip current post attempt
                }
                if (FORMAT == "video") { // If format is image, convert to video
                    if (imageValid) { // If image is not valid, skip to next iteration
                        if (!lastCoutWasReturn) {
                            std::cout << "\n"; // If last cout was not a return, print newline
                        }
                        else {
                            clear();
                        }
                        std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Converting image to video...\r"; // Print status
                        lastCoutWasReturn = true;

                        if (image_to_video(mediaURL, "INST")) { // Convert image to video - if failed, set imageValid to false (ImageUtils.h)
                            color(6); // Reset cout color to yellow (image_to_video can take a while)
                            if (!lastCoutWasReturn) {
                                std::cout << "\n"; // If last cout was not a return, print newline
                            }
                            else {
                                clear();
                            }
                            std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Successfully converted image to video\r"; // Print status
                            lastCoutWasReturn = true;
							fileDir = "../Cache/INST/temp.mp4"; // Set fileDir to temp.mov
                        }
                        else {
                            imageValid = false; // Set imageValid to false if conversion failed
                        }
                    }
				}
            }
            else {
                int randIndex = std::rand() % media.size(); // Generate random index of media
                fileDir = media[randIndex];
            }
            
			if (POSTMODE == "manual" || (FORMAT == "video" && imageValid == true)) { // Upload handling for manual or video posts
                if (POSTMODE == "auto") {
                    fileDir = "../Cache/INST/temp.mp4";
                }
                std::time_t t = std::time(nullptr); // Get timestamp for output
                std::tm tm_obj;
                localtime_s(&tm_obj, &t);

                /* Upload media to filebin */
                if (!lastCoutWasReturn) {
                    std::cout << "\n"; // If last cout was not a return, print newline
                }
                else {
                    clear();
                }
                color(6);
                std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Uploading media to temp filehoster...\r"; // Print status
                lastCoutWasReturn = true;
                CURL* curl = curl_easy_init();
                if (!curl) {
                    std::cerr << "Failed to initialize curl\n";
                    return 1;
                }

                struct curl_httppost* formpost = nullptr;
                struct curl_httppost* lastptr = nullptr;
                struct curl_slist* headers = nullptr;

                // Add the file to the form post
                curl_formadd(&formpost, &lastptr,
                    CURLFORM_COPYNAME, "file",
                    CURLFORM_FILE, fileDir.c_str(),
                    CURLFORM_END);

                // Set custom User-Agent header
                headers = curl_slist_append(headers, "User-Agent: HoneyWasp/1.0");

                curl_easy_setopt(curl, CURLOPT_URL, "https://0x0.st");
                curl_easy_setopt(curl, CURLOPT_HTTPPOST, formpost);
                curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

                std::string response_data;
                curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
                curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response_data);

                // Optional: Fail on HTTP errors (like 4xx, 5xx)
                curl_easy_setopt(curl, CURLOPT_FAILONERROR, 1L);

                CURLcode res = curl_easy_perform(curl);
                if (res != CURLE_OK) {
                    std::cerr << "curl_easy_perform() failed: " << curl_easy_strerror(res) << "\n";
                    curl_slist_free_all(headers);
                    curl_formfree(formpost);
                    curl_easy_cleanup(curl);
                    return 1;
                }

                long status_code = 0;
                curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &status_code);

                t = std::time(nullptr); // Get timestamp for output
                localtime_s(&tm_obj, &t);
                color(6); // Reset cout color to yellow (Image upload can take a while)

                mediaURL = response_data;
                if (!mediaURL.empty() && mediaURL.back() == '\n') { // Remove trailing newline that filebin adds for some reason
                    mediaURL.pop_back();
                }
                if(!lastCoutWasReturn) {
                    std::cout << "\n"; // If last cout was not a return, print newline
                }
                else {
                    clear();
                }
                if (status_code == 200) { // Check if upload was successful
                    std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Successfully uploaded to FileBin: " << mediaURL << "\r"; // Print status
                    lastCoutWasReturn = true;
                }
                else {
                    std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Failed to upload video to filebin. HTTP Status Code: " << status_code; // Print status
                    return 1;
                }

                curl_slist_free_all(headers);
                curl_formfree(formpost);
                curl_easy_cleanup(curl);
                std::this_thread::sleep_for(std::chrono::seconds(5)); // Sleep to prevent spam
			}

            if (POSTMODE == "manual" || imageValid == true) { // If post mode is manual (or automatic & image valid)
                json uploadData;

                std::string finalCaption;
                if (POSTMODE == "manual" || USE_REDDIT_CAPTION == false || tempDisableCaption == true) {
                    finalCaption = FALLBACK_CAPTION;
                }
                else {
                    finalCaption = caption;
                }

                finalCaption += "\n\n.\n\n" + HASHTAGS;
                std::string uploadURL;

                /* Build uploadData */
                if (FORMAT == "image") {
                    uploadData = {
                        {"image_url", mediaURL},
                        {"caption", finalCaption},
                        {"access_token", TOKEN}
                    };
                    uploadURL = "https://graph.facebook.com/v23.0/" + std::to_string(USER_ID) + "/media";
                }
                else { // VIDEO
                    uploadData = {
                        {"video_url", mediaURL},
                        {"media_type",  "REELS"},
                        {"caption", finalCaption},
                        {"access_token", TOKEN}
                    };
                    uploadURL = "https://graph.facebook.com/v23.0/" + std::to_string(USER_ID) + "/media?media_type=VIDEO";
                }

                json uploadJson = HTTP_Post(uploadURL, http_code, uploadData);
                if (http_code == 200) { // POST success
                    if (DEBUGMODE) {
                        color(6); // yellow
                        std::cout << "\n\tPOST 1 success";
                        lastCoutWasReturn = false;
                    }

                    // Inside your posting function, after you get the container id:
                    std::string id = uploadJson["id"];

                    /* If video, wait for Instagram to process */
                    if (FORMAT != "image") {
                        color(6); // Yellow for status
                        if (!lastCoutWasReturn) std::cout << "\n";
                        else clear();

                        std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Waiting for Instagram to process video. This may take a while...\r";
                        lastCoutWasReturn = true;

                        std::string statusCode;
                        int maxRetries = 40;
                        int retryCount = 0;
                        long http_code = 0;

                        do {
                            std::this_thread::sleep_for(std::chrono::seconds(5)); // Wait 5 seconds

                            std::string statusURL = "https://graph.facebook.com/v23.0/" + id +
                                "?fields=status_code,status&access_token=" + TOKEN;

                            CURL* curl = curl_easy_init();
                            std::string response_string;

                            if (!curl) {
                                if (DEBUGMODE) std::cout << "\n\tCurl init failed for status check.";
                                return 1;
                            }

                            curl_easy_setopt(curl, CURLOPT_URL, statusURL.c_str());
                            curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
                            curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response_string);
                            CURLcode res = curl_easy_perform(curl);
                            if (res != CURLE_OK) {
                                if (DEBUGMODE) std::cout << "\n\tCurl perform failed: " << curl_easy_strerror(res);
                                curl_easy_cleanup(curl);
                                return 1;
                            }
                            curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &http_code);
                            curl_easy_cleanup(curl);

                            if (http_code != 200) {
                                if (DEBUGMODE) std::cout << "\n\tFailed to get status_code, HTTP " << http_code << ": " << response_string;
                            }

                            try {
                                auto statusJson = json::parse(response_string);
                                statusCode = statusJson.value("status_code", "");

                                if (DEBUGMODE) std::cout << "\n\tStatus code: " << statusCode;

                                if (statusCode == "ERROR") {
                                    std::time_t t = std::time(nullptr);
                                    std::tm tm_obj;
                                    localtime_s(&tm_obj, &t);
                                    color(4); // red
                                    std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - [ERROR] - Video processing failed (Try re-opening the bot). Video is likely corrupted\n\tError Message: " << statusJson["status"];
                                    instagramcrash();
                                    return 1;
                                }

                            }
                            catch (...) {
                                if (DEBUGMODE) std::cout << "\n\tFailed to parse status JSON.";
                            }

                            retryCount++;
                        } while (statusCode != "FINISHED" && retryCount < maxRetries);

                        if (statusCode != "FINISHED") {
                            color(4);
                            std::cout << "\n\tVideo processing did not finish in time, aborting publish.";
                            imageValid = false;
                        }
                    }

                    uploadData = {
                        {"creation_id", id},
                        {"access_token", TOKEN}
                    };
                    uploadURL = "https://graph.facebook.com/v23.0/" + std::to_string(USER_ID) + "/media_publish";
                    uploadJson = HTTP_Post(uploadURL, http_code, uploadData);

                    if (http_code == 200) {
                        if (DEBUGMODE) {
                            color(6);
                            std::cout << "\n\tPOST 2 success";
                            lastCoutWasReturn = false;
                        }

                        std::time_t t = std::time(nullptr);
                        std::tm tm_obj;
                        localtime_s(&tm_obj, &t);

                        color(2); // green

                        std::string message;
                        if (POSTMODE == "auto") {
                            message = (redditURL + " from r/" + chosenSubreddit + " uploaded - x" + std::to_string(countattempt) + " Attempt(s)");
                        }
                        else {
                            message = (mediaURL + " uploaded to Instagram - x" + std::to_string(countattempt) + " Attempt(s)");
                        }
                        if (!lastCoutWasReturn) {
                            std::cout << "\n";
                        }
                        else {
                            clear();
                        }

                        std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - " << message;
                        lastCoutWasReturn = false;
                        send_webhook(message);

                        // Export URL to file
                        std::ifstream inFile("../Cache/INST/instagram_used_urls.json");
                        json j;

                        if (inFile) {
                            if (inFile.peek() == std::ifstream::traits_type::eof()) {
                                j = json::array();
                            }
                            else {
                                inFile >> j;
                                if (!j.is_array()) j = json::array();
                            }
                            inFile.close();
                        }
                        else {
                            j = json::array();
                        }

                        j.push_back(redditURL);
                        usedUrls.push_back(redditURL);
                        std::ofstream outFile("../Cache/INST/instagram_used_urls.json");
                        outFile << j.dump(4);
                        outFile.close();

                        lastCoutWasReturn = false;
                        std::this_thread::sleep_for(std::chrono::seconds(TIME_BETWEEN_POSTS * 60)); // Sleep to prevent spam
                        countattempt = 0;
                    }
                    else {
                        std::time_t t = std::time(nullptr);
                        std::tm tm_obj;
                        localtime_s(&tm_obj, &t);
                        color(4); // red
                        std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - HTTP POST 2 ERROR " << http_code << ":\n\t";
                        lastCoutWasReturn = false;
                        if (DEBUGMODE) {
                            std::cout << uploadJson;
                        }
                        else {
                            std::string detailsStr = uploadJson["details"].get<std::string>();
                            nlohmann::json detailsJson = nlohmann::json::parse(detailsStr);
                            try {
                                std::cout << detailsJson["error"]["message"];
                            }
                            catch (...) {
                                std::cout << uploadJson;
                            }
                        }
                        imageValid = false;
                    }
                }
                else {
                    std::time_t t = std::time(nullptr);
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    color(4);
                    std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - HTTP POST 1 ERROR " << http_code << ":\n\t";
                    lastCoutWasReturn = false;

                    if (DEBUGMODE) {
                        std::cout << uploadJson;
                    }
                    else {
                        std::string detailsStr = uploadJson["details"].get<std::string>();
                        nlohmann::json detailsJson = nlohmann::json::parse(detailsStr);
                        try {
                            std::cout << detailsJson["error"]["message"];
                        }
                        catch (...) {
                            std::cout << uploadJson;
                        }
                    }
                    imageValid = false;
                }
            }

            std::this_thread::sleep_for(std::chrono::seconds(1)); // Sleep to prevent spam
        }
        return 0;
    }
    catch (const std::exception& e) { // Error handling
        std::time_t t = std::time(nullptr); // Get timestamp for output
        std::tm tm_obj;
        localtime_s(&tm_obj, &t);
        color(4); // Set color to red
        std::cerr << "\n\n\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Instagram crashed: " << e.what() << '\n';

        lastCoutWasReturn = false;
        instagramcrash(); // Call crash function to handle the error
        return 1;
    }
    catch (...) {
        std::time_t t = std::time(nullptr); // Get timestamp for output
        std::tm tm_obj;
        localtime_s(&tm_obj, &t);
        color(4); // Set color to red
        std::cerr << "\n\n\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Instagram crashed with unknown error.\n";

        lastCoutWasReturn = false;
        instagramcrash();  // Call crash function to handle the error
        return 1;
    }

}

bool imageValidCheck(json data, bool& tempDisableCaption, int countattempt, bool& wasreturn) { // Ensure image is valid for instagram
    color(4); // Set color to red

    std::string mediaURL = data["url"];
    std::string caption = data["title"];
    boost::to_lower(mediaURL);
    boost::to_lower(caption);
    bool nsfw = data["nsfw"];
    std::time_t t = std::time(nullptr); // Get timestamp for output
    std::tm tm_obj;
    localtime_s(&tm_obj, &t);
    if (mediaURL.find(".gif") != std::string::npos) { // If file is gif, skip
        if (!lastCoutWasReturn) {
            std::cout << "\n";
        }
        else {
            clear();
        }
        std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Image is GIF - x" << countattempt << " Attempt(s)\r";
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
            std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Using fallback caption - x" << countattempt << " Attempt(s)\r";
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
            std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Caption contains blacklisted string - x" << countattempt << " Attempt(s)\r";
            wasreturn = true; // Set true so next cout knows to not print on newline
            return false;
        }
    }

    for (int i = 0; i < usedUrls.size(); i++) { // Test if URL is duplicate
        if (mediaURL == usedUrls[i]) {
            if (!lastCoutWasReturn) {
                std::cout << "\n";
            }
            else {
                clear();
            }
            std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Duplicate URL - x" << countattempt << " Attempt(s)\r";
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
        std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Image is marked as NSFW - x" << countattempt << " Attempt(s)\r";
        wasreturn = true;
        return false;
    }

    if (!(imageRatio(mediaURL))) { // Test image aspect ratio (Whether or not it can fit in instagram)
        if (!lastCoutWasReturn) {
            std::cout << "\n";
        }
        else {
            clear();
        }
        std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - [Instagram] - Image has invalid aspect ratio - x" << countattempt << " Attempt(s)\r";
        wasreturn = true; // Set true so next cout knows to not print on newline
        return false;
    }

    color(6);

    return true;
}

void instagramClearCache() { // Upon /clear, clear used_images.json
    try {
        keeploop = false;
        std::ofstream outFile("../Cache/INST/instagram_used_urls.json", std::ios::trunc); // Clears contents of cache
        if (outFile) {
            outFile << "[]";
            outFile.close();
        }
    }
    catch (...) {
        std::cerr << "\n\tFailed to clear instagram cache. Please check file permissions or if the file is open in another program.\n";
    }
}


void instagramStop() { // Upon /stop, activate flag to stop loop
    keeploop = false;
}

size_t WriteCallback(void* contents, size_t size, size_t nmemb, void* userp) { // Writes the instaresponse into string
    try {
        size_t totalSize = size * nmemb;
        std::string* instaresponse = static_cast<std::string*>(userp);
        instaresponse->append(static_cast<char*>(contents), totalSize);
        return totalSize;
    }
    catch (...) {
        std::cerr << "\n\tError in WriteCallback: Failed to write data to string.\n";
    }
}

json HTTP_Post(const std::string& base_url, long long& http_code, const std::map<std::string, std::string>& params) { // HTTP POST request
    try {
        CURL* curl;
        CURLcode res;
        std::string instaresponse;
        std::string post_fields;

        curl_global_init(CURL_GLOBAL_DEFAULT);
        curl = curl_easy_init();
        if (curl) {
            for (auto it = params.begin(); it != params.end(); ++it) {
                char* key = curl_easy_escape(curl, it->first.c_str(), 0);
                char* val = curl_easy_escape(curl, it->second.c_str(), 0);
                post_fields += key;
                post_fields += "=";
                post_fields += val;
                if (std::next(it) != params.end()) {
                    post_fields += "&";
                }
                curl_free(key);
                curl_free(val);
            }

            curl_easy_setopt(curl, CURLOPT_URL, base_url.c_str());
            curl_easy_setopt(curl, CURLOPT_POST, 1L);
            curl_easy_setopt(curl, CURLOPT_POSTFIELDS, post_fields.c_str());
            curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
            curl_easy_setopt(curl, CURLOPT_WRITEDATA, &instaresponse);
            curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);

            res = curl_easy_perform(curl);

            if (res != CURLE_OK) {
                color(4);
                std::cerr << "\n\tCURL POST error: " << curl_easy_strerror(res) << std::endl;
                std::cerr << "\n\tError details: " << instaresponse << std::endl;
                color(6);
                curl_easy_cleanup(curl);
                curl_global_cleanup();
                return json::object({ {"error", curl_easy_strerror(res)}, {"details", instaresponse} });
            }

            // Get HTTP status code
            http_code = 0;
            curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &http_code);

            curl_easy_cleanup(curl);
            curl_global_cleanup();

            if (http_code == 200) {
                return json::parse(instaresponse);
            }
            else {
                return json::object({ {"error", "HTTP instaresponse code " + std::to_string(http_code)}, {"details", instaresponse} });
            }
        }
        else {
            curl_global_cleanup();
            color(4);
            return json::object({ {"error", "Failed to initialize CURL"} });
        }
    }
    catch (const std::exception& e) {
        color(4);
        std::cerr << "\n\tHTTP POST error: " << e.what() << std::endl;
        return json::object({ {"error", e.what()} });
    }
    catch (...) {
        color(4);
        std::cerr << "\n\tHTTP POST error: Unknown error occurred." << std::endl;
        return json::object({ {"error", "Unknown error occurred"} });
    }
}
