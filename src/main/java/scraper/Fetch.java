package scraper;

import services.Services;
import utils.Output;

public abstract class Fetch {
    public static String fetchURL(Services service) {
        try {
            boolean tiktokscraping = false;
            String hashtag = "memes";
            if (tiktokscraping) {
                return TikTok.fetch(service, hashtag);
            } else {
                return Reddit.fetch(service, hashtag);
            }
        } catch (Exception _) {
            return "";
        }
    }
}
