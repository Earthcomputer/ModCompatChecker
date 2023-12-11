package testMod;

import testLib.IAddedAbstractMethod;

public class TestAbstractMethodUnimplementedDespiteSuperclass extends TestAbstractMethodUnimplementedDespiteSuperclass_Superclass implements IAddedAbstractMethod {
}

class TestAbstractMethodUnimplementedDespiteSuperclass_Superclass {
    private void newMethod() {}
}
