package testMod;

import testLib.ClassMadeInaccessible;

public class TestMultiANewArrayInaccessible {
    public void foo() {
        ClassMadeInaccessible[][] inaccessible = new ClassMadeInaccessible[5][3];
    }
}
