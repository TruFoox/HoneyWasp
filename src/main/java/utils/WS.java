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

        @OnWebSocketMessage
        public void onMessage(Session session, String message) {
            Output.debugPrint(null, "Received: " + message);

            try {
                switch (message) {
                    case "webui-ready":
                        session.sendText(String.valueOf(HoneyWasp.currentVersion), null);
                        break;
                    case "send-config":
                        session.sendText("config/" + Files.readString(Path.of("config.json")), null);
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