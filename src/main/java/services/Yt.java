package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.Config;
import config.YoutubeSettings;
import org.json.JSONObject;
import utils.HTTPSend;
import utils.Output;
import utils.StringToJson;

import java.awt.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static services.YouTube.postYouTubeVideo;

public class Yt  extends Services implements HasRefreshToken{
    private final String CLIENT_ID, SECRET;

    public Yt() {
        super("YouTube","YT",Config.getInstance());
        settings = config.Platform("youtube"); // Establish settings

        YoutubeSettings yt = Config.getInstance().Youtube(); // Set service-specific stuff
        SECRET = yt.getClient_secret();
        CLIENT_ID = yt.getClient_id();
        REFRESH_TOKEN = yt.getRefresh_token();
        VIDEO_MODE = true; // YouTube only supports videos

    }

    @Override
    public boolean fetchRefreshToken() {

        // Generate OAuth URL & prompt user to go there to get token
        String oauthURL = "https://accounts.google.com/o/oauth2/auth?client_id=" + CLIENT_ID + "&redirect_uri=http://localhost&response_type=code&scope=https://www.googleapis.com/auth/youtube.upload&access_type=offline&prompt=consent";

        Output.webhookPrint("BEFORE YOU CAN POST TO YOUTUBE, YOU MUST RETRIEVE YOUR ACCESS TOKEN." +
                "\n\tATTEMPTING TO REDIRECT YOU TO THE AUTHENTICATION SITE NOW (OR GO TO " + oauthURL + ")", Output.RED);

        Output.debugPrint("[YT] Attempting redirect");
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) { // Test if browser allows going to URL from here
            try {
                Desktop.getDesktop().browse(new URI(oauthURL));
            } catch (Exception e) {
                // Ignore
            }
        }

        Output.webhookPrint("[THIS STEP MUST BE DONE IN-CONSOLE] PLEASE PASTE THE ENTIRE URL YOU WERE JUST REDIRECTED TO (SEE https://github.com/TruFoox/HoneyWasp/#youtube-setup FOR HELP):", Output.YELLOW);
        String redirectUrl = scanner.nextLine(); // Read user input

        Output.debugPrint("[YT] Extracting code from user input");
        String authCode = redirectUrl.split("code=")[1].split("&")[0]; // split on "code=" and stop at next "&"
        Output.debugPrint("[YT] Code: " + authCode);

        // Build upload data
        Map<String, String> formData = new HashMap<>();

        formData.put("client_id", CLIENT_ID);
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
            REFRESH_TOKEN = StringToJson.getData(response, "refresh_token");

            config.Youtube().setRefresh_token(REFRESH_TOKEN);
            config.saveConfig(); // Write to file

            return true;  // Success
        } else {
            Output.webhookPrint("[YT] Failed to fetch token. Quitting..." +
                    "\n\tError message: " + response, Output.RED);

            return false;
        }
    }

    @Override
    boolean upload() throws Exception {
        if (!AUTO_POST_MODE || !USE_REDDIT_CAPTION || tempDisableCaption) { // Set caption depending on settings
            caption = FALLBACK_CAPTION; // Set caption if no reddit post or if post failed caption validation (avoids needing larger if statement later)

        }

        /* Create data to send */
        Map<String, Object> snippet = new HashMap<>(); // Part 1 of data
        snippet.put("title", caption);
        snippet.put("description", HASHTAGS);
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
        String strResponse = postYouTubeVideo("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=multipart&part=snippet,status", Path.of(fileDir), metadataJson, TOKEN);

        JSONObject response = StringToJson.getJSON(strResponse); // Convert to json for check

        return false;
    }

    @Override
    boolean publish() throws Exception {
        return true; // YouTube is one-step
    }

    @Override
    boolean fetchUserToken() {
        // Build upload data
        Map<String, String> formData = new HashMap<>();

        formData.put("client_id", CLIENT_ID);
        formData.put("client_secret", SECRET);
        formData.put("refresh_token", REFRESH_TOKEN);
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
            TOKEN = StringToJson.getData(response, "access_token");

            return true;  // Success
        } else {
            Output.webhookPrint("[YT] Failed to fetch token. Quitting..." +
                    "\n\tError message: " + response, Output.RED);

            return false;
        }
    }
}
