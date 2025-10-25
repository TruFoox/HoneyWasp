package config;

import java.util.List;

public class YoutubeSettings {
    private String refresh_token;
    private String client_secret;
    private String client_id;
    private String post_mode;
    private int time_between_posts;
    private int attempts_before_timeout;
    private List<String> subreddits;
    private List<String> blacklist;
    private boolean duplicates_allowed;
    private boolean nsfw_allowed;
    private boolean use_reddit_caption;
    private List<String> caption_blacklist;
    private String caption;
    private String description;

    public YoutubeSettings() {}

    // Getters and setters
    public String getRefresh_token() { return refresh_token; }
    public void setRefresh_token(String refresh_token) { this.refresh_token = refresh_token; }

    public String getClient_secret() { return client_secret; }
    public void setClient_secret(String client_secret) { this.client_secret = client_secret; }

    public String getClient_id() { return client_id; }
    public void setClient_id(String client_id) { this.client_id = client_id; }

    public String getPost_mode() { return post_mode; }
    public void setPost_mode(String post_mode) { this.post_mode = post_mode; }

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

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
