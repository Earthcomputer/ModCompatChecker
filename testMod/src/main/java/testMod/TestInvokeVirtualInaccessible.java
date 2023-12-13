package testMod;

import testLib.SignatureChange;

public class TestInvokeVirtualInaccessible {
    public void foo() {
        new SignatureChange().nonStaticMethodMadePrivate();
    }
}
