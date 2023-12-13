package testMod;

import testLib.SignatureChangeInterface;

public class TestInvokeInterfaceInaccessible {
    public void foo(SignatureChangeInterface itf) {
        itf.methodMadePrivate();
    }
}
