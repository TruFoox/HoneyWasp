package IO;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.Status;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import main.HoneyWasp;
import IO.Command;
import utils.ErrorHandling;
import utils.StringToJson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Update {
    public static boolean check(boolean justOpened) {
        try {
            Output.debugPrint(null, "Checking for new version");
            String responseString = HTTPSend.get(null, "https://api.github.com/repos/trufoox/honeywasp/releases/latest");


            // Fetch latest version, remove "v" (e.g. v2), then parse as float
            float version;
            try {
                version = Float.parseFloat(StringToJson.getData(responseString, "tag_name").replace("v", ""));
            } catch (Exception _) {
                Output.print(null, "GitHub failed to give the expected response", Output.RED);
                return false;
            }

            if (version > HoneyWasp.currentVersion) {
                if (justOpened) {
                    Output.print(null, "A new version is available! : v" + version + " (Current : v" + HoneyWasp.currentVersion + ")" +
                            "\n\tRun /update to update! ", Output.YELLOW, false, false);
                } else {
                    Output.webhookPrint(null, "A new version is available! : v" + version + " (Current : v" + HoneyWasp.currentVersion + ")", Output.GREEN);
                }
                return true;
            }
        } catch (Exception e) {
            Output.print(null, "Failed to check for new version" +
                    "\n\tReason: " + e.getMessage(), Output.RED);
        }
        return false;
    }
    public static boolean check() {return check(false);}

    public static boolean attemptUpdate() {
        Boolean requiresConfigUpdate = false;
        try {
            Output.print(null, "Attempting to update - this may take a while. Do not close HoneyWasp...");

            String responseString = HTTPSend.get(null, "https://api.github.com/repos/trufoox/honeywasp/releases/latest");

            JsonArray assets = JsonParser.parseString(responseString).getAsJsonObject().getAsJsonArray("assets");

            String fileUrl = null;

            for (JsonElement asset : assets) {
                JsonObject obj = asset.getAsJsonObject();
                if (obj.get("name").getAsString().toLowerCase().contains("updatefile")) {
                    if (obj.get("name").getAsString().contains("cc")) { // (CC means change config) This needs to be in the name of the update jars
                        Output.print(null, "If you want to update HoneyWasp, you will need to manually update your config" +
                                "\n\tAll changes you must make are listed in the changelog at https://github.com/TruFoox/HoneyWasp/releases/latest" +

                                "\n\n\tType \"Y\" to continue update, or anything else to cancel...");

                        if (!Command.response().equalsIgnoreCase("y")) {return false;} // Cancel if not y

                        Output.print(null, "Attempting to update...");

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

                // Open new HoneyWasp process with update flag
                new ProcessBuilder("java", "-XX:+UseSerialGC", "-Xms256m", "-Xmx512m", "--enable-native-access=ALL-UNNAMED", "-jar", "HoneyWasp.jar", "update").inheritIO().start();

                System.exit(0); // Immediately exit
            } else {
                Output.webhookPrint(null, "You will need to update the names of/add the config values listed at https://github.com/TruFoox/HoneyWasp/releases/latest");

                ErrorHandling.exitProgram(); // Prompt to press enter then exit
            }
            return true;
        } catch (Exception e) {
            Output.debugPrint(null, "Encountered an error while trying to update");
            e.printStackTrace();
            return false;
        }
    }
}
