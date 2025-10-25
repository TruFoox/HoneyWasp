package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class fileIO {
    public static void writeList(String in, String service) {
        try {
            long timestamp = System.currentTimeMillis();
            // Generate filepath "./cache/[Service]/cache.txt" for given OS & write to file
            Path cachePath = Paths.get(".", "cache", service, "cache.txt");

            Files.write(cachePath, (in + "," + timestamp + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
    public static List<String[]> readList(String service) {
        Path cachePath = Paths.get(".", "cache", service, "cache.txt");

        try {
            List<String> temp = Files.readAllLines(cachePath);
            List<String[]> splitList = new ArrayList<>();

            // Split each line by "," and add to list
            for (String line : temp) {
                splitList.add(line.split(","));
            }

            return splitList;

        } catch (IOException e) {
            Output.webhookPrint("[INSTA] No cache.txt found. Quitting...", Output.RED);
            return new ArrayList<>(); // safer than returning null
        }
    }

}
