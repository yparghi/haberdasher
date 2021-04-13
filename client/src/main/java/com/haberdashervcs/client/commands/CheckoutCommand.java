package com.haberdashervcs.client.commands;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.haberdashervcs.common.io.ProtobufObjectInputStream;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpStatus;


public class CheckoutCommand implements Command {

    private final List<String> otherArgs;

    CheckoutCommand(List<String> otherArgs) {
        this.otherArgs = otherArgs;
    }

    @Override
    public void perform() throws Exception {
        // TODO: Get this from a config
        // TODO: Close the client -- on shutdown?
        final String serverUrl = "localhost:15367";
        HttpClient httpClient = new HttpClient();
        httpClient.start();

        InputStreamResponseListener listener = new InputStreamResponseListener();
        httpClient.newRequest(serverUrl)
                .send(listener);

        Response response = listener.get(10, TimeUnit.SECONDS);
        if (response.getStatus() != HttpStatus.OK_200) {
            throw new RuntimeException("Request failed with status: " + response.getStatus());
        } else {
            processResponseBody(listener.getInputStream());
        }
    }

    private void processResponseBody(InputStream in) {
        ProtobufObjectInputStream protoIn = ProtobufObjectInputStream.forInputStream(in);
    }
}
