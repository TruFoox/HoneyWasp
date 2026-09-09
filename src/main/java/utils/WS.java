package utils;

import main.HoneyWasp;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.ServerWebSocketContainer;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;

public class WS {

    private Server server;

    public void start(int port) throws Exception {
        server = new Server(port);

        ContextHandler contextHandler = new ContextHandler("/");
        server.setHandler(contextHandler);

        WebSocketUpgradeHandler webSocketHandler =
                WebSocketUpgradeHandler.from(
                        server,
                        contextHandler,
                        container -> {
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
            Output.print(null, "WebSocket client connected", Output.YELLOW, false, false);

            session.sendText("connected", null);

            session.sendText(String.valueOf(HoneyWasp.currentVersion), null);
        }

        @OnWebSocketMessage
        public void onMessage(Session session, String message) {
            Output.debugPrint(null, "Received: " + message);
        }
    }
}