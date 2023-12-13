package testMod;

import testLib.SignatureChange;

public class TestInvokeStaticRemoved {
    public void foo() {
        SignatureChange.removedStaticMethod();
    }
}
