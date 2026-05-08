package config;

import java.util.List;

public interface PlatformSettings {
    boolean isAuto_post_mode();
    int getTime_between_posts();
    int getAttempts_before_timeout();
    List<String> getSubreddits();
    List<String> getBlacklist();
    boolean isDuplicates_allowed();
    boolean isNsfw_allowed();
    boolean isUse_reddit_caption();
    List<String> getCaption_blacklist();
    int getHours_before_duplicate_removed();
    boolean isAudio_enabled();
    String getCaption();
    String getHashtags();
}