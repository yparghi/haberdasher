package com.haberdashervcs.server.frontend;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.base.Splitter;
import com.haberdashervcs.common.io.ProtobufObjectOutputStream;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.server.operations.checkout.CheckoutOperation;
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


    // $ curl 'localhost:15367/yashorg/basic-test-repo/checkout?path=somepath&commit=xxx'
    private static class RootHandler extends AbstractHandler {

        private static final HdLogger LOG = HdLoggers.create(RootHandler.class);

        private static final Splitter PATH_PART_SPLITTER = Splitter.on('/');

        @Override
        public void handle(
                String target,
                Request baseRequest,
                HttpServletRequest request,
                HttpServletResponse response)
                throws IOException {
            final String path = request.getPathInfo().substring(1);
            final List<String> parts = PATH_PART_SPLITTER.splitToList(path);
            final Map<String, String[]> params = request.getParameterMap();
            if (parts.size() != 3) {
                response.setStatus(HttpStatus.NOT_FOUND_404);
                return;
            }

            final String org = parts.get(0);
            final String repo = parts.get(1);
            final String op = parts.get(2);

            switch (op) {
                case "checkout":
                    handleCheckout(response, org, repo, params);
                    break;
                default:
                    response.setStatus(HttpStatus.NOT_FOUND_404);
                    return;
            }

            baseRequest.setHandled(true);
        }

        private void handleCheckout(
                HttpServletResponse response, String org, String repo, Map<String, String[]> params)
                throws IOException {
            ProtobufObjectOutputStream protoOut = ProtobufObjectOutputStream.forOutputStream(response.getOutputStream());
            protoOut.writeCommit(
                    "some-test-commit", CommitEntry.forRootFolderId("some-root-folder"));

            String path = getOneParam("path", params);
            String commit = getOneParam("commit", params);
            if (path == null || commit == null) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                return;
            }

            CheckoutOperation op = new CheckoutOperation(org, repo, path, commit);
            // TODO run it...

            response.setStatus(HttpStatus.OK_200);
            response.setContentType("application/octet-stream");
        }

        private String getOneParam(String key, Map<String, String[]> map) {
            if (!map.containsKey(key)) {
                return null;
            }
            String[] value = map.get(key);
            if (value.length != 1) {
                return null;
            }
            return value[0];
        }
    }
}
