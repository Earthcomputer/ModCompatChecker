package testLib;

public interface Diamond1 {
    default void foo() {
        System.out.println("Diamond1");
    }
}
