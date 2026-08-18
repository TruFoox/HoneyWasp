package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.HoneyWasp;
import org.json.JSONObject;
import utils.FileIO;
import utils.HTTPSend;
import utils.Output;
import utils.StringToJson;

import java.awt.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TikTok extends Services implements HasRefreshToken {
    private final String CLIENT_KEY, SECRET;
    String codeVerifier = "y4kfXj5DRBOWYgKHafscM5alOZ5nyXEO42iL1KjL_6RvkoKU1npwKS6_3iulzGXR";
    String codeChallenge = "38b07f366c70e0726e8a60d2e266bf4ff413f152e54aff22fe2b75f434231090";

    public TikTok() {
        super("TikTok","TT");

        SECRET = HoneyWasp.config.Tiktok().getClient_secret().trim();
        CLIENT_KEY = HoneyWasp.config.Tiktok().getClient_key().trim();
        REFRESH_TOKEN = HoneyWasp.config.Tiktok().getRefresh_token().trim();
        VIDEO_MODE = true; // TikTok technically supports images, but I just don't want to bother
        supportedAspectRatio = new double[]{0.82, 1.70}; // ~4:5 to 17:10
    }

    @Override
    public boolean fetchRefreshToken() {
        String redirectURI = "http://localhost:8000/callback"; // URL to redirect to after authentication

        // Generate OAuth URL & prompt user to go there to get token
        String url = "https://www.tiktok.com/v2/auth/authorize/" +
                "?client_key=" + CLIENT_KEY +
                "&scope=video.publish" +
                "&response_type=code" +
                "&redirect_uri=" + redirectURI +
                "&state=" + "tsinsecureasf" +
                "&code_challenge=" + codeChallenge +
                "&code_challenge_method=S256";

        Output.webhookPrint(this, "BEFORE YOU CAN POST TO TIKTOK, YOU MUST RETRIEVE YOUR ACCESS TOKEN." +
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

        // Now convert Oauth to actual token
        Output.debugPrint(this, "");

        // Build upload data
        Map<String, String> formData = new HashMap<>();

        formData.put("client_key", CLIENT_KEY);
        formData.put("client_secret", SECRET);
        formData.put("code", authCode);
        formData.put("grant_type", "authorization_code");
        formData.put("redirect_uri", redirectURI);
        formData.put("code_verifier", codeVerifier);

        String response;
        try {
            response = HTTPSend.postForm(this,"https://open.tiktokapis.com/v2/oauth/token/", formData);
        } catch (Exception e) {
            Output.webhookPrint(this, "Failed to fetch refresh token. Quitting..." +
                    "\n\tError: " + e, Output.RED);

            return false;
        }

        if (HTTPSend.HTTPCode.get() == 200 && response.contains("access_token")) {
            REFRESH_TOKEN = StringToJson.getData(response, "refresh_token");

            HoneyWasp.config.Tiktok().setRefresh_token(REFRESH_TOKEN);
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

        return true;
    }

    @Override
    protected boolean publish() throws Exception {

        return true;
    }

    @Override
    protected boolean fetchUserToken() throws Exception {

        return true;
    }
}
