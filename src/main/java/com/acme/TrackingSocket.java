package com.acme;

import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket
public class TrackingSocket
{
    private static final Logger LOG = Log.getLogger(TrackingSocket.class);

    private final String name;
    private Session session;
    public CountDownLatch closed = new CountDownLatch(1);

    public TrackingSocket()
    {
        this(TrackingSocket.class.getSimpleName());
    }

    public TrackingSocket(String name)
    {
        this.name = name;
    }

    public Session getSession()
    {
        return session;
    }

    @OnWebSocketConnect
    public void onOpen(Session session)
    {
        LOG.info(toString()+"onOpen():  " + session);
        this.session = session;
    }

    @OnWebSocketMessage
    public void onMessage(String message)
    {
        LOG.info(toString()+"onMessage():  " + message);
    }

    @OnWebSocketClose
    public void onClosed(int statusCode, String reason)
    {
        LOG.info(toString()+"onClose():  " + statusCode + ":" + reason);
        closed.countDown();
    }

    @OnWebSocketError
    public void onError(Throwable cause)
    {
        LOG.warn(toString()+"onError():  " + cause);
    }

    @Override
    public String toString()
    {
        return "["+name+"] ";
    }
}
