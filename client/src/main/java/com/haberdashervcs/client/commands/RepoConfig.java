package com.haberdashervcs.client.commands;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import org.yaml.snakeyaml.Yaml;


public final class RepoConfig {

    static Optional<RepoConfig> find() throws IOException {
        Path current = Paths.get("");
        while (current != null) {
            Path configFile = current.resolve("hdlocal");
            if (configFile.toFile().exists()) {
                String yamlContents = Files.readString(configFile, StandardCharsets.UTF_8);
                return Optional.of(parseConfig(current, yamlContents));
            } else {
                current = current.getParent();
            }
        }
        return Optional.empty();
    }

    private static RepoConfig parseConfig(Path root, String yamlContents) {
        Yaml yaml = new Yaml();
        Map<String, String> parsed = yaml.load(yamlContents);
        return new RepoConfig(
                root,
                parsed.get("host"),
                parsed.get("org"),
                parsed.get("repo"));
    }


    private final Path root;
    private final String host;
    private final String org;
    private final String repo;

    private RepoConfig(Path root, String host, String org, String repo) {
        this.root = root;
        this.host = host;
        this.org = org;
        this.repo = repo;
    }

    public Path getRoot() {
        return root;
    }

    public String getHost() {
        return host;
    }

    public String getOrg() {
        return org;
    }

    public String getRepo() {
        return repo;
    }
}
