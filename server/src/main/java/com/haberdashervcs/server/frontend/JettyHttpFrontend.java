package com.haberdashervcs.server.frontend;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.haberdashervcs.common.io.ProtobufObjectOutputStream;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.server.datastore.HdDatastore;
import com.haberdashervcs.server.operations.checkout.CheckoutResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;


public class JettyHttpFrontend implements Frontend {

    public static JettyHttpFrontend forDatastore(HdDatastore datastore) {
        return new JettyHttpFrontend(datastore);
    }


    private final HdDatastore datastore;

    private JettyHttpFrontend(HdDatastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public void startInBackground() throws Exception {
        // $ echo -n "haberdasher" | md5
        // 6b07689d9997423c2abd564445ac3c07
        // 3c07 as decimal: 15367
        Server server = new Server(15367);
        server.setHandler(new RootHandler(datastore));
        server.start();
    }


    // TODO:
    // - General exception handler -- output some json envelope?
    //
    // $ curl -v 'localhost:15367/some_org/some_repo/checkout?path=%2Fsomepath&commit=xxx'
    private static class RootHandler extends AbstractHandler {

        private static final HdLogger LOG = HdLoggers.create(RootHandler.class);

        private static final Splitter PATH_PART_SPLITTER = Splitter.on('/');


        private final HdDatastore datastore;

        private RootHandler(HdDatastore datastore) {
            this.datastore = datastore;
        }

        @Override
        public void handle(
                String target,
                Request baseRequest,
                HttpServletRequest request,
                HttpServletResponse response)
                throws IOException {
            baseRequest.setHandled(true);

            final String path = request.getPathInfo().substring(1);
            final List<String> parts = PATH_PART_SPLITTER.splitToList(path);
            final Map<String, String[]> params = request.getParameterMap();
            if (parts.size() != 3) {
                LOG.info("Got parts: %s", String.join(", ", parts));
                response.setStatus(HttpStatus.NOT_FOUND_404);
                return;
            }

            final String org = parts.get(0);
            final String repo = parts.get(1);
            final String op = parts.get(2);
            LOG.info("Org %s, Repo %s, op %s", org, repo, op);

            switch (op) {
                case "checkout":
                    handleCheckout(response, org, repo, params);
                    break;
                default:
                    response.setStatus(HttpStatus.NOT_FOUND_404);
                    break;
            }
        }

        private void handleCheckout(
                HttpServletResponse response, String org, String repo, Map<String, String[]> params)
                throws IOException {
            String path = getOneParam("path", params);
            String commit = getOneParam("commit", params);
            if (path == null || commit == null) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                return;
            }

            ProtobufObjectOutputStream protoOut = ProtobufObjectOutputStream.forOutputStream(response.getOutputStream());
            CheckoutResult result = datastore.checkout(org, repo, commit, path, protoOut);

            response.setContentType("application/octet-stream");
            if (result.getStatus() == CheckoutResult.Status.OK) {
                response.setStatus(HttpStatus.OK_200);
            } else {
                LOG.error("Failed checkout: %s", result.getErrorMessage());
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            }
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
