package testMod;

import testLib.InterfaceMadeClass;

public class TestInvokeInterfaceClass {
    public void foo(InterfaceMadeClass instance) {
        instance.nonStaticMethod();
    }
}
