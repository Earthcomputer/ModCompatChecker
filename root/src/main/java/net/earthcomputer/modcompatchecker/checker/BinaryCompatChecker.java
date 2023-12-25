package net.earthcomputer.modcompatchecker.checker;

import net.earthcomputer.modcompatchecker.config.BuiltinPlugin;
import net.earthcomputer.modcompatchecker.config.Config;
import net.earthcomputer.modcompatchecker.config.Plugin;
import net.earthcomputer.modcompatchecker.indexer.Index;
import org.jetbrains.annotations.VisibleForTesting;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@BuiltinPlugin
public class BinaryCompatChecker implements Plugin {
    @Override
    public String id() {
        return "binary_compat";
    }

    @Override
    public void check(Index index, Config config, Path modPath, JarFile modJar, ProblemCollector problems, List<CompletableFuture<Void>> futures, Executor executor) throws IOException {
        CheckerConfig checkerConfig = new CheckerConfig(config);
        Enumeration<JarEntry> entries = modJar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
                futures.add(CompletableFuture.runAsync(() -> checkClass(index, checkerConfig, modJar, entry, problems), executor));
            }
        }
    }

    @VisibleForTesting
    public static void checkClass(Index index, CheckerConfig config, JarFile jarFile, JarEntry entry, ProblemCollector problems) {
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
