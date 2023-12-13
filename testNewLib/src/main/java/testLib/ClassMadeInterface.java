package testLib;

public interface ClassMadeInterface {
    default void nonStaticMethod() {
    }

    static void staticMethod() {
    }
}
