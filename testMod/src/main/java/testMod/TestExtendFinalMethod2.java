package testMod;

import testLib.MethodMadeFinal;

public class TestExtendFinalMethod2 extends TestExtendFinalMethod2_Intermediary {
    public void addedFinalMethod() {}
}

class TestExtendFinalMethod2_Intermediary extends MethodMadeFinal {
    private void addedFinalMethod() {}
}
