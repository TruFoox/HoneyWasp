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

using json = nlohmann::json; // redefines json as one from nlohmann
 

/* Prototypes */
size_t WriteCallback(void* contents, size_t size, size_t nmemb, void* userp);
json HTTP_Get(const std::string& base_url, long& http_code);
json HTTP_Post(const std::string& base_url, long& http_code, const std::map<std::string, std::string>& params = {});
void instagramStop();
bool imageValidCheck(json data, bool& tempDisableCaption, int countattempt);
void clear();

/* Global variables */
int TIME_BETWEEN_POSTS, ATTEMPTS_BEFORE_TIMEOUT, countattempt;
long long USER_ID;
long http_code;
bool DUPLICATES_ALLOWED, NSFW_ALLOWED, USE_REDDIT_CAPTION, instakeeploop, tempDisableCaption, imageValid;
std::string TOKEN, SUBREDDITS_RAW, SUBREDDIT_WEIGHTS_RAW, BLACKLIST_RAW, CAPTION_BLACKLIST_RAW, FALLBACK_CAPTION, caption, tempstring, HASHTAGS, INSTAPOSTMODE, imageURL;
std::vector<std::string> SUBREDDITS, BLACKLIST, CAPTION_BLACKLIST, usedUrls, manualMedia;
std::vector<int> SUBREDDIT_WEIGHTS;

