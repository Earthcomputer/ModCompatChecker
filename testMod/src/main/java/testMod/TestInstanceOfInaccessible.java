package testMod;

import testLib.ClassMadeInaccessible;

public class TestInstanceOfInaccessible {
    public boolean foo() {
        return object() instanceof ClassMadeInaccessible;
    }

    private Object object() {
        return null;
    }
}
