package net.earthcomputer.modcompatchecker.checker;

import net.earthcomputer.modcompatchecker.indexer.Index;
import org.jetbrains.annotations.VisibleForTesting;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class Checker {
    private Checker() {
    }

    public static void check(Index index, ICheckerConfig config, List<Path> modJars, ProblemCollector problems) throws IOException {
        for (Path modJar : modJars) {
            check(index, config, modJar, problems);
        }
    }

    private static void check(Index index, ICheckerConfig config, Path modJar, ProblemCollector problems) throws IOException {
        Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try (JarFile jarFile = new JarFile(modJar.toFile())) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    futures.add(CompletableFuture.runAsync(() -> checkClass(index, config, jarFile, entry, problems), executor));
                }
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

    @VisibleForTesting
    public static void checkClass(Index index, ICheckerConfig config, JarFile jarFile, JarEntry entry, ProblemCollector problems) {
        ClassReader classReader;
        try {
            classReader = new ClassReader(jarFile.getInputStream(entry));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!config.shouldCheckClass(classReader.getClassName())) {
            return;
        }
        classReader.accept(new ClassCheckVisitor(index, config, problems), 0);
    }
}
