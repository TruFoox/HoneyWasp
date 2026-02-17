package services;

import config.ReadConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.awt.Image;
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
    String REFRESH_TOKEN = config.getTwitter().getRefresh_token().trim();  // Not final because it can be fetched while still running
    String TOKEN; // Temporary token that must be fetched every cycle

    public void run() {
        if (!getAccessToken()) {return;} // Fetch user access token (Quit if failed)
        if (!getUserID()) {return;} // Fetch user's UserID (Quit if failed)
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
            Map<String, String> headers = new HashMap<>(); // Build Uplaod Data
            headers.put("Authorization", "Bearer " + TOKEN); // TOKEN = your X API Bearer token
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
    private boolean getUserID() { // Fetch UserID from X
        try {
            Map<String, String> headers = new HashMap<>(); // Build Uplaod Data
            headers.put("Authorization", "Bearer " + TOKEN); // TOKEN = your X API Bearer token
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
