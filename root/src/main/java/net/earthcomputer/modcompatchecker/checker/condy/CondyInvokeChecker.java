package net.earthcomputer.modcompatchecker.checker.condy;

import net.earthcomputer.modcompatchecker.checker.Errors;
import net.earthcomputer.modcompatchecker.checker.indy.IndyContext;
import net.earthcomputer.modcompatchecker.indexer.IResolvedClass;
import net.earthcomputer.modcompatchecker.util.AsmUtil;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import java.lang.constant.ClassDesc;
import java.util.Arrays;

public enum CondyInvokeChecker implements CondyChecker {
    INSTANCE;

    @Override
    public void check(IndyContext context) {
        if (context.args().length == 0 || !(context.args()[0] instanceof Handle handle)) {
            return;
        }
        Object[] args = Arrays.copyOfRange(context.args(), 1, context.args().length);
        Object result = CondyInvokeEvaluator.evaluate(handle, args);
        if (result instanceof ClassDesc classDesc) {
            Type type = Type.getType(classDesc.descriptorString());
            String referredClass = AsmUtil.getReferredClass(type);
            if (referredClass == null) {
                return;
            }
            IResolvedClass resolvedClass = context.index().findClass(referredClass);
            if (resolvedClass == null) {
                context.addProblem(Errors.CODE_REFERENCES_REMOVED_CLASS, referredClass);
                return;
            }
            if (!AsmUtil.isClassAccessible(context.className(), referredClass, resolvedClass.getAccess().accessLevel())) {
                context.addProblem(Errors.CODE_REFERENCES_INACCESSIBLE_CLASS, referredClass, resolvedClass.getAccess().accessLevel().getLowerName());
            }
        } else if (result instanceof Enum.EnumDesc<?> enumDesc) {
            Type type = Type.getType(enumDesc.constantType().descriptorString());
            if (type.getSort() != Type.OBJECT) {
                return;
            }
            IResolvedClass resolvedClass = context.index().findClass(type.getInternalName());
            if (resolvedClass == null) {
                // this error is handled by recursively checking the condy
                return;
            }

            if (!resolvedClass.getAccess().isEnum() || !AsmUtil.ENUM.equals(resolvedClass.getSuperclass())) {
                context.addProblem(Errors.ENUM_CONSTANT_NOT_AN_ENUM_CONSTANT, type.getInternalName(), enumDesc.constantName());
                return;
            }

            if (resolvedClass.getFields().stream().noneMatch(field -> field.access().isEnum() && field.name().equals(enumDesc.constantName()))) {
                context.addProblem(Errors.ACCESS_REMOVED_FIELD, type.getInternalName(), enumDesc.constantName(), type.getDescriptor());
            }
        }
    }

    @Override
    public boolean overrideDefaultChecking() {
        return false;
    }
}
