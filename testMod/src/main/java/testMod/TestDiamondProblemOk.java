package testMod;

import testLib.Diamond1;
import testLib.Diamond2;

public class TestDiamondProblemOk implements Diamond1, Diamond2 {
    @Override
    public void foo() {
        System.out.println("TestDiamondProblemOk");
    }
}
