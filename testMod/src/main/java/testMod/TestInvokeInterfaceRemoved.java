package testMod;

import testLib.SignatureChangeInterface;

public class TestInvokeInterfaceRemoved {
    public void foo(SignatureChangeInterface itf) {
        itf.removedMethod();
    }
}
