package testMod;

import testLib.FunctionalInterfaceMethodRename;

public class TestLambdaMethodRename {
    public void foo() {
        FunctionalInterfaceMethodRename itf = () -> {};
    }
}
