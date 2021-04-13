package com.haberdashervcs.server.frontend;

import java.io.IOException;

import com.haberdashervcs.common.io.ProtobufObjectOutputStream;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.CommitEntry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;


public class JettyHttpFrontend implements Frontend {

    @Override
    public void startInBackground() throws Exception {
        // $ echo -n "haberdasher" | md5
        // 6b07689d9997423c2abd564445ac3c07
        // 3c07 as decimal: 15367
        Server server = new Server(15367);
        server.setHandler(new RootHandler());

        server.start();
    }


    // localhost:15367/basic-test-repo/checkout/%s?commit=xxx
    private static class RootHandler extends AbstractHandler {

        private static final HdLogger LOG = HdLoggers.create(RootHandler.class);

        @Override
        public void handle(
                String target,
                Request jettyRequest,
                HttpServletRequest request,
                HttpServletResponse response)
                throws IOException {
            LOG.info("Request target: {}", target);

            response.setStatus(HttpStatus.OK_200);
            response.setContentType("application/octet-stream");

            ProtobufObjectOutputStream protoOut = ProtobufObjectOutputStream.forOutputStream(response.getOutputStream());

            protoOut.writeCommit(
                    "some-test-commit", CommitEntry.forRootFolderId("some-root-folder"));
        }
    }
}
