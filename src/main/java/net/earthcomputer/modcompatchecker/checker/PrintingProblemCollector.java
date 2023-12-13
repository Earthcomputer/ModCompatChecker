package net.earthcomputer.modcompatchecker.checker;

public final class PrintingProblemCollector implements ProblemCollector {
    @Override
    public void addProblem(String className, Errors problem, String... args) {
        System.out.println(className + ": " + problem.getDescription().formatted((Object[]) args));
    }

    @Override
    public void addProblem(String className, String memberName, String memberDesc, Errors problem, String... args) {
        System.out.println(className + "." + memberName + " " + memberDesc + ": " + problem.getDescription().formatted((Object[]) args));
    }

    @Override
    public void addProblem(String className, String memberName, String memberDesc, int lineNumber, Errors problem, String... args) {
        System.out.println(className + "." + memberName + " " + memberDesc + ": L" + lineNumber + ": " + problem.getDescription().formatted((Object[]) args));
    }
}
