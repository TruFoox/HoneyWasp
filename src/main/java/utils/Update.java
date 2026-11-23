package utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import main.HoneyWasp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

public class Update {
    public static boolean check() {
        try {
            Output.debugPrint(null, "Checking for new version");
            String responseString = HTTPSend.get(null, "https://api.github.com/repos/trufoox/honeywasp/releases/latest");

            // Fetch latest version, remove "v" (e.g. v2), then parse as float
            float version;
            try {
                version = Float.parseFloat(StringToJson.getData(responseString, "tag_name").replace("v", ""));
            } catch (Exception _){
                Output.print(null, "GitHub failed to give the expected response", Output.RED);
                return false;
            }

            if (version > HoneyWasp.currentVersion) {
                Output.webhookPrint(null, "A new version is available! : v" + version + " (Current : v" + HoneyWasp.currentVersion + ")");
                return true;
            }
        } catch (Exception e) {
            Output.print(null, "Failed to check for new version" +
                    "\n\tReason: " + e.getMessage(), Output.RED);
        }
        return false;
    }
    public static boolean attemptUpdate() {
        Boolean requiresConfigUpdate = false;
        try {
            Output.debugPrint(null, "Attempting to update - this may take a while. Do not close HoneyWasp...");

            String responseString = HTTPSend.get(null, "https://api.github.com/repos/trufoox/honeywasp/releases/latest");

            JsonArray assets = JsonParser.parseString(responseString).getAsJsonObject().getAsJsonArray("assets");

            String fileUrl = null;

            for (JsonElement asset : assets) {
                JsonObject obj = asset.getAsJsonObject();
                if (obj.get("name").getAsString().contains(".jar")) {
                    if (obj.get("name").getAsString().contains("confup")) { // This needs to be in the name of the update jars
                        Output.print(null, "You need to update your config to use the latest version of HoneyWasp" +
                                "\n\tAll changes you must make are listed in the changelog at https://github.com/TruFoox/HoneyWasp/releases/latest");
                        requiresConfigUpdate = true;
                    }
                    fileUrl = obj.get("browser_download_url").getAsString();
                    break;
                }
            }

            if (fileUrl == null) {
                Output.webhookPrint(null, "Failed to update, no valid file found", Output.RED);
                return false;
            }

            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(fileUrl)).GET().build();

            // Download update (Overriding this jar)
            client.send(request, HttpResponse.BodyHandlers.ofFile(Paths.get(".", "HoneyWasp.jar"), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
            if (!requiresConfigUpdate) {
                Output.webhookPrint(null, "Successfully downloaded HoneyWasp update. Restarting HoneyWasp...");

                // Open new HoneyWasp process
                new ProcessBuilder("java", "-XX:+UseSerialGC", "-Xms256m", "-Xmx512m", "--enable-native-access=ALL-UNNAMED", "-jar", "HoneyWasp.jar", "--update").inheritIO().start();

                System.exit(0);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
