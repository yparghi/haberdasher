package com.haberdashervcs.server.frontend;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Splitter;
import com.google.common.html.HtmlEscapers;
import com.haberdashervcs.common.io.HdObjectByteConverter;
import com.haberdashervcs.common.io.HdObjectInputStream;
import com.haberdashervcs.common.io.ProtobufObjectByteConverter;
import com.haberdashervcs.common.io.ProtobufObjectInputStream;
import com.haberdashervcs.common.io.ProtobufObjectOutputStream;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;
import com.haberdashervcs.server.datastore.HdDatastore;
import com.haberdashervcs.server.operations.checkout.CheckoutResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;


public class JettyHttpVcsFrontend implements VcsFrontend {

    public static JettyHttpVcsFrontend forDatastore(HdDatastore datastore) {
        return new JettyHttpVcsFrontend(datastore);
    }


    private final HdDatastore datastore;

    private JettyHttpVcsFrontend(HdDatastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public void startInBackground() throws Exception {
        // $ echo -n "haberdasher" | md5
        // 6b07689d9997423c2abd564445ac3c07
        // 3c07 as decimal: 15367
        Server server = new Server();
        ServerConnector httpConnector = new ServerConnector(server);
        httpConnector.setHost("0.0.0.0");
        httpConnector.setPort(15367);
        server.addConnector(httpConnector);

        server.setHandler(new RootHandler(datastore));
        server.start();
    }


    // TODO:
    // - General exception handler -- output some json envelope?
    //
    // $ curl -v 'localhost:15367/some_org/some_repo/checkout?path=%2Fsomepath&commit=xxx'
    // $ curl -v 'localhost:15367/some_org/some_repo/headCommitOnBranch?branchName=xxx'
    private static class RootHandler extends AbstractHandler {

        private static final HdLogger LOG = HdLoggers.create(RootHandler.class);

        private static final Splitter PATH_PART_SPLITTER = Splitter.on('/');


        private final HdDatastore datastore;
        private final HdObjectByteConverter byteConv;

        private RootHandler(HdDatastore datastore) {
            this.datastore = datastore;
            // TODO: Pass this in.
            this.byteConv = ProtobufObjectByteConverter.getInstance();
        }

        @Override
        public void handle(
                String target,
                Request baseRequest,
                HttpServletRequest request,
                HttpServletResponse response)
                throws IOException {
            try {
                handleInternal(target, baseRequest, request, response);
            } catch (Throwable ex) {
                LOG.exception(ex, "Error handling request");
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            }
        }

        private void handleInternal(
                String target,
                Request baseRequest,
                HttpServletRequest request,
                HttpServletResponse response)
                throws IOException {
            baseRequest.setHandled(true);

            final String path = request.getPathInfo().substring(1);
            if (path.equals("health")) {
                response.setStatus(HttpStatus.OK_200);
                return;
            }
            final List<String> parts = PATH_PART_SPLITTER.splitToList(path);
            final Map<String, String[]> params = request.getParameterMap();
            if (parts.size() != 4) {
                LOG.info("Got parts: %s", String.join(", ", parts));
                response.setStatus(HttpStatus.NOT_FOUND_404);
                return;
            }

            final String backendType = parts.get(0);
            if (!backendType.equals("vcs")) {
                notFound(response);
                return;
            }

            final String org = parts.get(1);
            final String repo = parts.get(2);
            final String op = parts.get(3);
            LOG.info("Org %s, Repo %s, op %s", org, repo, op);

            switch (op) {
                case "checkout":
                    handleCheckout(response, org, repo, params);
                    break;
                case "headCommitOnBranch":
                    handleHeadCommitOnBranch(response, org, repo, params);
                    break;
                case "push":
                    handlePush(baseRequest, request, response, org, repo, params);
                    break;
                default:
                    notFound(response);
                    break;
            }
        }

        private void handlePush(
                Request baseRequest,
                HttpServletRequest request,
                HttpServletResponse response,
                String org,
                String repo,
                Map<String, String[]> params)
                throws IOException {

            LOG.debug("TEMP: got push: %s", params);

            String branchName = getOneParam("branchName", params);
            long baseCommitId = Long.parseLong(getOneParam("baseCommitId", params));
            long newHeadCommitId = Long.parseLong(getOneParam("newHeadCommitId", params));
            HdObjectInputStream objectsIn = ProtobufObjectInputStream.forInputStream(
                    request.getInputStream());

            datastore.writeObjectsFromPush(org, repo, branchName, baseCommitId, newHeadCommitId, objectsIn);

            response.setStatus(HttpStatus.OK_200);
        }


        private void handleHeadCommitOnBranch(
                HttpServletResponse response, String org, String repo, Map<String, String[]> params)
                throws IOException {
            String branchName = getOneParam("branchName", params);
            if (branchName == null) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                return;
            }

            Optional<BranchAndCommit> branchAndCommit = datastore.getHeadCommitForBranch(org, repo, branchName);

            response.setContentType("application/octet-stream");
            if (branchAndCommit.isPresent()) {
                response.setStatus(HttpStatus.OK_200);
                response.getOutputStream().write(byteConv.branchAndCommitToBytes(branchAndCommit.get()));
            } else {
                response.setStatus(HttpStatus.NOT_FOUND_404);
            }
        }


        private void handleCheckout(
                HttpServletResponse response, String org, String repo, Map<String, String[]> params)
                throws IOException {
            String path = getOneParam("path", params);
            long commit = Long.parseLong(getOneParam("commit", params));
            String branchName = getOneParam("branchName", params);
            if (path == null || commit <= 0 || branchName == null) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                return;
            }

            ProtobufObjectOutputStream protoOut = ProtobufObjectOutputStream.forOutputStream(response.getOutputStream());
            CheckoutResult result = datastore.checkout(org, repo, branchName, commit, path, protoOut);

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


        private void notFound(HttpServletResponse response) throws IOException {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            response.getWriter().print("<html><body><p>404 Not Found</p></body></html>");
        }

        private String htmlEnc(String s) {
            return HtmlEscapers.htmlEscaper().escape(s);
        }

    }
}
