package config;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({ // Force this order when writing
        "refresh_token",
        "client_secret",
        "client_id",
        "autostart",
        "auto_post_mode",
        "minutes_between_posts",
        "attempts_before_timeout",
        "subreddits",
        "blacklist",
        "duplicates_allowed",
        "nsfw_allowed",
        "use_reddit_caption",
        "caption_blacklist",
        "hours_before_duplicate_removed",
        "audio_enabled",
        "caption",
        "hashtags"
})

public class YoutubeSettings implements ConfigSettings,Refreshable {
    private String refresh_token;
    private String client_secret;
    private String client_id;
    private boolean auto_post_mode;
    private int minutes_between_posts;
    private int attempts_before_timeout;
    private List<String> subreddits;
    private List<String> blacklist;
    private boolean duplicates_allowed;
    private boolean audio_enabled;
    private boolean nsfw_allowed;
    private boolean use_reddit_caption;
    private int hours_before_duplicate_removed;
    private List<String> caption_blacklist;
    private boolean autostart;
    private String caption;
    private String hashtags;

    public YoutubeSettings() {}

    // Getters and setters
    public String getRefresh_token() { return refresh_token; }
    public void setRefresh_token(String refresh_token) { this.refresh_token = refresh_token; }

    public String getClient_secret() { return client_secret; }
    public void setClient_secret(String client_secret) { this.client_secret = client_secret; }

    public String getClient_id() { return client_id; }
    public void setClient_id(String client_id) { this.client_id = client_id; }

    public boolean isAuto_post_mode() { return auto_post_mode; }
    public void setAuto_post_mode(boolean auto_post_mode) { this.auto_post_mode = auto_post_mode; }

    public boolean isAutostart() { return autostart; }
    public void setAutostart(boolean autostart) { this.autostart = autostart; }

    public int getMinutes_between_posts() { return minutes_between_posts; }
    public void setMinutes_between_posts(int minutes_between_posts) { this.minutes_between_posts = minutes_between_posts; }

    public int getHours_before_duplicate_removed() { return hours_before_duplicate_removed; }
    public void setHours_before_duplicate_removed(int hours_before_duplicate_removed) {this.hours_before_duplicate_removed = hours_before_duplicate_removed;}

    public int getAttempts_before_timeout() { return attempts_before_timeout; }
    public void setAttempts_before_timeout(int attempts_before_timeout) { this.attempts_before_timeout = attempts_before_timeout; }

    public List<String> getSubreddits() { return subreddits; }
    public void setSubreddits(List<String> subreddits) { this.subreddits = subreddits; }

    public List<String> getBlacklist() { return blacklist; }
    public void setBlacklist(List<String> blacklist) { this.blacklist = blacklist; }

    public boolean isDuplicates_allowed() { return duplicates_allowed; }
    public void setDuplicates_allowed(boolean duplicates_allowed) { this.duplicates_allowed = duplicates_allowed; }

    public boolean isAudio_enabled() { return audio_enabled; }
    public void setAudio_enabled(boolean audio_enabled) { this.audio_enabled = audio_enabled; }

    public boolean isNsfw_allowed() { return nsfw_allowed; }
    public void setNsfw_allowed(boolean nsfw_allowed) { this.nsfw_allowed = nsfw_allowed; }

    public boolean isUse_reddit_caption() { return use_reddit_caption; }
    public void setUse_reddit_caption(boolean use_reddit_caption) { this.use_reddit_caption = use_reddit_caption; }

    public List<String> getCaption_blacklist() { return caption_blacklist; }
    public void setCaption_blacklist(List<String> caption_blacklist) { this.caption_blacklist = caption_blacklist; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getHashtags() { return hashtags; }
    public void setHashtags(String hashtags) { this.hashtags = hashtags; }
    @Override
    public Object get(String setting) {
        return switch (setting.toLowerCase()) {
            case "refresh_token" -> getRefresh_token();
            case "client_secret" -> getClient_secret();
            case "client_id" -> getClient_id();
            case "auto_post_mode" -> isAuto_post_mode();
            case "autostart" -> isAutostart();
            case "minutes_between_posts" -> getMinutes_between_posts();
            case "hours_before_duplicate_removed" -> getHours_before_duplicate_removed();
            case "attempts_before_timeout" -> getAttempts_before_timeout();
            case "subreddits" -> getSubreddits();
            case "blacklist" -> getBlacklist();
            case "duplicates_allowed" -> isDuplicates_allowed();
            case "audio_enabled" -> isAudio_enabled();
            case "nsfw_allowed" -> isNsfw_allowed();
            case "use_reddit_caption" -> isUse_reddit_caption();
            case "caption_blacklist" -> getCaption_blacklist();
            case "caption" -> getCaption();
            case "hashtags" -> getHashtags();
            default ->
                    throw new IllegalArgumentException(
                            "Unknown setting: " + setting
                    );
        };
    }
    @Override
    public void set(String setting, String newValue) {
        switch (setting.toLowerCase()) {
            case "refresh_token" -> setRefresh_token(newValue);
            case "client_secret" -> setClient_secret(newValue);
            case "client_id" -> setClient_id(newValue);
            case "auto_post_mode" -> setAuto_post_mode(Boolean.parseBoolean(newValue));
            case "autostart" -> setAutostart(Boolean.parseBoolean(newValue));
            case "minutes_between_posts" -> setMinutes_between_posts(Integer.parseInt(newValue));
            case "hours_before_duplicate_removed" -> setHours_before_duplicate_removed(Integer.parseInt(newValue));
            case "attempts_before_timeout" -> setAttempts_before_timeout(Integer.parseInt(newValue));
            case "subreddits" -> setSubreddits(List.of(newValue.split(",")));
            case "blacklist" -> setBlacklist(List.of(newValue.split(",")));
            case "duplicates_allowed" -> setDuplicates_allowed(Boolean.parseBoolean(newValue));
            case "audio_enabled" -> setAudio_enabled(Boolean.parseBoolean(newValue));
            case "nsfw_allowed" -> setNsfw_allowed(Boolean.parseBoolean(newValue));
            case "use_reddit_caption" -> setUse_reddit_caption(Boolean.parseBoolean(newValue));
            case "caption_blacklist" -> setCaption_blacklist(List.of(newValue.split(",")));
            case "caption" -> setCaption(newValue);
            case "hashtags" -> setHashtags(newValue);
            default ->
                    throw new IllegalArgumentException(
                            "Unknown setting: " + setting
                    );
        }
    }
}
