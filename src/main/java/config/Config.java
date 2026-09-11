package config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.HoneyWasp;
import utils.Output;

import java.io.File;
import java.io.IOException;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.*;

@JsonAutoDetect(
        fieldVisibility = ANY,
        getterVisibility = NONE,
        isGetterVisibility = NONE
)

public class Config {
    private static Config instance;

    @JsonProperty("General_Settings")
    private GeneralSettings General_Settings;

    @JsonProperty("Instagram_Settings")
    private InstagramSettings Instagram_Settings;

    @JsonProperty("Youtube_Settings")
    private YoutubeSettings Youtube_Settings;

    @JsonProperty("Tiktok_Settings")
    private TiktokSettings Tiktok_Settings;

    //@JsonProperty("Twitter_Settings")
    //private TwitterSettings Twitter_Settings;

    private Config() {}

    public static Config getInstance() throws Exception {

        if (instance == null) { // If config not already loaded
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Create jackson reader, set to not crash immediately if invalid config

            instance = mapper.readValue(new File("config.json"), Config.class); // read config
        }
        return instance;
    }

    public void saveConfig() {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT); // Tell jackson to pretty print json

        try {
            mapper.writeValue(new File("config.json"), this);
        } catch (IOException e) {
            System.err.println("Could not save config: " + e.getMessage());
        }

        try {
            HoneyWasp.config = mapper.readValue(new File("config.json"), Config.class); // Attempt to refresh current config instance
        } catch (Exception e) {
            Output.webhookPrint(null, "Failed to update config value" +
                    "\n\tReason: " + e.getMessage());
        }
    }

    public ConfigSettings get(String settingGroup) {
        return switch (settingGroup.toLowerCase()) {
            case "instagram" -> Instagram_Settings;
            case "youtube" -> Youtube_Settings;
            case "tiktok" -> Tiktok_Settings;
            case "general" -> General_Settings;
            default -> throw new IllegalArgumentException("Unknown setting: " + settingGroup);
        };
    }

    public GeneralSettings General() {
        return General_Settings;
    }

    public InstagramSettings Instagram() {
        return Instagram_Settings;
    }

    public YoutubeSettings Youtube() {
        return Youtube_Settings;
    }

    public TiktokSettings Tiktok() {
        return Tiktok_Settings;
    }

    //public TwitterSettings Twitter() {return Twitter_Settings;}

}