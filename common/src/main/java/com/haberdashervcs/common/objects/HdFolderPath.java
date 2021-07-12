package com.haberdashervcs.common.objects;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Preconditions;


/**
 * Represents an folder path in the repo, like "/" or "/subfolder/"
 */
public final class HdFolderPath {

    public static HdFolderPath fromFolderListingFormat(String folderListingPath) {
        return new HdFolderPath(folderListingPath);
    }

    public static HdFolderPath fromStringArg(String argString, Path localRoot) throws IOException {
        Path argPath = Paths.get(argString);
        if (Files.isSameFile(argPath, localRoot)) {
            return fromFolderListingFormat("/");
        } else {
            return fromFolderListingFormat(
                    "/" + localRoot.relativize(argPath).toString() + "/");
        }
    }


    private final String withTrailingSlash;

    private HdFolderPath(String withTrailingSlash) {
        Preconditions.checkArgument(withTrailingSlash.startsWith("/"));
        Preconditions.checkArgument(withTrailingSlash.endsWith("/"));
        this.withTrailingSlash = withTrailingSlash;
    }


    public String forFolderListing() {
        return withTrailingSlash;
    }


    public Path toLocalPathFromRoot(Path root) {
        String relPathStr = withTrailingSlash.substring(1);
        // I hate the Java Path API. Paths.get("") gives you the PWD. But does
        // Paths.get("").equals(Paths.get(".")) ? Nope!
        if (relPathStr.equals("")) {
            relPathStr = ".";
        }
        Path relPath = Paths.get(relPathStr);
        return root.relativize(relPath);
    }


    public HdFolderPath joinWithSubfolder(String name) {
        Preconditions.checkArgument(!name.endsWith("/"));
        return new HdFolderPath(withTrailingSlash + name + "/");
    }


    public String filePathForName(String fileName) {
        Preconditions.checkArgument(!fileName.contains("/"));
        return withTrailingSlash + fileName;
    }


    @Override
    public String toString() {
        return withTrailingSlash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HdFolderPath that = (HdFolderPath) o;
        return withTrailingSlash.equals(that.withTrailingSlash);
    }

    @Override
    public int hashCode() {
        return withTrailingSlash.hashCode();
    }
}
