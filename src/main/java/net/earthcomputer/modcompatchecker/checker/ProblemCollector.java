package net.earthcomputer.modcompatchecker.checker;

public interface ProblemCollector {
    void addProblem(String className, Errors problem, Object... args);
    default void addProblem(String className, String memberName, String memberDesc, Errors problem, Object... args) {
        addProblem(className, problem, args);
    }
}
