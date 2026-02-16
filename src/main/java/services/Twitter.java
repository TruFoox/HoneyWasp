package services;

import config.ReadConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Twitter {
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
    String SECRET = config.getYoutube().getClient_secret().trim();
    String ID = config.getYoutube().getClient_id().trim();
    final String POSTMODE = config.getYoutube().getPost_mode().trim().toLowerCase();
    final int TIME_BETWEEN_POSTS = config.getYoutube().getTime_between_posts();
    final int sleepTime = TIME_BETWEEN_POSTS * 60000; // Generate time to sleep between posts in milliseconds
    final int ATTEMPTS_BEFORE_TIMEOUT = config.getYoutube().getAttempts_before_timeout();
    final List<String> SUBREDDITS = config.getYoutube().getSubreddits();
    final boolean USE_REDDIT_CAPTION = config.getYoutube().isUse_reddit_caption();
    final boolean AUDIO_ENABLED = config.getYoutube().isAudio_enabled();
    final String FALLBACK_CAPTION = config.getYoutube().getCaption();
    final String DESCRIPTION = config.getYoutube().getDescription();
    String REFRESHTOKEN = config.getYoutube().getRefresh_token(); // Not final because it can be fetched while still running

}
