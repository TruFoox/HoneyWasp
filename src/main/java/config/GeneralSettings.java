package config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class GeneralSettings implements ConfigSettings {

    @JsonProperty("discord_bot_token")
    private String discordBotToken;

    @JsonProperty("webhook_url")
    private String discordWebhook;

    @JsonProperty("proxies")
    private boolean proxies;

    @JsonProperty("restart")
    private boolean restart;

    @JsonProperty("debug_mode")
    private boolean debug_mode;

    public GeneralSettings() {}

    public String getDiscordBotToken() { return discordBotToken; }
    public void setDiscordBotToken(String discordBotToken) { this.discordBotToken = discordBotToken; }

    public String getDiscordWebhook() { return discordWebhook; }
    public void setDiscordWebhook(String discordWebhook) { this.discordWebhook = discordWebhook; }

    public boolean isProxies() { return proxies; }
    public void setProxies(boolean proxies) { this.proxies = proxies; }

    public boolean isRestart() { return restart; }
    public void setRestart(boolean restart) { this.restart = restart; }

    public boolean isDebug_mode() { return debug_mode; }
    public void setDebug_mode(boolean debug_mode) { this.debug_mode = debug_mode; }


    @Override
    public Object get(String setting) {
        return switch (setting.toLowerCase()) {
            case "discordbottoken" -> getDiscordBotToken();
            case "discordwebhook" -> getDiscordWebhook();
            case "proxies" -> isProxies();
            case "restart" -> isRestart();
            case "debug_mode" -> isDebug_mode();
            default ->
                    throw new IllegalArgumentException(
                            "Unknown setting: " + setting
                    );
        };
    }
    @Override
    public void set(String setting, String newValue) {
        switch (setting.toLowerCase()) {
            case "discordbottoken" -> setDiscordBotToken(newValue);
            case "discordwebhook" -> setDiscordWebhook(newValue);
            case "proxies" -> setProxies(Boolean.parseBoolean(newValue));
            case "restart" -> setRestart(Boolean.parseBoolean(newValue));
            case "debug_mode" -> setDebug_mode(Boolean.parseBoolean(newValue));
            default ->
                    throw new IllegalArgumentException(
                            "Unknown setting: " + setting
                    );
        }
    }
}
