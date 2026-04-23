package utils;

import config.ReadConfig;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.List;

// Output 0 ; success
// Output 1 ; fail
// Output 2 ; Disable caption but still post
public class ImageValidity { // Need to break into individual classes
    static ReadConfig config = ReadConfig.getInstance();

    public static int check(String response, long countattempt, List<String[]> usedURLs, boolean testSize, String platform) {
        List<String> BLACKLIST;
        boolean NSFW_ALLOWED;
        String caption;
        long hours_before_duplicate_removed;
        List<String> CAPTION_BLACKLIST;
        String mediaURL;
        boolean nsfw;

        Output.debugPrint("Validating image");

        /* Get config data */

        // This should prob be replaced w/ something like
        // PlatformConfig platformConfig = config.getPlatform(platform);
        // BLACKLIST = platformConfig.getBlacklist();
        // NSFW_ALLOWED = platformConfig.isNsfwAllowed();
        // etc
        if (platform.equals("instagram")) {
            BLACKLIST = config.getInstagram().getBlacklist();
            NSFW_ALLOWED = config.getInstagram().isNsfw_allowed();
            hours_before_duplicate_removed = config.getInstagram().getHours_before_duplicate_removed();
            CAPTION_BLACKLIST = config.getInstagram().getCaption_blacklist();
            mediaURL = StringToJson.getData(response, "url");
            caption = StringToJson.getData(response, "title");
            nsfw = Boolean.parseBoolean(StringToJson.getData(response, "nsfw"));

        } else if (platform.equals("youtube")) {
            BLACKLIST = config.getYoutube().getBlacklist();
            NSFW_ALLOWED = config.getYoutube().isNsfw_allowed();
            hours_before_duplicate_removed = config.getYoutube().getHours_before_duplicate_removed();
            CAPTION_BLACKLIST = config.getYoutube().getCaption_blacklist();
            mediaURL = StringToJson.getData(response, "url");
            caption = StringToJson.getData(response, "title");
            nsfw = Boolean.parseBoolean(StringToJson.getData(response, "nsfw"));
        } else if (platform.equals("twitter")) {
            BLACKLIST = config.getTwitter().getBlacklist();
            NSFW_ALLOWED = config.getTwitter().isNsfw_allowed();
            hours_before_duplicate_removed = config.getTwitter().getHours_before_duplicate_removed();
            CAPTION_BLACKLIST = config.getTwitter().getCaption_blacklist();
            mediaURL = StringToJson.getData(response, "url");
            caption = StringToJson.getData(response, "title");
            nsfw = Boolean.parseBoolean(StringToJson.getData(response, "nsfw"));
        } else {
            return 1; // To appease the compiler
        }

        // Download image & check aspect ratio
        if (testSize) {
            Output.debugPrint("Attempting to download image to verify aspect ratio validity");
            Image image;

            try {
                URL url = new URL(mediaURL);
                image = ImageIO.read(url);
            } catch(IOException e)  {
                Output.webhookPrint("[INSTA] Failed to download image from Reddit to check aspect ratio..."
                        + "\n\tError message: " + e, Output.RED);

                return 1;
            }

            float ratio = (float) image.getWidth(null) / image.getHeight(null);

            Output.debugPrint("Image aspect ratio is " + image.getWidth(null) + ":" + image.getHeight(null));
            if (ratio < 0.82 || ratio > 1.70) {
                Output.print("Image has invalid aspect ratio", Output.RED, true);
                return 1;
            }

        }

        // Test image validity
        Output.debugPrint("Testing if image is gif");
        if (mediaURL.contains(".gif")) { // Ensure image is not gif
            Output.print("Image is gif - x" + countattempt + " attempts", Output.RED, true);

            return 1;
        }

        // Ensure no blacklisted string in post caption
        Output.debugPrint("Testing if image caption contains blacklisted string");
        for (String word : BLACKLIST) {
            if (caption.toLowerCase().contains(word.toLowerCase())) {
                Output.print("Caption contains blacklisted string - x" + countattempt + " attempts", Output.RED, true);

                return 1;
            }
        }

        // Ensure post is not duplicate
        Output.debugPrint("Testing if image url is duplicate");
        for (String[] row : usedURLs) {
            String url = row[0];
            String timestampStr = row[1];

            long timestamp = Long.parseLong(timestampStr);

            if ((System.currentTimeMillis() - timestamp) < hours_before_duplicate_removed * 3600000) { // Test if cached url is too old to be considered duplicate
                if (caption.toLowerCase().contains(url.toLowerCase())) {
                    Output.print("Duplicate URL - x" + countattempt + " attempts", Output.RED, true);

                    return 1;
                }
            }
        }

        Output.debugPrint("Testing if image is marked as NSFW");
        if (!NSFW_ALLOWED && nsfw) { // If post is marked as NSFW and NSFW is disallowed
            Output.print("Image is marked as NSFW - x" + countattempt + " attempts", Output.RED, true);

            return 1;
        }

        Output.debugPrint("Testing if caption contains blacklisted strings to use preset caption");
        for (String word : CAPTION_BLACKLIST) { // Ensure no semi-blacklisted string in post caption. If found, discard caption but still post
            if (caption.toLowerCase().contains(word.toLowerCase())) {
                Output.print("Using fallback caption (\"" + word + "\" found) - x" + countattempt + " attempts", Output.RED, true);

                return 2;
            }
        }

        return 0;
    }
}
