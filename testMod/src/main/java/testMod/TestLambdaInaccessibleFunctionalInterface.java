package testMod;

import testLib.FunctionalInterfaceMadeInaccessible;

public class TestLambdaInaccessibleFunctionalInterface {
    public void foo() {
        FunctionalInterfaceMadeInaccessible itf = () -> {};
    }
}
