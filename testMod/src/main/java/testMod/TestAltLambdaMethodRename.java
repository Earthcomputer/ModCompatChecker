package testMod;

import testLib.FunctionalInterfaceMethodRename;

import java.util.RandomAccess;

public class TestAltLambdaMethodRename {
    public void foo() {
        ((FunctionalInterfaceMethodRename & RandomAccess) () -> {}).foo();
    }
}
