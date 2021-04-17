package com.haberdashervcs.client.checkout;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.haberdashervcs.client.commands.Command;
import com.haberdashervcs.client.db.LocalDb;
import com.haberdashervcs.client.db.sqlite.SqliteLocalDb;
import com.haberdashervcs.common.io.HdObjectId;
import com.haberdashervcs.common.io.ProtobufObjectInputStream;
import com.haberdashervcs.common.logging.HdLogger;
import com.haberdashervcs.common.logging.HdLoggers;
import com.haberdashervcs.common.objects.CommitEntry;
import com.haberdashervcs.common.objects.FileEntry;
import com.haberdashervcs.common.objects.FolderListing;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.UrlEncoded;
import org.yaml.snakeyaml.Yaml;


public class CheckoutCommand implements Command {

    private static final HdLogger LOG = HdLoggers.create(CheckoutCommand.class);

    // TODO move these constants somewhere common?
    private static final String INIT_FILENAME = "hdlocal";


    private final List<String> otherArgs;
    private final LocalDb db;

    CheckoutCommand(List<String> otherArgs) {
        this.otherArgs = otherArgs;
        this.db = SqliteLocalDb.getInstance();
    }

    @Override
    // TODO rollback on failure?
    public void perform() throws Exception {
        final String currentCommit = db.getCurrentCommit();
        final String path = otherArgs.get(0);
        final String commit = otherArgs.get(1);

        final String initContents = Files.readString(Paths.get(INIT_FILENAME));
        Yaml yamlParser = new Yaml();
        Map<String, String> parsedYaml = yamlParser.load(initContents);
        if (true) {
            LOG.info("Parsed yaml: %s", parsedYaml);
            return;
        }

        // TODO: Get this from a config
        final String serverUrl = String.format(
                "localhost:15367/basic-test-repo/checkout/%s?commit=",
                UrlEncoded.encodeString(path), UrlEncoded.encodeString(currentCommit));

        // TODO: Close the client -- on shutdown?
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

    private void processResponseBody(InputStream in) throws IOException {
        ProtobufObjectInputStream protoIn = ProtobufObjectInputStream.forInputStream(in);
        Optional<HdObjectId> next;
        while ((next = protoIn.next()).isPresent()) {
            LOG.info("Got next obj: " + next.get().getType());
            switch (next.get().getType()) {
                case FILE:
                    FileEntry file = protoIn.getFile();
                    break;
                case FOLDER:
                    FolderListing folder = protoIn.getFolder();
                    break;
                case COMMIT:
                    CommitEntry commit = protoIn.getCommit();
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown type!");
            }
        }
        LOG.info("Done with checkout stream.");
    }
}
