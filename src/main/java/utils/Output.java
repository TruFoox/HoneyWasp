package utils;

import config.ReadConfig;
import java.util.Scanner;

// Output
//
// Void Output.webhookPrint  ; Print message to console, send message to discord with webhook if capable
// Inputs : Message to print, color to print as (Default white), whether to use timestamp (Default true)
//
// Void Output.print  ; Print message to console, no webhook
// Inputs : Message to print, color to print as (Default white), whether to mark this line with \r as overridable (default false), whether to use timestamp (Default true)
public class Output {

    // Colors (Use Output.[COLOR])
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    static boolean lastOutputWasNewline = true;

    public static synchronized void webhookPrint(String message, String color, boolean useTimestamp) { // Needs added replacement of "/n" with "(displacement for timestamp) + /n"
        if (lastOutputWasNewline) {System.out.println();}

        // Replaces /t with spacing required to line up with previous outputs
        String prefix = "    [" + DateTime.time() + "] - ";
        String spacing = " ".repeat(prefix.length());

        String outputLine= message.replaceAll("\t", spacing);

        if (!useTimestamp) {
            System.out.print(color + "\t" + outputLine + RESET);
            lastOutputWasNewline = true;
        } else {
            System.out.print(color + "\t[" + DateTime.time() + "] - " + outputLine + RESET);
            lastOutputWasNewline = true;
        }

        ReadConfig config = ReadConfig.getInstance();
        if (config != null && config.getGeneral() != null) {
            String botToken = config.getGeneral().getDiscordBotToken();
            if (botToken != null && !botToken.isEmpty()) {
                SendWebhook webhook = new SendWebhook();

                String webhookMessage = message.replace("\t", "")
                        .replace("\r", "");

                webhook.sendMessage(webhookMessage);
            }
        }
    }

    public static synchronized void print(String message, String color, boolean overwriteThisLine, boolean useTimestamp) {
        if (lastOutputWasNewline) {System.out.println();}
        if (!useTimestamp) {
            if (overwriteThisLine) {
                System.out.print("\r\033[2K");
                System.out.print(color + "\t" + message + RESET + "\r");
                lastOutputWasNewline = false;
            } else {
                System.out.print(color + "\t" + message + RESET);
                lastOutputWasNewline = true;
            }
        } else {
            if (overwriteThisLine) {
                System.out.print("\r\033[2K");
                System.out.print(color + "\t[" + DateTime.time() + "] - " + message + RESET+ "\r");
                lastOutputWasNewline = false;
            } else {

                System.out.print(color + "\t[" + DateTime.time() + "] - " + message + RESET);
                lastOutputWasNewline = true;
            }
        }
    }

    public static void exitProgram() {
        System.out.println("\nPress Enter to exit...");

        Scanner scanner = new Scanner(System.in);

        scanner.nextLine(); // Waits for user input
        scanner.close();

        System.exit(0); // Exits the program
    }
    // Default overloads
    public static void webhookPrint(String message) {webhookPrint(message, YELLOW, true);}
    public static void webhookPrint(String message, String color) {webhookPrint(message, color, true);}

    public static void print(String message) {print(message, YELLOW, false, true);}
    public static void print(String message, String color) {print(message, color, false, true);}
    public static void print(String message, String color, boolean overwriteThisLine) {print(message, color, overwriteThisLine, true);}
}
