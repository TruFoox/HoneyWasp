package config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class GeneralSettings implements ConfigSettings {

    @JsonProperty("discord_bot_token")
    private String discordBotToken;

    @JsonProperty("webhook_url")
    private String discordWebhook;

    @JsonProperty("account_name")
    private String accountName;

    @JsonProperty("scrape_tiktok")
    private boolean scrapeTiktok;

    @JsonProperty("ping_on_error")
    private boolean pingOnError;

    @JsonProperty("generate_twitter_container")
    private boolean generateTwitterContainer;

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

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public boolean isScrapeTiktok() { return scrapeTiktok; }
    public void setScrapeTiktok(boolean scrapeTiktok) { this.scrapeTiktok = scrapeTiktok; }

    public boolean isGenerateTwitterContainer() { return generateTwitterContainer; }
    public void setGenerateTwitterContainer(boolean generateTwitterContainer) { this.generateTwitterContainer = generateTwitterContainer; }

    public boolean isPingOnError() { return pingOnError; }
    public void setPingOnError(boolean pingOnError) { this.pingOnError = pingOnError; }

    public boolean isProxies() { return proxies; }
    public void setProxies(boolean proxies) { this.proxies = proxies; }

    public boolean isRestart() { return restart; }
    public void setRestart(boolean restart) { this.restart = restart; }

    public boolean isDebug_mode() { return debug_mode; }
    public void setDebug_mode(boolean debug_mode) { this.debug_mode = debug_mode; }


    @Override
    public Object get(String setting) {
        return switch (setting.toLowerCase()) {
            case "discord_bottoken" -> getDiscordBotToken();
            case "discord_webhook" -> getDiscordWebhook();
            case "account_name" -> getAccountName();
            case "scrape_tiktok" -> isScrapeTiktok();
            case "generate_twitter_container" -> isGenerateTwitterContainer();
            case "ping_on_error" -> isPingOnError();
            case "proxies" -> isProxies();
            case "restart" -> isRestart();
            case "debug_mode" -> isDebug_mode();
            default -> throw new IllegalArgumentException("Unknown setting: " + setting);
        };
    }
    @Override
    public void set(String setting, String newValue) {
        switch (setting.toLowerCase()) {
            case "discord_bot_token" -> setDiscordBotToken(newValue);
            case "discord_webhook" -> setDiscordWebhook(newValue);
            case "account_name" -> setAccountName(newValue);
            case "scrape_tiktok" -> setScrapeTiktok(Boolean.parseBoolean(newValue));
            case "generate_twitter_container" -> setGenerateTwitterContainer(Boolean.parseBoolean(newValue));
            case "ping_on_error" -> setPingOnError(Boolean.parseBoolean(newValue));
            case "proxies" -> setProxies(Boolean.parseBoolean(newValue));
            case "restart" -> setRestart(Boolean.parseBoolean(newValue));
            case "debug_mode" -> setDebug_mode(Boolean.parseBoolean(newValue));
            default -> throw new IllegalArgumentException("Unknown setting: " + setting);
        }
    }
}
