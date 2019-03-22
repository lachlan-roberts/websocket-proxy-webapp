package com.acme;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
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

    public void configure(WebSocketServletFactory factory)
    {
        WebSocketCreator creator = (req, resp) -> new ForwardingSocket(client, serverUri);
        factory.setCreator(creator);
    }

    WebSocketClient client = new WebSocketClient();
    URI serverUri;

    @Override
    public void init() throws ServletException
    {
        super.init();

        try
        {
            client.start();
            serverUri = new URI("ws://localhost:8080/test/stdout");
        }
        catch (Exception e)
        {
            throw new ServletException(e);
        }
    }

    @Override
    public void destroy()
    {
        try
        {
            client.stop();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        super.destroy();
    }

    @WebSocket
    public static class ForwardingSocket
    {
        private final WebSocketClient client;
        private final URI serverUri;
        private ClientSocket clientSocket;

        public ForwardingSocket(WebSocketClient client, URI serverUri)
        {
            this.client = client;
            this.serverUri = serverUri;
        }

        @OnWebSocketConnect
        public void onOpen(Session session)
        {
            try
            {
                clientSocket = new ClientSocket();
                CompletableFuture<Session> connect = client.connect(clientSocket, serverUri);
                connect.get(5, TimeUnit.SECONDS);
            }
            catch (Exception e)
            {
                session.close(StatusCode.SERVER_ERROR, e.getMessage());
            }
        }

        @OnWebSocketMessage
        public void onMessage(Session session, String message)
        {
            try
            {
                if (message.contains("/close"))
                {
                    System.err.println("[ForwardingSocket] sendingClose: " + message);
                    session.close();
                    clientSocket.getSession().close();
                }
                else
                {
                    System.err.println("[ForwardingSocket] onMessage: " + message);
                    clientSocket.getSession().getRemote().sendString(message);
                }
            }
            catch (IOException e)
            {
                session.close(StatusCode.SERVER_ERROR, e.getMessage());
                clientSocket.getSession().close(StatusCode.SERVER_ERROR, e.getMessage());
            }
        }

        @OnWebSocketClose
        public void onClosed(int statusCode, String reason)
        {
            System.err.println("[ForwardingSocket] onClosed: " + statusCode + ":" + reason);
            clientSocket.getSession().close();
        }

        @OnWebSocketError
        public void onError(Throwable error)
        {
            System.err.println("[ForwardingSocket] onError: " + error);
            error.printStackTrace(System.err);
        }
    }
}
