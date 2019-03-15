/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.benchmark.utils;

import static software.amazon.awssdk.benchmark.utils.BenchmarkUtil.JSON_BODY;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyStoreException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Local mock server used to stub fixed response.
 */
public class MockServer {
    private final Server server;

    public MockServer(int port) {
        server = new Server(port);
        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(AlwaysSuccessServlet.class, "/*");
        server.setHandler(handler);
    }

    public MockServer(int httpPort, int httpsPort) throws KeyStoreException {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(httpPort);

        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setTrustAll(true);
        sslContextFactory.setValidateCerts(false);
        sslContextFactory.setNeedClientAuth(false);
        sslContextFactory.setWantClientAuth(false);
        sslContextFactory.setValidatePeerCerts(false);
        sslContextFactory.setKeyStorePassword("123456");
        sslContextFactory.setKeyManagerPassword("123456");
        sslContextFactory.setKeyStorePath(MockServer.class.getResource("jetty.pkcs12").toExternalForm());

        ServerConnector sslConnector = new ServerConnector(server,
                                                           new SslConnectionFactory(sslContextFactory,
                                                                                    HttpVersion.HTTP_1_1.asString()),
                                                           new HttpConnectionFactory(https));
        sslConnector.setPort(httpsPort);

        server.setConnectors(new Connector[] {connector, sslConnector});
        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(AlwaysSuccessServlet.class, "/*");
        server.setHandler(handler);
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

    /**
     * Always succeeds with with a 200 response.
     */
    public static class AlwaysSuccessServlet extends HttpServlet {

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setStatus(HttpStatus.OK_200);
            if (request.getContentType().equals("application/xml")) {
                response.setContentType("application/xml");
            } else {
                response.setContentType("application/json");
            }
            try(PrintWriter writer = response.getWriter()){
                writer.write(JSON_BODY);
            }
        }
    }
}