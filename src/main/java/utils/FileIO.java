package utils;

import services.Services;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class FileIO { // Currently only readList works for any file
    public static void writeCache(String url, Services service, boolean permanent) {
        try {
            long timestamp = System.currentTimeMillis();
            // Generate filepath "./cache/[Service]/cache.txt" for given OS & write to file
            Path cachePath = Paths.get(".", "cache", service.name.toLowerCase(), "cache.txt");
            Output.debugPrint(null, "Attempting to write to " + cachePath);

            long fileTimestamp; // Timestamp to actually be written to file

            if (permanent) {
                fileTimestamp = 0L;
            } else {
                fileTimestamp = timestamp;
            }

            Files.write(cachePath, (url + "," + fileTimestamp + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            service.usedURLs.add(new String[]{url, String.valueOf(fileTimestamp)});

        } catch (IOException ex) {
            Output.webhookPrint(null,"No /cache/" + service.name.toLowerCase() + "/cache.txt found", Output.RED); // Rn doesnt actually quit, function is void
        }

    }
    public static List<String[]> readCache(Services service, Path path, String delimiter) {
        Output.debugPrint(service, "Attempting to read from " + path);

        try {
            List<String> temp = Files.readAllLines(path);
            List<String[]> splitList = new ArrayList<>();

            // Split each line by delimiter and add to list
            for (String line : temp) {
                splitList.add(line.split(delimiter));
            }

            return splitList;

        } catch (IOException e) {
            Output.webhookPrint(service,"No " + path + " found. Quitting...", Output.RED);
            return null;
        }
    }
    public static void autoClear(Services service) { // Removes outdated cached URLs
        try {
            Path cachePath = Paths.get(".", "cache", service.name.toLowerCase(), "cache.txt");
            Output.debugPrint(service, "Attempting to auto-clear cache");

            service.usedURLs.removeIf(row -> (Long.parseLong(row[1]) + (service.HOURS_BEFORE_DUPLICATES_REMOVED * 3600000L) < System.currentTimeMillis()) && Long.parseLong(row[1]) != 0L); // Remove all w/ duplicate removal time < current time & not set to permanent

            Files.writeString(cachePath, ""); // Clear file before overriding

            for (String[] row : service.usedURLs) { // Override old file
                Files.writeString(cachePath, row[0] + "," + row[1] + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            Output.webhookPrint(null,"No /cache/" + service.name.toLowerCase() + "/cache.txt found", Output.RED);
        }
    }
    public static void clearCache(String service) { // Not using services object because it must be able to run without that service running
        try {
            Path cachePath = Paths.get(".", "cache", service.toLowerCase(), "cache.txt");
            Files.writeString(cachePath, "");
            Output.webhookPrint(null, service + " cache successfully cleared");
        } catch (IOException e) {
            Output.webhookPrint(null,"No /cache/" + service.toLowerCase() + "/cache.txt found", Output.RED);
        }
    }

    public static void log(Services service, String line) { // Log to file
        try {
            Path logPath = Paths.get(".", "cache", "logs.txt");

            String formatted = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"));

            Files.write(logPath, ("[" + formatted + " | " + service.name + "] " + line + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            Output.webhookPrint(null,"Failed to log to file", Output.RED);
        }
    }

}
