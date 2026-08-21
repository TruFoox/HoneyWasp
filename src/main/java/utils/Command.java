package utils;
import main.HoneyWasp;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.Status;

public class Command extends Thread{
    static Terminal terminal;
    static LineReader reader;
    static Status status;

    public void run() {
        try {
            terminal = TerminalBuilder.builder().system(true).build();

            status = Status.getStatus(terminal);

            reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            // Read lines from the user
            while (true) {
                String line = reader.readLine("    >");

                int space;
                try {
                    space = line.indexOf(' ');
                } catch (Exception _) {
                    Output.print(null, "Commands need two fields: A command, and a service (Eg /start Instagram)");
                    continue;
                }

                String command = line.substring(line.indexOf('/') + 1, space).toLowerCase(); // Works both with / & without
                String service = line.substring(space + 1).toLowerCase();

                switch (command) {
                    case "start": {
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
                        if (service.equals("all")) {
                            for (String name : HoneyWasp.services.keySet()) {
                                if (HoneyWasp.runningServices.containsKey(name)) {
                                    HoneyWasp.runningServices.get(name).halt();
                                } else {
                                    Output.print(null, HoneyWasp.services.get(name).capsName() + " is not running.");
                                }
                            }
                        } else {
                            if (HoneyWasp.runningServices.containsKey(service)) {
                                HoneyWasp.runningServices.get(service).halt();
                            } else {
                                Output.print(null, HoneyWasp.services.get(service).capsName() + " is not running");
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
                    case "help": {
                        Output.print(null, "A list of commands can be found below:" +
                                "\n\t/start - Start a service" +
                                "\n\t/stop - Stop a service" +
                                "\n\t/clear - Clear a service's duplicate cache" +
                                "\n\tAfter the command, put which service you want to use it on, or \"All\" for all services:" +
                                "\n\tExamples: /start Instagram, /clear Youtube, /stop all");
                    }
                    default: {
                        Output.print(null, "Command not recognized. Try /help for a list of commands");
                    }
                }

            }
        } catch (Exception e) {
            Output.webhookPrint(null, "Terminal had failed." +
                    "\n\tReason: " + e.getMessage());
        }
    }


}
