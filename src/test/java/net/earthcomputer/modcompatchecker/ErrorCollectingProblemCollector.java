package net.earthcomputer.modcompatchecker;

import net.earthcomputer.modcompatchecker.checker.Errors;
import net.earthcomputer.modcompatchecker.checker.ProblemCollector;

import java.util.EnumSet;

public final class ErrorCollectingProblemCollector implements ProblemCollector {
    private final EnumSet<Errors> problems = EnumSet.noneOf(Errors.class);

    @Override
    public void addProblem(String className, Errors problem, String... args) {
        problems.add(problem);
    }

    public EnumSet<Errors> getProblems() {
        return problems;
    }
}
