package testMod;

import testLib.SignatureChange;

public class TestFieldRemoved {
    public void foo() {
        int i = new SignatureChange().removedField;
    }
}
