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
    final String TOKEN = config.getTwitter().getApi_key().trim();
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



}
