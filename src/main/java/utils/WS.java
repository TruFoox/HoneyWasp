package utils;

import main.HoneyWasp;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import config.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class WS {

    private Server server;

    public void start(int port) throws Exception {
        server = new Server(port);

        ContextHandler contextHandler = new ContextHandler("/");
        server.setHandler(contextHandler);

        WebSocketUpgradeHandler webSocketHandler =
                WebSocketUpgradeHandler.from(server, contextHandler, container -> {
                            container.setIdleTimeout(Duration.ZERO); // Inf idle timeout
                            container.addMapping("/", (request, response, callback) -> new Socket());
                        }
                );

        contextHandler.setHandler(webSocketHandler);

        server.start();

        Output.print(null, "WebSocket server started", Output.YELLOW, false, false);
    }

    @WebSocket
    public static class Socket {

        @OnWebSocketOpen
        public void onOpen(Session session) {
            Output.debugPrint(null, "WebSocket client connected");
        }

        // Programmed to have two supported formats:
        // Requests with no payload are formatted as just the command
        // Otherwise payloads require format command/payload
        @OnWebSocketMessage
        public void onMessage(Session session, String message) {
            Output.debugPrint(null, "Received: " + message);
            String command = message.substring(0, message.indexOf("\\"));
            String data = null;

            if (message.contains("/")) {data = message.substring(message.indexOf("\\"));} // Only relevant if contains \

            try {
                switch (command) {
                    case "webui-ready":
                        Output.debugPrint(null, "Sending version to WebUI");
                        session.sendText(String.valueOf(HoneyWasp.currentVersion), null);
                        break;
                    case "request-config":
                        Output.debugPrint(null, "Sending config to WebUI");
                        session.sendText("config\\" + Files.readString(Path.of("config.json")), null);
                        break;
                    case "set-value": // set-value\{group of setting}\{config option name}\{new config option value} | Sets config value
                        String settingGroup = data.substring(0, data.indexOf("\\"));
                        String configOption = data.substring(data.indexOf("\\", data.indexOf("\\") + 1) + 1, data.indexOf("\\", data.indexOf("\\") + 2));
                        String configValue = data.substring(data.indexOf("\\", data.indexOf("\\") + 2) + 1);

                        Output.debugPrint(null, "Setting " + configOption + " to " + configValue + " in " + settingGroup);

                        HoneyWasp.config.get(settingGroup).set(configOption, configValue);
                        break;
                    case "restart-service": // restart-service\{service} | For after config value is changed
                        Output.debugPrint(null, "Restarting " + data);

                        if (HoneyWasp.runningServices.containsKey(data)) {
                            HoneyWasp.runningServices.get(data).halt();

                            do { // Busy waiting, hell yeah
                                Sleep.milliseconds(null, 1000);
                            } while (HoneyWasp.runningServices.containsKey(data)); // Wait until bot stopped

                            HoneyWasp.bot = HoneyWasp.services.get(data).serviceObject().get(); // new Instagram, new YouTube, etc
                            HoneyWasp.runningServices.put(data.toLowerCase(), HoneyWasp.bot);
                            HoneyWasp.bot.start();
                        }
                        break;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        @OnWebSocketError
        public void onError(Session session, Throwable error) {
            if (error instanceof EofException) {
                return;
            }

            Output.print(null, "WebSocket error: " + error.getMessage(), Output.RED);
        }
    }
}