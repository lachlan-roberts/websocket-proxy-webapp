package com.acme;

import java.net.URI;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.eclipse.jetty.websocket.core.FrameHandler;
import org.eclipse.jetty.websocket.core.client.WebSocketCoreClient;
import org.eclipse.jetty.websocket.servlet.FrameHandlerFactory;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class WebSocketProxyServlet extends WebSocketServlet
{
    private WebSocketCoreClient client;
    private URI uri;

    @Override
    public void configure(WebSocketServletFactory factory)
    {
        WebSocketCreator creator = (req, resp) -> new WebSocketProxy(client, uri).client2Proxy;
        factory.setCreator(creator);
    }

    @Override
    public FrameHandlerFactory getFactory()
    {
        return (pojo, req, resp) -> (FrameHandler)pojo;
    }

    @Override
    public void destroy()
    {
        super.destroy();

        try
        {
            client.stop();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        try
        {
            client = new WebSocketCoreClient();
            client.start();
            uri = new URI("ws://localhost:8080/test/echo");
        }
        catch (Throwable t)
        {
            throw new ServletException(t);
        }

        super.init(config);
    }
}
