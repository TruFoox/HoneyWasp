package services;

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


public class Instagram implements Runnable {
    ReadConfig config = ReadConfig.getInstance(); // Get config
    Random rand = new Random(); // Generate seed for random number generation

    // Empty global variables
    long USERID, countAttempt = 0;
    List<String[]> usedURLs = new ArrayList<>();
    String chosenSubreddit, mediaURL, redditURL, caption, fileDir;
    boolean run = true, nsfw, tempDisableCaption;
    int randIndex;
    File[] media;

    // Load config
    final String TOKEN = config.getInstagram().getApi_key().trim();
    final String POSTMODE = config.getInstagram().getPost_mode().trim().toLowerCase();
    final int TIME_BETWEEN_POSTS = config.getInstagram().getTime_between_posts();
    final int sleepTime = TIME_BETWEEN_POSTS * 60000; // Generate time to sleep between posts in milliseconds
    final int ATTEMPTS_BEFORE_TIMEOUT = config.getInstagram().getAttempts_before_timeout();
    final List<String> SUBREDDITS = config.getInstagram().getSubreddits();
    final String FORMAT = config.getInstagram().getFormat().trim().toLowerCase();
    final boolean USE_REDDIT_CAPTION = config.getInstagram().isUse_reddit_caption();
    final String FALLBACK_CAPTION = config.getInstagram().getCaption();
    final String HASHTAGS = config.getInstagram().getHashtags();


