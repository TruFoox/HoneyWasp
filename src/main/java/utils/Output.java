package utils;

import club.minnced.discord.webhook.exception.HttpException;
import config.Config;
import services.Services;

// Output
//
// Void Output.webhookPrint  ; Print message to console, send message to discord with webhook if capable
// Inputs : Message to print, color to print as (Default white), whether to use timestamp (Default true)
//
// Void Output.print  ; Print message to console, no webhook
// Inputs : Message to print, color to print as (Default white), whether to mark this line with \r as overridable (default false), whether to use timestamp (Default true)
public class Output {
    static Config config = Config.getInstance();

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

    static boolean lastOutputWasNewline = true;

    public static synchronized void webhookPrinttemptest(Services service, String message, String color, boolean useTimestamp) { // Needs added replacement of "/n" with "(displacement for timestamp) + /n"
        try {
            if (lastOutputWasNewline || config.General().isDebug_mode()) {System.out.println();} else {System.out.print("\r\033[2K");}

            // Replaces /t with spacing required to line up with previous outputs
            String prefix = "     [" + DateTime.time() + "] - ";
            String spacing = " ".repeat(prefix.length());

            String outputLine= message.replaceAll("\t", spacing);

            if (!useTimestamp) {
                System.out.print(color + "     [" + service.getShortname() + "] " +  outputLine + RESET);
            } else {
                System.out.print(color + prefix + "[" + service.getShortname() + "] " + outputLine + RESET);
            }
            lastOutputWasNewline = true;

            if (config != null && config.General() != null) {
                String webhook_url = config.General().getDiscordWebhook();
                if (webhook_url != null && !webhook_url.isEmpty()) {
                    SendWebhook webhook = new SendWebhook();

                    String webhookMessage = message.replace("\t", "")
                            .replace("\r", "");

                    webhook.sendMessage(webhookMessage);
                }
            }
        } catch (HttpException e) { // Webhook error
            System.err.print(color + "     [" + DateTime.time() + "] - Discord webhook URL is likely invalid. Either make the field blank, or replace it with a valid one. This message will spam until you do so." + RESET);
        } catch (Exception e) {
            System.err.print(e);
        }
    }
    public static synchronized void printtest(Services service, String message, String color, boolean overwriteThisLine, boolean useTimestamp) {
        boolean debug = config.General().isDebug_mode();
        if (lastOutputWasNewline || debug) {System.out.println();} else {System.out.print("\r\033[2K");}

        if (!useTimestamp) {
            if (overwriteThisLine && !debug) {
                System.out.print("\r\033[2K");
                System.out.print(color + "     "  + "[" + service.getShortname() + "] " +  message + RESET + "\r");
                lastOutputWasNewline = false;
            } else {
                System.out.print(color + "     "  + "[" + service.getShortname() + "] " +  message + RESET);
                lastOutputWasNewline = true;
            }
        } else {
            if (overwriteThisLine && !debug) {
                System.out.print("\r\033[2K");
                System.out.print(color + "     [" + DateTime.time() + "] - " + "[" + service.getShortname() + "] " + message + RESET+ "\r");
                lastOutputWasNewline = false;
            } else {

                System.out.print(color + "     [" + DateTime.time() + "] - "  + "[" + service.getShortname() + "] " +  message + RESET);
                lastOutputWasNewline = true;
            }
        }

    }
    public static synchronized void  debugPrinttest(Services service, String message) {
        if (config.General().isDebug_mode()) { // Only print if debug mode is enabled
            if (lastOutputWasNewline) {System.out.println();}

            // Replaces /t with spacing required to line up with previous outputs
            String prefix = "     [" + DateTime.time() + "] - ";
            String spacing = " ".repeat(prefix.length());

            String outputLine= message.replaceAll("\t", spacing);

            System.out.print(YELLOW + prefix + "[" + service.getShortname() + "] " + outputLine + RESET);
            lastOutputWasNewline = true;

        }
    }
    // \t not used, instead use "     " - avoids the fact that \t can be different lengths depending on environment & break formatting
    public static synchronized void webhookPrint(String message, String color, boolean useTimestamp) { // Needs added replacement of "/n" with "(displacement for timestamp) + /n"
            try {
            if (lastOutputWasNewline || config.General().isDebug_mode()) {System.out.println();} else {System.out.print("\r\033[2K");}

            // Replaces /t with spacing required to line up with previous outputs
            String prefix = "     [" + DateTime.time() + "] - ";
            String spacing = " ".repeat(prefix.length());

            String outputLine= message.replaceAll("\t", spacing);

            if (!useTimestamp) {
                System.out.print(color + "     " + outputLine + RESET);
            } else {
                System.out.print(color + prefix + outputLine + RESET);
            }
            lastOutputWasNewline = true;

            if (config != null && config.General() != null) {
                String webhook_url = config.General().getDiscordWebhook();
                if (webhook_url != null && !webhook_url.isEmpty()) {
                    SendWebhook webhook = new SendWebhook();

                    String webhookMessage = message.replace("\t", "")
                            .replace("\r", "");

                    webhook.sendMessage(webhookMessage);
                }
            }
            } catch (HttpException e) { // Webhook error
                System.err.print(color + "     [" + DateTime.time() + "] - Discord webhook URL is likely invalid. Either make the field blank, or replace it with a valid one. This message will spam until you do so." + RESET);
            } catch (Exception e) {
                System.err.print(e);
            }
    }

