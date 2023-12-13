package testMod;

import testLib.ClassMadeInaccessible;

public class TestNewInaccessible {
    public void foo() {
        new ClassMadeInaccessible();
    }
}
