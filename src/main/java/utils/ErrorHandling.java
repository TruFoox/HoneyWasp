package utils;

import java.util.Scanner;

public class ErrorHandling {
    public static void exitProgram() {
        Output.print(null, "Press Enter to Exit...", Output.RESET, false, false);

        Scanner scanner = new Scanner(System.in);

        scanner.nextLine();

        System.exit(1); // Exit program
    }
}