    public static synchronized void print(String message, String color, boolean overwriteThisLine, boolean useTimestamp) {
        boolean debug = config.General().isDebug_mode();
        if (lastOutputWasNewline || debug) {System.out.println();} else {System.out.print("\r\033[2K");}

        if (!useTimestamp) {
            if (overwriteThisLine && !debug) {
                System.out.print("\r\033[2K");
                System.out.print(color + "     " + message + RESET + "\r");
                lastOutputWasNewline = false;
            } else {
                System.out.print(color + "     " + message + RESET);
                lastOutputWasNewline = true;
            }
        } else {
            if (overwriteThisLine && !debug) {
                System.out.print("\r\033[2K");
                System.out.print(color + "     [" + DateTime.time() + "] - " + message + RESET+ "\r");
                lastOutputWasNewline = false;
            } else {

                System.out.print(color + "     [" + DateTime.time() + "] - " + message + RESET);
                lastOutputWasNewline = true;
            }
        }

    }
    public static synchronized void debugPrint(String message) {
        if (config.General().isDebug_mode()) { // Only print if debug mode is enabled
            if (lastOutputWasNewline) {System.out.println();}

            // Replaces /t with spacing required to line up with previous outputs
            String prefix = "     [" + DateTime.time() + "] - ";
            String spacing = " ".repeat(prefix.length());

            String outputLine= message.replaceAll("\t", spacing);

            System.out.print(YELLOW + prefix + outputLine + RESET);
            lastOutputWasNewline = true;

        }
    }

    // Default overloads
    public static synchronized void webhookPrinttemptest(Services service, String message, String color) {webhookPrinttemptest(service, message, color, true);}
    public static synchronized void webhookPrinttemptest(Services service, String message) {webhookPrinttemptest(service, message, YELLOW, true);}
    public static void webhookPrint(String message) {webhookPrint(message, YELLOW, true);}
    public static void webhookPrint(String message, String color) {webhookPrint(message, color, true);}

    public static void printtest(Services service, String message) {printtest(service, message, YELLOW, false, true);}
    public static void printtest(Services service, String message, String color) {printtest(service, message, color, false, true);}
    public static void printtest(Services service, String message, String color, boolean overwriteThisLine) {printtest(service, message, color, overwriteThisLine, true);}

    public static void print(String message) {print(message, YELLOW, false, true);}
    public static void print(String message, String color) {print(message, color, false, true);}
    public static void print(String message, String color, boolean overwriteThisLine) {print(message, color, overwriteThisLine, true);}
}
