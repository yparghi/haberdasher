package com.haberdashervcs.server.frontend;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.html.HtmlEscapers;
import com.haberdashervcs.common.io.HdObjectByteConverter;
import com.haberdashervcs.common.io.ProtobufObjectByteConverter;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchEntry;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.server.browser.BranchDiff;
import com.haberdashervcs.server.browser.FileDiff;
import com.haberdashervcs.server.browser.LineDiff;
import com.haberdashervcs.server.browser.RepoBrowser;
import com.haberdashervcs.server.datastore.HdDatastore;
import com.haberdashervcs.common.objects.user.AuthResult;
import com.haberdashervcs.common.objects.user.UserAuthToken;
import com.haberdashervcs.common.objects.user.HdAuthenticator;
import com.haberdashervcs.common.objects.user.HdUserStore;
import freemarker.template.Configuration;
import freemarker.template.Template;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;


public class JettyHttpWebFrontend implements WebFrontend {

    public static JettyHttpWebFrontend forDatastore(
            HdDatastore datastore, HdUserStore userStore, HdAuthenticator authenticator) {
        return new JettyHttpWebFrontend(datastore, userStore, authenticator);
    }


    private final HdDatastore datastore;
    private final HdUserStore userStore;
    private final HdAuthenticator authenticator;

    private JettyHttpWebFrontend(HdDatastore datastore, HdUserStore userStore, HdAuthenticator authenticator) {
        this.datastore = datastore;
        this.userStore = userStore;
        this.authenticator = authenticator;
    }

