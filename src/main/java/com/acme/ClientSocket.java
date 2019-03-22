package com.acme;

import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket
public class ClientSocket
{
    public CountDownLatch closed = new CountDownLatch(1);

    private String behavior;
    private Session session;

    @OnWebSocketConnect
    public void onOpen(Session session)
    {
        behavior = session.getPolicy().getBehavior().name();
        this.session = session;
        System.err.println(toString() + " Socket Connected: " + session);
    }

    @OnWebSocketMessage
    public void onMessage(String message)
    {
        System.err.println(toString() + " Received TEXT message: " + message);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason)
    {
        System.err.println(toString() + " Socket Closed: " + statusCode + ":" + reason);
        closed.countDown();
    }

    @OnWebSocketError
    public void onError(Throwable cause)
    {
        System.err.println("[ClientSocket] onError(): " + cause);
        cause.printStackTrace(System.err);
    }

    @Override
    public String toString()
    {
        return String.format("[%s@%s]", behavior, Integer.toHexString(hashCode()));
    }

    public Session getSession()
    {
        return session;
    }
}
