package testMod;

import testLib.ClassMadeInaccessible;

public class TestInvokeInaccessibleClass {
    public void foo() {
        ClassMadeInaccessible.method();
    }
}
