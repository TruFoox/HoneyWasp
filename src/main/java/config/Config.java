package config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    @JsonProperty("Twitter_Settings")
    private TwitterSettings Twitter_Settings;

    private Config() {}

    public static Config getInstance() {
        if (instance == null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

            try {
                instance = mapper.readValue(
                        new File("config.json"),
                        Config.class
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return instance;
    }

    public void saveConfig() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(
                com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
        );

        try {
            mapper.writeValue(
                    new File("config.json"),
                    this
            );
        } catch (IOException e) {
            System.err.println(
                    "Could not save config: " + e.getMessage()
            );
        }
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

    public TwitterSettings Twitter() {
        return Twitter_Settings;
    }

    public PlatformSettings Platform(String platform) {
        return switch (platform.toLowerCase()) {
            case "instagram" -> Instagram_Settings;
            case "youtube" -> Youtube_Settings;
            case "twitter" -> Twitter_Settings;
            default -> throw new IllegalArgumentException(
                    "Unknown platform: " + platform
            );
        };
    }
}