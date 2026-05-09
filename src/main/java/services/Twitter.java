package services;
/* Commented to avoid errors for now
import config.Config;
import config.TwitterSettings;
import config.YoutubeSettings;
import utils.HTTPSend;
import utils.Output;
import utils.StringToJson;

import java.awt.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class Twitter extends Services implements HasRefreshToken {
    String securityKey = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"; // verifier
    String codeChallenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"; // precomputed SHA256
    String KEY, REFRESH_TOKEN;

    public Twitter() {
        super("Twitter","X", Config.getInstance());
        settings = config.Platform("twitter"); // Establish settings

        TwitterSettings x = Config.getInstance().Twitter(); // Set service-specific stuff
        KEY = config.Twitter().getConsumer_key().trim();
        REFRESH_TOKEN = config.Twitter().getRefresh_token();
    }
    
    public boolean fetchRefreshToken() {
        Output.debugPrint(this, "[TWIT] Fetching refresh token");
        if (REFRESH_TOKEN.isEmpty()) { // Only run if no refresh token
            try {
                String oauthURL = "https://twitter.com/i/oauth2/authorize?response_type=code" +
                        "&client_id=" + KEY +
                        "&redirect_uri=http://localhost" +
                        "&scope=tweet.write%20media.write%20users.read%20offline.access"+
                        "&state=anything" +
                        "&code_challenge=" + codeChallenge +
                        "&code_challenge_method=S256";


                Output.webhookPrint(this, "BEFORE YOU CAN POST TO TWITTER, YOU MUST RETRIEVE YOUR ACCESS TOKEN." +
                        "\n\tATTEMPTING TO REDIRECT YOU TO THE AUTHENTICATION SITE NOW (OR GO TO " + oauthURL + ")", Output.RED);

                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) { // Test if browser allows going to URL from here
                    try {
                        Desktop.getDesktop().browse(new URI(oauthURL));
                    } catch (Exception e) {
                        // Ignore
                    }
                }

                Output.webhookPrint(this, "[THIS STEP MUST BE DONE IN-CONSOLE] PLEASE PASTE THE ENTIRE URL YOU WERE JUST REDIRECTED TO (SEE https://github.com/TruFoox/HoneyWasp/#twitter-setup FOR HELP):", Output.YELLOW);
                String redirectUrl = scanner.nextLine(); // Read user input

                String authCode = redirectUrl.replace("http://localhost/?state=anything&code=", ""); // Remove non-code part of URL

                oauthURL = "https://api.twitter.com/2/oauth2/token";

                // Build upload data
                Map<String, String> formData = new HashMap<>();

                formData.put("client_id", KEY);
                formData.put("grant_type", "authorization_code");
                formData.put("redirect_uri", "http://localhost");
                formData.put("code", authCode);
                formData.put("code_verifier", securityKey);

                String response = HTTPSend.postForm(this, oauthURL, formData);

                if (HTTPSend.HTTPCode.get() == 200 && response.contains("refresh_token")) {
                    REFRESH_TOKEN = StringToJson.getData(response, "refresh_token");

                    config.Twitter().setRefresh_token(REFRESH_TOKEN);
                    config.saveConfig();

                    return true;  // Success
                } else {
                    Output.webhookPrint(this, "[TWIT] Failed to fetch refresh token. Quitting..." +
                            "\n\tError message: " + response, Output.RED);

                    return false;
                }
            } catch (Exception e) {
                try {
                    Output.webhookPrint(this, String.valueOf(e), Output.RED);
                } catch (Exception ex) {
                    throw new RuntimeException(e);
                }
                return false;
            }
        }
        Output.debugPrint(this, "[TWIT] refresh_token was found to contain data");
        return true;
    }

    public boolean fetchUserToken() {
        try {
            Output.debugPrint(this, "[TWIT] Fetching access token");
            String oauthURL = "https://api.twitter.com/2/oauth2/token";

            // Build upload data
            Map<String, String> formData = new HashMap<>();

            formData.put("grant_type", "refresh_token");
            formData.put("refresh_token", REFRESH_TOKEN);
            formData.put("client_id", KEY);

            String response = HTTPSend.postForm(this, oauthURL, formData);

            if (HTTPSend.HTTPCode.get() == 200 && response.contains("refresh_token") && response.contains("access_token")) {
                // Set both access and refresh, as Twitter refresh tokens are re-given on every use
                TOKEN = StringToJson.getData(response, "access_token");
                REFRESH_TOKEN = StringToJson.getData(response, "refresh_token");

                config.Twitter().setRefresh_token(REFRESH_TOKEN);
                config.saveConfig(); // Write to file

                return true;  // Success

            } else if (response.contains("token was invalid")) { // If refresh token is invalid
                Output.webhookPrint(this, "[TWIT] Refresh token appears to be invalid. You need to reauthenticate it.", Output.RED);

                REFRESH_TOKEN = "";

                return fetchRefreshToken(); // Attempt to fetch refresh token again

            } else {
                Output.webhookPrint(this, "[TWIT] Failed to fetch token. Quitting..." +
                        "\n\tError message: " + response, Output.RED);

                return false;
            }
        } catch (Exception e) {
            try {
                Output.webhookPrint(this, String.valueOf(e), Output.RED);
            } catch (Exception ex) {
                throw new RuntimeException(e);
            }
            return false;
        }
    }
    

    @Override
    boolean upload() throws Exception {
        return false;
    }

    @Override
    boolean publish() throws Exception {
        return false;
    }

}
*/