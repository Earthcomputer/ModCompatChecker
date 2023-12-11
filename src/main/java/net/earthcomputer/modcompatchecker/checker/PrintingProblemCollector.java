package net.earthcomputer.modcompatchecker.checker;

public final class PrintingProblemCollector implements ProblemCollector {
    @Override
    public void addProblem(String className, Errors problem, Object... args) {
        System.out.println(className + ": " + problem.getDescription().formatted(args));
    }

    @Override
    public void addProblem(String className, String memberName, String memberDesc, Errors problem, Object... args) {
        System.out.println(className + "." + memberName + " " + memberDesc + ": " + problem.getDescription().formatted(args));
    }
}
