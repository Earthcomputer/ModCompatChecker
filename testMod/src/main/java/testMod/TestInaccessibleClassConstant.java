package testMod;

import testLib.ClassMadeInaccessible;

public class TestInaccessibleClassConstant {
    public void foo() {
        Class<?> inaccessible = ClassMadeInaccessible.class;
    }
}
