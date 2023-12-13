package testMod;

import testLib.SignatureChange;

public class TestInvokeSpecialInaccessible {
    public void foo() {
        new SignatureChange("Hello, World!");
    }
}
