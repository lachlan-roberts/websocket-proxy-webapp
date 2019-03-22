package com.acme;

import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class WebSocketStdoutServlet extends WebSocketServlet
{
    public void configure(WebSocketServletFactory factory)
    {
        WebSocketCreator creator = (req, resp) -> new StdoutSocket();
        factory.setCreator(creator);
    }


    @WebSocket
    public static class StdoutSocket
    {
        @OnWebSocketMessage
        public void onMessage(String message)
        {
            System.err.println("[StdoutSocket] Message Received: " + message);
        }

        @OnWebSocketClose
        public void onClosed(int statusCode, String reason)
        {
            System.err.println("[StdoutSocket] onClosed: " + statusCode + ":" + reason);
        }

        @OnWebSocketError
        public void onError(Throwable error)
        {
            System.err.println("[StdoutSocket] onError: " + error);
            error.printStackTrace(System.err);
        }
    }
}
