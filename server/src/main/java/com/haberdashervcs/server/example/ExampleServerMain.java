package com.haberdashervcs.server.example;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.haberdashervcs.server.core.logging.HdLogger;
import com.haberdashervcs.server.core.logging.HdLoggers;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;


/**
 * TODO
 */
public class ExampleServerMain {

    private static HdLogger LOG = HdLoggers.create(ExampleServerMain.class);

    public static void main(String[] args) throws Exception {
        System.out.println( "Hello Haberdasher!" );

        // TODO: Figure out the right way to set up an example cluster -- with some fake URL?
        /*HaberdasherServer server = HaberdasherServer.builder()
                .withDatastore(new HBaseDatastore())
                .build();*/

        // BEGIN: TEMP JETTY TESTING

        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);

        // $ echo -n "haberdasher" | md5
        // 6b07689d9997423c2abd564445acc07
        // As decimal: 15367
        connector.setPort(15367);

        server.setConnectors(new Connector[] {connector});

        ServletContextHandler handler = new ServletContextHandler(server, "/example");
        handler.addServlet(ExampleServlet.class, "/");

        server.start();
        // END: TEMP JETTY TESTING

        System.out.println("Done!");
    }

    private static class ExampleServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            resp.setStatus(HttpStatus.OK_200);
            resp.getWriter().println("EmbeddedJetty");
        }
    }
}
