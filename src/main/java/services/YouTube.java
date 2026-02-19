package services;

import java.awt.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.net.URI;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.*;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.*;

import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;



public class YouTube implements Runnable {
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

    public void run() {
        if (!getRefreshToken()) {return;} // If refresh token is not set, fetch it. Otherwise, run bot like normal (Quit if failed)

        if (!getMediaSource()) {return;}// Gets media location, cache files (Quit if failed)

        Output.webhookPrint("[SYS] Bot successfully started on YouTube");
        run = true;

        try {
            // Start bot
            while (run) {
                countAttempt++;

                if (countAttempt > ATTEMPTS_BEFORE_TIMEOUT && ATTEMPTS_BEFORE_TIMEOUT != 0) { // If max # of attempts have been reached
                    Output.webhookPrint("[YT] Max # of attempts reached. Skipping attempt...", Output.YELLOW, true);

                    if (!Sleep.safeSleep(sleepTime)) break; // Sleep (Easy way to fake a "skipped attempt")
                }

                if (countAttempt == 1) { // Print first attempt message
                    Output.print("[YT] Attempting new post", Output.YELLOW, true,true);
                }


                /* Get media */
                if (POSTMODE.equals("auto")) {
                    switch (getMemeAPI()) { // Get data from meme-api.com
                        case 0: // Success
                            break;
                        case 1: // Soft fail (retry)
                            continue;
                        case 2:
                            return;

                    }

                    /* Convert image to video */
                    {
                        Image image; // Holds image data

                        try {
                            // Download image from Reddit
                            URL url = new URL(mediaURL);
                            image = ImageIO.read(url);

                        } catch (IOException e) {
                            Output.webhookPrint("[YT] Failed to download image from Reddit to convert to video. Skipping attempt w/ +2 hour delay..."
                                    + "\n\tError message: " + e, Output.RED);

                            if (!Sleep.safeSleep(7200000)) break;
                            continue;
                        }

                        Output.print("[YT] Converting image to video...", Output.YELLOW, true);

                        /* Select mp4 for audio if audio enabled */
                        String audioDir = null; // Default value

                        if (AUDIO_ENABLED) {
                            randIndex = rand.nextInt(audio.length); // Select random audio file
                            audioDir = String.valueOf(audio[randIndex]);
                        }

                        if (ImageToVideo.convert(String.valueOf(Paths.get(".", "cache", "youtube", "temp")), image, audioDir)) { // Convert image to video
                            Output.print("[YT] Successfully converted image to video", Output.YELLOW, true);
                        } else {
                            Output.print("[YT] Failed to convert image to video for upload. Skipping attempt...", Output.RED);

                            if (!Sleep.safeSleep(sleepTime)) break;
                            continue;
                        }
                        fileDir = String.valueOf(Paths.get("cache","youtube","temp.mp4"));

                    }
                } else {
                    randIndex = rand.nextInt(media.length); // Select random image
                    fileDir = String.valueOf(media[randIndex]);
                }

                /* Post media to YouTube */
                {
                    if (!getAccessToken()) {return;} // Fetch temporary token from YouTube

                    if (POSTMODE.equals("manual") || !USE_REDDIT_CAPTION || tempDisableCaption) { // Set caption depending on settings
                        caption = FALLBACK_CAPTION; // Set caption if no reddit post or if post failed caption validation (avoids needing larger if statement later)

                    }

                    /* Create data to send */
                    Map<String, Object> snippet = new HashMap<>(); // Part 1 of data
                    snippet.put("title", caption);
                    snippet.put("description", DESCRIPTION);
                    snippet.put("tags", List.of("meme", "memes"));
                    snippet.put("categoryId", "24"); // Entertainment

                    Map<String, Object> status = new HashMap<>(); // Part 2
                    status.put("privacyStatus", "public");
                    status.put("selfDeclaredMadeForKids", false);

                    Map<String, Object> metadata = new HashMap<>(); // Merge parts 1 & 2
                    metadata.put("snippet", snippet);
                    metadata.put("status", status);

                    /* Convert metadata map to JSON string */
                    String metadataJson = new ObjectMapper().writeValueAsString(metadata);

                    // Publish YouTube video
                    String strResponse = postYouTubeVideo("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=multipart&part=snippet,status", Path.of(fileDir), metadataJson, accessToken);

                    JSONObject response = StringToJson.getJSON(strResponse); // Convert to json for check

                    if (HTTPSend.HTTPCode.get() != 200) { // Error handling
                        /* Get ready for YouTube's terrible nested JSON (I'm sure there's an easier way) */
                        String reason = "";
                        if (response.has("error")) {
                            reason = response.getJSONObject("error").getJSONArray("errors").getJSONObject(0).getString("reason");
                        }

                        /* Error handling */
                        if (reason.equals("uploadLimitExceeded") || reason.equals("rateLimitExceeded") || reason.equals("quotaExceeded")) {
                             Output.webhookPrint("[YT] Failed to post " + fileDir.substring(fileDir.lastIndexOf("/") + 1) + ". Skipping this attempt..."
                                        + "\n\tYou are being rate limited. You can only post a few times per day to the YouTube API", Output.RED);

                            if (!Sleep.safeSleep(sleepTime)) break; // Sleep

                            continue;

                        } else { // General error handling
                            Output.webhookPrint("[YT] Failed to post " + fileDir.substring(fileDir.lastIndexOf("/") + 1) + ". Trying again, and marking this URL as invalid..."
                                    + "\n\tError message: " + response, Output.RED);

                            // Blacklist image URL permanently, as it is likely corrupted
                            FileIO.writeList(mediaURL, "youtube", false);
                        }

                    } else { // Post success handling

                        if (POSTMODE.equals("auto")) {
                            Output.webhookPrint("[YT] " + redditURL + " from r/" + chosenSubreddit + " uploaded - x" + countAttempt + " attempt(s)", Output.GREEN);
                        } else {
                            Output.webhookPrint("[YT] " + redditURL + " uploaded to YouTube - x" + countAttempt + " attempt(s)", Output.GREEN);
                        }

                        countAttempt = 0;

                        // Store image URL to prevent duplicates
                        FileIO.writeList(mediaURL, "youtube", false);

                        long timestamp = System.currentTimeMillis();
                        usedURLs.add(new String[]{mediaURL, String.valueOf(timestamp)});

                        if (!Sleep.safeSleep(sleepTime)) break; // Sleep
                    }
                }

                Thread.sleep(1500); // Sleep 1.5 sec to prevent spam
            }
        } catch (InterruptedException e) { // When interrupted
            Thread.currentThread().interrupt(); // restore interrupt flag
            Output.webhookPrint("[YT] Unexpected error during sleep: " + e.getMessage(), Output.RED);

        } catch (Exception e) { // General error handling
            try {
                Output.webhookPrint("[YT] Bot crashed with unexpected error: " + e.getMessage(), Output.RED);
            } catch (Exception inner) {
                inner.printStackTrace();
            }
        } finally { // Crash/Stop handling
            Output.webhookPrint("[SYS] YouTube stopped");

            Status.youtubeRunning = false;
        }
    }
    private boolean getAccessToken() {
        // Build upload data
        Map<String, String> formData = new HashMap<>();

        formData.put("client_id", ID);
        formData.put("client_secret", SECRET);
        formData.put("refresh_token", REFRESHTOKEN);
        formData.put("grant_type", "refresh_token");

        String response;

        try {
            response = HTTPSend.postForm("https://oauth2.googleapis.com/token", formData);
        } catch (Exception e) {
            Output.webhookPrint("[YT] Failed to fetch token. Quitting..." +
                    "\n\tError: " + e, Output.RED);

            return false;
        }


        if (HTTPSend.HTTPCode.get() == 200 && response.contains("access_token")) {
            accessToken = StringToJson.getData(response, "access_token");

            return true;  // Success
        } else {
            Output.webhookPrint("[YT] Failed to fetch token. Quitting..." +
                    "\n\tError message: " + response, Output.RED);

            return false;
        }
    }

