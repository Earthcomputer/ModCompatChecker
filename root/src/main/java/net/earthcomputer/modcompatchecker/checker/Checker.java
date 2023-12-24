package net.earthcomputer.modcompatchecker.checker;

import net.earthcomputer.modcompatchecker.config.Config;
import net.earthcomputer.modcompatchecker.config.Plugin;
import net.earthcomputer.modcompatchecker.config.PluginLoader;
import net.earthcomputer.modcompatchecker.indexer.Index;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.jar.JarFile;

public final class Checker {
    private Checker() {
    }

    public static void check(Index index, Config config, List<Path> modJars, ProblemCollector problems) throws IOException {
        for (Path modJar : modJars) {
            check(index, config, modJar, problems);
        }
    }

    private static void check(Index index, Config config, Path modJar, ProblemCollector problems) throws IOException {
        Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try (JarFile jarFile = new JarFile(modJar.toFile())) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (Plugin plugin : PluginLoader.plugins()) {
                plugin.check(index, config, modJar, jarFile, problems, futures, executor);
            }
            try {
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof UncheckedIOException e1) {
                    throw e1.getCause();
                }
            }
        } catch (IOException e) {
            throw new IOException("Error reading jar file: ", e);
        }
    }
}
