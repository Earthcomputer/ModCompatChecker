package testMod;

import testLib.Diamond1;
import testLib.Diamond2;

public class TestAltLambdaDiamondProblem {
    public void foo() {
        ((Runnable & Diamond1 & Diamond2) () -> {}).run();
    }
}
