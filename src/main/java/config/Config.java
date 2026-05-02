package config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.*;

@JsonAutoDetect( // Prevents jackson from writing its own config after the old one and ruining everything
        fieldVisibility = ANY,
        getterVisibility = NONE,
        isGetterVisibility = NONE
)

// ReadConfig
//
// ReadConfig.getInstance  ; Load and return the singleton configuration object from bot.json
// Inputs : automatically reads bot.json from working directory
//
// ReadConfig.getGeneral  ; Retrieve general configuration section
// ReadConfig.getInstagram  ; Retrieve Instagram configuration section
// ReadConfig.getYoutube  ; Retrieve YouTube configuration section
// ReadConfig.getTwitter  ; Retrieve Twitter configuration section
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

    // Private constructor to prevent external instantiation
    private Config() {}

    // Singleton getter
    public static Config getInstance() { // Didnt make this
        if (instance == null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

            try {
                instance = mapper.readValue(new File("config.json"), Config.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    public void saveConfig() { // Didnt make this
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
        try {
            mapper.writeValue(new File("config.json"), this);
        } catch (IOException e) {
            System.err.println("Could not save config: " + e.getMessage());
        }
    }


    // Getters and setters
    public GeneralSettings getGeneral() { return General_Settings; }

    public InstagramSettings getInstagram() { return Instagram_Settings; }

    public YoutubeSettings getYoutube() { return Youtube_Settings; }

    public TwitterSettings getTwitter() { return Twitter_Settings; }
}
