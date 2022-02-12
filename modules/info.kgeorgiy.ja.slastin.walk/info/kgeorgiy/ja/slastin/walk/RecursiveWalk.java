package info.kgeorgiy.ja.slastin.walk;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class RecursiveWalk {
    private static Path createPath(final String file, final String errorMessage) throws WalkException {
        try {
            return Path.of(file);
        } catch (final InvalidPathException e) {
            throw new WalkException(errorMessage + ": " + e.getMessage());
        }
    }

    private static void createParentDirectories(final Path file) throws WalkException {
        final Path parent;
        if ((parent = file.getParent()) != null) {
            try {
                Files.createDirectories(parent);
            } catch (final FileAlreadyExistsException e) {
                WalkException.throwOut("Output file directory exists but is not a directory",
                        e.getMessage());
            } catch (final SecurityException e) {
                WalkException.throwOut("Thrown by security manager during creating output file directories",
                        e.getMessage());
            } catch (final IOException e) {
                // Продолжаем работу, ждем ошибки записи в output file
            }
        }
    }

    private static void walk(final String input, final String output, final boolean isDirectoriesAllowed)
            throws WalkException {
        final var inputPath = createPath(input, "Incorrect path to the input file");
        final var outputPath = createPath(output, "Incorrect path to the output file");

        createParentDirectories(outputPath);

        try (final var reader = Files.newBufferedReader(inputPath)) {
            try (final var writer = Files.newBufferedWriter(outputPath)) {

                final var visitor = new Pjw64WriterVisitor(writer, isDirectoriesAllowed);

                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        try {
                            Files.walkFileTree(Path.of(line), visitor);
                        } catch (final InvalidPathException e) {
                            visitor.writeError(line);
                        } catch (final IOException e) {
                            WalkException.throwOut("I/O error during walkFileTree", e.getMessage());
                        } catch (final SecurityException e) {
                            WalkException.throwOut("Thrown by security manager during walkFileTree",
                                    e.getMessage());
                        }
                    }
                } catch (final IOException e) {
                    WalkException.throwOut("I/O error occurs during processing input file", e.getMessage());
                }
            } catch (final IOException e) {
                WalkException.throwOut("I/O error occurs during opening or creating output file",
                        e.getMessage());
            } catch (final SecurityException e) {
                WalkException.throwOut("Thrown by security manager during processing output file",
                        e.getMessage());
            }
        } catch (final IOException e) {
            WalkException.throwOut("I/O error occurs during opening input file", e.getMessage());
        } catch (final SecurityException e) {
            WalkException.throwOut("Thrown by security manager during processing input file", e.getMessage());
        }
    }

    public static void launchWalk(final String[] args, final boolean isDirectoriesAllowed) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Expected args: <input file> <output file>");
            return;
        }
        try {
            walk(args[0], args[1], isDirectoriesAllowed);
        } catch (final WalkException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void main(final String[] args) {
        launchWalk(args, true);
    }
}
