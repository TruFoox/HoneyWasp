package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import main.HoneyWasp;
import org.json.JSONObject;
import utils.*;

import java.awt.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YouTube extends Services implements HasRefreshToken {
    private final String CLIENT_ID, SECRET;

    public YouTube() {
        super("YouTube","YT");

        SECRET = HoneyWasp.config.Youtube().getClient_secret().trim();
        CLIENT_ID = HoneyWasp.config.Youtube().getClient_id().trim();
        REFRESH_TOKEN = HoneyWasp.config.Youtube().getRefresh_token().trim();
        VIDEO_MODE = true; // YouTube only supports videos
        doSizeTest = false; // Youtube generally doesnt care about media dimensions

    }

    @Override
    public boolean fetchRefreshToken() {
        String redirectURI = "http://localhost:8000/callback"; // URL to redirect to after authentication

        // Generate OAuth URL & prompt user to go there to get token
        String url = "https://accounts.google.com/o/oauth2/auth?client_id=" + CLIENT_ID +
                "&redirect_uri=" + redirectURI +
                "&response_type=code" +
                "&scope=https://www.googleapis.com/auth/youtube.upload" +
                "&access_type=offline" +
                "&prompt=consent";

        Output.webhookPrint(this, "BEFORE YOU CAN POST TO YOUTUBE, YOU MUST RETRIEVE YOUR ACCESS TOKEN." +
                "\n\tATTEMPTING TO REDIRECT YOU TO THE AUTHENTICATION SITE NOW (OR GO TO " + url + ")", Output.RED);

        HoneyWasp.Redirect(this, url);

        String authCode = "";
        try {
            response = HTTPSend.awaitResponse(); // Listen for response

            Output.debugPrint(this, "Extracting code from HTTP response");
        } catch (Exception E) { // Fallback
            Output.webhookPrint(this, "Defaulting to fallback authentication retrieval method." +
                    "\nReason: " + E, Output.RED);
            Output.webhookPrint(this, "PLEASE PASTE THE ENTIRE URL YOU WERE JUST REDIRECTED TO. IT SHOULD CONTAIN \"code=\": ", Output.YELLOW);
            response = scanner.nextLine(); // Read user input

            Output.debugPrint(this, "Extracting code from user input");
        }
        authCode = response.split("code=")[1].split("&")[0]; // split on "code=" and stop at next "&"
        Output.debugPrint(this, "");

        // Build upload data
        Map<String, String> formData = new HashMap<>();

        formData.put("client_id", CLIENT_ID);
        formData.put("client_secret", SECRET);
        formData.put("code", authCode);
        formData.put("grant_type", "authorization_code");
        formData.put("redirect_uri", redirectURI);

        String response;

        try {
            response = HTTPSend.postForm(this,"https://oauth2.googleapis.com/token", formData);
        } catch (Exception e) {
            Output.webhookPrint(this, "Failed to fetch refresh token. Quitting..." +
                    "\n\tError: " + e, Output.RED);

            return false;
        }

        if (HTTPSend.HTTPCode.get() == 200 && response.contains("refresh_token")) {
            REFRESH_TOKEN = StringToJson.getData(response, "refresh_token");

            HoneyWasp.config.Youtube().setRefresh_token(REFRESH_TOKEN);
            HoneyWasp.config.saveConfig(); // Write to file

            return true;  // Success
        } else {
            Output.webhookPrint(this, "Failed to fetch token. Quitting..." +
                    "\n\tError message: " + response, Output.RED);

            return false;
        }
    }

    @Override
    protected boolean upload() throws Exception {

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
        String strResponse = HTTPSend.postYouTubeVideo(this,"https://www.googleapis.com/upload/youtube/v3/videos?uploadType=multipart&part=snippet,status", Path.of(fileDir), metadataJson, TOKEN);

        JSONObject response = StringToJson.getJSON(strResponse); // Convert to json for check

        if (HTTPSend.HTTPCode.get() != 200) { // Error handling
            String reason = ""; // Stores reason for error

            if (response.has("error")) {
                reason = response.getJSONObject("error").getJSONArray("errors").getJSONObject(0).getString("reason");
            }

            /* Error handling */
            if (reason.equals("uploadLimitExceeded") || reason.equals("rateLimitExceeded") || reason.equals("quotaExceeded")) {
                Output.webhookPrint(this, "Failed to post. Skipping this attempt..."
                        + "\n\tYou are being rate limited. You can only post a few times per day to the YouTube API", Output.RED);

                Thread.sleep(SLEEPTIME);
            } else if (HTTPSend.HTTPCode.get() == 500) { // Internal server error
                Output.webhookPrint(this, "YouTube API appears to be down. Skipping attempt... HTTP code: " + HTTPSend.HTTPCode.get() +
                        "\n\tError message: " + response, Output.RED);

                Thread.sleep(SLEEPTIME);
            } else { // General error handling
                Output.webhookPrint(this, "Failed to post. Trying again, and marking this URL as invalid..."
                        + "\n\tError message: " + response, Output.RED);

                // Blacklist image URL permanently, as it is likely corrupted
                FileIO.writeList(mediaURL, this, true);

                Thread.sleep(5000);
            }
            return false;
        }
        return true;
    }

    @Override
    protected boolean publish() throws Exception {
        return true; // YouTube is one-step
    }

    @Override
    protected boolean fetchUserToken() throws Exception {
        // Build upload data
        Map<String, String> formData = new HashMap<>();

        formData.put("client_id", CLIENT_ID);
        formData.put("client_secret", SECRET);
        formData.put("refresh_token", REFRESH_TOKEN);
        formData.put("grant_type", "refresh_token");

        String response;

        response = HTTPSend.postForm(this,"https://oauth2.googleapis.com/token", formData);


        if (HTTPSend.HTTPCode.get() == 200 && response.contains("access_token")) {
            TOKEN = StringToJson.getData(response, "access_token");

            return true;  // Success
        } else {
            Output.webhookPrint(this, "Failed to fetch token. Quitting..." +
                    "\n\tError message: " + response, Output.RED);

            return false;
        }
    }
}
