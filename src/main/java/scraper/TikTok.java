package scraper;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.playwright.*;
import scraper.Fetch;
import scraper.Reddit;
import services.Services;
import utils.Output;


public class TikTok { // Heavily used https://github.com/n-ochs/tt-proxy as an example (Though im pretty sure that repo is 100% vibecoded lol)
    static List<Map<String, String>> allVideos = new ArrayList<>();
    static Set<String> collectedURLs = new HashSet<>();
    static String currentHashtag = "";
    static int maxVideos = 100; // Max videos to scrape at once
    static int videosAlreadyScanned = 0; // Used to increase number of maximum videos if entire list is exhausted

    protected static synchronized String fetch(Services service, String chosenHashtag) throws Exception {
        if (!currentHashtag.equalsIgnoreCase(chosenHashtag)) { // Logic breaks whenever the hashtag changes so wipe the lists so they recollect
            Output.debugPrint(service, "New hashtag detected, clearing previous internal cache");
            allVideos.clear();
            collectedURLs.clear();
            service.nonDuplicateTikTokVideos.clear();
            currentHashtag = chosenHashtag;
            videosAlreadyScanned = 0;
        }

        try {
            if (service.nonDuplicateTikTokVideos.isEmpty()) {
                Playwright playwright = Playwright.create();
                Process chrome;
                try {
                    chrome = new ProcessBuilder( // Open chrome such that i can remotely control it with playwright
                            "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                            "--remote-debugging-port=9222",
                            "--user-data-dir=" + Paths.get("tiktok-profile").toAbsolutePath(),
                            "--no-first-run",
                            "--no-default-browser-check",
                            "https://www.tiktok.com/tag/memes"
                    ).start();
                } catch (Exception _) {
                    return "quit";
                }


                // For god knows what reason this much simpler implementation stopped working overnight
                /*Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false)); // I currently have this set to use chromium

                Page page = browser.newPage();
                page.navigate("https://www.tiktok.com/tag/memes");*/

                Browser browser = null;

                for (int i = 0; i < 30; i++) { // Wait until chrome opens
                    try {
                        browser = playwright.chromium()
                                .connectOverCDP("http://127.0.0.1:9222");
                        break;
                    } catch (Exception e) {
                        Thread.sleep(500);
                    }
                }

                Page page = browser.contexts().get(0).pages().get(0);
                page.waitForTimeout(5000);

                int previousCount = allVideos.size();

                while (allVideos.size() < maxVideos + videosAlreadyScanned) { // Keep scrolling down until we find something new

                    // Extract all currently loaded video URLs and captions
                    String json = (String) page.locator("a[href]").evaluateAll("""
                        elements => JSON.stringify(
                            elements
                                .map(a => ({
                                    url: a.href,
                                    text: a.innerText.trim().slice(0, 100)
                                }))
                                .filter(x => x.url.includes('/video/'))
                        )
                        """);

                    Type type = new TypeToken<List<Map<String, String>>>() {}.getType();

                    List<Map<String, String>> foundVideos = new Gson().fromJson(json, type);

                    // Add newly discovered videos
                    for (Map<String, String> video : foundVideos) {
                        String videoURL = video.get("url");

                        if (videoURL == null || !collectedURLs.add(videoURL)) {
                            continue;
                        }

                        allVideos.add(video);

                        boolean duplicate = false;
                        for (String[] row : service.usedURLs) {
                            if (row[0].equalsIgnoreCase(videoURL)) {
                                duplicate = !service.DUPLICATES_ALLOWED && Long.parseLong(row[1]) > System.currentTimeMillis() && service.HOURS_BEFORE_DUPLICATES_REMOVED != 0;
                                break;
                            }
                        }

                        if (duplicate) {
                            videosAlreadyScanned++; // Increase sliding target for future iterations
                        } else {
                            service.nonDuplicateTikTokVideos.add(video);
                        }
                    }

                    Output.debugPrint(service, "Videos collected: " + allVideos.size());


                    // Scroll to the bottom
                    page.evaluate("window.scrollTo(0, document.body.scrollHeight)");

                    // Give TikTok time to load more videos
                    page.waitForTimeout(2500);
                }

                Output.debugPrint(service, "Finished collecting " + allVideos.size() + " videos.");

                browser.close();
                playwright.close();
                chrome.destroy();

            }

            if (service.nonDuplicateTikTokVideos.isEmpty()) {
                // All collected videos have already been used, clear the collection so the next fetch recollects videos.
                allVideos.clear();
                collectedURLs.clear();

                return fetch(service, chosenHashtag);
            }

            Map<String, String> video = service.nonDuplicateTikTokVideos.removeFirst(); // Get first video in list and remove it from the list at the same time
            service.postURL = video.get("url");
            service.caption = video.get("text");

            // Makes normal HTTP GET request because we don't need Playwright for ripping data
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request  = HttpRequest.newBuilder()
                    .uri(URI.create(service.postURL))
                    .GET()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " // Fake-ass user agent to fool tiktok
                            + "AppleWebKit/537.36 (KHTML, like Gecko) "
                            + "Chrome/118.0.0.0 Safari/537.36"
                    ).build();;

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            String cookies = response.headers() // TikTok dumps a cookie to make sure you're a real browser. Obviously, we aren't one, so we need to rip that shit
                    .allValues("Set-Cookie")
                    .stream()
                    .map(c -> c.split(";", 2)[0])
                    .collect(java.util.stream.Collectors.joining("; "));

            // Fetches the url of the video (could use downloadAddr but i think it has a watermark)
            Matcher matcher = Pattern.compile("\"playAddr\":\"(.*?)\"").matcher(responseBody); // Matcher is apparently an easy way to fetch part of the data

            if (!matcher.find()) { // Kinda screwed if it ain't there
                throw new Exception("No playAddr found: " + responseBody);
            }

            // Prob a better way to clean the URL but idfc
            String mediaUrl = matcher.group(1).replaceAll("\\\\u002F", "/").replaceAll("\\\\u002f", "/").replaceAll("\\\\u0026", "&"); // needs "\\\\" instead of "\\" cuz both regex and java itself replaces \\ with \

            Output.debugPrint(service, "Media URL: " + mediaUrl);

            request = HttpRequest.newBuilder() // Essentially ripped directly from that other repo
                    .uri(URI.create(mediaUrl))
                    .header("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/118.0.0.0 Safari/537.36")
                    .header("Referer", "https://www.tiktok.com/")
                    .header("Accept", "*/*")
                    .header("Cookie", cookies)
                    .GET()
                    .build();

            HttpResponse<byte[]> videoResponse = client.send(request, HttpResponse.BodyHandlers.ofByteArray()); // byte cuz we want the raw data

            Output.debugPrint(service, "Video status: " + videoResponse.statusCode());

            if (videoResponse.statusCode() == 200) {
                Files.write(Path.of("cache", String.valueOf(service.name), "temp.mp4"), videoResponse.body());
            }

            Thread.sleep(30000);

            return mediaUrl;
        } catch (Exception e) {
            Output.debugPrint(service, e.getMessage());
            return "fail"; // Fat failure
        }
    }
}