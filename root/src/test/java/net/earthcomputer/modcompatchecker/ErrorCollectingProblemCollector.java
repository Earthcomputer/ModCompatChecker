package net.earthcomputer.modcompatchecker;

import net.earthcomputer.modcompatchecker.checker.Errors;
import net.earthcomputer.modcompatchecker.checker.ProblemCollector;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class ErrorCollectingProblemCollector implements ProblemCollector {
    private final EnumSet<Errors> problems = EnumSet.noneOf(Errors.class);
    private final List<String> messages = new ArrayList<>();

    @Override
    public void addProblem(String className, Errors problem, String... args) {
        problems.add(problem);
        messages.add(className + ": " + problem.getDescription().formatted((Object[]) args));
    }

    @Override
    public void addProblem(String className, String memberName, String memberDesc, Errors problem, String... args) {
        problems.add(problem);
        messages.add(className + "." + memberName + " " + memberDesc + ": " + problem.getDescription().formatted((Object[]) args));
    }

    @Override
    public void addProblem(String className, String memberName, String memberDesc, int lineNumber, Errors problem, String... args) {
        problems.add(problem);
        messages.add(className + "." + memberName + " " + memberDesc + ": L" + lineNumber + ": " + problem.getDescription().formatted((Object[]) args));
    }

    public EnumSet<Errors> getProblems() {
        return problems;
    }

    public List<String> getMessages() {
        return messages;
    }
}
