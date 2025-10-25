package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class fileIO {
    public static void writeList(String in, String service) {
        try {
            // Generate filepath "./cache/[Service]/cache.txt" for given OS & write to file
            Path cachePath = Paths.get(".", "cache", service, "cache.txt");
            Files.write(cachePath, (in + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
    public static List<String> readList(String service) {
        try {
            // Generate filepath "./cache/[Service]/cache.txt" for given OS & read file
            return Files.readAllLines(Paths.get(".", "cache", service, "cache.txt"));

        } catch (IOException e) {
            Output.webhookPrint("[INSTA] No cache.txt found at ./cache/instagram/cache.txt. Quitting...", Output.RED);

            return null;

        }
    }
}
