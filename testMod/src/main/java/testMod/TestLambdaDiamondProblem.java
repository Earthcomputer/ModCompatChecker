package testMod;

import testLib.Diamond1;
import testLib.Diamond2;

public class TestLambdaDiamondProblem {
    public void foo() {
        TestLambdaDiamondProblem_FunctionalInterface itf = () -> {};
    }
}

interface TestLambdaDiamondProblem_FunctionalInterface extends Diamond1, Diamond2 {
    void sam();
}
