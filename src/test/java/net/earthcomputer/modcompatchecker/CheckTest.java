package net.earthcomputer.modcompatchecker;

import net.earthcomputer.modcompatchecker.checker.Checker;
import net.earthcomputer.modcompatchecker.checker.Errors;
import net.earthcomputer.modcompatchecker.indexer.Index;
import net.earthcomputer.modcompatchecker.indexer.Indexer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CheckTest {
    private static Index index;
    private final List<DynamicTest> tests = new ArrayList<>();

    private void registerAll() {
        register("testMod/TestExtendFinalClass", Errors.CLASS_EXTENDS_FINAL);
        register("testMod/TestExtendInterface", Errors.CLASS_EXTENDS_INTERFACE);
        register("testMod/TestExtendSealed", Errors.CLASS_EXTENDS_SEALED);
        register("testMod/TestExtendFinalMethod1", Errors.METHOD_OVERRIDES_FINAL);
        register("testMod/TestExtendFinalMethod2", Errors.METHOD_OVERRIDES_FINAL);
        register("testMod/TestExtendsRemovedClassAndInterface", Errors.CLASS_EXTENDS_REMOVED, Errors.CLASS_IMPLEMENTS_REMOVED);
        register("testMod/TestExtendsInaccessibleClassAndInterface", Errors.CLASS_EXTENDS_INACCESSIBLE, Errors.CLASS_IMPLEMENTS_INACCESSIBLE);
        register("testMod/TestImplementClass", Errors.CLASS_IMPLEMENTS_CLASS);
        register("testMod/TestImplementSealed", Errors.CLASS_IMPLEMENTS_SEALED);
        register("testMod/TestFieldRemovedType", Errors.FIELD_TYPE_REMOVED);
        register("testMod/TestFieldInaccessibleType", Errors.FIELD_TYPE_INACCESSIBLE);
        register("testMod/TestMethodWithRemovedReturnType", Errors.METHOD_RETURN_TYPE_REMOVED);
        register("testMod/TestMethodWithRemovedParam", Errors.METHOD_PARAM_TYPE_REMOVED);
        register("testMod/TestMethodWithInaccessibleReturnType", Errors.METHOD_RETURN_TYPE_INACCESSIBLE);
        register("testMod/TestMethodWithInaccessibleParam", Errors.METHOD_PARAM_TYPE_INACCESSIBLE);
        register("testMod/TestThrowsRemoved", Errors.METHOD_THROWS_TYPE_REMOVED);
        register("testMod/TestThrowsInaccessible", Errors.METHOD_THROWS_TYPE_INACCESSIBLE);
        register("testMod/TestAbstractMethodUnimplemented", Errors.ABSTRACT_METHOD_UNIMPLEMENTED);
        register("testMod/TestIAbstractMethodUnimplemented", Errors.ABSTRACT_METHOD_UNIMPLEMENTED);
        register("testMod/TestAbstractMethodImplemented");
        register("testMod/TestAbstractMethodImplementedViaSuperclass");
        register("testMod/TestAbstractMethodUnimplementedDespiteSuperclass", Errors.ABSTRACT_METHOD_UNIMPLEMENTED);
        register("testMod/TestInterfaceMethodResolvingToSuperclass", Errors.INCORRECT_INTERFACE_METHOD_LOOKUP);
        register("testMod/TestStringParamChangedToObject");
        register("testMod/TestObjectReturnTypeChangedToString");
    }

    @BeforeAll
    public static void createIndex() throws IOException {
        index = new Index();
        Path libPath = Path.of(System.getProperty("testNewLib.jar"));
        Indexer.indexJar(libPath, index);
        Path modPath = Path.of(System.getProperty("testMod.jar"));
        Indexer.indexJar(modPath, index);
    }

    @AfterAll
    public static void destroyIndex() {
        index = null;
    }

    @TestFactory
    public Iterable<DynamicTest> testRegistered() {
        tests.clear();
        registerAll();
        return tests;
    }

    private void register(String className, Errors... expectedErrors) {
        EnumSet<Errors> expectedErrorsSet = expectedErrors.length == 0 ? EnumSet.noneOf(Errors.class) : EnumSet.copyOf(Arrays.asList(expectedErrors));
        tests.add(DynamicTest.dynamicTest(className, () -> {
            Path modPath = Path.of(System.getProperty("testMod.jar"));
            try (JarFile modJar = new JarFile(modPath.toFile())) {
                JarEntry entry = modJar.getJarEntry(className + ".class");
                if (entry == null) {
                    throw new IOException("Class " + className + " not found in mod jar");
                }
                ErrorCollectingProblemCollector problems = new ErrorCollectingProblemCollector();
                Checker.checkClass(index, new DebugCheckerConfig(), modJar, entry, problems);
                Assertions.assertEquals(expectedErrorsSet, problems.getProblems());
            }
        }));
    }
}
