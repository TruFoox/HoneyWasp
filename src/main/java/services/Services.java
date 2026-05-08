package services;

import config.Config;
import config.InstagramSettings;
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

public abstract class Services extends Thread {
    private final String shortName, name;
    public String getShortname() {return shortName;}
    protected Config config;
    protected PlatformSettings settings;

    public Services(String name, String shortName, Config config) {
        this.name = name;
        this.shortName = shortName;
        this.config = config;
    }
    
    abstract boolean upload();
    abstract boolean publish();
    abstract boolean
    getUserToken();
    
    Random rand = new Random(); // Generate seed for random number generation

    // Empty global variables
    protected java.util.List<String[]> usedURLs = new ArrayList<>();
    protected String chosenSubreddit, mediaURL, redditURL, caption, fileDir;
    protected boolean run = true;
    protected boolean nsfw, tempDisableCaption;
    protected int randIndex, USERID, countAttempt = 0;
    protected File[] media, audio;

    // Load config - make these grab whatever service is fed
    protected String TOKEN, FALLBACK_CAPTION, DESCRIPTION;
    protected List<String> SUBREDDITS;
    protected boolean AUTO_POST_MODE, VIDEO_MODE, AUDIO_ENABLED, USE_REDDIT_CAPTION;
    protected int sleepTime, ATTEMPTS_BEFORE_TIMEOUT, TIME_BETWEEN_POSTS;

    public void run(String service) {
        try {
            settings = Config.getInstance().Platform(service);

            AUTO_POST_MODE = settings.isAuto_post_mode();
            TIME_BETWEEN_POSTS = settings.getTime_between_posts();
            ATTEMPTS_BEFORE_TIMEOUT = settings.getAttempts_before_timeout();
            SUBREDDITS = settings.getSubreddits();
            AUDIO_ENABLED = settings.isAudio_enabled();
            USE_REDDIT_CAPTION = settings.isUse_reddit_caption();
            FALLBACK_CAPTION = settings.getCaption();

            // VIDEO_MODE and DESCRIPTION are platform-specific & thus set seperately

            // Start bot
            while (run) {
                countAttempt++; // Iterate count for number of attempts to post that have been made
                Output.debugPrint("Attempt " + countAttempt + " started");

                if (countAttempt == 1) { // Print first attempt message
                    Output.print("Attempting new post", Output.YELLOW, true,true);
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
                    Output.debugPrint("Successfully fetched URL " + mediaURL);

                    /* If format is video, convert image to video */
                    if (VIDEO_MODE) {
                        Image image; // Holds image data

                        Output.debugPrint("Attempting to retrieve image data");
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
                            Output.debugPrint("Attempting to select audio file for use");
                            randIndex = rand.nextInt(audio.length); // Select random audio file
                            audioDir = String.valueOf(audio[randIndex]);
                        }

                        Output.print("Converting image to video...", Output.YELLOW, true);

                        if (ImageToVideo.convert(String.valueOf(Paths.get(".", "cache", name.toLowerCase(), "temp")), image, audioDir)) { // Convert image to video
                            Output.print("Successfully converted image to video", Output.YELLOW, true);
                        } else {
                            Output.print("Failed to convert image to video for upload. Skipping attempt...", Output.RED);

                            if (!Sleep.safeSleep(sleepTime)) break;
                            continue;
                        }
                        fileDir = "./cache/" + name.toLowerCase() + "/temp.mp4";
                    }
                } else {
                    randIndex = rand.nextInt(media.length); // Select random image
                    fileDir = String.valueOf(media[randIndex]);
                }

                /* Upload manual media/generated video to temp file hoster */
                if (!AUTO_POST_MODE || VIDEO_MODE) {
                    // Upload media to 0x0
                    Output.print("Uploading media to temp file hoster...", Output.YELLOW, true);

                    String response = HTTPSend.postFile("https://0x0.st", Path.of(fileDir)); // Send file to 0x0

                    // Error handling
                    if (HTTPSend.HTTPCode.get() == 403) {
                        Output.webhookPrinttemptest(this,"0x0.su (temp storage provider) returned HTTP 403 - Oh no! You've likely been flagged as a bot by the temp storage site!" +
                                "\n\tYour IP should be cycled and unblocked in a few months." +
                                "\n\n\tIn the meantime, you should set 'video_mode' to 'false' & 'post_mode' to 'auto' under [" + name + " Settings]" +
                                "\n\tin config.json to bypass the need for temporary storage. Quitting..." +
                                "\n\n\tError message: " + response, Output.RED);

                        return;
                    } else if (!(HTTPSend.HTTPCode.get() == 200)) { // Misc error handling
                        Output.webhookPrinttemptest(this,"Error uploading file to 0x0.su (temp storage provider). Quitting..." +
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
                

                Thread.sleep(1500); // Sleep 1.5 sec to prevent spam
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
            Output.webhookPrinttemptest(this,"[SYS] " + name + " stopped");

            // Set status running false here
        }
    }

    private int getMemeAPI() throws Exception {
        String response;

        randIndex = rand.nextInt(SUBREDDITS.size()); // Generate random subreddit index

        chosenSubreddit = SUBREDDITS.get(randIndex);

        String URL = "https://meme-api.com/gimme/" + chosenSubreddit;

        Output.debugPrint("[this.] Fetching media URL from " + URL);
        try {
            response = HTTPSend.get(URL);
        } catch (ConnectException e) {
            Output.print("Connection drop detected. Trying again in 10 seconds...");

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

                Output.debugPrint("Reddit post data successfully retrieved");

                /* Check image validity (Ensures not gif, not blacklisted, not already used, valid aspect ratio) */
                switch (ImageValidity.check(response, countAttempt, usedURLs, true, name.toLowerCase())) {
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
                Output.debugPrint("Reading automatic cache");
                usedURLs = FileIO.readList(name.toLowerCase()); // Generate filepath "./cache/[name]/cache.txt" for given OS & read file
                if (usedURLs == null) {return false;}

            } else { // Log manual media
                String format = (VIDEO_MODE) ? "videos" : "images";
                File directory = Paths.get(".", format).toFile(); // Generate filepath "./{Format}"
                Output.debugPrint("Media source set to " + directory);

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

                Output.debugPrint("Logging media from manual directory");

                // Start logging media
                media = directory.listFiles((_, name) -> name.toLowerCase().endsWith(".mp4")); // Gets all relevant files in the directory
            }

            // Get audio
            if (AUDIO_ENABLED && VIDEO_MODE) {
                File directory = Paths.get(".", "audio").toFile(); // Generate filepath "./audio"
                Output.debugPrint("Audio source set to " + directory);

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

                Output.debugPrint("Logging audio from manual directory");
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

}

class Insta extends Services {
    Config config;

    public Insta() {
        super("Instagram","INSTA",Config.getInstance());
        this.settings = config.Platform("instagram"); // Establish settings\

        InstagramSettings ig = Config.getInstance().Instagram();
        TOKEN = ig.getApi_key();
        DESCRIPTION = ig.getCaption();

    }
    boolean upload() {

        return false;
    }
    boolean publish() {

        return false;
    }
    public boolean getUserToken() {

        return false;
    }
}