package testMod;

import testLib.SignatureChange;

public class TestInvokeVirtualRemoved {
    public void foo() {
        new SignatureChange().removedNonStaticMethod();
    }
}
