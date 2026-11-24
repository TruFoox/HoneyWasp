package IO;

import services.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Logger {
    public static void log(Services service, String line) { // Log to file
        try {
            Path logPath = Paths.get(".", "cache", "logs.txt");

            String formatted = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"));

            String name;
            if (service == null) {
                name = "HoneyWasp";
            } else {
                name = service.name;
            }

            Files.write(logPath, ("[" + formatted + " | " + name + "] " + line + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            Output.webhookPrint(null,"Failed to log to file", Output.RED);
        }
    }
}
