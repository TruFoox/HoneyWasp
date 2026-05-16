package utils;

import services.Services;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class FileIO {
    public static void writeList(String in, Services service, boolean permanent) {
        try {

            long timestamp = System.currentTimeMillis();
            // Generate filepath "./cache/[Service]/cache.txt" for given OS & write to file
            Path cachePath = Paths.get(".", "cache", service.name.toLowerCase(), "cache.txt");
            Output.debugPrint(null, "Attempting to write to " + cachePath);

            if (permanent) { // will break on November 20, 2286, so make sure to increase this by then
                Files.write(cachePath, (in + ",9999999999999999" + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                service.usedURLs.add(new String[]{in, String.valueOf(9999999999999999L)});
            } else {
                Files.write(cachePath, (in + "," + timestamp + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                service.usedURLs.add(new String[]{in, String.valueOf(timestamp)});
            }

        } catch (IOException ex) {
            Output.webhookPrint(null,"No /cache/" + service.name.toLowerCase() + "/cache.txt found. Quitting...", Output.RED);
        }

    }
    public static List<String[]> readList(Services service) {
        Path cachePath = Paths.get(".", "cache", service.name.toLowerCase(), "cache.txt");
        Output.debugPrint(null, "Attempting to read from " + cachePath);

        try {
            List<String> temp = Files.readAllLines(cachePath);
            List<String[]> splitList = new ArrayList<>();

            // Split each line by "," and add to list
            for (String line : temp) {
                splitList.add(line.split(","));
            }

            return splitList;

        } catch (IOException e) {
            Output.webhookPrint(null,"No /cache/" + service.name.toLowerCase() + "/cache.txt found. Quitting...", Output.RED);
            return null;
        }
    }

    public static void clearList(String service) {
        try {
            Path cachePath = Paths.get(".", "cache", service.toLowerCase(), "cache.txt");
            Files.writeString(cachePath, "");
            Output.webhookPrint(null, service + " cache successfully cleared");
        } catch (IOException e) {
            Output.webhookPrint(null,"No /cache/" + service.toLowerCase() + "/cache.txt found. Quitting...", Output.RED);
        }
    }

}
