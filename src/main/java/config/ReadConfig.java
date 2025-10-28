package config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

// ReadConfig
//
// ReadConfig.getInstance  ; Load and return the singleton configuration object from bot.json
// Inputs : automatically reads bot.json from working directory
//
// ReadConfig.getGeneral  ; Retrieve general configuration section
// ReadConfig.getInstagram  ; Retrieve Instagram configuration section
// ReadConfig.getYoutube  ; Retrieve YouTube configuration section
public class ReadConfig {
    private static ReadConfig instance;

    @JsonProperty("General_Settings")
    private GeneralSettings General_Settings;

    @JsonProperty("Instagram_Settings")
    private InstagramSettings Instagram_Settings;

    @JsonProperty("Youtube_Settings")
    private YoutubeSettings Youtube_Settings;

    // Private constructor to prevent external instantiation
    private ReadConfig() {}

    // Singleton getter
    public static ReadConfig getInstance() {
        if (instance == null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

            try {
                instance = mapper.readValue(new File("config.json"), ReadConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    // Getters and setters
    public GeneralSettings getGeneral() { return General_Settings; }
    public void setGeneral(GeneralSettings general_Settings) { General_Settings = general_Settings; }

    public InstagramSettings getInstagram() { return Instagram_Settings; }
    public void setInstagram(InstagramSettings instagram_Settings) { Instagram_Settings = instagram_Settings; }

    public YoutubeSettings getYoutube() { return Youtube_Settings; }
    public void setYoutube(YoutubeSettings youtube_Settings) { Youtube_Settings = youtube_Settings; }
}
