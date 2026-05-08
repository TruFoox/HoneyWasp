package services;

import config.Config;
import config.PlatformSettings;
import utils.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

// This makes the old separate classes, ImageValidity redundant

public abstract class Services extends Thread {
    private final String shortName, name;

    protected Config config;
    protected PlatformSettings settings;

    Scanner scanner = new Scanner(System.in); // Input scanner
    Random rand = new Random(); // Generate seed for random number generation

    public Services(String name, String shortName, Config config) {
        this.name = name;
        this.shortName = shortName;
        this.config = config;
    }
    public String getShortname() {return shortName;}
    abstract boolean upload() throws Exception;
    abstract boolean publish() throws Exception;
    abstract boolean fetchUserToken();

    // Empty global/commonly used variables
    protected java.util.List<String[]> usedURLs = new ArrayList<>();
    protected String chosenSubreddit, mediaURL, redditURL, caption, fileDir, response, postID;
    protected boolean nsfw, tempDisableCaption, doSizeTest = true, run = true, use0x0 = false;
    protected int randIndex, sleepTime, countAttempt = 0;
    protected File[] media, audio;

    // Config
    protected String TOKEN, FALLBACK_CAPTION, CAPTION, HASHTAGS, REFRESH_TOKEN;
    protected List<String> SUBREDDITS, CAPTION_BLACKLIST, BLACKLIST;
    protected boolean AUTO_POST_MODE, VIDEO_MODE, AUDIO_ENABLED, USE_REDDIT_CAPTION, NSFW_ALLOWED, DUPLICATES_ALLOWED;
    protected int ATTEMPTS_BEFORE_TIMEOUT, MINS_BETWEEN_POSTS, HOURS_BEFORE_DUPLICATES_REMOVED;

