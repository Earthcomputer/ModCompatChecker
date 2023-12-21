package net.earthcomputer.modcompatchecker.checker.indy;

import net.earthcomputer.modcompatchecker.checker.Errors;
import net.earthcomputer.modcompatchecker.checker.ProblemCollector;
import net.earthcomputer.modcompatchecker.indexer.Index;
import org.objectweb.asm.Handle;

public record IndyContext(
    Index index,
    ProblemCollector problems,
    String className,
    String methodName,
    String methodDesc,
    int lineNumber,
    String name,
    String descriptor,
    Handle bsm,
    Object[] args
) {
    public void addProblem(Errors problem, String... args) {
        problems.addProblem(className, methodName, methodDesc, lineNumber, problem, args);
    }
}
