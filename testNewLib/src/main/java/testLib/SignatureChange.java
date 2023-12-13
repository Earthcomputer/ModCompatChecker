package testLib;

public class SignatureChange {
    private int fieldMadePrivate;
    public static int fieldMadeStatic;
    public int fieldMadeNonStatic;
    public final int fieldMadeFinal = 0;

    public SignatureChange() {
    }

    private SignatureChange(String constructorMadePrivate) {
    }

    private void nonStaticMethodMadePrivate() {
    }

    private static void staticMethodMadePrivate() {
    }

    public void stringParamChangedToObject(Object param) {}

    public String objectReturnChangedToString() {
        return null;
    }
}
