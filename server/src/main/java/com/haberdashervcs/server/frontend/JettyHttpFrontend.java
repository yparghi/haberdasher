package com.haberdashervcs.server.frontend;

import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.haberdashervcs.server.protobuf.CommitsProto;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;


public class JettyHttpFrontend implements Frontend {

    @Override
    public void startInBackground() throws Exception {
        // BEGIN: TEMP JETTY TESTING

        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);

        // $ echo -n "haberdasher" | md5
        // 6b07689d9997423c2abd564445ac3c07
        // 3c07 as decimal: 15367
        connector.setPort(15367);

        server.setConnectors(new Connector[] {connector});

        ServletContextHandler handler = new ServletContextHandler(server, "/example");
        handler.addServlet(ExampleServlet.class, "/");

        server.start();
        // END: TEMP JETTY TESTING
    }


    // $ curl -v 'localhost:15367/example/'
    public static class ExampleServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {

            CommitsProto.CommitEntry out = CommitsProto.CommitEntry.newBuilder()
                    .setRootFolderId("someRootFolder")
                    .build();

            resp.setStatus(HttpStatus.OK_200);
            resp.setContentType("application/octet-stream");
            ServletOutputStream outStream = resp.getOutputStream();
            outStream.write(out.toByteArray());
        }
    }
}