    @Override
    public void startInBackground() throws Exception {
        QueuedThreadPool threadPool = new QueuedThreadPool(20, 5);
        Server server = new Server(threadPool);

        ServerConnector httpConnector = new ServerConnector(server);
        httpConnector.setHost("0.0.0.0");
        httpConnector.setPort(15368);
        server.addConnector(httpConnector);

        server.setHandler(new RootHandler(datastore, userStore, authenticator));
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
        private final HdUserStore userStore;
        private final HdAuthenticator authenticator;
        private final HdObjectByteConverter byteConv;
        private final Configuration templateConfig;

        private RootHandler(HdDatastore datastore, HdUserStore userStore, HdAuthenticator authenticator) {
            this.datastore = datastore;
            this.userStore = userStore;
            this.authenticator = authenticator;
            // TODO: Pass this in.
            this.byteConv = ProtobufObjectByteConverter.getInstance();

            this.templateConfig = new Configuration(Configuration.VERSION_2_3_31);
            templateConfig.setClassForTemplateLoading(this.getClass(), "/webtemplates");
            templateConfig.setDefaultEncoding("UTF-8");
        }

        @Override
        public void handle(
                String target,
                Request baseRequest,
                HttpServletRequest request,
                HttpServletResponse response) {
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
                throws Exception {
            baseRequest.setHandled(true);
            final String path = request.getPathInfo().substring(1);
            LOG.info("Web: Got path: %s", path);

            final Map<String, String[]> params = request.getParameterMap();
            if (path.equals("health")) {
                response.setStatus(HttpStatus.OK_200);
                return;
            } else if (path.equals("login")) {
                handleLogin(baseRequest, request, response, params);
                return;
            }

            String webTokenId = request.getHeader("X-Haberdasher-Web-Auth-Token");
            if (webTokenId == null) {
                notAuthorized(response, "Please log in to perform this action.");
                return;
            }
            UserAuthToken token = authenticator.webTokenForId(webTokenId);

            final List<String> parts = PATH_PART_SPLITTER.splitToList(path);
            if (parts.size() != 4) {
                LOG.info("Got parts: %s", String.join(", ", parts));
                notFound(response);
                return;
            }

            final String backendType = parts.get(0);
            if (!backendType.equals("web")) {
                notFound(response);
                return;
            }

            final String org = parts.get(1);
            final String repo = parts.get(2);
            final String op = parts.get(3);
            LOG.info("Org %s, Repo %s, op %s", org, repo, op);

            AuthResult authResult = authenticator.canAccessRepo(token, org, repo);
            if (authResult.getType() == AuthResult.Type.AUTH_EXPIRED) {
                notAuthorized(response, "Your login session has expired. Please log in again.");
                return;
            } else if (authResult.getType() == AuthResult.Type.FORBIDDEN) {
                notAuthorized(response, "You are not authorized to perform that action.");
                return;
            } else if (authResult.getType() != AuthResult.Type.PERMITTED) {
                throw new IllegalStateException("Unexpected Auth type!");
            }

            switch (op) {
                case "view":
                    handleView(baseRequest, request, response, org, repo, params);
                    break;
                case "diff":
                    handleDiff(baseRequest, request, response, org, repo, params);
                    break;
                default:
                    notFound(response);
                    break;
            }
        }


        private void handleLogin(
                Request baseRequest,
                HttpServletRequest request,
                HttpServletResponse response,
                Map<String, String[]> params)
                throws Exception {
            // TODO! If the user is already logged in, redirect...

            if (request.getMethod().equals("GET")) {
                response.setStatus(HttpStatus.OK_200);
                Template template = templateConfig.getTemplate("login.ftlh");
                template.process(ImmutableMap.of(), response.getWriter());

            } else if (request.getMethod().equals("POST") && request.getHeader("Content-Type").equals("application/x-www-form-urlencoded")) {
                LOG.info("Got urlencoded params: %s", params);
                response.setStatus(HttpStatus.OK_200);
                response.getWriter().print("<html><body><p>OK, logged in</p></body></html>");

            } else if (request.getMethod().equals("POST") && request.getHeader("Content-Type").equals("multipart/form-data")) {
                Collection<Part> formParts = request.getParts();
                LOG.info("Got form parts: %s", formParts);
                response.setStatus(HttpStatus.OK_200);
                response.getWriter().print("<html><body><p>OK, logged in</p></body></html>");

            } else {
                response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405);
                return;
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

        private void handleView(
                Request baseRequest,
                HttpServletRequest request,
                HttpServletResponse response,
                String org,
                String repo,
                Map<String, String[]> params)
                throws IOException {

            LOG.debug("TEMP: got view: %s", params);

            // TODO: Optionally, a specific commit?
            String branchName = getOneParam("branchName", params);
            String path = getOneParam("path", params);
            if (branchName == null || path == null) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                return;
            }

            RepoBrowser browser = datastore.getBrowser(org, repo);
            Optional<BranchEntry> branch = browser.getBranch(branchName);
            if (!branch.isPresent()) {
                notFound(response);
                return;
            }

            FolderListing rootListing = browser.getFolderAt(branchName, path, branch.get().getHeadCommitId());

            // TODO: Templating engine
            StringBuilder out = new StringBuilder();
            out.append("<html><head><title>Browse Repo</title></head><body>");
            out.append("<h3>Viewing path: " + htmlEnc(path) + "</h3>");
            out.append("<ul>");
            for (FolderListing.Entry entry : rootListing.getEntries()) {
                if (entry.getType() == FolderListing.Entry.Type.FOLDER) {
                    out.append("<li>Folder: " + htmlEnc(entry.getName()));
                } else {
                    out.append("<li>File: " + htmlEnc(entry.getName()));
                }
            }
            out.append("</ul></body></html>");

            response.setStatus(HttpStatus.OK_200);
            response.getWriter().print(out.toString());
        }

        private void notFound(HttpServletResponse response) throws IOException {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            response.getWriter().print("<html><body><p>404 Not Found</p></body></html>");
        }

        private void notAuthorized(HttpServletResponse response, String message) throws IOException {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            response.getWriter().print(
                    String.format("<html><body><p>401 Not Authorized</p><p>%s</p></body></html>", message));
        }

        private String htmlEnc(String s) {
            return HtmlEscapers.htmlEscaper().escape(s);
        }


        private void handleDiff(
                Request baseRequest,
                HttpServletRequest request,
                HttpServletResponse response,
                String org,
                String repo,
                Map<String, String[]> params)
                throws IOException {

            LOG.debug("TEMP: got diff: %s", params);

            // TODO: Optionally, a specific commit?
            String branchName = getOneParam("branchName", params);
            if (branchName == null) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                return;
            }

            RepoBrowser browser = datastore.getBrowser(org, repo);
            Optional<BranchEntry> branch = browser.getBranch(branchName);
            if (!branch.isPresent()) {
                notFound(response);
                return;
            }

            BranchDiff diff = browser.getDiffToMain(branch.get().getName());

            // TODO: Templating engine
            StringBuilder out = new StringBuilder();
            out.append("<html><head><title>Diff Branch</title></head><body>");
            out.append("<h2>Diff for branch: " + htmlEnc(branchName) + "</h3>");
            out.append("<ul>");

            for (FileDiff fDiff : diff.getFileDiffs()) {
                out.append("<h3>Diff: " + htmlEnc(fDiff.getPath())+ "</h3>");
                out.append("<ul>");
                for (LineDiff lDiff : fDiff.getDiffs()) {
                    out.append(String.format("<li>%s", htmlEnc(lDiff.getDisplayString())));
                }
                out.append("</ul>");
            }

            out.append("</ul></body></html>");

            response.setStatus(HttpStatus.OK_200);
            response.getWriter().print(out.toString());
        }

    }
}
