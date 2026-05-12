package utils;

import club.minnced.discord.webhook.exception.HttpException;
import main.HoneyWasp;
import services.Services;

// Output
//
// Void Output.webhookPrint  ; Print message to console, send message to discord with webhook if capable
// Inputs : Message to print, color to print as (Default white), whether to use timestamp (Default true)
//
// Void Output.print  ; Print message to console, no webhook
// Inputs : Message to print, color to print as (Default white), whether to mark this line with \r as overridable (default false), whether to use timestamp (Default true)
public class Output {

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

    static boolean DEBUG_MODE = HoneyWasp.config.General().isDebug_mode();
    
    static boolean lastOutputWasNewline = true;
    public static synchronized void webhookPrint(Services service, String message, String color, boolean useTimestamp) { // Needs added replacement of "/n" with "(displacement for timestamp) + /n"
        try {
            if (lastOutputWasNewline ||  DEBUG_MODE) {System.out.println();} else {System.out.print("\r\033[2K");}
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

            if (!useTimestamp) {
                System.out.print(color + "     " + shortName +  message + RESET); // Not outputLine because lacks prefix
            } else {
                System.out.print(color + prefix + shortName + outputLine + RESET);
            }
            lastOutputWasNewline = true;

            if (HoneyWasp.config.General() != null) {
                String webhook_url = HoneyWasp.config.General().getDiscordWebhook();
                if (webhook_url != null && !webhook_url.isEmpty()) {
                    SendWebhook webhook = new SendWebhook();

                    String webhookMessage = message.replace("\t", "");

                    webhook.sendMessage(shortName + webhookMessage);
                }
            }
        } catch (HttpException e) { // Webhook error
            System.err.print(color + "     [" + DateTime.time() + "] - Discord webhook URL is likely invalid. Either make the field blank, or replace it with a valid one. This message will spam until you do so." + RESET);
        } catch (Exception e) {
            System.err.print(e);
        }
    }
    public static synchronized void print(Services service, String message, String color, boolean overwriteThisLine, boolean useTimestamp) {
        if (lastOutputWasNewline || DEBUG_MODE) {System.out.println();} else {System.out.print("\r\033[2K");}
        String shortName;

        if (service == null) {
            shortName = "[SYS] ";
        } else {
            shortName = "[" + service.shortName + "] ";
        }

        String prefix = "     [" + DateTime.time() + "] - ";
        String spacing = " ".repeat(prefix.length());

        String outputLine= message.replaceAll("\t", spacing);

        if (!useTimestamp) {
            if (overwriteThisLine && !DEBUG_MODE) {
                System.out.print("\r\033[2K");
                System.out.print(color + "     "  + shortName +  message + RESET + "\r");
                lastOutputWasNewline = false;
            } else {
                System.out.print(color + "     "  + shortName +  message + RESET);
                lastOutputWasNewline = true;
            }
        } else {
            if (overwriteThisLine && !DEBUG_MODE) {
                System.out.print("\r\033[2K");
                System.out.print(color + prefix + shortName + outputLine + RESET+ "\r");
                lastOutputWasNewline = false;
            } else {

                System.out.print(color + prefix  + shortName +  outputLine + RESET);
                lastOutputWasNewline = true;
            }
        }

    }
    public static synchronized void  debugPrint(Services service, String message) {
        if (DEBUG_MODE) { // Only print if DEBUG_MODE mode is enabled
            if (lastOutputWasNewline) {System.out.println();}
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

            System.out.print(YELLOW + prefix + shortName + outputLine + RESET);
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
