package services;

import config.Config;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import utils.*;

import javax.imageio.ImageIO;
import java.nio.file.*;
import java.util.*;

// Remember to add debug outputs after finished with implementation

public class Twitter implements Runnable {
    Config config = Config.getInstance(); // Get config
    Random rand = new Random(); // Generate seed for random number generation
    Scanner scanner = new Scanner(System.in); // Scanner

    // Empty global variables
    long USERID, countAttempt = 0;
    List<String[]> usedURLs = new ArrayList<>();
    String chosenSubreddit, mediaURL, redditURL, caption, fileDir, accessToken;
    boolean nsfw, tempDisableCaption;
    static boolean run = true;
    int randIndex;
    File[] media, audio;

    // Twitter API expects a randomly generated hash every time you post but... why would I do that? It's Twitter, not fort knox
    String securityKey = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"; // verifier
    String codeChallenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"; // precomputed SHA256


    // Load config
    String KEY = config.Twitter().getConsumer_key().trim();
    final boolean AUTO_POST_MODE = config.Twitter().isAuto_post_mode();
    final int TIME_BETWEEN_POSTS = config.Twitter().getTime_between_posts();
    final int sleepTime = TIME_BETWEEN_POSTS * 60000; // Generate time to sleep between posts in milliseconds
    final int ATTEMPTS_BEFORE_TIMEOUT = config.Twitter().getAttempts_before_timeout();
    final List<String> SUBREDDITS = config.Twitter().getSubreddits();
    final boolean VIDEO_MODE = config.Twitter().isVideo_mode();
    final boolean AUDIO_ENABLED = config.Twitter().isAudio_enabled();
    final boolean USE_REDDIT_CAPTION = config.Twitter().isUse_reddit_caption();
    final String FALLBACK_CAPTION = config.Twitter().getCaption();
    final String HASHTAGS = config.Twitter().getHashtags();
    String REFRESHTOKEN = config.Twitter().getRefresh_token().trim();
    String TOKEN; // Temporary access token that must be fetched every cycle

