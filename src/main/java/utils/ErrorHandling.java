package utils;

import java.util.Scanner;

public class ErrorHandling {
    public static void exitProgram() {
        System.out.println("\nPress Enter to exit...");

        Scanner scanner = new Scanner(System.in);

        scanner.nextLine();
        System.exit(0); // Exit program
    }
}
