package com.haberdashervcs.client.commands;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;


class InitCommand implements Command {

    private static final String INIT_FILENAME = "hdlocal";
    private static final String INIT_FILE_TEMPLATE = Joiner.on('\n').join(
            "---",
            "url: %s",
            "");

    private final List<String> otherArgs;

    InitCommand(List<String> otherArgs) {
        Preconditions.checkArgument(otherArgs.size() >= 1);
        this.otherArgs = otherArgs;
    }

    @Override
    public void perform() {
        String serverAddress = otherArgs.get(0);
        final File initFile = new File(INIT_FILENAME);
        if (initFile.exists()) {
            throw new IllegalStateException(
                    String.format("The file `%s` already exists!", INIT_FILENAME));
        }

        try {
            String initFileContents = initFileContents(serverAddress);
            Files.write(initFileContents.getBytes(StandardCharsets.UTF_8), initFile);
        } catch (IOException ioEx) {
            throw new RuntimeException(ioEx);
        }
    }

    private String initFileContents(String serverAddress) {
        return String.format(INIT_FILE_TEMPLATE, serverAddress);
    }
}