    public int getMemeAPI() throws Exception {
        String response;

        randIndex = rand.nextInt(SUBREDDITS.size()); // Generate random subreddit index

        chosenSubreddit = SUBREDDITS.get(randIndex);

        try {
            response = HTTPSend.get("https://meme-api.com/gimme/" + chosenSubreddit);
        } catch (Exception e) {
            Output.webhookPrint("[YT] Failed to fetch image from meme-api.com"
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

                Output.print("[YT] Reddit post data successfully retrieved", Output.YELLOW, true);

                /* Check image validity (Ensures not gif, not blacklisted, not already used, valid aspect ratio) */
                switch (ImageValidity.check(response, countAttempt, usedURLs, false, "youtube")) {
                    case 0: // Image valid
                        return 0;
                    case 1: // General failed validation
                        return 1;
                    case 2: // Caption is blacklisted, but allowed to post (CAPTION BLACKLIST)
                        tempDisableCaption = true;
                        return 0;
                }

            case 503: // Cloudflare error
                Output.webhookPrint("[YT] Failed. Cloudflare HTTP Status Code 503 - The API this program utilizes appears to be under maintenance."
                        + "\n\tThere is nothing that can be done to fix this but wait. Skipping attempt w/ +6 hour delay...", Output.RED);

                if (!Sleep.safeSleep(sleepTime + 21600000)) break; // Sleep normal time + 6 hours
                return 1;

            default: // General error handling
                Output.webhookPrint("[YT] Failed to retrieve image data from meme-api.com with error code " + HTTPSend.HTTPCode.get() + ". Quitting..."
                        + "\n\tError message: " + response, Output.RED);

                return 2;
        }

        Output.webhookPrint("[YT] How did the bot get here? This shouldn't be possible. Quitting..."
                + "\n\tError message: " + response, Output.RED);
        return 2;
    }

    // Get media location (based on POSTMODE & selected media format)
    private boolean getMediaSource() {
        try {
            if (POSTMODE.equals("auto")) {
                usedURLs = FileIO.readList("youtube"); // Generate filepath "./cache/[youtube]/cache.txt" for given OS & read file


            } else { // Log manual media
                File directory = Paths.get(".","videos").toFile(); // Generate filepath "./videos"

                if (!directory.exists() || !directory.isDirectory()) {
                    Output.webhookPrint("[YT] /videos directory does not exist. Please create it or set post_mode to auto. Quitting...", Output.RED);
                    return false;
                }

                // Ensure there is at least 1 file in directory
                int fileCount = Objects.requireNonNull(directory.list()).length;
                if (fileCount == 0) {
                    Output.webhookPrint(String.format("[YT] No videos found in /videos directory. Add media or set post_mode to auto. Quitting..."), Output.RED);
                    return false;
                }

                // Start logging media
                media = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp4")); // Gets all relevant files in the directory
            }

            // Get audio
            if (AUDIO_ENABLED) {
                File directory = Paths.get(".", "audio").toFile(); // Generate filepath "./audio"

                if (!directory.exists() || !directory.isDirectory()) {
                    Output.webhookPrint("[YT] /audio directory does not exist. Please create it, or set 'audio_enabled' to 'false' under [Youtube_Settings] in config.json. Quitting...", Output.RED);
                    return false;
                }

                // Ensure there is at least 1 file in directory
                int fileCount = Objects.requireNonNull(directory.list()).length;
                if (fileCount == 0) {
                    Output.webhookPrint("[YT] No audio found in /audio directory. Add audio or set 'audio_enabled' to 'false' under [Youtube_Settings] in config.json. Quitting...", Output.RED);
                    return false;
                }

                audio = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3")); // Gets all relevant files in the directory
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

    /* If refresh token is not set, fetch it. Otherwise, skip */
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

            if (HTTPSend.HTTPCode.get() == 200 && response.contains("refresh_token")) {
                REFRESHTOKEN = StringToJson.getData(response, "refresh_token");

                Output.webhookPrint("PLEASE INPUT THE FOLLOWING INTO 'refresh_token' UNDER [YouTube_Settings] IN config.json:" +
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

    /* I put this in a separate class because it's not my code (YouTube uploading is annoyingly specific) */
    public static String postYouTubeVideo(String url, Path videoPath, String metadataJson, String oauthToken) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String boundary = UUID.randomUUID().toString();
        String CRLF = "\r\n";

        // Read video bytes
        byte[] videoBytes = Files.readAllBytes(videoPath);
        String fileName = videoPath.getFileName().toString();

        // Determine video content type from extension
        int dotPos = fileName.lastIndexOf('.');
        if (dotPos == -1 || dotPos == fileName.length() - 1) {
            throw new IOException("No file extension found for: " + fileName);
        }
        String ext = fileName.substring(dotPos + 1);
        String videoContentType = "video/" + ext;

        // Build multipart/related body manually
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(baos, "UTF-8"));

        // JSON metadata part
        writer.write("--" + boundary + CRLF);
        writer.write("Content-Type: application/json; charset=UTF-8" + CRLF + CRLF);
        writer.write(metadataJson + CRLF);

        // Video part
        writer.write("--" + boundary + CRLF);
        writer.write("Content-Type: " + videoContentType + CRLF + CRLF);
        writer.flush(); // headers written before video bytes

        baos.write(videoBytes);
        baos.write(CRLF.getBytes());

        // End boundary
        writer.write("--" + boundary + "--" + CRLF);
        writer.flush();

        // Build request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Authorization", "Bearer " + oauthToken)
                .header("Content-Type", "multipart/related; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        HTTPSend.HTTPCode.set((long) response.statusCode());

        return response.body();
    }

    public static void stop() { // Stop bot
        run = false;
        Output.webhookPrint("YouTube successfully stopped");
    }

    public static void clear() { // Clear cache
        FileIO.clearList("youtube");
        Output.webhookPrint("YouTube cache successfully cleared");
    }

}