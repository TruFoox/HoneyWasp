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

    // Twitter API expects a randomly generated hash every time you post but... why would I do that?
    String securityKey = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"; // verifier
    String codeChallenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"; // precomputed SHA256


    // Load config
    String KEY = config.getTwitter().getConsumer_key().trim();
    final String SECRET = config.getTwitter().getClient_secret().trim();
    final String POSTMODE = config.getTwitter().getPost_mode().trim().toLowerCase();
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
        if (!getRefreshToken()) {return;} // If refresh token is not set, fetch it. Otherwise, run bot like normal (Quit if failed)

        if (!getAccessToken()) {return;} // Fetch temporary user access token (Quit if failed)

        if (!getUserID()) {return;} // Fetch user's UserID using temporary token (Quit if failed)
    }

    private boolean getRefreshToken() {
        if (REFRESHTOKEN.isEmpty()) { // Only run if no refresh token
            try {
                String scope = "tweet.write media.write users.read offline.access";
                String oauthURL = "https://twitter.com/i/oauth2/authorize?response_type=code" +
                        "&client_id=" + KEY +
                        "&redirect_uri=http://localhost" +
                        "&scope=" + URLEncoder.encode(scope, "UTF-8") + // Convert to UTF-8 to add %20
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

                Output.webhookPrint("[THIS STEP MUST BE DONE IN-CONSOLE] PLEASE PASTE THE ENTIRE URL YOU WERE JUST REDIRECTED TO (SEE https://github.com/TruFoox/HoneyWasp/#twitter-setup FOR HELP):", Output.RED);
                String redirectUrl = scanner.nextLine(); // Read user input

                String authCode = redirectUrl.split("code=")[1].split("&")[0]; // split on "code=" and stop at next "&"


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

    public static void stop() { // Stop bot
        run = false;
        Output.webhookPrint("Twitter successfully stopped");
    }

    public static void clear() { // Clear cache
        FileIO.clearList("twitter");
        Output.webhookPrint("Twitter cache successfully cleared");
    }
}
