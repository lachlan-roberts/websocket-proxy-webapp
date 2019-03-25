package com.acme;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class WebSocketForwardingServlet extends WebSocketServlet
{
    WebSocketClient client;
    URI serverUri;

    public void configure(WebSocketServletFactory factory)
    {
        try
        {
            WebSocketCreator creator = (req, resp) -> new ForwardingSocket(client, serverUri);
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
    public static class ForwardingSocket
    {
        private final WebSocketClient client;
        private final URI serverUri;
        private ClientSocket clientSocket;

        public ForwardingSocket(WebSocketClient client, URI serverUri)
        {
            this.client = Objects.requireNonNull(client);
            this.serverUri = Objects.requireNonNull(serverUri);
        }

        @OnWebSocketConnect
        public void onOpen(Session session)
        {
            System.err.println("[ForwardingSocket] onOpen: " + session);
            try
            {
                clientSocket = new ClientSocket("ForwardingSocketClient");
                CompletableFuture<Session> connect = client.connect(clientSocket, serverUri);
                connect.get(5, TimeUnit.SECONDS);
            }
            catch (Throwable t)
            {
                throw new RuntimeException(t);
            }
        }

        @OnWebSocketMessage
        public void onMessage(Session session, String message)
        {
            try
            {
                System.err.println("[ForwardingSocket] onMessage: " + message);
                clientSocket.getSession().getRemote().sendString(message);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        @OnWebSocketClose
        public void onClosed(int statusCode, String reason)
        {
            System.err.println("[ForwardingSocket] onClosed: " + statusCode + ":" + reason);
            clientSocket.getSession().close();
            try
            {
                clientSocket.closed.await(10,TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        @OnWebSocketError
        public void onError(Throwable error)
        {
            System.err.println("[ForwardingSocket] onError: " + error);
            error.printStackTrace(System.err);
        }
    }
}
