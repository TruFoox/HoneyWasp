package scraper;

import services.Services;
import utils.*;

import javax.net.ssl.SSLHandshakeException;
import java.awt.*;
import java.net.ConnectException;

public class Reddit {
    protected static synchronized String fetch(Services service, String chosenSubreddit) throws Exception {
        String response;
        String URL = "https://meme-api.com/gimme/" + chosenSubreddit;

        Output.debugPrint(service, "Fetching media URL from " + URL);
        try {
            response = HTTPSend.get(service, URL);
            service.connectionDropWait = 0; // Reset connection drop wait on success
        } catch (SSLHandshakeException | ConnectException e) {
            // ConnectionDropWait = Pre-processing time, waitTime = Post-processed time
            int waitTime = (service.connectionDropWait == 0) ? 1 : service.connectionDropWait; // Forces first wait to be 1, and allows sequential waits to be 5n minutes
            if (e instanceof ConnectException) {
                Output.print(service, "Connection drop detected. Try disabling Proxies, if enabled. Trying again in " + waitTime + " minute(s)...", Output.RED);
            } else { //if (e instanceof SSLHandshakeException) { ("if" redundant rn but it may not be in the future)
                Output.webhookPrint(service, "An error occurred while establishing a connection with meme-api.com. Trying again in " + waitTime + " minute(s)...", Output.RED);
            }
            service.connectionDropWait += 5;

            if (service.connectionDropWait > (5 * service.ATTEMPTS_BEFORE_TIMEOUT)) {
                Output.webhookPrint(service,"[NOTIFY]A connection error occurred " + service.ATTEMPTS_BEFORE_TIMEOUT + " times in a row. Quitting to prevent spam..."
                        + "\n\tError message: " + e, Output.RED);
            }

            Sleep.milliseconds(service, waitTime * 60000L); // 60000 is the conversion factor from minutes to ms
            return "retry";
        } catch (Exception e) {
            service.connectionDropWait = 0;
            Output.webhookPrint(service,"[NOTIFY]Failed to fetch image from meme-api.com. Quitting..."
                    + "\n\tError message: " + e, Output.RED);
            return "fail";
        }

        String mediaURL;
        /* Check meme-api status code */
        int HTTPCode = HTTPSend.HTTPCode.get().intValue();
        switch (HTTPCode) {
            case 200: // Success
                // Parse JSON data
                mediaURL = StringToJson.getData(response, "url");
                service.postURL = StringToJson.getData(response, "postLink");
                service.caption = StringToJson.getData(response, "title");
                boolean nsfw = Boolean.parseBoolean(StringToJson.getData(response, "nsfw"));
                service.tempDisableCaption = false;

                Output.debugPrint(service, "Reddit post data successfully retrieved");

                /* Check image validity (Ensures not gif, not blacklisted, not already used, valid aspect ratio) */
                Output.debugPrint(service, "Validating image");

                // Download image & check aspect ratio (If relevant)
                if (service.doSizeTest) {
                    Output.debugPrint(service, "Attempting to download image to verify aspect ratio");
                    Image image;

                    try {
                        image = HTTPSend.getImageData(mediaURL);
                    } catch(Exception e)  {
                        Output.webhookPrint(service, "Failed to download image from Reddit to check aspect ratio. Marking URL as invalid & retrying..."
                                + "\n\tError message: " + e.getMessage(), Output.RED);

                        // Blacklist image URL permanently, as it is likely corrupted
                        FileIO.writeList(mediaURL, service, true);
                        return "retry";
                    }

                    float ratio = (float) image.getWidth(null) / image.getHeight(null); // Eg 0.9 "0.9x as wide as it is tall"

                    Output.debugPrint(service, "Image aspect ratio is " + image.getWidth(null) + ":" + image.getHeight(null));
                    if (ratio < service.supportedAspectRatio[0] || ratio > service.supportedAspectRatio[1]) {
                        Output.print(service, "Image has invalid aspect ratio - x" + service.countAttempt + " attempts", Output.RED, true);
                        return "retry";
                    }

                }

                // Ensure non-gif/apng
                Output.debugPrint(service, "Testing if image is .gif/.apng");
                if (mediaURL.toLowerCase().contains(".gif") || mediaURL.toLowerCase().contains(".apng")) { // Ensure image is not .gif or .apng
                    Output.print(service, "Image is animated - x" + service.countAttempt + " attempts", Output.RED, true);

                    return "retry";
                }

                // Ensure not NSFW
                Output.debugPrint(service, "Testing if image is marked as NSFW");
                if (!service.NSFW_ALLOWED && nsfw) { // If NSFW is disallowed and post is marked as NSFW
                    Output.print(service, "Image is marked as NSFW - x" + service.countAttempt + " attempts", Output.RED, true);

                    return "retry";
                }

                // Ensure no blacklisted string in post caption
                Output.debugPrint(service, "Testing if image caption contains blacklisted string");
                for (String word : service.BLACKLIST) {
                    if (service.caption.toLowerCase().contains(word.toLowerCase())) {
                        Output.print(service, "Caption contains blacklisted string (\"" + word.toLowerCase() + "\") - x" + service.countAttempt + " attempts", Output.RED, true);

                        return "retry";
                    }
                }

                // Decide whether to use fallback or post caption (If relevant)
                if (service.USE_POST_CAPTION) {
                    Output.debugPrint(service, "Testing whether to use default caption");
                    for (String word : service.CAPTION_BLACKLIST) { // Ensure no caption-blacklisted string in post caption. If found, discard caption but still post
                        if (service.caption.toLowerCase().contains(word.toLowerCase())) {
                            Output.print(service, "Using default (Non-Reddit) caption (\"" + word.toLowerCase() + "\" found) - x" + service.countAttempt + " attempts", Output.RED, true);
                            service.caption = service.FALLBACK_CAPTION;
                        }
                    }
                }

                // Ensure post is not duplicate
                Output.debugPrint(service, "Testing if image url is duplicate");
                for (String[] row : service.usedURLs) {
                    if (row[0].equalsIgnoreCase(service.postURL)) {
                        if (!service.DUPLICATES_ALLOWED && (Long.parseLong(row[1]) > System.currentTimeMillis() && service.HOURS_BEFORE_DUPLICATES_REMOVED != 0)) { // Test if cached url is too old to be considered duplicate
                            Output.print(service, "Duplicate URL - x" + service.countAttempt + " attempts", Output.RED, true);

                            return "retry"; // Retry
                        }
                    }
                }

                Output.print(service, "Valid image found after x" + service.countAttempt + " attempts", Output.GREEN, true);
                return mediaURL; // Success
            case 503:
                Output.webhookPrint(service,"Cloudflare HTTP Status Code 503 - The API this program utilizes appears to be under maintenance."
                        + "\n\tThere is nothing that can be done to fix this but wait. Skipping attempt w/ +6 hour delay...", Output.RED);

                Sleep.milliseconds(service, service.SLEEPTIME + 21600000L); // Sleep normal time + 6 hours
                return "retry";
            case 500: // General server error
            case 530:
                Output.webhookPrint(service,"Cloudflare HTTP Status Code 530 - The API this program utilizes is temporarily unreachable"
                        + "\n\tThere is nothing that can be done to fix this but wait. Skipping attempt w/ +2 hour delay...", Output.RED);

                Sleep.milliseconds(service, service.SLEEPTIME + 7200000L); // Sleep normal time + 2 hours
                return "retry";
            case 520: // General malformed/null response
            case 502: // Bad gateway
                Output.webhookPrint(service,"Cloudflare HTTP Status Code " + HTTPCode + " - The API this program utilizes gave a bad response"
                        + "\n\tThere is nothing that can be done to fix this but wait. Skipping attempt...", Output.RED);

                Sleep.milliseconds(service, service.SLEEPTIME); // Sleep
                return "retry";
            default: // General error handling
                Output.webhookPrint(service,"[NOTIFY]Failed to retrieve image data from meme-api.com with error code " + HTTPCode + ". Quitting..."
                        + "\n\tError message: " + response, Output.RED);

                return "fail";
        }
    }
}
