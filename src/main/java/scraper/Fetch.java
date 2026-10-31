package scraper;

import main.HoneyWasp;
import services.Services;
import utils.Output;

public abstract class Fetch {
    public static String fetchURL(Services service) {
        try {
            String hashtag = "memes";
            if (HoneyWasp.SCRAPE_TIKTOK) {
                return TikTok.fetch(service, hashtag);
            } else {
                return Reddit.fetch(service, hashtag);
            }
        } catch (Exception _) {
            return "";
        }
    }
}
