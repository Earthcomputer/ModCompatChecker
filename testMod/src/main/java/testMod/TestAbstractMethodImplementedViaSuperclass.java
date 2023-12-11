package testMod;

import testLib.IAddedAbstractMethod;

public class TestAbstractMethodImplementedViaSuperclass extends TestAbstractMethodImplementedViaSuperclass_Superclass implements IAddedAbstractMethod {
}

class TestAbstractMethodImplementedViaSuperclass_Superclass {
    public void newMethod() {}
}
