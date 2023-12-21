package testMod;

import testLib.InterfaceMadeInaccessible;

public class TestAltLambdaInaccessibleFunctionalInterface {
    public void foo() {
        ((Runnable & InterfaceMadeInaccessible) () -> {}).run();
    }
}