    public void run() {
        if (!getRefreshToken()) {return;} // 1 If refresh token is not set, fetch it. Otherwise, run bot like normal (Quit if failed)

        if (!refreshAccessToken()) {return;} // 2 Fetch temporary user access token & replace old refresh token with new one for next time (+ test validity)

        if (!getMediaSource()) {return;} // 3 Gets media location, cache files (Quit if failed)

        Output.webhookPrint("[SYS] Bot successfully started on Twitter");
        Status.twitterRunning = true;


        try {
            while (run) {
                countAttempt++;
                Output.debugPrint("[YT] Attempt " + countAttempt + " started");

                if (countAttempt > ATTEMPTS_BEFORE_TIMEOUT && ATTEMPTS_BEFORE_TIMEOUT != 0) { // If max # of attempts have been reached
                    Output.webhookPrint("[TWIT] Max # of attempts reached. Skipping attempt...", Output.YELLOW, true);

                    if (!Sleep.safeSleep(sleepTime)) break; // Sleep (Easy way to fake a "skipped attempt")
                    countAttempt = 1;
                }

                if (countAttempt == 1) { // Print first attempt message
                    Output.print("[TWIT] Attempting new post", Output.YELLOW, true,true);
                }

                if (!refreshAccessToken()) {return;} // Refresh temporary user access token
                
                /* Fetch media */
                if (AUTO_POST_MODE) {
                    switch (getMemeAPI()) {
                        case 0: // Success
                            break;
                        case 1: // Soft fail (retry)
                            continue;
                        case 2: // Fail (quit)
                            return;

                    }
                    Output.debugPrint("[TWIT] Successfully fetched URL " + mediaURL);

                    /* If format is video, convert image to video */
                    if (VIDEO_MODE) {
                        Image image; // Holds image data (not usually needed unless converting to video as twitter takes image url as input)

                        Output.debugPrint("[TWIT] Attempting to retrieve image data");
                        try {
                            // Download image from Reddit
                            URL url = new URL(mediaURL);
                            image = ImageIO.read(url);

                        } catch (javax.imageio.IIOException e) { // Corrupt image (or similar)
                            Output.webhookPrint("[TWIT] Image appears to be in an unhandleable format. Trying again, and marking this URL as invalid...");

                            // Blacklist image URL permanently, as it is likely corrupted
                            FileIO.writeList(mediaURL, "twitter", true);
                            continue;
                        } catch (IOException e) {
                            Output.webhookPrint("[TWIT] Failed to download image from Reddit to convert to video. Skipping attempt w/ +2 hour delay..."
                                    + "\n\tError message: " + e, Output.RED);

                            if (!Sleep.safeSleep(7200000)) break;
                            continue;
                        }

                        /* Select mp4 for audio if audio enabled */
                        String audioDir = null; // Default value

                        if (AUDIO_ENABLED) {
                            Output.debugPrint("[TWIT] Attempting to select audio file for use");
                            randIndex = rand.nextInt(audio.length); // Select random audio file
                            audioDir = String.valueOf(audio[randIndex]);
                        }

                        Output.print("[TWIT] Converting image to video...", Output.YELLOW, true);

                        if (ImageToVideo.convert(String.valueOf(Paths.get(".", "cache", "twitter", "temp")), image, audioDir)) { // Convert image to video
                            Output.print("[TWIT] Successfully converted image to video", Output.YELLOW, true);
                        } else {
                            Output.print("[TWIT] Failed to convert image to video for upload. Skipping attempt...", Output.RED);

                            if (!Sleep.safeSleep(sleepTime)) break;
                            continue;
                        }
                        fileDir = "./cache/twitter/temp.mp4";
                    }
                } else {
                    randIndex = rand.nextInt(media.length); // Select random image
                    fileDir = String.valueOf(media[randIndex]);
                }

                /* Upload manual media/generated video to temp file hoster */
                if (!AUTO_POST_MODE || VIDEO_MODE) {
                    // Upload media to 0x0
                    Output.print("[TWIT] Uploading media to temp file hoster...", Output.YELLOW, true);

                    String response = HTTPSend.postFile("https://0x0.st", Path.of(fileDir)); // Send file to 0x0

                    // Error handling
                    if (HTTPSend.HTTPCode.get() == 403) {
                        Output.webhookPrint("[TWIT] 0x0.su (temp storage provider) returned HTTP 403 - Oh no! You've likely been flagged as a bot by the temp storage site!" +
                                "\n\tYour IP should be cycled and unblocked in a few months." +
                                "\n\n\tIn the meantime, you should set 'video_mode' to 'false' & 'post_mode' to 'auto' under [twitter Settings]" +
                                "\n\tin config.json to bypass the need for temporary storage. Quitting..." +
                                "\n\n\tError message: " + response, Output.RED);

                        return;
                    } else if (!(HTTPSend.HTTPCode.get() == 200)) { // Misc error handling
                        Output.webhookPrint("Error uploading file to 0x0.su (temp storage provider). Quitting..." +
                                "\n\tError message: " + response, Output.RED);

                        return;
                    }

                    mediaURL = response;

                    if (mediaURL.endsWith("\n")) { // Remove trailing newline 0x0 adds for some reason
                        mediaURL = mediaURL.substring(0, mediaURL.length() - 1);
                    }

                    // Success message
                    Output.print("Successfully uploaded to temp storage: " + mediaURL, Output.YELLOW, true);
                }




            }
        } catch (InterruptedException e) { // When interrupted
            Thread.currentThread().interrupt(); // restore interrupt flag
            Output.webhookPrint("[TWIT] Unexpected error during sleep: " + e.getMessage(), Output.RED);

        } catch (Exception e) { // General error handling
            try {
                Output.webhookPrint("[TWIT] Bot crashed with unexpected error: " + e.getMessage(), Output.RED);
            } catch (Exception inner) {
                inner.printStackTrace();
            }
        } finally { // Crash/Stop handling
            Output.webhookPrint("[SYS] Twitter stopped");

            Status.youtubeRunning = false;
        }
    }

