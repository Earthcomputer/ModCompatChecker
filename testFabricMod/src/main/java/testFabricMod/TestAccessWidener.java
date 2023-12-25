package testFabricMod;

import testLib.ClassMadeFinal;
import testLib.ClassMadeInaccessible;
import testLib.SignatureChange;

public class TestAccessWidener extends ClassMadeFinal {
    public void foo() {
        new ClassMadeInaccessible();
        new SignatureChange().fieldMadePrivate = 0;
        new SignatureChange().fieldMadeFinal = 0;
        SignatureChange.staticMethodMadePrivate();
    }
}
