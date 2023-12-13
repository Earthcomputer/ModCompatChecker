package testMod;

import testLib.SignatureChange;

public class TestFieldMadeFinal {
    public void foo() {
        new SignatureChange().fieldMadeFinal = 1;
    }
}
