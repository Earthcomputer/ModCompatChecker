package testMod;

import testLib.ClassMadeInaccessible;

public class TestANewArrayInaccessible {
    public void foo() {
        ClassMadeInaccessible[] array = new ClassMadeInaccessible[0];
    }
}
