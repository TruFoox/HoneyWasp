package utils;

import java.util.Scanner;

public class ErrorHandling {
    public static void exitProgram() {
        Output.print(null, "Press Enter to Exit...", Output.RESET, false, false); // .RESET just breaks my code and forces console white

        Scanner scanner = new Scanner(System.in);

        scanner.nextLine();
        System.exit(0); // Exit program
    }
}
