package info.kgeorgiy.ja.slastin.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class Pjw64WriterVisitor extends SimpleFileVisitor<Path> {
    private final static int BUFFER_SIZE = 8192;

    private final byte[] buffer = new byte[BUFFER_SIZE];
    private final BufferedWriter writer;
    private final boolean isDirectoriesAllowed;

    public Pjw64WriterVisitor(final BufferedWriter writer, final boolean isDirectoriesAllowed) {
        this.writer = writer;
        this.isDirectoriesAllowed = isDirectoriesAllowed;
    }

    public void writeError(final String file) throws WalkException {
        writeHash(file, 0);
    }

    private FileVisitResult writeHash(final String file, final long hash) throws WalkException {
        try {
            writer.write(String.format("%016x %s", hash, file));
            writer.newLine();
        } catch (final IOException e) {
            WalkException.throwOut("Unable to write to the output file", e.getMessage());
        }
        return FileVisitResult.CONTINUE;
    }

    private FileVisitResult writeHash(final Path file, final long hash) throws WalkException {
        return writeHash(file.toString(), hash);
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if (isDirectoriesAllowed) {
            return FileVisitResult.CONTINUE;
        } else {
            writeError(dir.toString());
            return FileVisitResult.SKIP_SUBTREE;
        }
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
        long hash = 0;
        try (final var in = Files.newInputStream(file)) {
            long high;
            int sz;
            while ((sz = in.read(buffer)) >= 0) {
                for (int i = 0; i < sz; i++) {
                    hash = (hash << 8) + (buffer[i] & 0xff);
                    high = hash & 0xff00_0000_0000_0000L;
                    if (high != 0) {
                        hash ^= high >> 48;
                        hash &= ~high;
                    }
                }
            }
        } catch (final IOException e) {
            hash = 0;
        }
        return writeHash(file, hash);
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
        return writeHash(file, 0);
    }
}
