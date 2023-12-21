package testMod;

import testLib.InterfaceMadeClass;

public class TestAltLambdaFunctionalInterfaceMadeClass {
    public void foo() {
        ((Runnable & InterfaceMadeClass) () -> {}).run();
    }
}
