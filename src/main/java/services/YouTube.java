package services;

import java.util.Scanner;
import java.awt.Desktop;
import java.awt.Image;
import java.net.URI;
import java.net.URL;
import javax.imageio.ImageIO;
import config.*;
import org.json.*;
import utils.*;

import java.nio.file.*;
import java.io.*;
import java.util.*;



public class YouTube implements Runnable {
    ReadConfig config = ReadConfig.getInstance(); // Get config
    Random rand = new Random(); // Generate seed for random number generation
    Scanner scanner = new Scanner(System.in); // Scanner

    // Empty global variables
    long USERID, countAttempt = 0;
    List<String[]> usedURLs = new ArrayList<>();
    String chosenSubreddit, mediaURL, redditURL, caption, fileDir;
    boolean run = true, nsfw, tempDisableCaption;
    int randIndex;
    File[] media;

    // Load config
    String SECRET = config.getYoutube().getClient_secret().trim();
    String ID = config.getYoutube().getClient_id().trim();
    final String POSTMODE = config.getYoutube().getPost_mode().trim().toLowerCase();
    final int TIME_BETWEEN_POSTS = config.getYoutube().getTime_between_posts();
    final int sleepTime = TIME_BETWEEN_POSTS * 60000; // Generate time to sleep between posts in milliseconds
    final int ATTEMPTS_BEFORE_TIMEOUT = config.getYoutube().getAttempts_before_timeout();
    final List<String> SUBREDDITS = config.getYoutube().getSubreddits();
    final boolean USE_REDDIT_CAPTION = config.getYoutube().isUse_reddit_caption();
    final String FALLBACK_CAPTION = config.getYoutube().getCaption();
    final String DESCRIPTION = config.getYoutube().getDescription();
    String REFRESHTOKEN = config.getYoutube().getRefresh_token(); // Not final because it can be fetched while still running

    public void run() {
        if (!getRefreshToken()) {return;} // If refresh token is not set, fetch it. Otherwise, run bot like normal (Quit if failed)

        while (run) {

        }
    }

    /* If refresh token is not set, fetch it. Otherwise, skip*/
    private boolean getRefreshToken() {
        if (REFRESHTOKEN.isEmpty()) { // Only run if no refresh token
            // Generate OAuth URL & prompt user to go there to get token
            String oauthURL = "https://accounts.google.com/o/oauth2/auth?client_id=" + ID + "&redirect_uri=http://localhost&response_type=code&scope=https://www.googleapis.com/auth/youtube.upload&access_type=offline&prompt=consent";

            Output.webhookPrint("BEFORE YOU CAN POST TO YOUTUBE, YOU MUST RETRIEVE YOUR ACCESS TOKEN." +
                    "\n\tATTEMPTING TO REDIRECT YOU TO THE AUTHENTICATION SITE NOW (OR GO TO " + oauthURL + ")", Output.RED);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) { // Test if browser allows going to URL from here
                try {
                    Desktop.getDesktop().browse(new URI(oauthURL));
                } catch (Exception e) {
                    // Ignore
                }
            }

            Output.webhookPrint("[THIS STEP MUST BE DONE IN-CONSOLE] PLEASE INPUT THE AUTHORIZATION CODE YOU RECEIVED AFTER GRANTING ACCESS (SEE https://github.com/TruFoox/HoneyWasp/#youtube-setup FOR HELP):", Output.RED);
            String authCode = scanner.nextLine(); // Read user input


            // Build upload data
            Map<String, String> formData = new HashMap<>();

            formData.put("client_id", ID);
            formData.put("client_secret", SECRET);
            formData.put("code", authCode);
            formData.put("grant_type", "authorization_code");
            formData.put("redirect_uri", "http://localhost");

            String response;

            try {
                response = HTTPSend.postForm("https://oauth2.googleapis.com/token", formData);
            } catch (Exception e) {
                Output.webhookPrint("[YT] Failed to fetch token. Quitting..." +
                        "\n\tError: " + e, Output.RED);

                return false;
            }

            if (HTTPSend.HTTPCode == 200) {
                REFRESHTOKEN = StringToJson.getData(response, "refresh_token");

                Output.webhookPrint("PLEASE INPUT THE FOLLOWING INTO 'refresh_token' UNDER [YouTube_Settings] IN bot.json:" +
                        "\n\t" + REFRESHTOKEN +
                        "\n\tBOT WILL STILL CONTINUE TO RUN BUT IF YOU DONT ADD TO CONFIG YOU WILL NEED TO REAUTHENTICATE NEXT LAUNCH", Output.GREEN);

                return true;  // Success
            } else {
                Output.webhookPrint("[YT] Failed to fetch token. Quitting..." +
                        "\n\tError message: " + response, Output.RED);

                return false;
            }
        }

        return true; // Skip this step
    }
}