package testMod;

import testLib.SignatureChange;

public class TestFieldMadeNonStatic {
    public void foo() {
        int i = SignatureChange.fieldMadeNonStatic;
    }
}
