//
//  ========================================================================
//  Copyright (c) 1995-2019 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package server;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.acme.EchoServlet;
import com.acme.TrackingSocket;
import com.acme.WebSocketProxyServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletContainerInitializer;

public class WebSocketProxyTest
{
    public static void main(String[] args) throws Exception
    {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        server.addConnector(connector);

        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/test/");
        server.setHandler(contextHandler);

        contextHandler.addServlet(WebSocketProxyServlet.class, "/proxy");
        contextHandler.addServlet(EchoServlet.class, "/echo");

        WebSocketClient client = new WebSocketClient();
        JettyWebSocketServletContainerInitializer.configureContext(contextHandler);

        try
        {
            server.start();
            client.start();

            URI uri = URI.create("ws://localhost:8080/test/proxy");
            TrackingSocket socket = new TrackingSocket();
            CompletableFuture<Session> connect = client.connect(socket, uri);
            try(Session session = connect.get(5, TimeUnit.SECONDS))
            {
                session.getRemote().sendString("hello world");
            }

            if(!socket.closed.await(5, TimeUnit.SECONDS))
                throw new IllegalStateException();
        }
        finally
        {
            client.stop();
            server.stop();
        }
    }
}