    public void run() {
        if (!authenticate()) {return;} // Get instagram User ID (Quit if failed)

        if (!getMediaSource()) {return;} // Gets media location, cache files (Quit if failed)

        try {
            // Start bot
            while (run) {
                countAttempt++; // Iterate count for number of attempts to post that have been made

                if (countAttempt == 1) { // Print first attempt message
                    Output.webhookPrint("[INSTA] Attempting new post", Output.YELLOW, true);
                }

                if (countAttempt > ATTEMPTS_BEFORE_TIMEOUT) { // If max # of attempts have been reached
                    Output.webhookPrint("[INSTA] Max # of attempts reached. Skipping attempt...", Output.YELLOW, true);

                    if (!safeSleep(sleepTime)) break; // Sleep (Easy way to fake a "skipped attempt")
                }

                /* Fetch media */
                if (POSTMODE.equals("auto")) {
                    String response;

                    randIndex = rand.nextInt(SUBREDDITS.size()); // Generate random subreddit index

                    chosenSubreddit = SUBREDDITS.get(randIndex);

                    try {
                        response = HTTPSend.get("https://meme-api.com/gimme/" + chosenSubreddit);
                    } catch (Exception e) {
                        Output.webhookPrint("[INSTA] Failed to fetch image from meme-api.com"
                                + "\n\tError message: " + e, Output.RED);
                        return;
                    }

                    /* Status code handling */
                    switch ((int) HTTPSend.HTTPCode) {
                        case 200: // Success
                            // Parse JSON data
                            mediaURL = StringToJson.getData(response, "url");
                            redditURL = StringToJson.getData(response, "postLink");
                            caption = StringToJson.getData(response, "title");
                            nsfw = Boolean.parseBoolean(StringToJson.getData(response, "nsfw"));
                            tempDisableCaption = false;

                            Output.print("[INSTA] Reddit post data successfully retrieved", Output.YELLOW, true);

                            /* Check image validity (Ensures not gif, not blacklisted, not already used, valid aspect ratio) */
                            switch (ImageValidity.check(response, tempDisableCaption, countAttempt, usedURLs)) {
                                case 0: // Image valid
                                    break;
                                case 1: // General failed validation
                                    continue;
                                case 2: // Caption is blacklisted, but allowed to post (CAPTION BLACKLIST)
                                    tempDisableCaption = true;
                            }

                            break;

                        case 530: // Cloudflare error
                            Output.webhookPrint("[INSTA] Failed. Cloudflare HTTP Status Code 530 - The API this program utilizes appears to be under maintenance."
                                    + "\n\tThere is nothing that can be done to fix this but wait. Skipping attempt w/ +6 hour delay...", Output.RED);

                            if (!safeSleep(sleepTime + 21600000)) break; // Sleep normal time + 6 hours
                            continue; // Return to beginning of loop

                        default: // General error handling
                            Output.webhookPrint("[INSTA] Failed to retrieve image data from meme-api.com with error code " + HTTPSend.HTTPCode + ". Quitting..."
                                    + "\n\tError message: " + response, Output.RED);

                            return;
                    }

                    /* If format is video, convert image to video */
                    if (FORMAT.equals("video")) {
                        Image image; // Holds image data (not usually needed unless converting to video as instagram takes image url as input)

                        try {
                            // Download image from Reddit
                            URL url = new URL(mediaURL);
                            image = ImageIO.read(url);

                        } catch (IOException e) {
                            Output.webhookPrint("[INSTA] Failed to download image from Reddit to convert to video. Skipping attempt w/ +2 hour delay..."
                                    + "\n\tError message: " + e, Output.RED);

                            if (!safeSleep(7200000)) break;
                            continue;
                        }

                        Output.print("[INSTA] Converting image to video...", Output.YELLOW, true);

                        if (ImageToVideo.convert(String.valueOf(Paths.get(".", "cache", "instagram", "temp")), image)) { // Convert image to video
                            Output.print("[INSTA] Successfully converted image to video", Output.YELLOW, true);
                        } else {
                            Output.print("[INSTA] Failed to convert image to video for upload. Skipping attempt...", Output.RED);

                            if (!safeSleep(sleepTime)) break;
                            continue;
                        }
                        fileDir = "./cache/instagram/temp.mp4";
                    }
                } else {
                    randIndex = rand.nextInt(media.length); // Select random image
                    fileDir = String.valueOf(media[randIndex]);

                    Output.webhookPrint(fileDir);
                }

                /* Upload manual media/generated video to temp file hoster */
                if (POSTMODE.equals("manual") || FORMAT.equals("video")) {
                    // Upload media to 0x0
                    Output.print("[INSTA] Uploading media to temp file hoster...", Output.YELLOW, true);

                    String response = HTTPSend.postFile("https://0x0.st", Path.of(fileDir)); // Send file to 0x0

                    // Error handling
                    if (HTTPSend.HTTPCode == 403) {
                        Output.webhookPrint("[INSTA] 0x0.su (temp storage provider) returned HTTP 403 - Oh no! I've likely been flagged as a bot, and now your ip cant access the temp storage site!" +
                                "\n\tYour IP should be cycled and unblocked in a few months." +
                                "\n\tIn the meantime, you should change 'format' to 'image' & 'post_mode' to 'auto' under [Instagram Settings] in bot.json to bypass the need for temporary storage. Quitting..." +
                                "\n\tError message: " + response, Output.RED);

                        return;
                    } else if (!(HTTPSend.HTTPCode == 200)) { // Misc error handling
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


                /* Upload & publish media */
                {
                    String jsonData, uploadURL, response; // Store json data & URL to be used with POST

                    if (POSTMODE.equals("manual") || !USE_REDDIT_CAPTION || tempDisableCaption) { // Set caption depending on settings
                        caption = FALLBACK_CAPTION; // Set caption if no reddit post or if post failed caption validation (avoids needing larger if statement later)
                    }

                    caption += "\n\n.\n\n" + HASHTAGS; // Add hashtags to caption

                    // Build upload data
                    Map<String, String> formData = new HashMap<>();

                    if (FORMAT.equals("image")) {
                        formData.put("image_url", mediaURL);
                        formData.put("caption", caption);
                        formData.put("access_token", TOKEN);
                        uploadURL = "https://graph.facebook.com/v23.0/" + USERID + "/media";

                    } else {
                        formData.put("video_url", mediaURL);
                        formData.put("caption", caption);
                        formData.put("media_type", "REELS");
                        formData.put("access_token", TOKEN);
                        uploadURL = "https://graph.facebook.com/v23.0/" + USERID + "/media?media_type=VIDEO";
                    }

                    response = HTTPSend.postForm(uploadURL, formData); // Send JSON data for upload (Step 1/2 - next is publish)

                    if (HTTPSend.HTTPCode != 200) {
                        Output.webhookPrint("[INSTA] Upload step failed! Skipping attempt... HTTP code:" + HTTPSend.HTTPCode +
                                "\n\tError message: " + response, Output.RED);

                        if (!safeSleep(sleepTime)) break;
                        continue;
                    } else {
                        Output.print("[INSTA] Upload step success (1/2)", Output.YELLOW, true);
                    }

                    String postID = StringToJson.getData(response, "id"); // Get post ID from previous HTTP step

                    Thread.sleep(500); // Sleep for 0.5s - gives Instagram time to get ready


                    /* Instagram needs time to render videos - this loop has the bot wait until it is finished */
                    String postStatus = "";
                    if (FORMAT.equals("video")) {
                        do {
                            Output.print("[INSTA] Waiting for Instagram to process media. This may take a while...", Output.YELLOW, true);

                            HTTPSend.get("https://graph.facebook.com/v23.0/" + postID +
                                    "?fields=status_code,status&access_token=" + TOKEN); // Send status check request

                            if (HTTPSend.HTTPCode != 200) { // Error handling
                                Output.print("Failed to get post upload status, waiting 30 seconds before attempting upload...", Output.YELLOW, true);

                                Thread.sleep(30000); // Wait 30s and break
                                break;
                            }

                            Output.webhookPrint(response);

                            postStatus = StringToJson.getData(response, "status_code");

                            if (postStatus.equals("ERROR")) {
                                Output.webhookPrint("Video processing failed. Video is likely corrupted. Attempting to post again..." +
                                        "\n\tError Message: " + response, Output.RED);
                                break;
                            }

                        } while (!postStatus.equals("FINISHED"));
                    }

                    if (postStatus.equals("ERROR")) { // If there was an error, retry attempt
                        continue;
                    }


                    /* Publish post */
                    formData.clear(); // Clear formData hashmap for publish

                    formData.put("creation_id", postID);
                    formData.put("access_token", TOKEN);
                    uploadURL = "https://graph.facebook.com/v23.0/" + USERID + "/media_publish";

                    HTTPSend.postForm(uploadURL, formData); // Send post for publish to Instagram

                    if (HTTPSend.HTTPCode != 200) {
                        Output.webhookPrint("[INSTA] Publish step failed! Trying again, and marking this URL as invalid... HTTP code:" + HTTPSend.HTTPCode +
                                "\n\tError message: " + response, Output.RED);

                        continue;
                    } else {
                        if (POSTMODE.equals("auto")) {
                            Output.webhookPrint("[INSTA] " + redditURL + " from r/" + chosenSubreddit + " uploaded - x" + countAttempt + " attempt(s)", Output.GREEN);
                        } else {
                            Output.webhookPrint("[INSTA] " + redditURL + " uploaded to Instagram - x" + countAttempt + " attempt(s)", Output.GREEN);
                        }

                        // Store image URL to prevent duplicates
                        fileIO.writeList(mediaURL, "instagram");

                        long timestamp = System.currentTimeMillis();
                        usedURLs.add(new String[]{mediaURL, String.valueOf(timestamp)});

                        if (!safeSleep(sleepTime)) break; // Sleep
                    }
                }

                Thread.sleep(1500); // Sleep 1.5 sec to prevent spam
            }
        } catch (InterruptedException e) { // When interrupted
            Thread.currentThread().interrupt(); // restore interrupt flag
            Output.webhookPrint("Sleep interrupted: " + e.getMessage(), Output.RED);

            return;
        } catch (Exception e) { // General error handling
            try {
                Output.webhookPrint("Unexpected error during sleep: " + e.getMessage(), Output.RED);
            } catch (Exception inner) {
                inner.printStackTrace();
            }

            return;
        }
    }


    // Get Facebook ID & use it to retrieve Instagram ID
    private boolean authenticate() {
        try {
            try {
                String response = HTTPSend.get("https://graph.facebook.com/v19.0/me/accounts?access_token=" + TOKEN);

                String facebookID;
                JSONArray data = StringToJson.getJSON(response).getJSONArray("data"); // Convert to JSON array format
                JSONObject dataObj = data.getJSONObject(0);

                facebookID = dataObj.getString("id"); // Temporarily store facebook ID

                // Get Instagram ID
                response = HTTPSend.get("https://graph.facebook.com/v19.0/" + facebookID + "?fields=instagram_business_account&access_token=" + TOKEN);

                if (!response.contains("instagram_business_account")) { // Ensure account is business account
                    Output.webhookPrint("Token valid, but no linked Instagram Business Account found. Please set your instagram account type to business. Quitting...", Output.RED);

                    return false;
                }
                dataObj = StringToJson.getJSON(response);

                dataObj = dataObj.getJSONObject("instagram_business_account"); // Get JSON["instagram_business_account"]["id"]
                USERID = dataObj.getLong("id");

            } catch (Exception e) {
                Output.webhookPrint("[INSTA] Failed to retrieve Instagram User ID. Your Access token may be invalid. Quitting..."
                        + "\n\tError message: " + e, Output.RED);
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

        return true; // Success
    }


    // Get media location (based on POSTMODE & selected media format)
    private boolean getMediaSource() {
        try {
            if (POSTMODE.equals("auto")) {
                usedURLs = fileIO.readList("instagram"); // Generate filepath "./cache/[Instagram]/cache.txt" for given OS & read file

                if (usedURLs == null) { // If failed, quit

                    return false;
                }

            } else { // Log manual media
                File directory = Paths.get(".", FORMAT + "s").toFile(); // Generate filepath "./{Format}s"

                // Ensure there is at least 1 file in directory
                int fileCount = Objects.requireNonNull(directory.list()).length;
                if (fileCount == 0) {
                    Output.webhookPrint(String.format("[INSTA] No %ss found in /%ss directory. Add media or set post_mode to auto. Quitting...", FORMAT, FORMAT), Output.RED);
                    return false;
                }

                // Start logging media
                media = directory.listFiles(); // Gets all files in the directory
            }
            Output.webhookPrint("Bot successfully started on Instagram");
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

    // Prevents the bot from sleeping if there is a real interruption (like attempted exit)
    private boolean safeSleep(long ms) throws Exception {
        try {
            Thread.sleep(ms);

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupt flag

            return false;
        }
    }
}