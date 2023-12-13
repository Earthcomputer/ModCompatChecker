package testMod;

import testLib.RemovedClass;

public class TestInstanceOfRemoved {
    public boolean foo() {
        return object() instanceof RemovedClass;
    }

    private Object object() {
        return null;
    }
}
