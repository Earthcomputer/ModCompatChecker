package testMod;

import testLib.SignatureChange;

public class TestInvokeStaticInaccessible {
    public void foo() {
        SignatureChange.staticMethodMadePrivate();
    }
}
