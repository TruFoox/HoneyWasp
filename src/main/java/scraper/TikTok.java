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
import com.microsoft.playwright.options.WaitForSelectorState;
import main.HoneyWasp;
import scraper.Fetch;
import scraper.Reddit;
import services.Services;
import utils.Output;
import utils.Sleep;


public class TikTok { // Heavily used https://github.com/n-ochs/tt-proxy as an example (Though im pretty sure that repo is 100% vibecoded lol)
    static List<Map<String, String>> allVideos = new ArrayList<>(); // list of all video urls on page
    static Set<String> collectedURLs = new HashSet<>(); // prevents scraper from collecting same video multiple times
    static String currentHashtag = "";

    protected static synchronized String fetch(Services service, String chosenHashtag) throws Exception {
        if (!currentHashtag.equalsIgnoreCase(chosenHashtag)) { // Logic breaks whenever the hashtag changes so wipe the lists so they recollect
            Output.debugPrint(service, "New hashtag detected, clearing previous internal cache");
            allVideos.clear();
            collectedURLs.clear();
            service.nonDuplicateTikTokVideos.clear();
            currentHashtag = chosenHashtag;
        }

        try {
            if (service.nonDuplicateTikTokVideos.isEmpty()) {
                Playwright playwright;

                try {
                    playwright = Playwright.create();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }

                Process chrome;
                try {
                    String chrome_path = getChromePath();
                    if (chrome_path.isBlank()) {return "retry";}

                    Output.debugPrint(service, "Chrome path: " + chrome_path);

                    chrome = new ProcessBuilder( // Open chrome such that i can remotely control it with playwright
                        getChromePath(),
                        "--remote-debugging-port=9222",
                        "--user-data-dir=" + Paths.get("tiktok-profile").toAbsolutePath(),
                        "--no-first-run",
                        "--no-default-browser-check",
                        "https://www.tiktok.com/tag/" + chosenHashtag
                    ).start();
                } catch (Exception e) {
                    Output.webhookPrint(service, "Failed to open chrome" +
                            "\n\tReason: " + e.getMessage(), Output.RED);
                    return "fail";
                }


                // For god knows what reason this much simpler implementation stopped working overnight
                /*Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false)); // I currently have this set to use chromium

                Page page = browser.newPage();
                page.navigate("https://www.tiktok.com/tag/memes");*/

                Page page;
                Browser browser = null;
                try {

                    for (int i = 0; i < 30; i++) {
                        try {
                            if (!chrome.isAlive()) {
                                Output.webhookPrint(service, "Chrome exited with code: " + chrome.exitValue(), Output.RED);
                                return "fail";
                            }

                            browser = playwright.chromium()
                                    .connectOverCDP("http://127.0.0.1:9222");
                            break;

                        } catch (Exception _) {
                            Thread.sleep(1000);
                        }
                    }

                    if (browser == null) {
                        Output.webhookPrint(service, "Chrome failed to open in time");
                        return "fail";
                    }

                    page = browser.contexts().get(0).pages().get(0);
                    page.waitForTimeout(5000);
                } catch (Exception e) {
                    Output.webhookPrint(service, "Failed to manage chrome" +
                            "\n\tReason: " + e.getMessage(), Output.RED);
                    return "fail";
                }

                // Wait up to 15 seconds for at least one video link to appear
                try {
                    page.locator("a[href*='/video/']").first().waitFor(
                            new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(15000)
                    );

                } catch (PlaywrightException e) {
                    Output.webhookPrint(service, "TikTok page loaded, but no videos appeared. Skipping this attempt...", Output.RED);

                    browser.close();
                    playwright.close();
                    chrome.destroy();

                    return "skip";
                }

                while (service.nonDuplicateTikTokVideos.isEmpty()) { // Keep scrolling down until we find something new

                    // Extract all currently loaded video URLs and captions. Gonna be real this json is AI generated
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

                        boolean throwOutThisPost = false;


                        // Ensure post is not duplicate
                        Output.debugPrint(service, "Testing if image url is duplicate");
                        for (String[] row : service.usedURLs) {
                            if (row[0].equalsIgnoreCase(videoURL)) {
                                throwOutThisPost = !service.DUPLICATES_ALLOWED && Long.parseLong(row[1]) > System.currentTimeMillis() && service.HOURS_BEFORE_DUPLICATES_REMOVED != 0;
                                break;
                            }
                        }

                        if (!throwOutThisPost) {
                            service.nonDuplicateTikTokVideos.add(video);
                        }
                    }

                    Output.debugPrint(service, "Videos collected: " + allVideos.size());


                    // Scroll to the bottom
                    page.evaluate("window.scrollBy(0, window.innerHeight)");

                    // Give TikTok time to load more videos
                    page.waitForTimeout(2500);
                }

                Output.debugPrint(service, "Finished collecting " + allVideos.size() + " videos.");

                browser.close();
                playwright.close();
                chrome.destroy();

            }

            if (service.nonDuplicateTikTokVideos.isEmpty()) { // All collected videos have already been used, clear the collection so the next fetch recollects videos.
                allVideos.clear();
                collectedURLs.clear();

                return fetch(service, chosenHashtag);
            }

            Map<String, String> video = service.nonDuplicateTikTokVideos.removeFirst(); // Gets first video in list and remove it from the list at the same time
            service.postURL = video.get("url");

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


            Matcher captionMatcher = Pattern.compile("\"desc\":\"(.*?)\",\"createTime\"").matcher(responseBody);
            service.caption = captionMatcher.find() ? captionMatcher.group(1).replaceAll("#\\S+\\s*", "").trim() : ""; // Janky ass way of grabbing the caption of the post and removing hashtags

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
                        Output.print(service, "Using default (Non-TikTok) caption (\"" + word.toLowerCase() + "\" found) - x" + service.countAttempt + " attempts", Output.RED, true);
                        service.caption = service.FALLBACK_CAPTION;
                    }
                }
            }
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

            Output.print(service, "Downloading video from TikTok...", Output.YELLOW, true);
            if (videoResponse.statusCode() == 200) {
                service.fileDir = "./cache/" + service.name.toLowerCase() + "/temp.mp4";
                Files.write(Path.of("cache", service.name.toLowerCase(), "temp.mp4"), videoResponse.body());
            }
            Output.print(service, "Successfully downloaded video from TikTok", Output.YELLOW, true);

            return mediaUrl;
        } catch (Exception e) {
            Output.webhookPrint(service, "Failed to scrape from TikTok" +
                    "\n\tReason: " + e.getMessage(), Output.RED);
            return "fail"; // Fat failure
        }
    }

    private static String getChromePath() throws Exception { // Admittedly this function is AI generated because I just couldnt be bothered
        String os = System.getProperty("os.name").toLowerCase();
        List<Path> candidates = new ArrayList<>();

        if (os.contains("win")) {

            // Check Windows Registry
            for (String key : List.of("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\App Paths\\chrome.exe", "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\App Paths\\chrome.exe")) {
                try {
                    Process p = new ProcessBuilder(
                            "reg", "query", key, "/ve"
                    ).redirectErrorStream(true).start();

                    String output = new String(
                            p.getInputStream().readAllBytes()
                    );

                    p.waitFor();

                    Matcher m = Pattern.compile("(?im)^\\s*\\(Default\\)\\s+REG_SZ\\s+(.+?)\\s*$").matcher(output);

                    if (m.find()) {
                        candidates.add(Path.of(m.group(1).trim()));
                    }
                } catch (Exception ignored) {}
            }

            // Common Windows installation locations
            for (String variable : List.of("PROGRAMFILES", "PROGRAMFILES(X86)", "LOCALAPPDATA")) {
                String directory = System.getenv(variable);

                if (directory != null) {
                    candidates.add(Path.of(directory, "Google", "Chrome", "Application", "chrome.exe"));

                    candidates.add(Path.of(directory, "Google", "Chrome", "Application", "chrome.exe"));
                }
            }

        } else if (os.contains("linux")) {

            for (String name : List.of(
                    "google-chrome",
                    "google-chrome-stable",
                    "chromium",
                    "chromium-browser"
            )) {
                try {
                    Process p = new ProcessBuilder("which", name).start();

                    String output = new String(p.getInputStream().readAllBytes()).trim();

                    p.waitFor();

                    if (p.exitValue() == 0 && !output.isEmpty()) {
                        candidates.add(Path.of(output));
                    }
                } catch (Exception ignored) {}
            }

            candidates.addAll(List.of(
                    Path.of("/usr/bin/google-chrome"),
                    Path.of("/usr/bin/google-chrome-stable"),
                    Path.of("/usr/bin/chromium"),
                    Path.of("/usr/bin/chromium-browser"),
                    Path.of("/snap/bin/chromium")
            ));

        } else if (os.contains("mac")) {

            candidates.add(Path.of("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"));

            candidates.add(Path.of(System.getProperty("user.home"), "Applications/Google Chrome.app/Contents/MacOS/Google Chrome"));

        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + os);
        }

        for (Path path : candidates) {
            if (Files.isRegularFile(path) && (os.contains("win") || Files.isExecutable(path))) {
                return path.toAbsolutePath().toString();
            }
        }

        Output.webhookPrint(null, "Google Chrome could not be located", Output.RED);
        HoneyWasp.SCRAPE_TIKTOK = false;
        return "";
    }
}