package com.acme;

import org.eclipse.jetty.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

public class StdoutServlet extends JettyWebSocketServlet
{
    @Override
    public void configure(JettyWebSocketServletFactory factory)
    {
        JettyWebSocketCreator creator = (req, resp) -> new TrackingSocket("StdoutServlet");
        factory.setCreator(creator);
    }
}
