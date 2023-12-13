package testMod;

import testLib.SignatureChange;

public class TestFieldInaccessible {
    public void foo() {
        int i = new SignatureChange().fieldMadePrivate;
    }
}
