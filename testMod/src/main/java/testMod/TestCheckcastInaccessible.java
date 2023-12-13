package testMod;

import testLib.ClassMadeInaccessible;

public class TestCheckcastInaccessible {
    public void foo() {
        ClassMadeInaccessible inaccessible = (ClassMadeInaccessible) object();
    }

    private Object object() {
        return null;
    }
}
