package testMod;

import testLib.RemovedClass;

public class TestCheckcastRemoved {
    public void foo() {
        RemovedClass removed = (RemovedClass) object();
    }

    private Object object() {
        return null;
    }
}
