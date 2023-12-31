package net.earthcomputer.modcompatchecker.checker;

public interface ProblemCollector {
    void addProblem(String className, Errors problem, String... args);
    default void addProblem(String className, String memberName, String memberDesc, Errors problem, String... args) {
        addProblem(className, problem, args);
    }

    default void addProblem(String className, String memberName, String memberDesc, int lineNumber, Errors problem, String... args) {
        addProblem(className, memberName, memberDesc, problem, args);
    }
}
