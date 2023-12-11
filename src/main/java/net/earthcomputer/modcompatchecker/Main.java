package net.earthcomputer.modcompatchecker;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.util.PathConverter;
import joptsimple.util.PathProperties;
import net.earthcomputer.modcompatchecker.checker.Checker;
import net.earthcomputer.modcompatchecker.checker.CheckerConfig;
import net.earthcomputer.modcompatchecker.checker.PrintingProblemCollector;
import net.earthcomputer.modcompatchecker.indexer.Index;
import net.earthcomputer.modcompatchecker.indexer.Indexer;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        OptionParser parser = new OptionParser();
        var checkOption = parser.acceptsAll(List.of("c", "check"), "the mod jar to check compatibility").withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.FILE_EXISTING, PathProperties.READABLE));
        var filterOption = parser.acceptsAll(List.of("f", "filter"), "the filter for which classes to check").withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.FILE_EXISTING, PathProperties.READABLE));
        var helpOption = parser.acceptsAll(List.of("h", "help"), "prints this help message").forHelp();
        var indexOption = parser.acceptsAll(List.of("i", "index"), "creates an index of the given jar").withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.FILE_EXISTING, PathProperties.READABLE));
        var libraryOption = parser.acceptsAll(List.of("l", "library"), "a library to check against").withRequiredArg().withValuesConvertedBy(new PathConverter(PathProperties.FILE_EXISTING, PathProperties.READABLE));
        var outputOption = parser.acceptsAll(List.of("o", "output"), "the output of this operation").availableIf(indexOption).requiredIf(indexOption).withRequiredArg().withValuesConvertedBy(new PathConverter());
        OptionSet options;
        try {
            options = parser.parse(args);
        } catch (OptionException e) {
            printHelp(parser);
            return;
        }
        if (options.has(helpOption)) {
            printHelp(parser);
            return;
        }

        if (options.has(indexOption)) {
            indexJar(indexOption.value(options), outputOption.value(options));
            return;
        }

        if (options.has(checkOption)) {
            checkMods(checkOption.values(options), libraryOption.values(options), filterOption.value(options));
        }
    }

    private static void printHelp(OptionParser parser) {
        try {
            parser.printHelpOn(System.err);
        } catch (IOException e1) {
            System.err.println("IO Exception occurred while printing help: " + e1);
        }
    }

    private static void indexJar(Path jarPath, Path outputPath) {
        Index index = new Index();
        try {
            Indexer.indexJar(jarPath, index);
        } catch (IOException e) {
            System.err.println("Failed to index jar: " + e);
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            PrintWriter pw = new PrintWriter(writer);
            index.serialize(pw);
            if (pw.checkError()) {
                System.err.println("Error writing to output path");
            }
        } catch (IOException e) {
            System.err.println("Error writing to output path: " + e);
        }
    }

    private static void checkMods(List<Path> modPaths, List<Path> libraryPaths, @Nullable Path filterPath) {
        Index index = new Index();

        for (Path libraryPath : libraryPaths) {
            try {
                if (libraryPath.toString().endsWith(".jar")) {
                    Indexer.indexJar(libraryPath, index);
                } else {
                    try (BufferedReader reader = Files.newBufferedReader(libraryPath)) {
                        index.deserializeFrom(reader);
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to index library: " + e);
                return;
            }
        }

        for (Path modPath : modPaths) {
            try {
                Indexer.indexJar(modPath, index);
            } catch (IOException e) {
                System.err.println("Failed to index mod jar: " + e);
                return;
            }
        }

        try {
            Checker.check(index, new CheckerConfig(), modPaths, new PrintingProblemCollector());
        } catch (IOException e) {
            System.err.println("Error checking mod jars: " + e);
        }
    }
}