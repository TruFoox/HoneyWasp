package services;

import config.Config;
import config.InstagramSettings;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.*;

import java.util.HashMap;
import java.util.Map;

public class Insta extends Services implements HasUserID {
    private long USERID;

    public Insta() {
        super("Instagram","INSTA",Config.getInstance());
        settings = config.Platform("instagram"); // Establish settings
        
        InstagramSettings ig = Config.getInstance().Instagram(); // Set instance-specific named stuff
        TOKEN = ig.getApi_key();
        VIDEO_MODE = ig.isVideo_mode();
        use0x0 = true; // Instagram only supports URL filehosting

    }
    public boolean fetchUserID() {
        try {
            try {
                Output.debugPrinttest(this, "Attempting to fetch User ID");

                Output.debugPrinttest(this, "Attempting to fetch access token (Step 1)");
                String response = HTTPSend.get("https://graph.facebook.com/v23.0/me/accounts?access_token=" + TOKEN);

                String facebookID;
                JSONArray data = StringToJson.getJSON(response).getJSONArray("data"); // Convert to JSON array format
                JSONObject dataObj = data.getJSONObject(0);

                facebookID = dataObj.getString("id"); // Temporarily store facebook ID

                Output.debugPrinttest(this, "Attempting to fetching User ID from token (Step 2)");
                response = HTTPSend.get("https://graph.facebook.com/v23.0/" + facebookID + "?fields=instagram_business_account&access_token=" + TOKEN);

                if (!response.contains("instagram_business_account")) { // Ensure account is business account
                    Output.webhookPrinttemptest(this, "Token valid, but no linked Instagram Business Account found. Please set your instagram account type to business. Quitting...", Output.RED);

                    return false;
                }
                dataObj = StringToJson.getJSON(response);

                dataObj = dataObj.getJSONObject("instagram_business_account"); // Get JSON["instagram_business_account"]["id"]
                USERID = dataObj.getLong("id");

            } catch (Exception e) {
                Output.webhookPrinttemptest(this, "Failed to retrieve Instagram User ID. Your Access token may be invalid. Quitting..."
                        + "\n\tError message: " + e, Output.RED);
                return false;
            }
        } catch (Exception e) {
            try {
                Output.webhookPrinttemptest(this, String.valueOf(e), Output.RED);
            } catch (Exception ex) {
                throw new RuntimeException(e);
            }
            return false;
        }

        return true; // Success
    }
    protected boolean upload() throws Exception {
        String uploadURL, response; // Store json data & URL to be used with POST

        if (!AUTO_POST_MODE || !USE_REDDIT_CAPTION || tempDisableCaption) { // Set caption depending on settings
            caption = FALLBACK_CAPTION; // Set caption if no reddit post or if post failed caption validation (avoids needing larger if statement later)
        }

        caption += "\n\n.\n\n" + HASHTAGS; // Add hashtags to caption

        // Build upload data
        Map<String, String> formData = new HashMap<>();

        if (!VIDEO_MODE) {
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


        if (HTTPSend.HTTPCode.get() != 200 && HTTPSend.HTTPCode.get() != 201) {
            if (response.contains("Only photo or video") && HTTPSend.HTTPCode.get() == 400) { // Instagram failed to fetch the image for reasons out of my control. The error message is misleading
                Output.webhookPrinttemptest(this, "Upload step failed because Instagram rejected the URL. Trying again... ", Output.RED);
            } else {
                Output.webhookPrinttemptest(this, "Upload step failed! Trying again, and marking this URL as invalid... HTTP code: " + HTTPSend.HTTPCode.get() +
                        "\n\tError message: " + response, Output.RED);

            }

            // Blacklist image URL permanently, as it is likely corrupted
            FileIO.writeList(mediaURL, "instagram", true);
            Sleep.safeSleep(1000);
            return false;
        } else {
            Output.printtest(this, "Upload step success (1/2)", Output.YELLOW, true);
        }

        postID = StringToJson.getData(response, "id"); // Get post ID from previous HTTP step

        Thread.sleep(500); // Sleep for 0.5s - gives Instagram time to get ready


        /* Instagram needs time to render videos - this loop has the bot wait until it is finished */
        String postStatus = "";
        if (VIDEO_MODE) {
            do {
                Output.printtest(this, "Waiting for Instagram to process media. This may take a while...", Output.YELLOW, true);

                HTTPSend.get("https://graph.facebook.com/v23.0/" + postID +
                        "?fields=status_code,status&access_token=" + TOKEN); // Send status check request

                if (HTTPSend.HTTPCode.get() != 200) { // Error handling
                    Output.printtest(this, "Failed to get post upload status, waiting 30 seconds before attempting upload...", Output.YELLOW, true);


                    if (!Sleep.safeSleep(30000)) return false;
                    break;
                }

                Output.webhookPrinttemptest(this, response);

                postStatus = StringToJson.getData(response, "status_code");

                if (postStatus.equals("ERROR")) {
                    Output.webhookPrinttemptest(this, "Video processing failed. Video is likely corrupted. Attempting to post again..." +
                            "\n\tError Message: " + response, Output.RED);
                    break;
                }

            } while (!postStatus.equals("FINISHED"));
        }

        if (postStatus.equals("ERROR")) { // If there was an error, retry attempt
            return false;
        }

        return true;
    }
    protected boolean publish() throws Exception {
        Map<String, String> formData = new HashMap<>();
        
        formData.put("creation_id", postID);
        formData.put("access_token", TOKEN);

        response = HTTPSend.postForm("https://graph.facebook.com/v23.0/" + USERID + "/media_publish", formData); // Send post for publish to Instagram

        if (HTTPSend.HTTPCode.get() != 200) {
            Output.webhookPrint("[INSTA] Publish step failed! Trying again, and marking this URL as invalid... HTTP code:" + HTTPSend.HTTPCode.get() +
                    "\n\tError message: " + response, Output.RED);

            // Blacklist image URL permanently, as it is likely corrupted
            FileIO.writeList(mediaURL, "instagram", true);

            return false;
        }

        return true;
    }
protected boolean fetchUserToken() {
        try {
            Output.debugPrint("Attempting to fetch User ID");

            Output.debugPrinttest(this, "Attempting to fetch access token (Step 1)");
            String response = HTTPSend.get("https://graph.facebook.com/v23.0/me/accounts?access_token=" + TOKEN);

            String facebookID;
            JSONArray data = StringToJson.getJSON(response).getJSONArray("data"); // Convert to JSON array format
            JSONObject dataObj = data.getJSONObject(0);

            facebookID = dataObj.getString("id"); // Temporarily store facebook ID

            Output.debugPrinttest(this, "Attempting to fetching User ID from token (Step 2)");
            // Get Instagram ID
            response = HTTPSend.get("https://graph.facebook.com/v23.0/" + facebookID + "?fields=instagram_business_account&access_token=" + TOKEN);

            if (!response.contains("instagram_business_account")) { // Ensure account is business account
                Output.webhookPrinttemptest(this,"Token valid, but no linked Instagram Business Account found. Please set your instagram account type to business. Quitting...", Output.RED);

                return false;
            }
            dataObj = StringToJson.getJSON(response);

            dataObj = dataObj.getJSONObject("instagram_business_account"); // Get JSON["instagram_business_account"]["id"]
            USERID = dataObj.getLong("id");

        } catch (Exception e) {
            Output.webhookPrinttemptest(this,"Failed to retrieve Instagram User ID. Your Access token may be invalid. Quitting..."
                    + "\n\tError message: " + e, Output.RED);
            return false;
        }

        return true; // Success
    }
}