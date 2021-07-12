package com.haberdashervcs.client.talker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.haberdashervcs.common.io.HdObjectByteConverter;
import com.haberdashervcs.common.io.HdObjectInputStream;
import com.haberdashervcs.common.io.ProtobufObjectByteConverter;
import com.haberdashervcs.common.io.ProtobufObjectInputStream;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.BranchAndCommit;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.util.OutputStreamRequestContent;
import org.eclipse.jetty.http.HttpStatus;


public final class JettyServerTalker implements ServerTalker {

    private static final HdLogger LOG = HdLoggers.create(JettyServerTalker.class);

    public static JettyServerTalker forHost(String host) {
        return new JettyServerTalker(host);
    }


    private final String host;
    private final HdObjectByteConverter byteConv;

    private JettyServerTalker(String host) {
        this.host = Preconditions.checkNotNull(host);
        // TODO: Pass this in
        this.byteConv = ProtobufObjectByteConverter.getInstance();
    }


    @Override
    // TODO rollback on failure?
    public BranchAndCommit headOnBranch(String branchName) throws Exception {
        String serverUrl = String.format(
                "http://%s/some_org/some_repo/headCommitOnBranch?branchName=%s",
                host, URLEncoder.encode(branchName, StandardCharsets.UTF_8));


        // TODO: Close the client -- on shutdown?
        HttpClient httpClient = new HttpClient();
        httpClient.start();

        InputStreamResponseListener listener = new InputStreamResponseListener();
        httpClient.newRequest(serverUrl)
                .send(listener);

        Response response = listener.get(10, TimeUnit.SECONDS);
        if (response.getStatus() != HttpStatus.OK_200) {
            httpClient.stop();
            throw new RuntimeException("Request failed with status: " + response.getStatus());
        } else {
            BranchAndCommit result = readBranchAndCommit(listener.getInputStream());
            // TODO: Apply this use of stop() to other calls, to prevent hanging when the call is
            //     complete.
            httpClient.stop();
            return result;
        }
    }


    private BranchAndCommit readBranchAndCommit(InputStream in) throws IOException {
        return byteConv.branchAndCommitFromBytes(in.readAllBytes());
    }


    private String urlEnc(String in) {
        return URLEncoder.encode(in, StandardCharsets.UTF_8);
    }


    private static class JettyCheckoutContext implements CheckoutContext {

        private final HttpClient httpClient;
        private final InputStreamResponseListener listener;
        private final HdObjectInputStream objIn;

        private JettyCheckoutContext(HttpClient httpClient, InputStreamResponseListener listener) {
            this.httpClient = httpClient;
            this.listener = listener;
            this.objIn = ProtobufObjectInputStream.forInputStream(listener.getInputStream());
        }

        @Override
        public HdObjectInputStream getInputStream() {
            return objIn;
        }

        @Override
        public void finish() throws Exception {
            Response response = listener.get(10, TimeUnit.SECONDS);
            httpClient.stop();
            if (response.getStatus() != HttpStatus.OK_200) {
                throw new RuntimeException(
                        "Push request failed with status: " + response.getStatus());
            }
        }
    }

    @Override
    public CheckoutContext checkout(String branchName, String path, long commitId) throws Exception {
        String serverUrl = String.format(
                "http://%s/some_org/some_repo/checkout?path=%s&commit=%d&branchName=%s",
                host, urlEnc(path), commitId, urlEnc(branchName));

        // TODO: Close the client -- on shutdown?
        HttpClient httpClient = new HttpClient();
        httpClient.start();

        InputStreamResponseListener listener = new InputStreamResponseListener();
        httpClient.newRequest(serverUrl)
                .send(listener);

        return new JettyCheckoutContext(httpClient, listener);
    }


    private static class JettyHttpPushContext implements PushContext {

        private final HttpClient httpClient;
        private final InputStreamResponseListener listener;
        private final OutputStreamRequestContent contentOut;

        private JettyHttpPushContext(
                HttpClient httpClient,
                InputStreamResponseListener listener,
                OutputStreamRequestContent contentOut) {
            this.httpClient = httpClient;
            this.listener = listener;
            this.contentOut = contentOut;
        }

        @Override
        public OutputStream getOutputStream() {
            return contentOut.getOutputStream();
        }

        @Override
        public void finish() throws Exception {
            contentOut.close();

            Response response = listener.get(10, TimeUnit.SECONDS);
            httpClient.stop();
            if (response.getStatus() != HttpStatus.OK_200) {
                throw new RuntimeException(
                        "Push request failed with status: " + response.getStatus());
            }
        }
    }


    @Override
    public PushContext push(String branchName, long baseCommitId, long newHeadCommitId)
            throws Exception {
        String serverUrl = String.format(
                "http://%s/some_org/some_repo/push?branchName=%s&baseCommitId=%d&newHeadCommitId=%d",
                host, urlEnc(branchName), baseCommitId, newHeadCommitId);

        // TODO: Close the client -- on shutdown?
        HttpClient httpClient = new HttpClient();
        httpClient.start();

        OutputStreamRequestContent outStream = new OutputStreamRequestContent();
        InputStreamResponseListener listener = new InputStreamResponseListener();

        httpClient.newRequest(serverUrl)
                .body(outStream)
                .send(listener);

        return new JettyHttpPushContext(httpClient, listener, outStream);
    }
}
