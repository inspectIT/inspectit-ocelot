package rocks.inspectit.ocelot.file.accessor.revision;

import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;

import java.util.List;
import java.util.Optional;

public class RevisionAccessor extends AbstractFileAccessor {



    @Override
    protected String verifyPath(String relativeBasePath, String path) {
        return null;
    }

    @Override
    protected Optional<byte[]> readFile(String path) {
        return Optional.empty();
    }

    @Override
    protected List<FileInfo> listFiles(String path) {
        return null;
    }

    @Override
    protected boolean exists(String path) {
        return false;
    }

    @Override
    protected boolean isDirectory(String path) {
        return false;
    }
}
