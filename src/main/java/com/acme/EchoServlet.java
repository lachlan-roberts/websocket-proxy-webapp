package com.acme;

import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

public class EchoServlet extends JettyWebSocketServlet
{
    @Override
    public void configure(JettyWebSocketServletFactory factory)
    {
        JettyWebSocketCreator creator = (req, resp) -> new EchoSocket();
        factory.setCreator(creator);
    }

    @WebSocket
    public static class EchoSocket extends TrackingSocket
    {
        @Override
        public void onMessage(String message)
        {
            super.onMessage(message);
            getSession().getRemote().sendStringByFuture(message);
        }
    }
}
