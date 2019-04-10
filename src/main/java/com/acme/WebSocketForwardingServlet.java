package com.acme;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

public class WebSocketForwardingServlet extends JettyWebSocketServlet
{
    private WebSocketClient client;
    private URI serverUri;

    public void configure(JettyWebSocketServletFactory factory)
    {
        try
        {
            JettyWebSocketCreator creator = (req, resp) ->
            {
                List<ExtensionConfig> configs = new ArrayList<>();
                for (ExtensionConfig config : req.getExtensions())
                    configs.add(ExtensionConfig.parse(config.getParameterizedName()));
                resp.setExtensions(configs);

                return new ForwardingSocket(client, serverUri);
            };

            factory.setCreator(creator);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
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
            client = new WebSocketClient();
            client.start();
            serverUri = new URI("ws://localhost:8080/test/stdout");
        }
        catch (Throwable e)
        {
            throw new ServletException(e);
        }

        super.init(config);
    }

    @WebSocket
    public static class ForwardingSocket extends TrackingSocket
    {
        private final WebSocketClient client;
        private final URI serverUri;
        private TrackingSocket clientSocket;

        public ForwardingSocket(WebSocketClient client, URI serverUri)
        {
            super(ForwardingSocket.class.getSimpleName());
            this.client = Objects.requireNonNull(client);
            this.serverUri = Objects.requireNonNull(serverUri);
        }

        @Override
        public void onOpen(Session session)
        {
            super.onOpen(session);

            try
            {
                clientSocket = new TrackingSocket("ForwardingSocketClient");

                CompletableFuture<Session> connect = client.connect(clientSocket, serverUri);
                connect.get(5, TimeUnit.SECONDS);
            }
            catch (Throwable t)
            {
                throw new RuntimeException(t);
            }
        }

        @Override
        public void onMessage(String message)
        {
            super.onMessage(message);

            try
            {
                clientSocket.getSession().getRemote().sendString(message);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onClosed(int statusCode, String reason)
        {
            super.onClosed(statusCode, reason);

            clientSocket.getSession().close(StatusCode.NORMAL, "forwarding initiated close to stdout");
            try
            {
                clientSocket.closed.await(10,TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
}
