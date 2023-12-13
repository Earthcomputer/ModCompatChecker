package testMod;

import testLib.ClassMadeInaccessible;

public class TestFieldInaccessibleClass {
    public void foo() {
        int i = ClassMadeInaccessible.field;
    }
}