/* Starts sending API calls to post to instagram*/
int instagram() {
    try {
        /* Load config data */
        INIReader reader("../Config.ini");
        std::string TOKEN = reader.Get("Instagram_Settings", "api_key", "");
        std::string INSTAPOSTMODE = reader.Get("Instagram_Settings", "post_mode", "auto");
        boost::to_lower(INSTAPOSTMODE);
        const long long USER_ID = std::stoll(reader.Get("Instagram_Settings", "user_id", "0"));
        const int TIME_BETWEEN_POSTS = std::stoi(reader.Get("Instagram_Settings", "time_between_posts", "60"));
        const int ATTEMPTS_BEFORE_TIMEOUT = std::stoi(reader.Get("Instagram_Settings", "attempts_before_timeout", "50"));
        std::string SUBREDDITS_RAW = reader.Get("Instagram_Settings", "subreddits", "memes,meme,comedyheaven");
        boost::erase_all(SUBREDDITS_RAW, " ");
        std::vector<std::string> SUBREDDITS = split(SUBREDDITS_RAW, ','); // Convert into list
        boost::to_lower(SUBREDDITS_RAW);
        std::string SUBREDDIT_WEIGHTS_RAW = reader.Get("Instagram_Settings", "subreddit_weights", "5,4,2");
        boost::erase_all(SUBREDDIT_WEIGHTS_RAW, " ");
        std::vector<int> SUBREDDIT_WEIGHTS = splitInts(SUBREDDIT_WEIGHTS_RAW, ',');
        std::string BLACKLIST_RAW = reader.Get("Instagram_Settings", "blacklist", "hitler,nazi,politic,democrat,republican,liberal,conservative,trump,biden,nsfw,sex,dick,pussy,selfie,18+,butt,anal,squirt,fag,nigg");
        boost::erase_all(BLACKLIST_RAW, " ");
        boost::to_lower(BLACKLIST_RAW);
        std::vector<std::string> BLACKLIST = split(BLACKLIST_RAW, ','); // Convert into list
        const bool DUPLICATES_ALLOWED = reader.GetBoolean("Instagram_Settings", "duplicates_allowed", false);
        const bool NSFW_ALLOWED = reader.GetBoolean("Instagram_Settings", "nsfw_allowed", false);
        const bool USE_REDDIT_CAPTION = reader.GetBoolean("Instagram_Settings", "use_reddit_caption", false);
        std::string CAPTION_BLACKLIST_RAW = reader.Get("Instagram_Settings", "caption_blacklist", "Fuck,Shit,Ass,Bitch,retard,republican,democrat");
        boost::to_lower(CAPTION_BLACKLIST_RAW);
        boost::erase_all(CAPTION_BLACKLIST_RAW, " ");
        std::vector<std::string> CAPTION_BLACKLIST = split(CAPTION_BLACKLIST_RAW, ',');
        std::string FALLBACK_CAPTION = reader.Get("Instagram_Settings", "caption", "");
        std::string HASHTAGS = reader.Get("Instagram_Settings", "hashtags", "");

        const bool DEBUGMODE = reader.GetBoolean("General_Settings", "debug_mode", false);
        int countattempt = 0;
        instakeeploop = true; // Ensures keeploop isnt false if restarted after /stop


        /* Abort if any required value is default */
        if (TOKEN.empty()|| USER_ID == 0 || (INSTAPOSTMODE == "auto" && (SUBREDDITS_RAW.empty() || SUBREDDIT_WEIGHTS_RAW.empty()))) {
            std::cout << "Config.ini is missing required Instagram settings. Aborting...\n";
            return 1;
        }

        for (int i = 0; i < SUBREDDIT_WEIGHTS.size(); i++) { // Scales subreddit list by the weights in Subreddit_Weights
            for (int o = 0; o < SUBREDDIT_WEIGHTS[i]; ++o) {
                SUBREDDITS.push_back(SUBREDDITS[i]);
            }
        }

        if (INSTAPOSTMODE == "auto") {
            /* Open and read used_urls.json */
            std::ifstream inFile("instagram_used_urls.json");
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

            std::filesystem::path filePath = "instagram_used_urls.json"; // Gets used_urls.json size on disk to approximate length
            if (std::filesystem::file_size(filePath) > 100000) {
                std::cout << "\n\tused_urls.json is getting large. You should consider using /clear to clear your old URLS to prevent slowdowns\n";
            }
        }
        else {
            /* Open and read Media.json for manual images */
            std::ifstream inFile("..\\Media_URLs.json");
            json j;

            if (inFile) {
                inFile >> j;// Parse JSON content from file
                for (const auto& item : j) { // Check if element is a string
                    if (item.is_string()) {
                        manualMedia.push_back(item.get<std::string>()); // Convert JSON string to std::string and add to vector
                    }
                }
            }
            inFile.close();
        }

        /* Start instagram bot */
        while (instakeeploop) { // Loops as long as /stop isnt used
            color(6); // Reset cout color to yellow (default)

            std::time_t t = std::time(nullptr); // Get timestamp for output
            std::tm tm_obj;
            localtime_s(&tm_obj, &t);

            if (countattempt == 0) { // Only output if on first post attempt
                std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Attempting new post\r";
            }

            if (countattempt >= ATTEMPTS_BEFORE_TIMEOUT) { // Check if stuck in loop
                color(4);
                std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - HIT RETRY LIMIT. WAITING 3 CYCLES BEFORE RETRYING\n";
                color(6);
                std::this_thread::sleep_for(std::chrono::seconds(TIME_BETWEEN_POSTS * 180));
            }
            countattempt++; // Iterate attempts to post during this cycle

            /* Change behavior based on chosen post mode */
            json data;
            std::string chosenSubreddit;
            if (INSTAPOSTMODE == "auto") { // If INSTAPOSTMODE is auto, use the meme API

                int randIndex = std::rand() % SUBREDDITS.size(); // Generate random index of subreddit
                chosenSubreddit = SUBREDDITS[randIndex];

                std::string apilink = "https://meme-api.com/gimme/" + chosenSubreddit; // Generate subreddit GET request URL

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
                        std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - CURL GET error: " << curl_easy_strerror(res) << std::endl;
                        std::cerr << "\n\tError details: " << instaresponse << std::endl;
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
                        std::cerr << "\n\tFailed to parse JSON: " << e.what() << "\n\tRaw response: " << instaresponse << std::endl;
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
                    std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Failed to initialize CURL";
                    return 1;
                }
                if (DEBUGMODE) {
                    color(6); // Reset cout color to yellow (default)
                    std::cout << "\n\tHTTP CODE : " << http_code << "\n\tJSON DATA : " << data;
                }
                /* Read JSON data and attempt post*/
                if (http_code == 200) { // Ensure GET success
                    imageURL = data["url"];
                    caption = data["title"];
                    bool nsfw = data["nsfw"];
                    bool tempDisableCaption = false;

                    if (DEBUGMODE) {
                        color(6); // Reset cout color to yellow (default)
                        std::cout << "\n\tGET success";
                    }

                    imageValid = imageValidCheck(data, tempDisableCaption, countattempt); // Test if image is valid for posting

                }
                else if (http_code == 530) { // If cloudfare error, give notice
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    color(4); // Set color to red
                    std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Failed. Cloudfare HTTP Status Code 530 - The API this program utilizes appears to be under maintenence.\n\tThere is nothing that can be done to fix this but wait. Skipping attempt w/ +6 hour delay...";
                    std::this_thread::sleep_for(std::chrono::seconds(21600)); // Sleep 6h
					imageValid = false; // Set imageValid to false to skip current post attempt
                }
                else { // Other error handling
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    color(4); // Set color to red
                    std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - INSTAGRAM HTTP GET ERROR " << http_code << ": \n\t" << data << std::endl;
                }
            }
            else { // If manual, choose random item from manual list
                int randIndex = std::rand() % manualMedia.size(); // Generate random index of subreddit
                imageURL = manualMedia[randIndex];

            }

            if (INSTAPOSTMODE == "manual" || imageValid == true) {
                json uploadData;
                if (INSTAPOSTMODE == "manual" || (USE_REDDIT_CAPTION == false || tempDisableCaption == true)) { // Sets caption to either defined or post caption
                    uploadData = {
                        {"image_url", imageURL},
                        {"caption", FALLBACK_CAPTION + "\n\n.\n\n" + HASHTAGS},
                        {"access_token", TOKEN}
                    };
                }
                else { // Disables caption if needed
                    uploadData = {
                        {"image_url", imageURL},
                        {"caption", caption + "\n\n.\n\n" + HASHTAGS},
                        {"access_token", TOKEN}
                    };
                }
                std::string uploadURL = "https://graph.facebook.com/v19.0/" + std::to_string(USER_ID) + "/media"; // Generates URL for uploading image
                json uploadJson = HTTP_Post(uploadURL, http_code, uploadData);

                if (http_code == 200) { // Ensure POST success
                    if (DEBUGMODE) {
                        color(6); // Reset cout color to yellow (default)
                        std::cout << "\n\tPOST 1 success";
                    }

                    std::string id = uploadJson["id"]; // Get ID from /media POST request
                    uploadData = {
                        {"creation_id", id},
                        {"access_token", TOKEN}
                    };

                    uploadURL = "https://graph.facebook.com/v19.0/" + std::to_string(USER_ID) + "/media_publish"; // Generates URL for publishing image
                    uploadJson = HTTP_Post(uploadURL, http_code, uploadData); // Send POST publish request

                    if (http_code == 200) { // Ensure POST success
                        if (DEBUGMODE) {
                            color(6); // Reset cout color to yellow (default)
                            std::cout << "\n\tPOST 2 success\n";
                        }

                        std::time_t t = std::time(nullptr); // Get timestamp for output
                        std::tm tm_obj;
                        localtime_s(&tm_obj, &t);

						color(2); // Set color to green

                        std::string message;

                        if (INSTAPOSTMODE == "auto") {
                            message = (imageURL + " from r/" + chosenSubreddit + " uploaded to Instagram - x" + std::to_string(countattempt) + " Attempt(s)"); // Create message for webhook / cout
                        }
                        else {
                            message = (imageURL + " uploaded to Instagram - x" + std::to_string(countattempt) + " Attempt(s)"); // Create message for webhook / cout
                        }

                        std::cout << "\t" << std::put_time(&tm_obj, "%H:%M") << " - " << message;
                        send_webhook(message);
                        /* Begin export of URL to file */
                        std::ifstream inFile("instagram_used_urls.json");
                        json j;

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
                        std::ofstream outFile("instagram_used_urls.json");
                        outFile << j.dump(4);
                        outFile.close();

                        std::this_thread::sleep_for(std::chrono::seconds(TIME_BETWEEN_POSTS * 60)); // Sleep

                        countattempt = 0; // Reset number of attempts to post this cycle
                    }
                    else {
                        std::time_t t = std::time(nullptr); // Get timestamp for output
                        std::tm tm_obj;
                        localtime_s(&tm_obj, &t);
                        color(4); // Set color to red
                        std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - INSTAGRAM HTTP POST 2 ERROR " << http_code << ": \n\t" << uploadJson << std::endl;
                        imageValid = false; // Set imageValid to false to skip current post attempt
                    }
                }
                else {
                    std::time_t t = std::time(nullptr); // Get timestamp for output
                    std::tm tm_obj;
                    localtime_s(&tm_obj, &t);
                    color(4); // Set color to red
                    std::cout << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - INSTAGRAM HTTP POST 1 ERROR " << http_code << ":\n\t";

                    if (DEBUGMODE) { // Print error details
                        std::cout << uploadJson << std::endl;
                    }
                    else {
                        std::string detailsStr = uploadJson["details"].get<std::string>();
                        nlohmann::json detailsJson = nlohmann::json::parse(detailsStr);
                        std::cout << detailsJson["error"]["message"] << std::endl;
                    }
					imageValid = false; // Set imageValid to false to skip current post attempt
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
        std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Instagram crashed: " << e.what() << '\n';

		instagramcrash(); // Call crash function to handle the error
        return 1;
    }
    catch (...) {
        std::time_t t = std::time(nullptr); // Get timestamp for output
        std::tm tm_obj;
        localtime_s(&tm_obj, &t);
        color(4); // Set color to red
        std::cerr << "\n\t" << std::put_time(&tm_obj, "%H:%M") << " - Instagram crashed with unknown error.\n";

        instagramcrash();  // Call crash function to handle the error
        return 1;
    }

}

bool imageValidCheck(json data, bool& tempDisableCaption, int countattempt) { // Ensure image is valid for instagram
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
        clear();
        std::cout << "\r\t" << std::put_time(&tm_obj, "%H:%M") << " - Image is GIF - x" << countattempt << " Attempt(s)\r";
        return false;
    }

    for (int i = 0; i < CAPTION_BLACKLIST.size(); i++) {
        if (caption.find(CAPTION_BLACKLIST[i]) != std::string::npos) { // Test if reddit title contains blackslisted string. Use defined caption if it does ( DOES NOT FLAG AS INVALID )
            tempDisableCaption = true;
            clear();
            std::cout << "\r\t" << std::put_time(&tm_obj, "%H:%M") << " - Using fallback caption - x" << countattempt << " Attempt(s)";
        }
    }

    for (int i = 0; i < BLACKLIST.size(); i++) {
        if (caption.find(BLACKLIST[i]) != std::string::npos) { // Test if reddit title contains blackslisted string. Block if it does
            clear();
            std::cout << "\r\t" << std::put_time(&tm_obj, "%H:%M") << " - Caption contains blacklisted string - x" << countattempt << " Attempt(s)\r";
            return false;
        }
    }

    for (int i = 0; i < usedUrls.size(); i++) { // Test if URL is duplicate
        if (imageURL == usedUrls[i]) {
            clear();
            std::cout << "\r\t" << std::put_time(&tm_obj, "%H:%M") << " - Duplicate URL - x" << countattempt << " Attempt(s)\r";
            return false;
        }
    }
    if (NSFW_ALLOWED == false && nsfw == true) { // If NSFW is disabled & post is marked as NSFW, return false
        clear();
        std::cout << "\r\t" << std::put_time(&tm_obj, "%H:%M") << " - Image is marked as NSFW - x" << countattempt << " Attempt(s)\r";
        return false;
    }

    if (!(imageRatio(imageURL))) { // Test image aspect ratio (Whether or not it can fit in instagram)
        clear();
        std::cout << "\r\t" << std::put_time(&tm_obj, "%H:%M") << " - Image has invalid aspect ratio - x" << countattempt << " Attempt(s)\r";
        return false;
    }

    color(6);

    return true;
}

void instagramClearCache() { // Upon /clear, clear used_images.json
    instakeeploop = false;
    std::ofstream outFile("instagram_used_urls.json.json", std::ios::trunc); // Clears contents of cache
    if (outFile) {
        outFile << "[]";
        outFile.close();
    }

}

void clear() { // Clear current line
    std::cout << "\r                                                                                   \r";
}

void instagramStop() { // Upon /stop, activate flag to stop loop
    instakeeploop = false;
}

size_t WriteCallback(void* contents, size_t size, size_t nmemb, void* userp) { // Writes the instaresponse into string
    size_t totalSize = size * nmemb;
    std::string* instaresponse = static_cast<std::string*>(userp);
    instaresponse->append(static_cast<char*>(contents), totalSize);
    return totalSize;
}

json HTTP_Post(const std::string& base_url, long& http_code, const std::map<std::string, std::string>& params) { // HTTP POST request
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
