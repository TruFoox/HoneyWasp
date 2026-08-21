package utils;
import main.HoneyWasp;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class Command extends Thread{
    static Terminal terminal;
    static LineReader reader;

    public void run() {
        try {
            terminal = TerminalBuilder.builder().system(true).build();

            reader = LineReaderBuilder.builder().terminal(terminal).build();

            // Read lines from the user
            while (true) {
                String line = reader.readLine("    >");

                int space = line.indexOf(' ');

                String command = line.substring(line.indexOf('/') + 1, space).toLowerCase(); // Works both with / & without
                String service = line.substring(space + 1).toLowerCase();

                switch (command) {
                    case "start": {
                        if (service.equals("all")) {

                            for (String name : HoneyWasp.services.keySet()) {
                                if (HoneyWasp.runningServices.containsKey(name)) {
                                    Output.webhookPrint(null, HoneyWasp.services.get(name).capsName() + " is already running.");
                                } else {
                                    HoneyWasp.bot = HoneyWasp.services.get(name).serviceObject().get();
                                    HoneyWasp.runningServices.put(name.toLowerCase(), HoneyWasp.bot);
                                    HoneyWasp.bot.start();
                                }
                            }
                        } else {
                            if (HoneyWasp.runningServices.containsKey(service)) {
                                Output.webhookPrint(null, HoneyWasp.services.get(service).capsName() + " is already running. Stop it first.");
                            } else {
                                HoneyWasp.bot = HoneyWasp.services.get(service).serviceObject().get();
                                HoneyWasp.runningServices.put(service, HoneyWasp.bot);
                                HoneyWasp.bot.start();
                            }
                        }
                        break;
                    }
                    case "stop": {
                        if (service.equals("all")) {
                            for (String name : HoneyWasp.services.keySet()) {
                                if (HoneyWasp.runningServices.containsKey(name)) {
                                    HoneyWasp.runningServices.get(name).halt();
                                } else {
                                    Output.webhookPrint(null, HoneyWasp.services.get(name).capsName() + " is not running.");
                                }
                            }
                        } else {
                            if (HoneyWasp.runningServices.containsKey(service)) {
                                HoneyWasp.runningServices.get(service).halt();
                            } else {
                                Output.webhookPrint(null, HoneyWasp.services.get(service).capsName() + " is not running");
                            }
                        }
                        break;
                    }
                    case "clear": {
                        if (service.equals("all")) {
                            for (String name : HoneyWasp.services.keySet()) {
                                FileIO.clearList(name);
                            }
                        } else {
                            FileIO.clearList(service);
                        }
                        break;
                    }
                    default: {
                        Output.print(null, "Command not recognized");
                    }
                }

            }
        } catch (Exception e) {
            Output.webhookPrint(null, "Terminal had failed." +
                    "\n\tReason: " + e.getMessage());
        }
    }

    public static synchronized void printAbove(String message) { // Makes prints leave the bottom line clear
        if (reader != null) {
            reader.printAbove(message);
        }
    }
}
