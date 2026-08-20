package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.HoneyWasp;
import utils.*;

import java.awt.*;
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

public class TikTok extends Services implements HasRefreshToken { // For some reason TikTok API always returns 200 unless a request was not understood
    private final String CLIENT_KEY, SECRET;
    String codeVerifier = "y4kfXj5DRBOWYgKHafscM5alOZ5nyXEO42iL1KjL_6RvkoKU1npwKS6_3iulzGXR";
    String codeChallenge = "38b07f366c70e0726e8a60d2e266bf4ff413f152e54aff22fe2b75f434231090";
    String publishID;

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


        if (HTTPSend.HTTPCode.get() == 200 && response.contains("refresh_token")) {
            REFRESH_TOKEN = StringToJson.getData(response, "refresh_token");

            HoneyWasp.config.Tiktok().setRefresh_token(REFRESH_TOKEN);
            HoneyWasp.config.saveConfig(); // Write to file

            return true;  // Success
        } else {
            Output.webhookPrint(this, "Failed to fetch refresh token. Quitting..." +
                    "\n\tError message: " + response, Output.RED);

            return false;
        }
    }

    @Override
    protected Boolean upload() throws Exception {Map<String, Object> postInfo = new HashMap<>();
        postInfo.put("title", caption);
        postInfo.put("privacy_level", "SELF_ONLY"); // Not EVERYONE because that requires app verification which no one will realistically be able to do, even myself

        Map<String, Object> sourceInfo = new HashMap<>();
        sourceInfo.put("source", "FILE_UPLOAD");
        sourceInfo.put("video_size", Files.size(Path.of(fileDir)));
        sourceInfo.put("chunk_size", Files.size(Path.of(fileDir)));
        sourceInfo.put("total_chunk_count", 1);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("post_info", postInfo);
        metadata.put("source_info", sourceInfo);

        String metadataJson = new ObjectMapper().writeValueAsString(metadata);

        HttpClient client;

        if (HoneyWasp.USE_PROXIES) {
            client = HttpClient.newBuilder()
                    .proxy(ProxySelector.of(new InetSocketAddress(proxy[0], Integer.parseInt(proxy[1]))))
                    .build();
        } else {
            client = HttpClient.newHttpClient();
        }

        Output.debugPrint(this, "Fetching account info");

        HttpRequest request = HttpRequest.newBuilder() // Get account info
                .uri(URI.create("https://open.tiktokapis.com/v2/post/publish/creator_info/query/"))
                .header("User-Agent", "HoneyWasp/5.0")
                .header("Authorization", "Bearer " + TOKEN)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        String response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();

        Output.debugPrint(this, "Response: " + response);
        HTTPSend.setLastResponse(response);

        Output.debugPrint(this, "Creating post on TikTok");

        request = HttpRequest.newBuilder() // Get account info
                .uri(URI.create("https://open.tiktokapis.com/v2/post/publish/video/init/"))
                .header("User-Agent", "HoneyWasp/5.0")
                .header("Authorization", "Bearer " + TOKEN)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(metadataJson))
                .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();

        Output.debugPrint(this, "Response: " + response);
        HTTPSend.setLastResponse(response);

        if (StringToJson.getJSON(response).has("error")) {
            if (StringToJson.getJSON(response).getJSONObject("error").has("message")) {
                String message = StringToJson.getJSON(response).getJSONObject("error").get("message").toString();
                Output.webhookPrint(this, "Failed to upload. Quitting..." +
                        "\n\tReason: " + message, Output.RED);
            } else {
                Output.webhookPrint(this, "Failed to upload. Quitting..." +
                        "\n\tError message: " + response, Output.RED);
            }
        }
        publishID = StringToJson.getJSON(response).getJSONObject("data").getString("publish_id");

        Output.webhookPrint(this, publishID);

        byte[] videoBytes = Files.readAllBytes(Path.of(fileDir));

        Output.debugPrint(this, "Uploading video file to TikTok");

        request = HttpRequest.newBuilder() // Get account info
                .uri(URI.create(StringToJson.getJSON(response).getJSONObject("data").getString("upload_url")))
                .header("Content-Range", "bytes 0-" + (Files.size(Path.of(fileDir)) - 1) + "/" + Files.size(Path.of(fileDir)))
                .header("Content-Type", "application/json; charset=UTF-8")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(videoBytes))
                .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();

        Output.debugPrint(this, "Response: " + response);
        HTTPSend.setLastResponse(response);

        if (HTTPSend.HTTPCode.get() != 200) {
            Output.webhookPrint(this, "Failed to upload. Quitting..." +
                    "\n\tError message: " + response, Output.RED);

            return false;
        }

        return true;  // Success
    }

    @Override
    protected Boolean publish() throws Exception { // Doesn't actually publish, just waits for upload to finish
        String postStatus = "PROCESSING_UPLOAD";
        do {
            Output.print(this, "Waiting for TikTok to process media. This may take a while...", Output.YELLOW, true);

            HttpClient client;
            if (HoneyWasp.USE_PROXIES) {
                client = HttpClient.newBuilder()
                        .proxy(ProxySelector.of(new InetSocketAddress(proxy[0], Integer.parseInt(proxy[1]))))
                        .build();
            } else {
                client = HttpClient.newHttpClient();
            }

            HttpRequest request = HttpRequest.newBuilder() // Get account info
                    .uri(URI.create("https://open.tiktokapis.com/v2/post/publish/status/fetch/"))
                    .header("User-Agent", "HoneyWasp/5.0")
                    .header("Authorization", "Bearer " + TOKEN)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"publish_id\":\"" + publishID + "\"}"))
                    .build();

            String response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();

            Output.debugPrint(this, "Response: " + response);
            HTTPSend.setLastResponse(response);

            postStatus = StringToJson.getJSON(response).getJSONObject("data").getString("status");

            if (postStatus.equals("FAILED")) {
                Output.webhookPrint(this, "Video processing failed. Video is likely corrupted. Attempting to post again..." +
                        "\n\tError Message: " + response, Output.RED);
                return false;
            }

            Sleep.milliseconds(this, 5000); // Wait 5s to prevent spam
        } while (postStatus.equals("PROCESSING_UPLOAD"));
        return true;
    }

    @Override
    protected boolean fetchUserToken() throws Exception {
        // Build upload data
        Map<String, String> formData = new HashMap<>();

        formData.put("client_key", CLIENT_KEY);
        formData.put("client_secret", SECRET);
        formData.put("grant_type", "refresh_token");
        formData.put("refresh_token", REFRESH_TOKEN);

        String response;

        response = HTTPSend.postForm(this,"https://open.tiktokapis.com/v2/oauth/token/", formData);


        if (HTTPSend.HTTPCode.get() == 200 && response.contains("access_token")) {
            TOKEN = StringToJson.getData(response, "access_token");
            REFRESH_TOKEN = StringToJson.getData(response, "refresh_token");

            return true;  // Success
        } else {
            Output.webhookPrint(this, "Failed to fetch token. Quitting..." +
                    "\n\tError message: " + response, Output.RED);

            return false;
        }
    }
}
