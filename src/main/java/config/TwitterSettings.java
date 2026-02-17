package config;

import java.util.List;

public class TwitterSettings {
    private String consumer_key;
    private String client_secret;
    private String refresh_token;
    private String post_mode;
    private String format;
    private int time_between_posts;
    private int attempts_before_timeout;
    private List<String> subreddits;
    private List<String> blacklist;
    private boolean duplicates_allowed;
    private boolean nsfw_allowed;
    private boolean use_reddit_caption;
    private List<String> caption_blacklist;
    private long hours_before_duplicate_removed;
    private boolean audio_enabled;
    private String caption;
    private String hashtags;

    // Default constructor
    public TwitterSettings() {}

    // Getters and setters
    public String getConsumer_key() { return consumer_key; }
    public void setConsumer_key(String api_key) { this.consumer_key = consumer_key; }

    public String getClient_secret() { return client_secret; }
    public void setClient_secret(String client_secret) { this.client_secret = client_secret; }

    public String getRefresh_token() { return refresh_token; }
    public void setRefresh_token(String refresh_token) { this.refresh_token = refresh_token; }

    public String getPost_mode() { return post_mode; }
    public void setPost_mode(String post_mode) { this.post_mode = post_mode; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public int getTime_between_posts() { return time_between_posts; }
    public void setTime_between_posts(int time_between_posts) { this.time_between_posts = time_between_posts; }

    public int getAttempts_before_timeout() { return attempts_before_timeout; }
    public void setAttempts_before_timeout(int attempts_before_timeout) { this.attempts_before_timeout = attempts_before_timeout; }

    public long getHours_before_duplicate_removed() { return hours_before_duplicate_removed; }
    public void setHours_before_duplicate_removed(long hours_before_duplicate_removed) {this.hours_before_duplicate_removed = hours_before_duplicate_removed;}

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

}
