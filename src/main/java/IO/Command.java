package IO;
import main.HoneyWasp;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.Status;
import utils.Sleep;

public class Command extends Thread { // Handles terminal commands
    static Terminal terminal;
    static LineReader reader;
    static Status status;
    static volatile String line;
    static volatile boolean newInput = false;

    // Input like Y/N etc
    static String miscInput = ""; // Set to nothing when acknowledged

    public synchronized void run() { // Commands technically dont require / before it
        while (true) {
            try {
                if (Terminal.class.getModule().isNativeAccessEnabled()) { // Apparently this is how you check if native access enabled
                    terminal = TerminalBuilder.builder().system(true).build();
                } else { // If --enable-native-access=ALL-UNNAMED not used default to legacy terminal
                    terminal = TerminalBuilder.builder().system(true).provider("exec").build();
                }
                status = Status.getStatus(terminal);

                reader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .build();

                // Read lines from the user
                while (true) {
                    line = reader.readLine("    >");

                    newInput = true;

                    int space = line.indexOf(' ');

                    String service = null;
                    String command = line.substring(line.indexOf('/') + 1).toLowerCase(); // Store whole input after / in case no space

                    if (space != -1) { // If a space exists in input fetch command and service, else keep whole input as command
                        command = line.substring(line.indexOf('/') + 1, space).toLowerCase(); // Works both with / & without
                        service = line.substring(space + 1).toLowerCase();
                    }

                    switch (command) {
                        case "start": {
                            if (service == null) {
                                Output.print(null, "/start need two fields: A command, and a service. Try /help for help");
                                continue;
                            }

                            if (service.equals("all")) {
                                for (String name : HoneyWasp.services.keySet()) {
                                    if (HoneyWasp.runningServices.containsKey(name)) {
                                        Output.print(null, HoneyWasp.services.get(name).capsName() + " is already running.");
                                    } else {
                                        HoneyWasp.bot = HoneyWasp.services.get(name).serviceObject().get();
                                        HoneyWasp.runningServices.put(name.toLowerCase(), HoneyWasp.bot);
                                        HoneyWasp.bot.start();
                                    }
                                }
                            } else {
                                if (!HoneyWasp.services.containsKey(service)) {
                                    Output.print(null, "Invalid service");
                                    break;
                                }
                                if (HoneyWasp.runningServices.containsKey(service)) {
                                    Output.print(null, HoneyWasp.services.get(service).capsName() + " is already running. Stop it first.");
                                } else {
                                    HoneyWasp.bot = HoneyWasp.services.get(service).serviceObject().get();
                                    HoneyWasp.runningServices.put(service, HoneyWasp.bot);
                                    HoneyWasp.bot.start();
                                }
                            }
                            break;
                        }
                        case "stop": {
                            if (service == null) {
                                Output.print(null, "/stop need two fields: A command, and a service. Try /help for help");
                                continue;
                            }

                            if (service.equals("all")) {
                                for (String name : HoneyWasp.services.keySet()) {
                                    if (HoneyWasp.runningServices.containsKey(name)) {
                                        HoneyWasp.runningServices.get(name).halt();
                                    } else {
                                        Output.print(null, HoneyWasp.services.get(name).capsName() + " is not running.");
                                    }
                                }
                            } else {
                                if (!HoneyWasp.services.containsKey(service)) {
                                    Output.print(null, "Invalid service");
                                    break;
                                }
                                if (HoneyWasp.runningServices.containsKey(service)) {
                                    HoneyWasp.runningServices.get(service).halt();
                                } else {
                                    Output.print(null, HoneyWasp.services.get(service).capsName() + " is not running");
                                }
                            }
                            break;
                        }
                        case "refresh": {
                            HoneyWasp.loadConfig();
                            for (String name : HoneyWasp.runningServices.keySet()) {
                                HoneyWasp.runningServices.get(name).refreshConfig();
                                HoneyWasp.runningServices.get(name).loadConfig();
                            }

                            Output.webhookPrint(null, "The config has been refreshed");

                            break;
                        }
                        case "clear": {
                            if (service == null) {
                                Output.print(null, "/clear need two fields: A command, and a service. Try /help for help");
                                continue;
                            }

                            if (service.equals("all")) {
                                for (String name : HoneyWasp.services.keySet()) {
                                    Cache.clearCache(name);
                                }
                            } else {
                                if (!HoneyWasp.services.containsKey(service)) {
                                    Output.print(null, "Invalid service");
                                    break;
                                }
                                Cache.clearCache(service);
                            }
                            break;
                        }
                        case "quit": {
                            System.exit(0);
                            break;
                        }
                        case "update": {
                            Output.print(null, "Are you sure you want to update HoneyWasp?" +
                                    "\n\tYou cannot close HoneyWasp while its updating" +
                                    "\n\tIt will use download a few hundred megabytes of data from GitHub" +
                                    "\n\tIt will close and reopen when it is finished" +

                                    "\n\n\tType \"Y\" to update, or anything else to cancel...");
                            line = reader.readLine("    >");

                            if (line.equalsIgnoreCase("y")) {
                                if (Update.check()) {
                                    Update.attemptUpdate();
                                } else {
                                    Output.print(null, "You are already on the latest version of HoneyWasp");
                                }
                            }

                            break;
                        }
                        default: {
                            if (command.startsWith("/")) { // If command, treat as such, else make available for other thread
                                Output.print(null, "Command not recognized. A list of commands can be found below:" +
                                        "\n\t/start [SERVICE/ALL] - Start a service" +
                                        "\n\t/stop [SERVICE/ALL] - Stop a service" +
                                        "\n\t/clear [SERVICE/ALL] - Clear a service's duplicate cache" +
                                        "\n\t/update - Attempt to update HoneyWasp to the latest version" +
                                        "\n\t/quit - Quit HoneyWasp" +


                                        "\n\n\tAfter per-service commands, put which service you want to use it on, or \"All\" for all services:" +
                                        "\n\tExamples: /start Instagram, /clear YouTube, /stop all" +
                                        "\n\tNote that /quit and /update do not require a service");
                            } else {
                                miscInput = command;
                                Sleep.milliseconds(null, 500); // Sleep to give response() time to catch it
                            }
                        }
                        break;
                    }

                }
            } catch (Exception e) {
                Output.debugPrint(null, "Input terminal encountered an error" +
                        "\n\tReason: " + e);
            }
        }
    }
    public static String response() { // Awaits a response from the user (Yeah, yeah, its busy waiting, I know.)
        newInput = false;
        while (!newInput) {
            line = reader.readLine("    >");
            newInput = true;
            try {Sleep.milliseconds(null,100);} catch (Exception _) {}
        }

        newInput = false;

        try {Sleep.milliseconds(null,1000);} catch (Exception _) {}
        return line;
    }
}
