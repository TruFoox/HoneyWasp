package services;

import config.ReadConfig;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.io.File;
import java.net.URL;
import javax.imageio.ImageIO;
import config.*;
import org.json.*;
import utils.*;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;

// Remember to add debug outputs after finished with implementation

public class Twitter implements Runnable {
    ReadConfig config = ReadConfig.getInstance(); // Get config
    Random rand = new Random(); // Generate seed for random number generation
    Scanner scanner = new Scanner(System.in); // Scanner

    // Empty global variables
    long USERID, countAttempt = 0;
    List<String[]> usedURLs = new ArrayList<>();
    String chosenSubreddit, mediaURL, redditURL, caption, fileDir, accessToken;
    boolean nsfw, tempDisableCaption;
    static boolean run;
    int randIndex;
    File[] media, audio;

    // Twitter API expects a randomly generated hash every time you post but... why would I do that? It's Twitter, not fort knox
    String securityKey = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"; // verifier
    String codeChallenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"; // precomputed SHA256


    // Load config
    String KEY = config.getTwitter().getConsumer_key().trim();
    final String SECRET = config.getTwitter().getClient_secret().trim();
    final String POSTMODE = config.getTwitter().getPost_mode().trim().toLowerCase();
    final boolean AUTOPOSTMODE = config.getTwitter().isAuto_post_mode();
    final int TIME_BETWEEN_POSTS = config.getTwitter().getTime_between_posts();
    final int sleepTime = TIME_BETWEEN_POSTS * 60000; // Generate time to sleep between posts in milliseconds
    final int ATTEMPTS_BEFORE_TIMEOUT = config.getTwitter().getAttempts_before_timeout();
    final List<String> SUBREDDITS = config.getTwitter().getSubreddits();
    final String FORMAT = config.getTwitter().getFormat().trim().toLowerCase();
    final boolean AUDIO_ENABLED = config.getTwitter().isAudio_enabled();
    final boolean USE_REDDIT_CAPTION = config.getTwitter().isUse_reddit_caption();
    final String FALLBACK_CAPTION = config.getTwitter().getCaption();
    final String HASHTAGS = config.getTwitter().getHashtags();
    String REFRESHTOKEN = config.getTwitter().getRefresh_token().trim();  // Not final because it can be fetched while still running
    String TOKEN; // Temporary access token that must be fetched every cycle

    public void run() {
        if (!getRefreshToken()) {return;} // 1 If refresh token is not set, fetch it. Otherwise, run bot like normal (Quit if failed)

        if (!getAccessToken()) {return;} // 2 Fetch temporary user access token (Quit if failed)

        if (!getUserID()) {return;} // 3 Fetch user's UserID using temporary token fetched in step 2(Quit if failed)

        if (!getMediaSource()) {return;} // 4 Gets media location, cache files (Quit if failed)
    }

