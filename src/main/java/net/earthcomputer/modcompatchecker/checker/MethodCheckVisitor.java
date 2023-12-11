package net.earthcomputer.modcompatchecker.checker;

import net.earthcomputer.modcompatchecker.indexer.Index;
import net.earthcomputer.modcompatchecker.util.AsmUtil;
import org.objectweb.asm.MethodVisitor;

public final class MethodCheckVisitor extends MethodVisitor {
    private final Index index;
    private final ProblemCollector problems;
    private final String className;
    private final String methodName;
    private final String methodDesc;

    public MethodCheckVisitor(Index index, ProblemCollector problems, String className, String methodName, String methodDesc) {
        super(AsmUtil.API);
        this.index = index;
        this.problems = problems;
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
    }
}
