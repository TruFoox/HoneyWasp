package config;

import java.util.List;

public class InstagramSettings {
    private String api_key;
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
    private String caption;
    private String hashtags;

    // Default constructor
    public InstagramSettings() {}

    // Getters and setters
    public String getApi_key() { return api_key; }
    public void setApi_key(String api_key) { this.api_key = api_key; }

    public String getPost_mode() { return post_mode; }
    public void setPost_mode(String post_mode) { this.post_mode = post_mode; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public int getTime_between_posts() { return time_between_posts; }
    public void setTime_between_posts(int time_between_posts) { this.time_between_posts = time_between_posts; }

    public int getAttempts_before_timeout() { return attempts_before_timeout; }
    public void setAttempts_before_timeout(int attempts_before_timeout) { this.attempts_before_timeout = attempts_before_timeout; }

    public List<String> getSubreddits() { return subreddits; }
    public void setSubreddits(List<String> subreddits) { this.subreddits = subreddits; }

    public List<String> getBlacklist() { return blacklist; }
    public void setBlacklist(List<String> blacklist) { this.blacklist = blacklist; }

    public boolean isDuplicates_allowed() { return duplicates_allowed; }
    public void setDuplicates_allowed(boolean duplicates_allowed) { this.duplicates_allowed = duplicates_allowed; }

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
