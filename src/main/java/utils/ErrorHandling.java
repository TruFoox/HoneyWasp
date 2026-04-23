package utils;

import java.util.Map;
import java.util.Scanner;

// This could be done with try/catch but that honestly this is just tremendously easier and cleaner
public class ErrorHandling {
    public static Map<String, String> messageTable = Map.of( // Prob gonna delete this at some point cuz.... why would I not just use catch exceptions
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
