package utils;

import club.minnced.discord.webhook.exception.HttpException;
import main.HoneyWasp;
import org.jline.utils.AttributedString;
import services.Services;

import java.io.IOException;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

// Output
//
// Void Output.webhookPrint  ; Print message to console, send message to discord with webhook if capable
// Inputs : Message to print, color to print as (Default white), whether to use timestamp (Default true)
//
// Void Output.print  ; Print message to console, no webhook
// Inputs : Message to print, color to print as (Default white), whether to mark this line with \r as overridable (default false), whether to use timestamp (Default true)
public class Output { // Uses JLine to output in Command.java
    // Use Output.[COLOR]
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";


    private static final SendWebhook webhookInstance = new SendWebhook(); // Initiate webhook instance

    static boolean lastOutputWasNewline = true;

    public static synchronized void webhookPrint(Services service, String message, String color, boolean useTimestamp) {
        try {
            String shortName;

            if (service == null) {
                shortName ="[SYS] ";
            } else {
                shortName = "[" + service.shortName + "] ";
            }

            String prefix = "     [" + DateTime.time() + "] - ";
            String spacing = " ".repeat(prefix.length());

            String outputLine = message.replaceAll("\t", spacing);

            String finalMessage;

            if (!useTimestamp) {
                finalMessage = color + "     " + shortName + message + RESET;
            } else {
                finalMessage = color + prefix + shortName + outputLine + RESET;
            }

            Command.reader.printAbove(finalMessage);
            lastOutputWasNewline = true;

            Command.status.update(List.of(new AttributedString("")));
            if (HoneyWasp.config.General() != null) {
                String webhookUrl =
                        HoneyWasp.config.General().getDiscordWebhook();

                if (webhookUrl != null && !webhookUrl.isEmpty()) {
                    String webhookMessage = message.replace("\t", finalMessage);
                    webhookInstance.sendMessage(shortName + webhookMessage);
                }
            }

        } catch (HttpException e) {
            System.err.print(Output.RED + "     [" + DateTime.time() + "] - Discord webhook URL is likely invalid. "
                                 + "\n     Either make the field blank, or replace it with a valid one. This message will spam until you do." + RESET);
        } catch (Exception e) {
            System.err.print(e);
        }
    }
    public static synchronized void print(Services service, String message, String color, boolean overwriteThisLine, boolean useTimestamp) {
        String shortName;

        if (service == null) {
            shortName = "[SYS] ";
        } else {
            shortName = "[" + service.shortName + "] ";
        }

        String prefix = "     [" + DateTime.time() + "] - ";
        String spacing = " ".repeat(prefix.length());

        String outputLine = message.replaceAll("\t", spacing);

        String finalMessage;

        if (!useTimestamp) {
            finalMessage = color + "     " + shortName + message + RESET;
        } else {
            finalMessage = color + prefix + shortName + outputLine + RESET;
        }

        if (overwriteThisLine && !HoneyWasp.DEBUG_MODE) {
            lastOutputWasNewline = false;
            Command.status.update(List.of(AttributedString.fromAnsi(finalMessage)));
        } else {
            Command.reader.printAbove(finalMessage);
            lastOutputWasNewline = true;

            Command.status.update(List.of());
        }
    }
    public static synchronized void  debugPrint(Services service, String message) {
        if (HoneyWasp.DEBUG_MODE) { // Only print if DEBUG_MODE mode is enabled
            if (lastOutputWasNewline) {Command.reader.printAbove("");}
            String shortName;

            if (service == null) {
                shortName = "[SYS] ";
            } else {
                shortName = "[" + service.shortName + "] ";
            }

            // Replaces /t with spacing required to line up with previous outputs
            String prefix = "     [" + DateTime.time() + "] - ";
            String spacing = " ".repeat(prefix.length());

            String outputLine= message.replaceAll("\t", spacing);

            Command.reader.printAbove(YELLOW + prefix + shortName + outputLine + RESET);
            lastOutputWasNewline = true;

        }
    }


    // Default overloads
    public static synchronized void webhookPrint(Services service, String message, String color) {webhookPrint(service, message, color, true);}
    public static synchronized void webhookPrint(Services service, String message) {webhookPrint(service, message, YELLOW, true);}

    public static void print(Services service, String message) {print(service, message, YELLOW, false, true);}
    public static void print(Services service, String message, String color) {print(service, message, color, false, true);}
    public static void print(Services service, String message, String color, boolean overwriteThisLine) {print(service, message, color, overwriteThisLine, true);}
}
