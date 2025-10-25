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

    public static int check(String response, boolean tempDisableCaption, long countattempt, List<String> usedURLs) {
        final List<String> BLACKLIST = config.getInstagram().getBlacklist();
        final boolean NSFW_ALLOWED = config.getInstagram().isNsfw_allowed();
        final List<String> CAPTION_BLACKLIST = config.getInstagram().getCaption_blacklist();
        String mediaURL = StringToJson.getData(response, "url");
        String caption = StringToJson.getData(response, "title");
        boolean nsfw = Boolean.parseBoolean(StringToJson.getData(response, "nsfw"));

        // Download image & check aspect ratio
        {
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

            if(ratio >1.8||ratio< 0.72) {
                Output.print("Image has invalid aspect ratio", Output.RED, true);

                return 1;
            }
        }


        // Test image validity
        if (mediaURL.contains(".gif")) { // Ensure image is not gif
            Output.print("Image is gif - x" + countattempt + " attempts", Output.RED, true);

            return 1;
        }

        for (String word : BLACKLIST) { // Ensure no blacklisted string in post caption
            if (caption.toLowerCase().contains(word.toLowerCase())) {
                Output.print("Caption contains blacklisted string - x" + countattempt + " attempts", Output.RED, true);

                return 1;
            }
        }

        for (String url : usedURLs) { // Ensure post is not duplicate
            if (caption.toLowerCase().contains(url.toLowerCase())) {
                Output.print("Duplicate URL - x" + countattempt + " attempts", Output.RED, true);

                return 1;
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
