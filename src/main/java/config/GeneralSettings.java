package config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class GeneralSettings {

    @JsonProperty("discord_bot_token")
    private String discordBotToken;

    @JsonProperty("webhook_url")
    private String discordWebhook;

    @JsonProperty("autostart")
    private List<String> autostart;

    @JsonProperty("restart")
    private boolean restart;

    @JsonProperty("debug_mode")
    private boolean debug_mode;

    public GeneralSettings() {}

    public String getDiscordBotToken() { return discordBotToken; }
    public void setDiscordBotToken(String discordBotToken) { this.discordBotToken = discordBotToken; }

    public String getDiscordWebhook() { return discordWebhook; }
    public void setDiscordWebhook(String discordWebhook) { this.discordWebhook = discordWebhook; }

    public List<String> getAutostart() { return autostart; }
    public void setAutostart(List<String> autostart) { this.autostart = autostart; }

    public boolean isRestart() { return restart; }
    public void setRestart(boolean restart) { this.restart = restart; }

    public boolean isDebug_mode() { return debug_mode; }
    public void setDebug_mode(boolean debug_mode) { this.debug_mode = debug_mode; }
}
