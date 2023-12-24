package net.earthcomputer.modcompatchecker.config;

import net.earthcomputer.modcompatchecker.checker.ProblemCollector;
import net.earthcomputer.modcompatchecker.indexer.ClassIndex;
import net.earthcomputer.modcompatchecker.indexer.Index;
import net.earthcomputer.modcompatchecker.util.ClassMember;
import net.earthcomputer.modcompatchecker.util.ThreeState;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.jar.JarFile;

public interface Plugin {
    String id();

    default Ordering order() {
        return Ordering.defaultOrdering();
    }

    default void initialize() {
    }

    default ThreeState shouldCheckClass(String className) {
        return ThreeState.UNKNOWN;
    }

    default ThreeState isClassAccessedViaReflection(String className) {
        return ThreeState.UNKNOWN;
    }

    default void preIndexLibrary(Index index, Path libraryPath) throws IOException {
    }

    default void preIndexMod(Index index, Path modPath) throws IOException {
    }

    @Nullable
    default ClassIndex onIndexClass(Index index, ClassIndex clazz) {
        return clazz;
    }

    @Nullable
    default ClassMember onIndexField(ClassIndex clazz, ClassMember field) {
        return field;
    }

    @Nullable
    default ClassMember onIndexMethod(ClassIndex clazz, ClassMember method) {
        return method;
    }

    default void check(Index index, Config config, Path modPath, JarFile modJar, ProblemCollector problems, List<CompletableFuture<Void>> futures, Executor executor) throws IOException {
    }

    final class Ordering {
        final Order order;
        final Set<String> before = new HashSet<>();
        final Set<String> after = new HashSet<>();

        private Ordering(Order order) {

            this.order = order;
        }

        public static Ordering defaultOrdering() {
            return new Ordering(Order.DEFAULT);
        }

        public static Ordering first() {
            return new Ordering(Order.FIRST);
        }

        public static Ordering last() {
            return new Ordering(Order.LAST);
        }

        public static Ordering before(String... plugins) {
            return defaultOrdering().andBefore(plugins);
        }

        public static Ordering after(String... plugins) {
            return defaultOrdering().andAfter(plugins);
        }

        public Ordering andBefore(String... plugins) {
            Collections.addAll(before, plugins);
            return this;
        }

        public Ordering andAfter(String... plugins) {
            Collections.addAll(after, plugins);
            return this;
        }

        enum Order {
            FIRST, DEFAULT, LAST
        }
    }
}
