package testMod;

import testLib.SignatureChange;

public class TestFieldMadeStatic {
    public void foo() {
        int i = new SignatureChange().fieldMadeStatic;
    }
}
