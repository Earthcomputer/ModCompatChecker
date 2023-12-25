package net.earthcomputer.modcompatchecker;

import net.earthcomputer.modcompatchecker.checker.Errors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;

public class BinaryCompatCheckerTest {
    private static final BinaryCompatFixture fixture = new BinaryCompatFixture().config(BinaryCompatCheckerTest.class.getResourceAsStream("/all_classes_loaded_by_reflection.cfg"));

    private void registerAll() {
        fixture.register("testMod/TestExtendFinalClass", Errors.CLASS_EXTENDS_FINAL);
        fixture.register("testMod/TestExtendInterface", Errors.CLASS_EXTENDS_INTERFACE, Errors.NON_INTERFACE_CALL_TO_INTERFACE_METHOD);
        fixture.register("testMod/TestExtendSealed", Errors.CLASS_EXTENDS_SEALED);
        fixture.register("testMod/TestExtendFinalMethod1", Errors.METHOD_OVERRIDES_FINAL);
        fixture.register("testMod/TestExtendFinalMethod2", Errors.METHOD_OVERRIDES_FINAL);
        fixture.register("testMod/TestExtendsRemovedClassAndInterface", Errors.CLASS_EXTENDS_REMOVED, Errors.CLASS_IMPLEMENTS_REMOVED, Errors.CODE_REFERENCES_REMOVED_CLASS);
        fixture.register("testMod/TestExtendsInaccessibleClassAndInterface", Errors.CLASS_EXTENDS_INACCESSIBLE, Errors.CLASS_IMPLEMENTS_INACCESSIBLE, Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        fixture.register("testMod/TestImplementClass", Errors.CLASS_IMPLEMENTS_CLASS);
        fixture.register("testMod/TestImplementSealed", Errors.CLASS_IMPLEMENTS_SEALED);
        fixture.register("testMod/TestFieldRemovedType", Errors.FIELD_TYPE_REMOVED);
        fixture.register("testMod/TestFieldInaccessibleType", Errors.FIELD_TYPE_INACCESSIBLE);
        fixture.register("testMod/TestMethodWithRemovedReturnType", Errors.METHOD_RETURN_TYPE_REMOVED);
        fixture.register("testMod/TestMethodWithRemovedParam", Errors.METHOD_PARAM_TYPE_REMOVED);
        fixture.register("testMod/TestMethodWithInaccessibleReturnType", Errors.METHOD_RETURN_TYPE_INACCESSIBLE);
        fixture.register("testMod/TestMethodWithInaccessibleParam", Errors.METHOD_PARAM_TYPE_INACCESSIBLE);
        fixture.register("testMod/TestThrowsRemoved", Errors.METHOD_THROWS_TYPE_REMOVED);
        fixture.register("testMod/TestThrowsInaccessible", Errors.METHOD_THROWS_TYPE_INACCESSIBLE);
        fixture.register("testMod/TestDiamondProblem", Errors.DIAMOND_PROBLEM);
        fixture.register("testMod/TestDiamondProblemOk");
        fixture.register("testMod/TestAbstractMethodUnimplemented", Errors.ABSTRACT_METHOD_UNIMPLEMENTED);
        fixture.register("testMod/TestIAbstractMethodUnimplemented", Errors.ABSTRACT_METHOD_UNIMPLEMENTED);
        fixture.register("testMod/TestAbstractMethodImplemented");
        fixture.register("testMod/TestAbstractMethodImplementedViaSuperclass");
        fixture.register("testMod/TestAbstractMethodUnimplementedDespiteSuperclass", Errors.ABSTRACT_METHOD_UNIMPLEMENTED);
        fixture.register("testMod/TestInterfaceMethodResolvingToSuperclass", Errors.INCORRECT_INTERFACE_METHOD_LOOKUP);
        fixture.register("testMod/TestNewRemoved", Errors.CODE_REFERENCES_REMOVED_CLASS);
        fixture.register("testMod/TestNewInaccessible", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        fixture.register("testMod/TestInstantiateAbstractClass", Errors.INSTANTIATING_ABSTRACT_CLASS);
        fixture.register("testMod/TestInstantiateInterface", Errors.INSTANTIATING_INTERFACE, Errors.NON_INTERFACE_CALL_TO_INTERFACE_METHOD);
        fixture.register("testMod/TestANewArrayRemoved", Errors.CODE_REFERENCES_REMOVED_CLASS);
        fixture.register("testMod/TestANewArrayInaccessible", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        fixture.register("testMod/TestCheckcastRemoved", Errors.CODE_REFERENCES_REMOVED_CLASS);
        fixture.register("testMod/TestCheckcastInaccessible", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        fixture.register("testMod/TestInstanceOfRemoved", Errors.CODE_REFERENCES_REMOVED_CLASS);
        fixture.register("testMod/TestInstanceOfInaccessible", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        fixture.register("testMod/TestMultiANewArrayRemoved", Errors.CODE_REFERENCES_REMOVED_CLASS);
        fixture.register("testMod/TestMultiANewArrayInaccessible", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        fixture.register("testMod/TestFieldRemovedClass", Errors.CODE_REFERENCES_REMOVED_CLASS);
        fixture.register("testMod/TestFieldInaccessibleClass", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        fixture.register("testMod/TestFieldRemoved", Errors.ACCESS_REMOVED_FIELD);
        fixture.register("testMod/TestFieldInaccessible", Errors.ACCESS_INACCESSIBLE_FIELD);
        fixture.register("testMod/TestFieldMadeStatic", Errors.NONSTATIC_ACCESS_TO_STATIC_FIELD);
        fixture.register("testMod/TestFieldMadeNonStatic", Errors.STATIC_ACCESS_TO_NONSTATIC_FIELD);
        fixture.register("testMod/TestFieldMadeFinal", Errors.WRITE_FINAL_FIELD);
        fixture.register("testMod/TestOkayFinalWrites");
        fixture.register("testMod/TestInvokeRemovedClass", Errors.CODE_REFERENCES_REMOVED_CLASS);
        fixture.register("testMod/TestInvokeInaccessibleClass", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        fixture.register("testMod/TestInvokeVirtualRemoved", Errors.ACCESS_REMOVED_METHOD);
        fixture.register("testMod/TestInvokeVirtualInaccessible", Errors.ACCESS_INACCESSIBLE_METHOD);
        fixture.register("testMod/TestInvokeVirtualInterface", Errors.NON_INTERFACE_CALL_TO_INTERFACE_METHOD);
        fixture.register("testMod/TestInvokeSpecialRemoved", Errors.ACCESS_REMOVED_METHOD);
        fixture.register("testMod/TestInvokeSpecialInaccessible", Errors.ACCESS_INACCESSIBLE_METHOD);
        fixture.register("testMod/TestInvokeInterfaceRemoved", Errors.ACCESS_REMOVED_METHOD);
        fixture.register("testMod/TestInvokeInterfaceInaccessible", Errors.ACCESS_INACCESSIBLE_METHOD);
        fixture.register("testMod/TestInvokeInterfaceClass", Errors.INTERFACE_CALL_TO_NON_INTERFACE_METHOD);
        fixture.register("testMod/TestInvokeStaticRemoved", Errors.ACCESS_REMOVED_METHOD);
        fixture.register("testMod/TestInvokeStaticInaccessible", Errors.ACCESS_INACCESSIBLE_METHOD);
        fixture.register("testMod/TestInvokeStaticMadeInterface", Errors.NON_INTERFACE_CALL_TO_INTERFACE_METHOD);
        fixture.register("testMod/TestInvokeStaticMadeNonInterface", Errors.INTERFACE_CALL_TO_NON_INTERFACE_METHOD);
        fixture.register("testMod/TestConstructorMovedToSuperclass", Errors.ACCESS_REMOVED_METHOD);
        fixture.register("testMod/TestStringParamChangedToObject", Errors.ACCESS_REMOVED_METHOD);
        fixture.register("testMod/TestObjectReturnTypeChangedToString", Errors.ACCESS_REMOVED_METHOD);
        fixture.register("testMod/TestRemovedClassConstant", Errors.CODE_REFERENCES_REMOVED_CLASS);
        fixture.register("testMod/TestInaccessibleClassConstant", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS);
        fixture.register("testMod/TestLambdaRemovedFunctionalInterface", Errors.LAMBDA_INTERFACE_REMOVED);
        fixture.register("testMod/TestLambdaInaccessibleFunctionalInterface", Errors.LAMBDA_INTERFACE_INACCESSIBLE);
        fixture.register("testMod/TestLambdaFunctionalInterfaceMadeClass", Errors.LAMBDA_INTERFACE_NOT_AN_INTERFACE);
        fixture.register("testMod/TestLambdaDiamondProblem", Errors.LAMBDA_DIAMOND_PROBLEM);
        fixture.register("testMod/TestLambdaMethodRename", Errors.LAMBDA_ABSTRACT_METHOD_UNIMPLEMENTED);
        fixture.register("testMod/TestAltLambdaRemovedFunctionalInterface", Errors.CODE_REFERENCES_REMOVED_CLASS, Errors.LAMBDA_INTERFACE_REMOVED);
        fixture.register("testMod/TestAltLambdaInaccessibleFunctionalInterface", Errors.CODE_REFERENCES_INACCESSIBLE_CLASS, Errors.LAMBDA_INTERFACE_INACCESSIBLE);
        fixture.register("testMod/TestAltLambdaFunctionalInterfaceMadeClass", Errors.LAMBDA_INTERFACE_NOT_AN_INTERFACE);
        fixture.register("testMod/TestAltLambdaDiamondProblem", Errors.LAMBDA_DIAMOND_PROBLEM);
        fixture.register("testMod/TestAltLambdaMethodRename", Errors.ACCESS_REMOVED_METHOD, Errors.LAMBDA_ABSTRACT_METHOD_UNIMPLEMENTED);
    }

    @BeforeAll
    public static void setup() throws IOException {
        fixture.setup();
    }

    @AfterAll
    public static void tearDown() {
        fixture.tearDown();
    }

    @TestFactory
    public Iterable<DynamicTest> testRegistered() {
        fixture.clearTests();
        registerAll();
        return fixture.getTests();
    }
}
