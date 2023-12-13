package testMod;

import testLib.ClassMadeInterface;

public class TestInvokeVirtualInterface {
    public void foo(ClassMadeInterface instance) {
        instance.nonStaticMethod();
    }
}
