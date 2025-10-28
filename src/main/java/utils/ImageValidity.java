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

    public static int check(String response, long countattempt, List<String[]> usedURLs, boolean testSize) {
        final List<String> BLACKLIST = config.getInstagram().getBlacklist();
        final boolean NSFW_ALLOWED = config.getInstagram().isNsfw_allowed();
        final long hours_before_duplicate_removed = config.getInstagram().getHours_before_duplicate_removed();
        final List<String> CAPTION_BLACKLIST = config.getInstagram().getCaption_blacklist();
        String mediaURL = StringToJson.getData(response, "url");
        String caption = StringToJson.getData(response, "title");
        boolean nsfw = Boolean.parseBoolean(StringToJson.getData(response, "nsfw"));

        // Download image & check aspect ratio
        if (testSize) {
            Image image;

            try {
                URL url = new URL(mediaURL);
                image = ImageIO.read(url);
            } catch(IOException e)  {
                Output.webhookPrint("[INSTA] Failed to download image from Reddit to check aspect ratio..."
                        + "\n\tError message: " + e, Output.RED);

                return 1;
            }

            double ratio = (double) image.getWidth(null) / image.getHeight(null);

            if (ratio > 0.8 || ratio < 1.75) { // Test aspect ratio
                Output.print("Image has invalid aspect ratio", Output.RED, true);

                return 1;
            }
        }

        // Test image validity
        if (mediaURL.contains(".gif")) { // Ensure image is not gif
            Output.print("Image is gif - x" + countattempt + " attempts", Output.RED, true);

            return 1;
        }

        // Ensure no blacklisted string in post caption
        for (String word : BLACKLIST) {
            if (caption.toLowerCase().contains(word.toLowerCase())) {
                Output.print("Caption contains blacklisted string - x" + countattempt + " attempts", Output.RED, true);

                return 1;
            }
        }

        // Ensure post is not duplicate
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


        if (!NSFW_ALLOWED && nsfw) { // If post is marked as NSFW and NSFW is disallowed
            Output.print("Image is marked as NSFW - x" + countattempt + " attempts", Output.RED, true);

            return 1;
        }


        for (String word : CAPTION_BLACKLIST) { // Ensure no semi-blacklisted string in post caption. If found, discard caption but still post
            if (caption.toLowerCase().contains(word.toLowerCase())) {
                Output.print("Using fallback caption - x" + countattempt + " attempts", Output.RED, true);

                return 2;
            }
        }

        return 0;
    }
}
