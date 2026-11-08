package services;

import main.HoneyWasp;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.*;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Instagram extends Services {
    private long USERID = 0;
    public Instagram() {
        super("Instagram","INSTA");

        refreshConfig();
        use0x0 = false;
        supportedAspectRatio = new double[]{0.55, 1.81}; // 11:20 to 20:11 - Their API docs says it supports 1:2 to 2:1 but it doesn't
    }

    public void refreshConfig() {
        TOKEN = HoneyWasp.config.Instagram().getApi_key().trim();
        VIDEO_MODE = HoneyWasp.config.Instagram().isVideo_mode();
    }

    protected int upload() throws Exception {
        String response; // Store json data & URL to be used with POST
        String containerId = ""; // Used for video processing progress check
        caption += "\n\n.\n\n" + HASHTAGS; // Add hashtags to caption

        // Build upload data
        Map<String, String> formData = new HashMap<>();

        if (!VIDEO_MODE) {
            formData.put("image_url", mediaURL);
            formData.put("caption", caption);
            formData.put("access_token", TOKEN);

        } else {
            formData.put("upload_type", "resumable");
            formData.put("caption", caption);
            formData.put("media_type", "REELS");
            formData.put("access_token", TOKEN);
        }

        response = HTTPSend.postForm(this,"https://graph.facebook.com/v23.0/" + USERID + "/media", formData); // Send JSON data for upload (Step 1/2 - next is publish)

        boolean isTransient;
        try {
            isTransient = Boolean.parseBoolean(StringToJson.getData(response, "is_transient"));
        } catch (Exception _) {
            isTransient = false;
        }

        if (VIDEO_MODE) {
            JSONObject container = StringToJson.getJSON(response);
            containerId = container.getString("id");
            String uploadUri = container.getString("uri");

            HttpClient client;

            if (HoneyWasp.USE_PROXIES) {
                client = HttpClient.newBuilder()
                        .proxy(ProxySelector.of(new InetSocketAddress(proxy[0], Integer.parseInt(proxy[1]))))
                        .build();
            } else {
                client = HttpClient.newHttpClient();
            }

            // First upload video data itself
            byte[] videoBytes = Files.readAllBytes(Path.of(fileDir));

            Output.debugPrint(this, "Uploading video file to Instagram");

            long fileSize = videoBytes.length;

            HttpRequest uploadRequest = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUri))
                    .header("Authorization", "OAuth " + TOKEN)
                    .header("offset", "0")
                    .header("file_size", String.valueOf(fileSize))
                    .header("Content-Type", "application/octet-stream")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(videoBytes))
                    .build();

            HttpResponse<String> HTTPresponse = client.send(uploadRequest, HttpResponse.BodyHandlers.ofString());
            HTTPSend.HTTPCode.set((long) HTTPresponse.statusCode());

            response = HTTPresponse.body();
            HTTPSend.setLastResponse(response);
        }
        if (HTTPSend.HTTPCode.get() != 200 && HTTPSend.HTTPCode.get() != 201) {
            if (HTTPSend.HTTPCode.get() == 500) { // Internal server error
                Output.webhookPrint(this, "Instagram API appears to be down. Skipping attempt... HTTP code: " + HTTPSend.HTTPCode.get() +
                        "\n\tError message: " + response, Output.RED);

                Sleep.milliseconds(this, SLEEPTIME);
            } else if (isTransient && postAttempts < uploadAttemptTimeout) { // If temporary error & not max attempts
                Output.webhookPrint(this, "Upload step failed! Attempting upload again in 5 seconds... HTTP code: " + HTTPSend.HTTPCode.get() +
                        "\n\tError message: " + response, Output.RED);

                postAttempts++;

                Sleep.milliseconds(this, 5000);
                return upload();
            } if (response.contains("Only photo or video") && HTTPSend.HTTPCode.get() == 400) { // Instagram failed to fetch the image for reasons out of my control. The error message is misleading
                Output.webhookPrint(this, "Upload step failed because Instagram rejected the URL. Trying again... ", Output.RED);
            } else {
                Output.webhookPrint(this, "Upload step failed! Trying again, and marking this URL as invalid... HTTP code: " + HTTPSend.HTTPCode.get() +
                        "\n\tError message: " + response, Output.RED);

                if (!VIDEO_MODE) {
                    // Blacklist image URL permanently, as it is likely corrupted
                    FileIO.writeList(postURL, this, true);
                }
            }

            Sleep.milliseconds(this, 1000);
            return 0;
        } else {
            Output.print(this, "Upload step success (1/2)", Output.YELLOW, true);
        }

        if (VIDEO_MODE) { // Get post ID from previous HTTP step
            postID = containerId;
        } else {
            postID = StringToJson.getData(response, "id");
        }

        Sleep.milliseconds(this, 500); // Sleep for 0.5s - gives Instagram a little more time to get ready

        String postStatus = "";
        if (VIDEO_MODE) {
            do {
                Output.print(this, "Waiting for Instagram to process media. This may take a while...", Output.YELLOW, true);

                if (!containerId.isEmpty()) {
                    response = HTTPSend.get(this, "https://graph.facebook.com/v23.0/" + containerId + "?fields=status_code&access_token=" + TOKEN);
                }

                if (HTTPSend.HTTPCode.get() != 200 || containerId.isEmpty()) { // Error handling
                    Output.print(this, "Failed to get post upload status, waiting 3 minutes before attempting upload to be safe..." +
                            "Error message: " + response, Output.RED, true);

                    Sleep.milliseconds(this, 180000);
                    continue;
                }

                postStatus = StringToJson.getData(response, "status_code");

                if (postStatus.equals("ERROR")) {
                    Output.webhookPrint(this, "Video processing failed. Video is likely corrupted. Attempting to post again..." +
                            "\n\tError Message: " + response, Output.RED);
                    return 0;
                }

                Sleep.milliseconds(this, 5000); // Wait 5s to prevent spam
            } while (!postStatus.equals("FINISHED"));
        }

        return 1;
    }
    protected int publish() throws Exception {
        Sleep.milliseconds(this, 5000); // Easiest way to prevent media ID not ready yet error

        Map<String, String> formData = new HashMap<>();

        formData.put("creation_id", postID);
        formData.put("access_token", TOKEN);

        response = HTTPSend.postForm(this,"https://graph.facebook.com/v23.0/" + USERID + "/media_publish", formData); // Send post for publish to Instagram

        boolean isTransient;
        try {
            isTransient = Boolean.parseBoolean(StringToJson.getData(response, "is_transient"));
        } catch (Exception _) {
            isTransient = false;
        }

        if (HTTPSend.HTTPCode.get() != 200 && HTTPSend.HTTPCode.get() != 201) {
            if (isTransient && postAttempts < uploadAttemptTimeout) { // If temporary error & not max attempts
                Output.webhookPrint(this, "Publish step failed! Attempting publish again in 5 seconds... HTTP code: " + HTTPSend.HTTPCode.get() +
                        "\n\tError message: " + response, Output.RED);

                postAttempts++;

                Sleep.milliseconds(this, 5000);
                return publish();
            } else {
                Output.webhookPrint(this, "Publish step failed! Trying again, and marking this URL as invalid... HTTP code:" + HTTPSend.HTTPCode.get() +
                        "\n\tError message: " + response, Output.RED);

                if (!VIDEO_MODE) {
                    // Blacklist image URL permanently, as it is likely corrupted
                    FileIO.writeList(postURL, this, true);
                }
            }
            return 0;
        }
        return 1;
    }

    protected int fetchUserToken() throws Exception { // Fetches user ID
        if (USERID == 0) { // If ID not already fetched this session
            Output.debugPrint(this, "Attempting to fetch User ID");

            Output.debugPrint(this, "Attempting to fetch access token (Step 1)");
            String response = HTTPSend.get(this, "https://graph.facebook.com/v23.0/me/accounts?access_token=" + TOKEN);

            if (!StringToJson.getJSON(response).has("data")) {
                try {
                    Output.webhookPrint(this, "[NOTIFY]Your access token is likely invalid. Please refresh it using the instructions in https://github.com/TruFoox/HoneyWasp#instagram-setup" +
                            "Response: " + StringToJson.getJSON(response).getJSONObject("error").get("message"), Output.RED);
                } catch (Exception e) {
                    Output.webhookPrint(this, response);
                }
                return 0;
            }

            String facebookID;
            JSONArray data = StringToJson.getJSON(response).getJSONArray("data"); // Convert to JSON array format
            JSONObject dataObj = data.getJSONObject(0);

            facebookID = dataObj.getString("id"); // Temporarily store facebook ID

            if (!run) {
                return 0;
            }

            Output.debugPrint(this, "Attempting to fetching User ID from token (Step 2)");
            // Get Instagram ID
            response = HTTPSend.get(this, "https://graph.facebook.com/v23.0/" + facebookID + "?fields=instagram_business_account&access_token=" + TOKEN);

            if (!response.contains("instagram_business_account")) { // Ensure account is business account
                Output.webhookPrint(this, "[NOTIFY]Token valid, but no linked Instagram Business Account found. Please set your instagram account type to business. Quitting...", Output.RED);

                return 0;
            }
            dataObj = StringToJson.getJSON(response);

            dataObj = dataObj.getJSONObject("instagram_business_account"); // Get JSON["instagram_business_account"]["id"]
            USERID = dataObj.getLong("id");
        }
        return 1; // Success
    }
}