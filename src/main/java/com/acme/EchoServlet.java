package com.acme;

import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class EchoServlet extends WebSocketServlet
{
    @Override
    public void configure(WebSocketServletFactory factory)
    {
        WebSocketCreator creator = (req, resp) -> new ClientSocket()
        {
            @Override
            public void onMessage(String message)
            {
                super.onMessage(message);
                getSession().getRemote().sendStringByFuture(message);
            }
        };

        factory.setCreator(creator);
    }
}
