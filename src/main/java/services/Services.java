package services;
import config.PlatformSettings;
import main.HoneyWasp;
import utils.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public abstract class Services extends Thread {
    public final String shortName, name;

    protected PlatformSettings settings;
    protected static final Scanner scanner = new Scanner(System.in); // Input scanner
    protected static final Random rand = new Random(); // Generate seed for random number generation

    protected abstract boolean upload() throws Exception;
    protected abstract boolean publish() throws Exception;
    protected abstract boolean fetchUserToken() throws Exception;

    // Empty global/commonly used variables
    public java.util.List<String[]> usedURLs = new ArrayList<>();
    protected String chosenSubreddit, mediaURL, redditURL, caption, fileDir, response, postID;
    protected boolean nsfw, tempDisableCaption, doSizeTest = true, run = true, use0x0 = false;
    protected int randIndex, countAttempt, connectionDropWait;
    protected File[] media, audio;

    // Config
    protected String TOKEN, FALLBACK_CAPTION, CAPTION, HASHTAGS, REFRESH_TOKEN;
    protected List<String> SUBREDDITS, CAPTION_BLACKLIST, BLACKLIST;
    protected boolean AUTO_POST_MODE, VIDEO_MODE, AUDIO_ENABLED, USE_REDDIT_CAPTION, NSFW_ALLOWED, DUPLICATES_ALLOWED;
    protected int ATTEMPTS_BEFORE_TIMEOUT, SLEEPTIME;
    public int HOURS_BEFORE_DUPLICATES_REMOVED; // Used in FileIO.java

    public Services(String name, String shortName) { // Constructor
        this.name = name;
        this.shortName = shortName;

        // Initialize settings
        settings = HoneyWasp.config.Platform(name.toLowerCase()); // Select config for this platform

        AUTO_POST_MODE = settings.isAuto_post_mode();
        SLEEPTIME = settings.getMinutes_between_posts() * 60000; // Generate time to sleep between posts in milliseconds
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

    }

    // Remember:
    // Don't use return - it skips removing the service from list of running services
    // Break to quit loop - keep in mind this will loop again if restart enabled
    // Continue to re-loop
    public void run() {
        Output.debugPrint(null, "New " + name + " instance running w/ thread ID " + Thread.currentThread().threadId());

        do { // Loop if restart enabled
            run = true;
            try {
                if (this instanceof HasRefreshToken) {  // Check if current instance contains fetchRefreshToken and run it if it does (Quit if failed)
                    Output.debugPrint(this, "Testing if refresh_token is empty");
                    if (REFRESH_TOKEN.isEmpty()) { // Only run if no refresh token
                        if (!((HasRefreshToken) this).fetchRefreshToken()) {break;} // Fetch token
                    } else {
                        Output.debugPrint(this, "refresh_token was found to contain data");
                    }
                }

                if (!getMediaSource()) {break;} // Fetch media source (Quit if failed)

                FileIO.autoClear(this); // Scan cache for posts whose duplicate check has expired

                // Start bot
                while (run) { // Main loop
                    countAttempt++; // Iterate count for number of attempts to post that have been made
                    Output.debugPrint(this, "Attempt " + countAttempt + " started");

                    if (countAttempt == 1) { // Print first attempt message
                        Output.print(this, "Attempting new post", Output.YELLOW, true, true);
                    }

                    if (countAttempt > ATTEMPTS_BEFORE_TIMEOUT && ATTEMPTS_BEFORE_TIMEOUT != 0) { // If max # of attempts have been reached
                        Output.webhookPrint(this, "Max # of attempts reached. Skipping attempt...", Output.YELLOW, true);

                        Thread.sleep(SLEEPTIME); // Sleep (Easy way to fake a "skipped attempt")
                        countAttempt = 0;
                    }

                    /* Fetch media */
                    if (AUTO_POST_MODE) {
                        boolean memeAPIFailed = false;

                        switch (getMemeAPI()) {
                            case 0: // Success
                                break;
                            case 1: // Soft fail (retry)
                                Thread.sleep(1000); // Sleep 1s to prevent spam
                                continue;
                            case 2: // Fail (quit) - this doesn't use break to exit main loop because that doesn't work in switch statements
                                memeAPIFailed = true;
                                break;
                        }
                        if (memeAPIFailed) break; // Workaround to case 2

                        if (!run) break;

                        Output.debugPrint(this, "Successfully fetched URL " + mediaURL);

                        /* If format is video, convert image to video */
                        if (VIDEO_MODE) {
                            Image image; // Holds image data

                            Output.debugPrint(this, "Attempting to retrieve image data");
                            try {
                                // Download image from Reddit
                                URL url = new URI(mediaURL).toURL();
                                image = ImageIO.read(url);

                            } catch (javax.imageio.IIOException e) { // Corrupt image (or similar)
                                Output.webhookPrint(this, "Image appears to be in an unhandleable format. Trying again, and marking this URL as invalid..." +
                                        "\n\tError message: " + e, Output.RED);

                                // Blacklist image URL permanently, as it is likely corrupted
                                FileIO.writeList(mediaURL, this, true);

                                Thread.sleep(5000); // Sleep 5 seconds in case it is a temporary error
                                continue;
                            } catch (IOException e) {
                                Output.webhookPrint(this, "Failed to download image from Reddit to convert to video. Skipping attempt w/ +2 hour delay..."
                                        + "\n\tError message: " + e, Output.RED);

                                Thread.sleep(7200000);
                                continue;
                            }

                            if (!run) break;

                            /* Select mp4 for audio if audio enabled */
                            String audioDir = null; // Default value

                            if (AUDIO_ENABLED) {
                                Output.debugPrint(this, "Attempting to select audio file for use");
                                randIndex = rand.nextInt(audio.length); // Select random audio file
                                audioDir = String.valueOf(audio[randIndex]);
                            }

                            Output.print(this, "Converting image to video...", Output.YELLOW, true);

                            if (ImageToVideo.convert(String.valueOf(Paths.get(".", "cache", name.toLowerCase(), "temp")), image, audioDir)) { // Convert image to video
                                Output.print(this, "Successfully converted image to video", Output.YELLOW, true);
                            } else {
                                Output.print(this, "Failed to convert image to video for upload. Skipping attempt...", Output.RED);

                                Thread.sleep(SLEEPTIME);
                                continue;
                            }
                            fileDir = "./cache/" + name.toLowerCase() + "/temp.mp4";
                        }
                    } else {
                        randIndex = rand.nextInt(media.length); // Select random image
                        fileDir = String.valueOf(media[randIndex]);
                    }

                    if (!run) break;

                    if (use0x0) { // If enabled for this service, upload manual media/generated video to temp file hoster (0x0.st)
                        if (!AUTO_POST_MODE || VIDEO_MODE) {
                            Output.print(this, "Uploading media to temp file hoster...", Output.YELLOW, true);

                            String response = HTTPSend.postFile(this, "https://0x0.st", Path.of(fileDir)); // Send file to 0x0

                            // Error handling
                            if (HTTPSend.HTTPCode.get() == 403) {
                                Output.webhookPrint(this, "0x0.su (temp storage provider) returned HTTP 403 - Oh no! You've likely been flagged as a bot by the temp storage site!" +
                                        "\n\tYour IP should be cycled and unblocked in a few months." +
                                        "\n\n\tIn the meantime, you should set 'video_mode' to 'false' & 'post_mode' to 'auto' under [" + name + " Settings]" +
                                        "\n\tin config.json to bypass the need for temporary storage. Quitting..." +
                                        "\n\n\tError message: " + response, Output.RED);

                                break;
                            } else if (!(HTTPSend.HTTPCode.get() == 200)) { // Misc error handling
                                Output.webhookPrint(this, "Error uploading file to 0x0.su (temp storage provider). Quitting..." +
                                        "\n\tError message: " + response, Output.RED);

                                break;
                            }

                            mediaURL = response;

                            if (mediaURL.endsWith("\n")) { // Remove trailing newline 0x0 adds for some reason
                                mediaURL = mediaURL.substring(0, mediaURL.length() - 1);
                            }

                            // Success message
                            Output.print(this, "Successfully uploaded to temp storage: " + mediaURL, Output.YELLOW, true);
                        }
                    }

                    /* Fetch token, upload, then publish media */

                    // Lots of if (!run) to combat /stop not working, especially on poor internet connections
                    if (!run) break;

                    if (!fetchUserToken()) { // Attempt to fetch access token (Quit if failed)
                        Output.webhookPrint(this, "Failed to fetch access token. Quitting...", Output.RED);
                        break;
                    }

                    if (!run) break;

                    if (upload()) {
                        Thread.sleep(1500); // Sleep 1.5 sec to allow server time to process (A complete waste of time 99% of the time, but better be safe than sorry)

                        if (!run) break;

                        if (publish()) {
                            if (AUTO_POST_MODE) {
                                Output.webhookPrint(this, redditURL + " from r/" + chosenSubreddit + " posted successfully - x" + countAttempt + " attempt(s)", Output.GREEN);
                                FileIO.writeList(mediaURL, this, false); // Store image URL to prevent duplicates
                            } else if (use0x0) {
                                Output.webhookPrint(this, mediaURL + " posted successfully - x" + countAttempt + " attempt(s)", Output.GREEN);
                            } else {
                                Output.webhookPrint(this, fileDir + " posted successfully - x" + countAttempt + " attempt(s)", Output.GREEN);
                            }


                            System.gc(); // Suggest garbage collection
                            if (run) {Thread.sleep(SLEEPTIME);} // Sleep if /stop not used
                            countAttempt = 0; // Reset count attempt
                        } else {Output.webhookPrint(this, "Failed to publish", Output.RED);}
                    } else {Output.webhookPrint(this, "Failed to upload", Output.RED);}

                    Thread.sleep(1500); // Sleep 1.5s to prevent spam
                } // Main loop
            } catch (InterruptedException e) { // This error is thrown whenever /stop is used while sleeping, so it's hidden by default
                Output.debugPrint(this, "Bot crashed - Error during sleep: " + e.getMessage());
            } catch (SocketException e) {
                Output.webhookPrint(this, "Bot crashed - Connection likely dropped: " + e.getMessage(), Output.RED);
            } catch (IOException e) {
                Output.webhookPrint(this, "Bot crashed - IO issue occurred: " + e.getMessage(), Output.RED);
            } catch (Exception e) { // General error handling
                Output.webhookPrint(this, "Bot crashed - Unknown error: " + e.getMessage(), Output.RED);
            } finally { // Crash/Stop handling
                Output.webhookPrint(this, "Stopped");
            }

            if (HoneyWasp.RESTART && run) {
                Output.webhookPrint(null, "It looks like " + name + " crashed. It will be restarted in 5 seconds...");
                try {Thread.sleep(5000);} catch (Exception _) {break;} // Sleep 5 secs to prevent spam if the crash is repeated
            }
        } while (HoneyWasp.RESTART && run); // Loop if restart enabled & /stop not used

        HoneyWasp.runningServices.remove(name.toLowerCase()); // Remove service from list of running ones
    }

    private int getMemeAPI() throws Exception {
        String response;

        randIndex = rand.nextInt(SUBREDDITS.size()); // Generate random subreddit index

        chosenSubreddit = SUBREDDITS.get(randIndex);

        String URL = "https://meme-api.com/gimme/" + chosenSubreddit;

        Output.debugPrint(this, "Fetching media URL from " + URL);
        try {
            connectionDropWait = 0; // Reset connection drop wait on success
            response = HTTPSend.get(this, URL);
        } catch (ConnectException e) {
            // ConnectionDropWait = Pre-processing time, waitTime = Post-processed time
            int waitTime = (connectionDropWait == 0) ? 1 : connectionDropWait; // Forces first wait to be 1, and allows sequential waits to be 5n minutes


            Output.print(this, "Connection drop detected. Trying again in " + waitTime + " minute(s)...");
            connectionDropWait += 5;

            Thread.sleep(waitTime * 60000L);
            return 1;
        } catch (Exception e) {
            connectionDropWait = 0;
            Output.webhookPrint(this,"Failed to fetch image from meme-api.com"
                    + "\n\tError message: " + e, Output.RED);
            return 2;
        }

        /* Status code handling */
        int HTTPCode = HTTPSend.HTTPCode.get().intValue();
        switch (HTTPCode) {
            case 200: // Success
                // Parse JSON data
                mediaURL = StringToJson.getData(response, "url");
                redditURL = StringToJson.getData(response, "postLink");
                caption = StringToJson.getData(response, "title");
                nsfw = Boolean.parseBoolean(StringToJson.getData(response, "nsfw"));
                tempDisableCaption = false;

                Output.debugPrint(this, "Reddit post data successfully retrieved");

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

            case 503: // Cloudflare error 1
                Output.webhookPrint(this,"Cloudflare HTTP Status Code 503 - The API this program utilizes appears to be under maintenance."
                        + "\n\tThere is nothing that can be done to fix this but wait. Skipping attempt w/ +6 hour delay...", Output.RED);

                Thread.sleep(SLEEPTIME + 21600000L); // Sleep normal time + 6 hours
                return 1;
            case 530: // Cloudflare error 2
                Output.webhookPrint(this,"Cloudflare HTTP Status Code 530 - The API this program utilizes is temporarily unreachable"
                        + "\n\tThere is nothing that can be done to fix this but wait. Skipping attempt w/ +2 hour delay...", Output.RED);

                Thread.sleep(SLEEPTIME + 7200000L); // Sleep normal time + 2 hours
                return 1;
            case 520: // Misc malformed server response error codes (520 - General malformed/null response)
            case 502: // 502 - Bad gateway
                Output.webhookPrint(this,"Cloudflare HTTP Status Code " + HTTPCode + " - The API this program utilizes gave a bad response"
                        + "\n\tThere is nothing that can be done to fix this but wait. Skipping attempt...", Output.RED);

                Thread.sleep(SLEEPTIME); // Sleep
                return 1;
            default: // General error handling
                Output.webhookPrint(this,"Failed to retrieve image data from meme-api.com with error code " + HTTPCode + ". Quitting..."
                        + "\n\tError message: " + response, Output.RED);

                return 2;
        }
    }

    // Get media location (based on POSTMODE & selected media format)
    boolean getMediaSource() {
        try {
            if (AUTO_POST_MODE) {
                Output.debugPrint(this, "Reading automatic cache");
                usedURLs = FileIO.readList(this); // Generate filepath "./cache/[name]/cache.txt" for given OS & read file
                if (usedURLs == null) {return false;}

            } else { // Log manual media
                String format = (VIDEO_MODE) ? "videos" : "images";
                File directory = Paths.get(".", format).toFile(); // Generate filepath "./{Format}"
                Output.debugPrint(this, "Media source set to " + directory);

                if (!directory.exists() || !directory.isDirectory()) {
                    Output.webhookPrint(this,String.format("/%s directory does not exist. Please create it or set post_mode to auto. Quitting...", format), Output.RED);
                    return false;
                }

                // Ensure there is at least 1 file in directory
                int fileCount = Objects.requireNonNull(directory.list()).length;
                if (fileCount == 0) {
                    Output.webhookPrint(this,String.format("No %s found in /%s directory. Add media or set post_mode to auto. Quitting...", format, format), Output.RED);
                    return false;
                }

                Output.debugPrint(this, "Logging media from manual directory");

                // Start logging media
                media = directory.listFiles((_, name) -> name.toLowerCase().endsWith(".mp4")); // Gets all relevant files in the directory
            }

            // Get audio
            if (AUDIO_ENABLED && VIDEO_MODE) {
                File directory = Paths.get(".", "audio").toFile(); // Generate filepath "./audio"
                Output.debugPrint(this, "Audio source set to " + directory);

                if (!directory.exists() || !directory.isDirectory()) {
                    Output.webhookPrint(this,"/audio directory does not exist. Please create it, or set 'audio_enabled' to 'false' under [" + name + "_Settings] in config.json. Quitting...", Output.RED);
                    return false;
                }

                // Ensure there is at least 1 file in directory
                int fileCount = Objects.requireNonNull(directory.list()).length;
                if (fileCount == 0) {
                    Output.webhookPrint(this,"No audio found in /audio directory. Add audio or set 'audio_enabled' to 'false' under [" + name + "_Settings] in config.json. Quitting...", Output.RED);
                    return false;
                }

                Output.debugPrint(this, "Logging audio from manual directory");
                audio = directory.listFiles((_, name) -> name.toLowerCase().endsWith(".mp3")); // Gets all relevant files in the directory
            }
        } catch (Exception e) {
            try {
                Output.webhookPrint(this,String.valueOf(e), Output.RED);
            } catch (Exception ex) {
                throw new RuntimeException(e);
            }
            return false;
        }
        return true; // Success
    }

    /* Check image validity (Ensures not gif, not blacklisted, not already used, valid aspect ratio) */
    public int checkValidity() {
        Output.debugPrint(this, "Validating image");

        // Ensure post is not duplicate
        Output.debugPrint(this, "Testing if image url is duplicate");
        for (String[] row : usedURLs) {
            String usedUrl = row[0];
            if (mediaURL.equalsIgnoreCase(usedUrl)) {
                long expireTime = Long.parseLong(row[1]);

                if (!DUPLICATES_ALLOWED && (expireTime > System.currentTimeMillis() && HOURS_BEFORE_DUPLICATES_REMOVED != 0)) { // Test if cached url is too old to be considered duplicate
                    Output.print(this, "Duplicate URL - x" + countAttempt + " attempts", Output.RED, true);

                    return 1;
                }
            }
        }

        // Download image & check aspect ratio if needed
        if (doSizeTest) {
            Output.debugPrint(this, "Attempting to download image to verify aspect ratio");
            Image image;

            try {
                image = HTTPSend.getImageData(mediaURL);
            } catch(Exception e)  {
                Output.webhookPrint(this, "Failed to download image from Reddit to check aspect ratio. Marking this URL as invalid..."
                        + "\n\tError message: " + e, Output.RED);

                // Blacklist image URL permanently, as it is likely corrupted
                FileIO.writeList(mediaURL, this, true);
                return 1;
            }

            float ratio = (float) image.getWidth(null) / image.getHeight(null);

            Output.debugPrint(this, "Image aspect ratio is " + image.getWidth(null) + ":" + image.getHeight(null));
            if (ratio < 0.82 || ratio > 1.70) {
                Output.print(this, "Image has invalid aspect ratio", Output.RED, true);
                return 1;
            }

        }

        // Test image validity
        Output.debugPrint(this, "Testing if image is gif");
        if (mediaURL.contains(".gif")) { // Ensure image is not gif
            Output.print(this, "Image is gif - x" + countAttempt + " attempts", Output.RED, true);

            return 1;
        }

        // Ensure no blacklisted string in post caption
        Output.debugPrint(this, "Testing if image caption contains blacklisted string");
        for (String word : BLACKLIST) {
            if (caption.toLowerCase().contains(word.toLowerCase())) {
                Output.print(this, "Caption contains blacklisted string - x" + countAttempt + " attempts", Output.RED, true);

                return 1;
            }
        }

        Output.debugPrint(this, "Testing if image is marked as NSFW");
        if (!NSFW_ALLOWED && nsfw) { // If post is marked as NSFW and NSFW is disallowed
            Output.print(this, "Image is marked as NSFW - x" + countAttempt + " attempts", Output.RED, true);

            return 1;
        }

        Output.debugPrint(this, "Testing if caption contains blacklisted strings to use preset caption");
        for (String word : CAPTION_BLACKLIST) { // Ensure no semi-blacklisted string in post caption. If found, discard caption but still post
            if (caption.toLowerCase().contains(word.toLowerCase())) {
                Output.print(this, "Using fallback caption (\"" + word + "\" found) - x" + countAttempt + " attempts", Output.RED, true);

                return 2;
            }
        }

        return 0;
    }

    public void halt() {
        run = false;
        this.interrupt();
    }

}
