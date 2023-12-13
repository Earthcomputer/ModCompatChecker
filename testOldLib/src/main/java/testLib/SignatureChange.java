package testLib;

public class SignatureChange {
    public int removedField;
    public int fieldMadePrivate;
    public int fieldMadeStatic;
    public static int fieldMadeNonStatic;
    public int fieldMadeFinal;

    public SignatureChange() {
    }

    public SignatureChange(int removedConstructor) {
    }

    public SignatureChange(String constructorMadePrivate) {
    }

    public void removedNonStaticMethod() {
    }

    public void nonStaticMethodMadePrivate() {
    }

    public static void removedStaticMethod() {
    }

    public static void staticMethodMadePrivate() {
    }

    public void stringParamChangedToObject(String param) {}

    public Object objectReturnChangedToString() {
        return null;
    }
}
