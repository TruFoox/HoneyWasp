package scraper;

import main.HoneyWasp;
import services.Services;
import utils.Output;

import java.util.Objects;

public abstract class Fetch {
    static int timesFailed = 0;
    public static String fetchURL(Services service) {
        try {

            service.randIndex = Services.rand.nextInt(service.MEDIA_SOURCES.size()); // Generate random subreddit index

            service.chosenSource = service.MEDIA_SOURCES.get(service.randIndex);

            if (HoneyWasp.SCRAPE_TIKTOK) {
                Output.debugPrint(service, "Scraping from TikTok");
                String attempt = TikTok.fetch(service, service.chosenSource);
                if (!attempt.equals("fail") && !attempt.equals("fatal")) {
                    timesFailed = 0;

                    return attempt; // Success
                } else if (attempt.equals("fail")) { // Fall back to Reddit mode if failed
                    timesFailed++;
                    int maxAttempts = service.ATTEMPTS_BEFORE_TIMEOUT / 5;
                    if (timesFailed > maxAttempts) {
                        // Only runs for first service that reaches failure point
                        if (HoneyWasp.SCRAPE_TIKTOK) {
                            Output.webhookPrint(service, "[NOTIFY]Failed to post with TikTok " + maxAttempts + " times." +
                                "\n\tReverting all services to legacy scrape mode (Reddit scraping)");
                        }
                        HoneyWasp.SCRAPE_TIKTOK = false;
                    } else {
                        Output.webhookPrint(service, "Failed to post with TikTok. Temporarily posting from Reddit. Will try with TikTok " + maxAttempts +" more times");
                    }
                    return Reddit.fetch(service, service.chosenSource);
                } else { // If fatal error just exit
                    return "quit";
                }
            } else {
                Output.debugPrint(service, "Scraping from Reddit");
                return Reddit.fetch(service, service.chosenSource); // Error handling for reddit mode handled in Services
            }
        } catch (Exception e) {
            Output.webhookPrint(service, "An error occurred before the bot could retrieve the media" +
                    "\n\tReason: " + e.getMessage());
            return "quit";
        }
    }
}
