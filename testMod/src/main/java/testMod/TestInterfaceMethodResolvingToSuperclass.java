package testMod;

public class TestInterfaceMethodResolvingToSuperclass extends TestInterfaceMethodResolvingToSuperclass_Superclass implements TestInterfaceMethodResolvingToSuperclass_Interface {
}

interface TestInterfaceMethodResolvingToSuperclass_Interface {
    default void foo() {}
}

class TestInterfaceMethodResolvingToSuperclass_Superclass {
    private void foo() {}
}
