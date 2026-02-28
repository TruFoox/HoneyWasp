package utils;

import java.util.Map;
import java.util.Scanner;

public class ErrorHandling {
    public static Map<String, String> messageTable = Map.of(
            "Connection reset", "Your internet likely cut out mid-request, causing a crash",
            "", ""
    );

    public static void exitProgram() {
        System.out.println("\nPress Enter to exit...");

        Scanner scanner = new Scanner(System.in);

        scanner.nextLine();
        System.exit(0); // Exit program
    }
}
