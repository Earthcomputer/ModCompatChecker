package net.earthcomputer.modcompatchecker;

import net.earthcomputer.modcompatchecker.checker.BinaryCompatChecker;
import net.earthcomputer.modcompatchecker.checker.Errors;
import net.earthcomputer.modcompatchecker.config.Config;
import net.earthcomputer.modcompatchecker.config.PluginLoader;
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

public class BinaryCompatCheckerTest {
    private static Index index;
    private final List<DynamicTest> tests = new ArrayList<>();

    private void registerAll() {
        register("testMod/TestExtendFinalClass", Errors.CLASS_EXTENDS_FINAL);
        register("testMod/TestExtendInterface", Errors.CLASS_EXTENDS_INTERFACE, Errors.NON_INTERFACE_CALL_TO_INTERFACE_METHOD);
        register("testMod/TestExtendSealed", Errors.CLASS_EXTENDS_SEALED);
        register("testMod/TestExtendFinalMethod1", Errors.METHOD_OVERRIDES_FINAL);
        register("testMod/TestExtendFinalMethod2", Errors.METHOD_OVERRIDES_FINAL);
        register("testMod/TestExtendsRemovedClassAndInterface", Errors.CLASS_EXTENDS_REMOVED, Errors.CLASS_IMPLEMENTS_REMOVED, Errors.CODE_REFERENCES_REMOVED_CLASS);
        register("testMod/TestExtendsInaccessibleClassAndInterface", Errors.CLASS_EXTENDS_INACCESSIBLE, Errors.CLASS_IMPLEMENTS_INACCESSIBLE, Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
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
        register("testMod/TestDiamondProblem", Errors.DIAMOND_PROBLEM);
        register("testMod/TestDiamondProblemOk");
        register("testMod/TestAbstractMethodUnimplemented", Errors.ABSTRACT_METHOD_UNIMPLEMENTED);
        register("testMod/TestIAbstractMethodUnimplemented", Errors.ABSTRACT_METHOD_UNIMPLEMENTED);
        register("testMod/TestAbstractMethodImplemented");
        register("testMod/TestAbstractMethodImplementedViaSuperclass");
        register("testMod/TestAbstractMethodUnimplementedDespiteSuperclass", Errors.ABSTRACT_METHOD_UNIMPLEMENTED);
        register("testMod/TestInterfaceMethodResolvingToSuperclass", Errors.INCORRECT_INTERFACE_METHOD_LOOKUP);
        register("testMod/TestNewRemoved", Errors.CODE_REFERENCES_REMOVED_CLASS);
        register("testMod/TestNewInaccessible", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        register("testMod/TestInstantiateAbstractClass", Errors.INSTANTIATING_ABSTRACT_CLASS);
        register("testMod/TestInstantiateInterface", Errors.INSTANTIATING_INTERFACE, Errors.NON_INTERFACE_CALL_TO_INTERFACE_METHOD);
        register("testMod/TestANewArrayRemoved", Errors.CODE_REFERENCES_REMOVED_CLASS);
        register("testMod/TestANewArrayInaccessible", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        register("testMod/TestCheckcastRemoved", Errors.CODE_REFERENCES_REMOVED_CLASS);
        register("testMod/TestCheckcastInaccessible", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        register("testMod/TestInstanceOfRemoved", Errors.CODE_REFERENCES_REMOVED_CLASS);
        register("testMod/TestInstanceOfInaccessible", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        register("testMod/TestMultiANewArrayRemoved", Errors.CODE_REFERENCES_REMOVED_CLASS);
        register("testMod/TestMultiANewArrayInaccessible", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        register("testMod/TestFieldRemovedClass", Errors.CODE_REFERENCES_REMOVED_CLASS);
        register("testMod/TestFieldInaccessibleClass", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        register("testMod/TestFieldRemoved", Errors.ACCESS_REMOVED_FIELD);
        register("testMod/TestFieldInaccessible", Errors.ACCESS_INACCESSIBLE_FIELD);
        register("testMod/TestFieldMadeStatic", Errors.NONSTATIC_ACCESS_TO_STATIC_FIELD);
        register("testMod/TestFieldMadeNonStatic", Errors.STATIC_ACCESS_TO_NONSTATIC_FIELD);
        register("testMod/TestFieldMadeFinal", Errors.WRITE_FINAL_FIELD);
        register("testMod/TestOkayFinalWrites");
        register("testMod/TestInvokeRemovedClass", Errors.CODE_REFERENCES_REMOVED_CLASS);
        register("testMod/TestInvokeInaccessibleClass", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        register("testMod/TestInvokeVirtualRemoved", Errors.ACCESS_REMOVED_METHOD);
        register("testMod/TestInvokeVirtualInaccessible", Errors.ACCESS_INACCESSIBLE_METHOD);
        register("testMod/TestInvokeVirtualInterface", Errors.NON_INTERFACE_CALL_TO_INTERFACE_METHOD);
        register("testMod/TestInvokeSpecialRemoved", Errors.ACCESS_REMOVED_METHOD);
        register("testMod/TestInvokeSpecialInaccessible", Errors.ACCESS_INACCESSIBLE_METHOD);
        register("testMod/TestInvokeInterfaceRemoved", Errors.ACCESS_REMOVED_METHOD);
        register("testMod/TestInvokeInterfaceInaccessible", Errors.ACCESS_INACCESSIBLE_METHOD);
        register("testMod/TestInvokeInterfaceClass", Errors.INTERFACE_CALL_TO_NON_INTERFACE_METHOD);
        register("testMod/TestInvokeStaticRemoved", Errors.ACCESS_REMOVED_METHOD);
        register("testMod/TestInvokeStaticInaccessible", Errors.ACCESS_INACCESSIBLE_METHOD);
        register("testMod/TestInvokeStaticMadeInterface", Errors.NON_INTERFACE_CALL_TO_INTERFACE_METHOD);
        register("testMod/TestInvokeStaticMadeNonInterface", Errors.INTERFACE_CALL_TO_NON_INTERFACE_METHOD);
        register("testMod/TestConstructorMovedToSuperclass", Errors.ACCESS_REMOVED_METHOD);
        register("testMod/TestStringParamChangedToObject", Errors.ACCESS_REMOVED_METHOD);
        register("testMod/TestObjectReturnTypeChangedToString", Errors.ACCESS_REMOVED_METHOD);
        register("testMod/TestRemovedClassConstant", Errors.CODE_REFERENCES_REMOVED_CLASS);
        register("testMod/TestInaccessibleClassConstant", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        register("testMod/TestLambdaRemovedFunctionalInterface", Errors.LAMBDA_INTERFACE_REMOVED);
        register("testMod/TestLambdaInaccessibleFunctionalInterface", Errors.LAMBDA_INTERFACE_INACCESSIBLE);
        register("testMod/TestLambdaFunctionalInterfaceMadeClass", Errors.LAMBDA_INTERFACE_NOT_AN_INTERFACE);
        register("testMod/TestLambdaDiamondProblem", Errors.LAMBDA_DIAMOND_PROBLEM);
        register("testMod/TestLambdaMethodRename", Errors.LAMBDA_ABSTRACT_METHOD_UNIMPLEMENTED);
        register("testMod/TestAltLambdaRemovedFunctionalInterface", Errors.CODE_REFERENCES_REMOVED_CLASS, Errors.LAMBDA_INTERFACE_REMOVED);
        register("testMod/TestAltLambdaInaccessibleFunctionalInterface", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS, Errors.LAMBDA_INTERFACE_INACCESSIBLE);
        register("testMod/TestAltLambdaFunctionalInterfaceMadeClass", Errors.LAMBDA_INTERFACE_NOT_AN_INTERFACE);
        register("testMod/TestAltLambdaDiamondProblem", Errors.LAMBDA_DIAMOND_PROBLEM);
        register("testMod/TestAltLambdaMethodRename", Errors.ACCESS_REMOVED_METHOD, Errors.LAMBDA_ABSTRACT_METHOD_UNIMPLEMENTED);
    }

    @BeforeAll
    public static void createIndex() throws IOException {
        PluginLoader.setTestingPlugins(PluginLoader.createBuiltinPlugins());

        index = new Index();
        Path libPath = Path.of(System.getProperty("testNewLib.jar"));
        Indexer.indexJar(libPath, index);
        Path modPath = Path.of(System.getProperty("testMod.jar"));
        Indexer.indexJar(modPath, index);
    }

    @AfterAll
    public static void destroyIndex() {
        index = null;
        PluginLoader.setTestingPlugins(null);
        Config.unregisterAll();
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
                BinaryCompatChecker.checkClass(index, new DebugCheckerConfig(), modJar, entry, problems);
                Assertions.assertEquals(expectedErrorsSet, problems.getProblems(), problems.getMessages().isEmpty() ? null : () -> String.join("\n", problems.getMessages()));
            }
        }));
    }
}