    private boolean getRefreshToken() {
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

                    Output.webhookPrint("PLEASE INPUT THE FOLLOWING INTO 'refresh_token' UNDER [Twitter_Settings] IN config.json:" +
                            "\n\t" + REFRESHTOKEN +
                            "\n\tBOT WILL STILL CONTINUE TO RUN BUT IF YOU DONT ADD TO CONFIG YOU WILL NEED TO REAUTHENTICATE NEXT LAUNCH", Output.GREEN);

                    return true;  // Success
                } else {
                    Output.webhookPrint("[X] Failed to fetch token. Quitting..." +
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
        return true;
    }
    private boolean getAccessToken() {
        try {

            return true;
        } catch (Exception e) {
            try {
                Output.webhookPrint(String.valueOf(e), Output.RED);
            } catch (Exception ex) {
                throw new RuntimeException(e);
            }
            return false;
        }
    }

    private boolean getUserContextToken() { // Fetch UserID from X
        try {

            return true;
        } catch (Exception e) {
            try {
                Output.webhookPrint(String.valueOf(e), Output.RED);
            } catch (Exception ex) {
                throw new RuntimeException(e);
            }
            return false;
        }
    }

    private boolean getUserID() { // Fetch UserID from X
        try {
            Map<String, String> headers = new HashMap<>(); // Build Upload Data
            headers.put("Authorization", "Bearer " + TOKEN);
            headers.put("Content-Type", "application/json");
            String request = HTTPSend.get("https://api.x.com/2/users/me", headers);

            Output.webhookPrint(request);
            return true;
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

        try {
            response = HTTPSend.get("https://meme-api.com/gimme/" + chosenSubreddit);
            if (response == "CD") {
                Output.print("[X] Connection drop detected. Trying again...");
                return 1;
            }
        } catch (Exception e) {
            Output.webhookPrint("[X] Failed to fetch image from meme-api.com"
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

                Output.print("[X] Reddit post data successfully retrieved", Output.YELLOW, true);

                /* Check image validity (Ensures not gif, not blacklisted, not already used, valid aspect ratio) */
                switch (ImageValidity.check(response, countAttempt, usedURLs, true, "twitter")) {
                    case 0: // Image valid
                        return 0;
                    case 1: // General failed validation
                        return 1;
                    case 2: // Caption is blacklisted, but allowed to post (CAPTION BLACKLIST)
                        tempDisableCaption = true;
                        return 0;
                }

            case 503: // Cloudflare error
                Output.webhookPrint("[X] Failed. Cloudflare HTTP Status Code 503 - The API this program utilizes appears to be under maintenance."
                        + "\n\tThere is nothing that can be done to fix this but wait. Skipping attempt w/ +6 hour delay...", Output.RED);

                if (!Sleep.safeSleep(sleepTime + 21600000)) break; // Sleep normal time + 6 hours
                return 1;

            default: // General error handling
                Output.webhookPrint("[X] Failed to retrieve image data from meme-api.com with error code " + HTTPSend.HTTPCode.get() + ". Quitting..."
                        + "\n\tError message: " + response, Output.RED);

                return 2;
        }

        Output.webhookPrint("[X] How did the bot get here? This shouldn't be possible. Quitting..."
                + "\n\tError message: " + response, Output.RED);
        return 2;
    }

    // Get media location (based on POSTMODE & selected media format)
    private boolean getMediaSource() {
        try {
            if (AUTOPOSTMODE) {
                Output.debugPrint("[X] Reading automatic cache");
                usedURLs = FileIO.readList("twitter"); // Generate filepath "./cache/[twitter]/cache.txt" for given OS & read file

            } else { // Log manual media
                File directory = Paths.get(".","videos").toFile(); // Generate filepath "./videos"
                Output.debugPrint("[X] Media source set to " + directory);

                if (!directory.exists() || !directory.isDirectory()) {
                    Output.webhookPrint("[X] /videos directory does not exist. Please create it or set post_mode to auto. Quitting...", Output.RED);
                    return false;
                }

                // Ensure there is at least 1 file in directory
                int fileCount = Objects.requireNonNull(directory.list()).length;
                if (fileCount == 0) {
                    Output.webhookPrint("[X] No videos found in /videos directory. Add media or set post_mode to auto. Quitting...", Output.RED);
                    return false;
                }

                Output.debugPrint("[X] Logging media from manual directory");

                // Start logging media
                media = directory.listFiles((_, name) -> name.toLowerCase().endsWith(".mp4")); // Gets all relevant files in the directory
            }

            // Get audio
            if (AUDIO_ENABLED) {
                File directory = Paths.get(".", "audio").toFile(); // Generate filepath "./audio"
                Output.debugPrint("[X] Audio source set to " + directory);

                if (!directory.exists() || !directory.isDirectory()) {
                    Output.webhookPrint("[X] /audio directory does not exist. Please create it, or set 'audio_enabled' to 'false' under [Twitter_Settings] in config.json. Quitting...", Output.RED);
                    return false;
                }

                // Ensure there is at least 1 file in directory
                int fileCount = Objects.requireNonNull(directory.list()).length;
                if (fileCount == 0) {
                    Output.webhookPrint("[X] No audio found in /audio directory. Add audio or set 'audio_enabled' to 'false' under [Twitter_Settings] in config.json. Quitting...", Output.RED);
                    return false;
                }

                Output.debugPrint("[X] Logging audio from manual directory");
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
