package scraper;

import main.HoneyWasp;
import services.Services;
import utils.Output;

import java.util.Objects;

public abstract class Fetch {
    public static String fetchURL(Services service) {
        try {

            service.randIndex = Services.rand.nextInt(service.MEDIA_SOURCES.size()); // Generate random subreddit index

            service.chosenSource = service.MEDIA_SOURCES.get(service.randIndex);

            if (HoneyWasp.SCRAPE_TIKTOK) {
                Output.debugPrint(service, "Scraping from TikTok");
                String attempt = TikTok.fetch(service, service.chosenSource);
                if (!attempt.equals("fail")) {
                    return attempt; // Success
                } else { // Fall back to Reddit mode if failed
                    HoneyWasp.SCRAPE_TIKTOK = false;
                    Output.webhookPrint(service, "Reverting to legacy scrape mode (Reddit scraping)");
                    return Reddit.fetch(service, service.chosenSource);
                }
            } else {
                Output.debugPrint(service, "Scraping from Reddit");
                return Reddit.fetch(service, service.chosenSource);
            }
        } catch (Exception e) {
            Output.webhookPrint(service, "An error occurred before the bot could retrieve the media" +
                    "\n\tReason: " + e.getMessage());
            return "quit";
        }
    }
}