    public void run() {
        try {
            settings = Config.getInstance().Platform(name.toLowerCase());

            // Initialize settings
            AUTO_POST_MODE = settings.isAuto_post_mode();
            MINS_BETWEEN_POSTS = settings.getTime_between_posts();
            ATTEMPTS_BEFORE_TIMEOUT = settings.getAttempts_before_timeout();
            SUBREDDITS = settings.getSubreddits();
            AUDIO_ENABLED = settings.isAudio_enabled();
            USE_REDDIT_CAPTION = settings.isUse_reddit_caption();
            FALLBACK_CAPTION = settings.getCaption();
            NSFW_ALLOWED = settings.isNsfw_allowed();
            DUPLICATES_ALLOWED = settings.isDuplicates_allowed();
            BLACKLIST = settings.getBlacklist();
            CAPTION_BLACKLIST = settings.getCaption_blacklist();
            HOURS_BEFORE_DUPLICATES_REMOVED = settings.getHours_before_duplicate_removed();
            CAPTION = settings.getCaption();
            HASHTAGS = settings.getHashtags();

            sleepTime = settings.getTime_between_posts() * 60000; // Generate time to sleep between posts in milliseconds

            if (this instanceof HasUserID) {  // Check if current instance contains fetchUserID and run it if it does (Quit if failed)
                if (!((HasUserID) this).fetchUserID()) {return;}
            }

            if (this instanceof HasRefreshToken) {  // Check if current instance contains fetchRefreshToken and run it if it does (Quit if failed)
                Output.debugPrint("Testing if refresh_token is empty");
                if (REFRESH_TOKEN.isEmpty()) { // Only run if no refresh token
                    if (!((HasRefreshToken) this).fetchRefreshToken()) {return;} // Fetch token
                } else {
                    Output.debugPrint("refresh_token was found to contain data");
                }
            }

            if (!getMediaSource()) {return;} // Fetch media source (Quit if failed)

            // Start bot
            while (run) {
                countAttempt++; // Iterate count for number of attempts to post that have been made
                Output.debugPrinttest(this, "Attempt " + countAttempt + " started");

                if (countAttempt == 1) { // Print first attempt message
                    Output.printtest(this, "Attempting new post", Output.YELLOW, true,true);
                }

                if (countAttempt > ATTEMPTS_BEFORE_TIMEOUT && ATTEMPTS_BEFORE_TIMEOUT != 0) { // If max # of attempts have been reached
                    Output.webhookPrinttemptest(this,"Max # of attempts reached. Skipping attempt...", Output.YELLOW, true);

                    if (!Sleep.safeSleep(sleepTime)) break; // Sleep (Easy way to fake a "skipped attempt")
                    countAttempt = 1;
                }

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
                    Output.debugPrinttest(this, "Successfully fetched URL " + mediaURL);

                    /* If format is video, convert image to video */
                    if (VIDEO_MODE) {
                        Image image; // Holds image data

                        Output.debugPrinttest(this, "Attempting to retrieve image data");
                        try {
                            // Download image from Reddit
                            URL url = new URL(mediaURL);
                            image = ImageIO.read(url);

                        } catch (javax.imageio.IIOException e) { // Corrupt image (or similar)
                            Output.webhookPrinttemptest(this,"Image appears to be in an unhandleable format. Trying again, and marking this URL as invalid...");

                            // Blacklist image URL permanently, as it is likely corrupted
                            FileIO.writeList(mediaURL, name.toLowerCase(), true);
                            continue;
                        } catch (IOException e) {
                            Output.webhookPrinttemptest(this,"Failed to download image from Reddit to convert to video. Skipping attempt w/ +2 hour delay..."
                                    + "\n\tError message: " + e, Output.RED);

                            if (!Sleep.safeSleep(7200000)) break;
                            continue;
                        }

                        /* Select mp4 for audio if audio enabled */
                        String audioDir = null; // Default value

                        if (AUDIO_ENABLED) {
                            Output.debugPrinttest(this, "Attempting to select audio file for use");
                            randIndex = rand.nextInt(audio.length); // Select random audio file
                            audioDir = String.valueOf(audio[randIndex]);
                        }

                        Output.printtest(this, "Converting image to video...", Output.YELLOW, true);

                        if (ImageToVideo.convert(String.valueOf(Paths.get(".", "cache", name.toLowerCase(), "temp")), image, audioDir)) { // Convert image to video
                            Output.printtest(this, "Successfully converted image to video", Output.YELLOW, true);
                        } else {
                            Output.printtest(this, "Failed to convert image to video for upload. Skipping attempt...", Output.RED);

                            if (!Sleep.safeSleep(sleepTime)) break;
                            continue;
                        }
                        fileDir = "./cache/" + name.toLowerCase() + "/temp.mp4";
                    }
                } else {
                    randIndex = rand.nextInt(media.length); // Select random image
                    fileDir = String.valueOf(media[randIndex]);
                }

                if (use0x0) { // If enabled for this service, upload manual media/generated video to temp file hoster (0x0.st)
                    if (!AUTO_POST_MODE || VIDEO_MODE) {
                        Output.printtest(this, "Uploading media to temp file hoster...", Output.YELLOW, true);

                        String response = HTTPSend.postFile("https://0x0.st", Path.of(fileDir)); // Send file to 0x0

                        // Error handling
                        if (HTTPSend.HTTPCode.get() == 403) {
                            Output.webhookPrinttemptest(this, "0x0.su (temp storage provider) returned HTTP 403 - Oh no! You've likely been flagged as a bot by the temp storage site!" +
                                    "\n\tYour IP should be cycled and unblocked in a few months." +
                                    "\n\n\tIn the meantime, you should set 'video_mode' to 'false' & 'post_mode' to 'auto' under [" + name + " Settings]" +
                                    "\n\tin config.json to bypass the need for temporary storage. Quitting..." +
                                    "\n\n\tError message: " + response, Output.RED);

                            return;
                        } else if (!(HTTPSend.HTTPCode.get() == 200)) { // Misc error handling
                            Output.webhookPrinttemptest(this, "Error uploading file to 0x0.su (temp storage provider). Quitting..." +
                                    "\n\tError message: " + response, Output.RED);

                            return;
                        }

                        mediaURL = response;

                        if (mediaURL.endsWith("\n")) { // Remove trailing newline 0x0 adds for some reason
                            mediaURL = mediaURL.substring(0, mediaURL.length() - 1);
                        }

                        // Success message
                        Output.printtest(this, "Successfully uploaded to temp storage: " + mediaURL, Output.YELLOW, true);
                    }
                }

                if (!fetchUserToken()) {return;}

                if (upload()) {
                    if (!Sleep.safeSleep(2000)) return; // Sleep 2 seconds to allow server time to process

                    if (publish()) {
                        if (AUTO_POST_MODE) {
                            Output.webhookPrinttemptest(this,redditURL + " from r/" + chosenSubreddit + " uploaded - x" + countAttempt + " attempt(s)", Output.GREEN);
                        } else {
                            Output.webhookPrinttemptest(this,redditURL + " uploaded to " + name + " - x" + countAttempt + " attempt(s)", Output.GREEN);
                        }


                        // Store image URL to prevent duplicates
                        FileIO.writeList(mediaURL, name.toLowerCase(), false);

                        long timestamp = System.currentTimeMillis();
                        usedURLs.add(new String[]{mediaURL, String.valueOf(timestamp)});

                        if (!Sleep.safeSleep(sleepTime)) break; // Sleep
                        countAttempt = 0;
                    }
                }

                if (!Sleep.safeSleep(1500)) return; // Sleep 1.5 secs
            }
        } catch (InterruptedException e) { // When the thread's stop flag is thrown while it is busy
            Output.webhookPrinttemptest(this,"Unexpected error during sleep: " + e.getMessage(), Output.RED);
        } catch (SocketException e) {
            Output.webhookPrinttemptest(this,"Bot crashed: Connection likely dropped", Output.RED);
        } catch (IOException e) {
            Output.webhookPrinttemptest(this,"Bot crashed: IO issue occurred", Output.RED);
        } catch (Exception e) { // General error handling
            Output.webhookPrinttemptest(this,"Bot crashed with unexpected error: " + e.getMessage(), Output.RED);

        } finally { // Crash/Stop handling
            Output.webhookPrinttemptest(this, "Stopped");

            // Set status running false here
        }
    }

    private int getMemeAPI() throws Exception {
        String response;

        randIndex = rand.nextInt(SUBREDDITS.size()); // Generate random subreddit index

        chosenSubreddit = SUBREDDITS.get(randIndex);

        String URL = "https://meme-api.com/gimme/" + chosenSubreddit;

        Output.debugPrinttest(this, "Fetching media URL from " + URL);
        try {
            response = HTTPSend.get(URL);
        } catch (ConnectException e) {
            Output.printtest(this, "Connection drop detected. Trying again in 10 seconds...");

            if (!Sleep.safeSleep(10000)) return 2; // Sleep 10 secs
            return 1;
        } catch (Exception e) {
            Output.webhookPrinttemptest(this,"Failed to fetch image from meme-api.com"
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

                Output.debugPrinttest(this, "Reddit post data successfully retrieved");

                /* Check image validity (Ensures not gif, not blacklisted, not already used, valid aspect ratio) */
                switch (checkValidity()) {
                    case 0: // Image valid
                        return 0;
                    case 1: // General failed validation
                        return 1;
                    case 2: // Caption is blacklisted, but allowed to post (CAPTION BLACKLIST)
                        tempDisableCaption = true;
                        return 0;
                }

            case 503: // Cloudflare error
                Output.webhookPrinttemptest(this,"Failed. Cloudflare HTTP Status Code 503 - The API this program utilizes appears to be under maintenance."
                        + "\n\tThere is nothing that can be done to fix this but wait. Skipping attempt w/ +6 hour delay...", Output.RED);

                if (!Sleep.safeSleep(sleepTime + 21600000)) break; // Sleep normal time + 6 hours
                return 1;
            case 502: // Cloudflare error 2
                Output.webhookPrinttemptest(this,"Failed. Cloudflare HTTP Status Code 502 - The API this program utilizes gave a bad response"
                        + "\n\tThere is nothing that can be done to fix this but wait. Skipping attempt...", Output.RED);

                if (!Sleep.safeSleep(sleepTime)) break; // Sleep normal time
                return 1;
            case 530: // Cloudflare error 3
                Output.webhookPrinttemptest(this,"Failed. Cloudflare HTTP Status Code 530 - The API this program utilizes is temporarily unreachable"
                        + "\n\tThere is nothing that can be done to fix this but wait, but it shouldn't take too long. Skipping attempt...", Output.RED);

                if (!Sleep.safeSleep(sleepTime)) break; // Sleep normal time + 6 hours
                return 1;
            default: // General error handling
                Output.webhookPrinttemptest(this,"Failed to retrieve image data from meme-api.com with error code " + HTTPSend.HTTPCode.get() + ". Quitting..."
                        + "\n\tError message: " + response, Output.RED);

                return 2;
        }
        return 2; // Isn't possible but the compiler whines
    }

    // Get media location (based on POSTMODE & selected media format)
    boolean getMediaSource() {
        try {
            if (AUTO_POST_MODE) {
                Output.debugPrinttest(this, "Reading automatic cache");
                usedURLs = FileIO.readList(name.toLowerCase()); // Generate filepath "./cache/[name]/cache.txt" for given OS & read file
                if (usedURLs == null) {return false;}

            } else { // Log manual media
                String format = (VIDEO_MODE) ? "videos" : "images";
                File directory = Paths.get(".", format).toFile(); // Generate filepath "./{Format}"
                Output.debugPrinttest(this, "Media source set to " + directory);

                if (!directory.exists() || !directory.isDirectory()) {
                    Output.webhookPrinttemptest(this,String.format("/%s directory does not exist. Please create it or set post_mode to auto. Quitting...", format), Output.RED);
                    return false;
                }

                // Ensure there is at least 1 file in directory
                int fileCount = Objects.requireNonNull(directory.list()).length;
                if (fileCount == 0) {
                    Output.webhookPrinttemptest(this,String.format("No %s found in /%s directory. Add media or set post_mode to auto. Quitting...", format, format), Output.RED);
                    return false;
                }

                Output.debugPrinttest(this, "Logging media from manual directory");

                // Start logging media
                media = directory.listFiles((_, name) -> name.toLowerCase().endsWith(".mp4")); // Gets all relevant files in the directory
            }

            // Get audio
            if (AUDIO_ENABLED && VIDEO_MODE) {
                File directory = Paths.get(".", "audio").toFile(); // Generate filepath "./audio"
                Output.debugPrinttest(this, "Audio source set to " + directory);

                if (!directory.exists() || !directory.isDirectory()) {
                    Output.webhookPrinttemptest(this,"/audio directory does not exist. Please create it, or set 'audio_enabled' to 'false' under [" + name + "_Settings] in config.json. Quitting...", Output.RED);
                    return false;
                }

                // Ensure there is at least 1 file in directory
                int fileCount = Objects.requireNonNull(directory.list()).length;
                if (fileCount == 0) {
                    Output.webhookPrinttemptest(this,"No audio found in /audio directory. Add audio or set 'audio_enabled' to 'false' under [" + name + "_Settings] in config.json. Quitting...", Output.RED);
                    return false;
                }

                Output.debugPrinttest(this, "Logging audio from manual directory");
                audio = directory.listFiles((_, name) -> name.toLowerCase().endsWith(".mp3")); // Gets all relevant files in the directory
            }
        } catch (Exception e) {
            try {
                Output.webhookPrinttemptest(this,String.valueOf(e), Output.RED);
            } catch (Exception ex) {
                throw new RuntimeException(e);
            }
            return false;
        }
        return true; // Success
    }

    /* Check image validity (Ensures not gif, not blacklisted, not already used, valid aspect ratio) */
    public int checkValidity() {
        Output.debugPrinttest(this, "Validating image");
        
        // Download image & check aspect ratio if needed
        if (doSizeTest) {
            Output.debugPrinttest(this, "Attempting to download image to verify aspect ratio validity");
            Image image;

            try {
                URL url = new URL(mediaURL);
                image = ImageIO.read(url);
            } catch(IOException e)  {
                Output.webhookPrinttemptest(this, "Failed to download image from Reddit to check aspect ratio..."
                        + "\n\tError message: " + e, Output.RED);

                return 1;
            }

            float ratio = (float) image.getWidth(null) / image.getHeight(null);

            Output.debugPrinttest(this, "Image aspect ratio is " + image.getWidth(null) + ":" + image.getHeight(null));
            if (ratio < 0.82 || ratio > 1.70) {
                Output.printtest(this, "Image has invalid aspect ratio", Output.RED, true);
                return 1;
            }

        }

        // Test image validity
        Output.debugPrinttest(this, "Testing if image is gif");
        if (mediaURL.contains(".gif")) { // Ensure image is not gif
            Output.printtest(this, "Image is gif - x" + countAttempt + " attempts", Output.RED, true);

            return 1;
        }

        // Ensure no blacklisted string in post caption
        Output.debugPrinttest(this, "Testing if image caption contains blacklisted string");
        for (String word : BLACKLIST) {
            if (caption.toLowerCase().contains(word.toLowerCase())) {
                Output.printtest(this, "Caption contains blacklisted string - x" + countAttempt + " attempts", Output.RED, true);

                return 1;
            }
        }

        // Ensure post is not duplicate
        Output.debugPrinttest(this, "Testing if image url is duplicate");
        for (String[] row : usedURLs) {
            String usedUrl = row[0];
            String timestampStr = row[1];

            long timestamp = Long.parseLong(timestampStr);

            if (DUPLICATES_ALLOWED || ((System.currentTimeMillis() - timestamp) < HOURS_BEFORE_DUPLICATES_REMOVED * 3600000)) { // Test if cached url is too old to be considered duplicate
                if (mediaURL.equalsIgnoreCase(usedUrl)) {
                    Output.printtest(this, "Duplicate URL - x" + countAttempt + " attempts", Output.RED, true);

                    return 1;
                }
            }
        }

        Output.debugPrinttest(this, "Testing if image is marked as NSFW");
        if (!NSFW_ALLOWED && nsfw) { // If post is marked as NSFW and NSFW is disallowed
            Output.printtest(this, "Image is marked as NSFW - x" + countAttempt + " attempts", Output.RED, true);

            return 1;
        }

        Output.debugPrinttest(this, "Testing if caption contains blacklisted strings to use preset caption");
        for (String word : CAPTION_BLACKLIST) { // Ensure no semi-blacklisted string in post caption. If found, discard caption but still post
            if (caption.toLowerCase().contains(word.toLowerCase())) {
                Output.printtest(this, "Using fallback caption (\"" + word + "\" found) - x" + countAttempt + " attempts", Output.RED, true);

                return 2;
            }
        }

        return 0;
    }

    public void halt() { // Stop bot
        run = false;
        Output.webhookPrinttemptest(this, "Successfully stopped");
    }

    public void clear() { // Clear cache
        FileIO.clearList(name.toLowerCase());
        Output.webhookPrinttemptest(this, "Cache successfully cleared");
    }

}
