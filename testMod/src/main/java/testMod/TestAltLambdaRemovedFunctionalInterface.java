package testMod;

import testLib.RemovedInterface;

public class TestAltLambdaRemovedFunctionalInterface {
    public void foo() {
        ((Runnable & RemovedInterface) () -> {}).run();
    }
}
