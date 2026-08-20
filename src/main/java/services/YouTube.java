package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import main.HoneyWasp;
import org.json.JSONObject;
import utils.*;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    protected Boolean upload() throws Exception {

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

        Output.debugPrint(this, "Fetching video data for upload");
        HttpClient client;

        if (HoneyWasp.USE_PROXIES) {
            client = HttpClient.newBuilder()
                    .proxy(ProxySelector.of(
                            new InetSocketAddress(proxy[0], Integer.parseInt(proxy[1]))
                    ))
                    .build();
        } else {
            client = HttpClient.newHttpClient();
        }

        String boundary = UUID.randomUUID().toString();
        String CRLF = "\r\n";

        // Read video bytes
        byte[] videoBytes = Files.readAllBytes(Path.of(fileDir));
        String fileName = Path.of(fileDir).getFileName().toString();

        // Determine video content type from extension
        int dotPos = fileName.lastIndexOf('.');
        if (dotPos == -1 || dotPos == fileName.length() - 1) {
            throw new IOException("No file extension found for: " + fileName);
        }
        String ext = fileName.substring(dotPos + 1);
        String videoContentType = "video/" + ext;

        // Build multipart/related body manually
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

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
                .uri(URI.create("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=multipart&part=snippet,status"))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Authorization", "Bearer " + TOKEN)
                .header("Content-Type", "multipart/related; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
                .build();

        Output.debugPrint(this, "Sending video data to YouTube");

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        HTTPSend.HTTPCode.set((long) response.statusCode());

        Output.debugPrint(this, "Response: " + response);
        HTTPSend.setLastResponse(response.toString());

        JSONObject responseJSON = StringToJson.getJSON(response.body()); // Convert to json for check

        if (HTTPSend.HTTPCode.get() != 200) { // Error handling
            String reason = ""; // Stores reason for error

            if (responseJSON.has("error")) {
                reason = responseJSON.getJSONObject("error").getJSONArray("errors").getJSONObject(0).getString("reason");
            }

            /* Error handling */
            if (reason.equals("uploadLimitExceeded") || reason.equals("rateLimitExceeded") || reason.equals("quotaExceeded")) {
                Output.webhookPrint(this, "Failed to post. Skipping this attempt..."
                        + "\n\tYou are being rate limited. You can only post a few times per day to the YouTube API", Output.RED);

                Sleep.milliseconds(this, SLEEPTIME);
            } else if (HTTPSend.HTTPCode.get() == 500) { // Internal server error
                Output.webhookPrint(this, "YouTube API appears to be down. Skipping attempt... HTTP code: " + HTTPSend.HTTPCode.get() +
                        "\n\tError message: " + response, Output.RED);

                Sleep.milliseconds(this, SLEEPTIME);
            } else { // General error handling
                Output.webhookPrint(this, "Failed to post. Trying again, and marking this URL as invalid..."
                        + "\n\tError message: " + response, Output.RED);

                // Blacklist image URL permanently, as it is likely corrupted
                FileIO.writeList(mediaURL, this, true);

                Sleep.milliseconds(this, 5000);
            }
            return false;
        }
        return true;
    }

    @Override
    protected Boolean publish() throws Exception {
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
