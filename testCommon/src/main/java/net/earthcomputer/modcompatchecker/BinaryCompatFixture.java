package net.earthcomputer.modcompatchecker;

import net.earthcomputer.modcompatchecker.checker.BinaryCompatChecker;
import net.earthcomputer.modcompatchecker.checker.CheckerConfig;
import net.earthcomputer.modcompatchecker.checker.Errors;
import net.earthcomputer.modcompatchecker.config.Config;
import net.earthcomputer.modcompatchecker.config.ConfigLoader;
import net.earthcomputer.modcompatchecker.config.Plugin;
import net.earthcomputer.modcompatchecker.config.PluginLoader;
import net.earthcomputer.modcompatchecker.indexer.Index;
import net.earthcomputer.modcompatchecker.indexer.Indexer;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class BinaryCompatFixture {
    private final List<DynamicTest> tests = new ArrayList<>();
    private Config config = Config.empty();
    private Index index;

    private final List<Class<? extends Plugin>> extraPlugins = new ArrayList<>();

    public BinaryCompatFixture config(@Nullable InputStream config) {
        if (config == null) {
            throw new AssertionError("Could not find config file");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(config, StandardCharsets.UTF_8))) {
            this.config = ConfigLoader.load(reader);
        } catch (IOException e) {
            throw new AssertionError("Could not read config file", e);
        }
        return this;
    }

    @SafeVarargs
    public final BinaryCompatFixture extraPlugins(Class<? extends Plugin>... extraPlugins) {
        Collections.addAll(this.extraPlugins, extraPlugins);
        return this;
    }

    public void setup() throws IOException {
        List<Plugin> plugins = PluginLoader.createBuiltinPlugins();
        for (Class<? extends Plugin> extraPlugin : extraPlugins) {
            try {
                plugins.add(extraPlugin.getConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Could not instantiate extra plugin", e);
            }
        }
        PluginLoader.setTestingPlugins(plugins);

        PluginLoader.plugins().forEach(Plugin::initialize);

        index = new Index();
        Path libPath = Path.of(System.getProperty("testNewLib.jar"));
        Path modPath = Path.of(System.getProperty("testMod.jar"));
        for (Plugin plugin : PluginLoader.plugins()) {
            plugin.preIndexLibrary(config, index, libPath);
        }
        for (Plugin plugin : PluginLoader.plugins()) {
            plugin.preIndexMod(config, index, modPath);
        }
        Indexer.indexJar(libPath, index);
        Indexer.indexJar(modPath, index);
    }

    public void register(String className, Errors... expectedErrors) {
        EnumSet<Errors> expectedErrorsSet = expectedErrors.length == 0 ? EnumSet.noneOf(Errors.class) : EnumSet.copyOf(Arrays.asList(expectedErrors));
        tests.add(DynamicTest.dynamicTest(className, () -> {
            Path modPath = Path.of(System.getProperty("testMod.jar"));
            try (JarFile modJar = new JarFile(modPath.toFile())) {
                JarEntry entry = modJar.getJarEntry(className + ".class");
                if (entry == null) {
                    throw new IOException("Class " + className + " not found in mod jar");
                }
                ErrorCollectingProblemCollector problems = new ErrorCollectingProblemCollector();
                BinaryCompatChecker.checkClass(index, new CheckerConfig(config), modJar, entry, problems);
                Assertions.assertEquals(expectedErrorsSet, problems.getProblems(), problems.getMessages().isEmpty() ? null : () -> String.join("\n", problems.getMessages()));
            }
        }));
    }

    public void clearTests() {
        tests.clear();
    }

    public Iterable<DynamicTest> getTests() {
        return tests;
    }

    public void tearDown() {
        index = null;
        PluginLoader.setTestingPlugins(null);
    }
}
