package scraper;

import main.HoneyWasp;
import services.Services;
import utils.Output;

public abstract class Fetch {
    public static String fetchURL(Services service) {
        try {

            service.randIndex = Services.rand.nextInt(service.MEDIA_SOURCES.size()); // Generate random subreddit index

            service.chosenSource = service.MEDIA_SOURCES.get(service.randIndex);

            if (HoneyWasp.SCRAPE_TIKTOK) {
                Output.debugPrint(service, "Scraping from TikTok");
                return TikTok.fetch(service, service.chosenSource);
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