    private boolean getRefreshToken() {
        Output.debugPrint("[TWIT] Fetching refresh token");
        if (REFRESHTOKEN.isEmpty()) { // Only run if no refresh token
            try {
                String oauthURL = "https://twitter.com/i/oauth2/authorize?response_type=code" +
                        "&client_id=" + KEY +
                        "&redirect_uri=http://localhost" +
                        "&scope=tweet.write%20media.write%20users.read%20offline.access"+
                        "&state=anything" +
                        "&code_challenge=" + codeChallenge +
                        "&code_challenge_method=S256";


                Output.webhookPrint("BEFORE YOU CAN POST TO TWITTER, YOU MUST RETRIEVE YOUR ACCESS TOKEN." +
                        "\n\tATTEMPTING TO REDIRECT YOU TO THE AUTHENTICATION SITE NOW (OR GO TO " + oauthURL + ")", Output.RED);

                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) { // Test if browser allows going to URL from here
                    try {
                        Desktop.getDesktop().browse(new URI(oauthURL));
                    } catch (Exception e) {
                        // Ignore
                    }
                }

                Output.webhookPrint("[THIS STEP MUST BE DONE IN-CONSOLE] PLEASE PASTE THE ENTIRE URL YOU WERE JUST REDIRECTED TO (SEE https://github.com/TruFoox/HoneyWasp/#twitter-setup FOR HELP):", Output.YELLOW);
                String redirectUrl = scanner.nextLine(); // Read user input

                String authCode = redirectUrl.replace("http://localhost/?state=anything&code=", ""); // Remove non-code part of URL

                oauthURL = "https://api.twitter.com/2/oauth2/token";

                // Build upload data
                Map<String, String> formData = new HashMap<>();

                formData.put("client_id", KEY);
                formData.put("grant_type", "authorization_code");
                formData.put("redirect_uri", "http://localhost");
                formData.put("code", authCode);
                formData.put("code_verifier", securityKey);

                String response = HTTPSend.postForm(oauthURL, formData);

                if (HTTPSend.HTTPCode.get() == 200 && response.contains("refresh_token")) {
                    REFRESHTOKEN = StringToJson.getData(response, "refresh_token");
                    TOKEN = StringToJson.getData(response, "refresh_token");

                    config.Twitter().setRefresh_token(REFRESHTOKEN);

                    return true;  // Success
                } else {
                    Output.webhookPrint("[TWIT] Failed to fetch refresh token. Quitting..." +
                            "\n\tError message: " + response, Output.RED);

                    return false;
                }
            } catch (Exception e) {
                try {
                    Output.webhookPrint(String.valueOf(e), Output.RED);
                } catch (Exception ex) {
                    throw new RuntimeException(e);
                }
                return false;
            }
        }
        Output.debugPrint("[TWIT] refresh_token was found to contain data");
        return true;
    }

    private boolean refreshAccessToken() {
        try {
            Output.debugPrint("[TWIT] Fetching access token");
            String oauthURL = "https://api.twitter.com/2/oauth2/token";

            // Build upload data
            Map<String, String> formData = new HashMap<>();

            formData.put("grant_type", "refresh_token");
            formData.put("refresh_token", REFRESHTOKEN);
            formData.put("client_id", KEY);

            String response = HTTPSend.postForm(oauthURL, formData);

            if (HTTPSend.HTTPCode.get() == 200 && response.contains("refresh_token") && response.contains("access_token")) {
                // Set both access and refresh, as Twitter refresh tokens are re-given on every use
                TOKEN = StringToJson.getData(response, "access_token");
                REFRESHTOKEN = StringToJson.getData(response, "refresh_token");

                config.Twitter().setRefresh_token(REFRESHTOKEN);
                config.saveConfig(); // Write to file

                return true;  // Success

            } else if (response.contains("token was invalid")) { // If refresh token is invalid
                Output.webhookPrint("[TWIT] Refresh token appears to be invalid. You need to reauthenticate it.", Output.RED);

                REFRESHTOKEN = "";

                return getRefreshToken(); // Attempt to fetch refresh token again

            } else {
                Output.webhookPrint("[TWIT] Failed to fetch token. Quitting..." +
                        "\n\tError message: " + response, Output.RED);

                return false;
            }
        } catch (Exception e) {
            try {
                Output.webhookPrint(String.valueOf(e), Output.RED);
            } catch (Exception ex) {
                throw new RuntimeException(e);
            }
            return false;
        }
    }


    public int getMemeAPI() throws Exception {
        String response;

        randIndex = rand.nextInt(SUBREDDITS.size()); // Generate random subreddit index

        chosenSubreddit = SUBREDDITS.get(randIndex);

        String URL = "https://meme-api.com/gimme/" + chosenSubreddit;

        Output.debugPrint("[TWIT] Fetching media URL from " + URL);
        try {
            response = HTTPSend.get(URL);

        } catch (ConnectException e) {
            Output.print("[TWIT] Connection drop detected. Trying again in 10 seconds...");

            if (!Sleep.safeSleep(10000)) return 2; // Sleep 10 secs
            return 1;
        } catch (Exception e) {
            Output.webhookPrint("[TWIT] Failed to fetch image from meme-api.com"
                    + "\n\tError message: " + e, Output.RED);
            return 2;
        }

        /* Status code handling */
        switch (HTTPSend.HTTPCode.get().intValue()) {
            case 200: // Success
                // Parse JSON data
                mediaURL = StringToJson.getData(response, "url");
                redditURL = StringToJson.getData(response, "postLink");
                caption = StringToJson.getData(response, "title");
                nsfw = Boolean.parseBoolean(StringToJson.getData(response, "nsfw"));
                tempDisableCaption = false;

                Output.debugPrint("[TWIT] Reddit post data successfully retrieved");

                /* Check image validity (Ensures not gif, not blacklisted, not already used, valid aspect ratio) */
                switch (ImageValidity.check(response, countAttempt, usedURLs, false, "twitter")) {
                    case 0: // Image valid
                        return 0;
                    case 1: // General failed validation
                        return 1;
                    case 2: // Caption is blacklisted, but allowed to post (CAPTION BLACKLIST)
                        tempDisableCaption = true;
                        return 0;
                }

            case 503: // Cloudflare error
                Output.webhookPrint("[TWIT] Failed. Cloudflare HTTP Status Code 503 - The API this program utilizes appears to be under maintenance."
                        + "\n\tThere is nothing that can be done to fix this but wait. Skipping attempt w/ +6 hour delay...", Output.RED);

                if (!Sleep.safeSleep(sleepTime + 21600000)) break; // Sleep normal time + 6 hours
                return 1;
            case 502: // Cloudflare error 2
                Output.webhookPrint("[TWIT] Failed. Cloudflare HTTP Status Code 502 - The API this program utilizes gave a bad response"
                        + "\n\tThere is nothing that can be done to fix this but wait. Skipping attempt...", Output.RED);

                if (!Sleep.safeSleep(sleepTime)) break; // Sleep normal time
                return 1;
            case 530: // Cloudflare error 3
                Output.webhookPrint("[TWIT] Failed. Cloudflare HTTP Status Code 530 - The API this program utilizes is temporarily unreachable"
                        + "\n\tThere is nothing that can be done to fix this but wait, but it shouldn't take too long. Skipping attempt...", Output.RED);

                if (!Sleep.safeSleep(sleepTime)) break; // Sleep normal time + 6 hours
                return 1;
            default: // General error handling
                Output.webhookPrint("[TWIT] Failed to retrieve image data from meme-api.com with error code " + HTTPSend.HTTPCode.get() + ". Quitting..."
                        + "\n\tError message: " + response, Output.RED);

                return 2;
        }

        return 2;
    }



    // Get media location (based on POSTMODE & selected media format)
    private boolean getMediaSource() {
        try {
            if (AUTO_POST_MODE) {
                Output.debugPrint("[TWIT] Reading automatic cache");
                usedURLs = FileIO.readList("twitter"); // Generate filepath "./cache/[twitter]/cache.txt" for given OS & read file

            } else { // Log manual media
                String format = (VIDEO_MODE) ? "videos" : "images";
                File directory = Paths.get(".", format).toFile(); // Generate filepath "./{Format}"
                Output.debugPrint("[TWIT] Media source set to " + directory);

                if (!directory.exists() || !directory.isDirectory()) {
                    Output.webhookPrint(String.format("[TWIT] /%s directory does not exist. Please create it or set post_mode to auto. Quitting...", format), Output.RED);
                    return false;
                }

                // Ensure there is at least 1 file in directory
                int fileCount = Objects.requireNonNull(directory.list()).length;
                if (fileCount == 0) {
                    Output.webhookPrint(String.format("[TWIT] No %s found in /%s directory. Add media or set post_mode to auto. Quitting...", format, format), Output.RED);
                    return false;
                }

                Output.debugPrint("[TWIT] Logging media from manual directory");

                // Start logging media
                media = directory.listFiles((_, name) -> name.toLowerCase().endsWith(".mp4")); // Gets all relevant files in the directory
            }

            // Get audio
            if (AUDIO_ENABLED && VIDEO_MODE) {
                File directory = Paths.get(".", "audio").toFile(); // Generate filepath "./audio"
                Output.debugPrint("[TWIT] Audio source set to " + directory);

                if (!directory.exists() || !directory.isDirectory()) {
                    Output.webhookPrint("[TWIT] /audio directory does not exist. Please create it, or set 'audio_enabled' to 'false' under [Twitter_Settings] in config.json. Quitting...", Output.RED);
                    return false;
                }

                // Ensure there is at least 1 file in directory
                int fileCount = Objects.requireNonNull(directory.list()).length;
                if (fileCount == 0) {
                    Output.webhookPrint("[TWIT] No audio found in /audio directory. Add audio or set 'audio_enabled' to 'false' under [Twitter_Settings] in config.json. Quitting...", Output.RED);
                    return false;
                }

                Output.debugPrint("[TWIT] Logging audio from manual directory");
                audio = directory.listFiles((_, name) -> name.toLowerCase().endsWith(".mp3")); // Gets all relevant files in the directory
            }
        } catch (Exception e) {
            try {
                Output.webhookPrint(String.valueOf(e), Output.RED);
            } catch (Exception ex) {
                throw new RuntimeException(e);
            }
            return false;
        }
        return true; // Success
    }

    public static void stop() { // Stop bot
        run = false;
        Output.webhookPrint("Twitter successfully stopped");
    }

    public static void clear() { // Clear cache
        FileIO.clearList("twitter");
        Output.webhookPrint("Twitter cache successfully cleared");
    }
}
