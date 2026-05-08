package services;

import config.Config;
import config.InstagramSettings;
import org.json.JSONArray;
import org.json.JSONObject;
import services.Services;
import utils.HTTPSend;
import utils.Output;
import utils.StringToJson;

public class Insta extends Services implements HasUserID {
    public Insta() {
        super("Instagram","INSTA",Config.getInstance());
        settings = config.Platform("instagram"); // Establish settings\

        InstagramSettings ig = Config.getInstance().Instagram();
        TOKEN = ig.getApi_key();
        DESCRIPTION = ig.getCaption();

    }
    public boolean fetchUserID() {
        try {
            try {
                Output.debugPrint("[INSTA] Attempting to fetch User ID");

                Output.debugPrint("[INSTA] Attempting to fetch access token (Step 1)");
                String response = HTTPSend.get("https://graph.facebook.com/v23.0/me/accounts?access_token=" + TOKEN);

                String facebookID;
                JSONArray data = StringToJson.getJSON(response).getJSONArray("data"); // Convert to JSON array format
                JSONObject dataObj = data.getJSONObject(0);

                facebookID = dataObj.getString("id"); // Temporarily store facebook ID

                Output.debugPrint("[INSTA] Attempting to fetching User ID from token (Step 2)");
                // Get Instagram ID
                response = HTTPSend.get("https://graph.facebook.com/v23.0/" + facebookID + "?fields=instagram_business_account&access_token=" + TOKEN);

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
    protected boolean upload() {
        Output.debugPrint("e");
        return false;
    }
    protected boolean publish() {

        return false;
    }
    protected boolean fetchUserToken() {

        return false;
    }
}