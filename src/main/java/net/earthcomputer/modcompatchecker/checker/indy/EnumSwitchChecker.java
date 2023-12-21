package net.earthcomputer.modcompatchecker.checker.indy;

import net.earthcomputer.modcompatchecker.checker.Errors;
import net.earthcomputer.modcompatchecker.indexer.IResolvedClass;
import net.earthcomputer.modcompatchecker.util.AsmUtil;
import org.objectweb.asm.Type;

public enum EnumSwitchChecker implements IndyChecker {
    INSTANCE;

    @Override
    public void check(IndyContext context) {
        Type[] argumentTypes = Type.getMethodType(context.descriptor()).getArgumentTypes();
        if (argumentTypes.length != 2) {
            return;
        }
        Type enumType = argumentTypes[0];
        if (enumType.getSort() != Type.OBJECT) {
            return;
        }
        String enumClass = enumType.getInternalName();
        IResolvedClass resolvedEnum = context.index().findClass(enumClass);
        if (resolvedEnum == null) {
            context.addProblem(Errors.CODE_REFERENCES_REMOVED_CLASS, enumClass);
            return;
        }
        if (!AsmUtil.isClassAccessible(context.className(), enumClass, resolvedEnum.getAccess().accessLevel())) {
            context.addProblem(Errors.CODE_REFERENCES_INACCESSIBLE_CLASS, enumClass, resolvedEnum.getAccess().accessLevel().getLowerName());
        }
        if (!resolvedEnum.getAccess().isEnum() || !AsmUtil.ENUM.equals(resolvedEnum.getSuperclass())) {
            context.addProblem(Errors.ENUM_SWITCH_ON_NON_ENUM, enumClass);
            return;
        }

        for (Object label : context.args()) {
            if (label instanceof Type type && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY)) {
                String className = type.getInternalName();
                IResolvedClass resolvedClass = context.index().findClass(className);
                if (resolvedClass == null) {
                    context.addProblem(Errors.CODE_REFERENCES_REMOVED_CLASS, className);
                } else if (!AsmUtil.isClassAccessible(context.className(), className, resolvedClass.getAccess().accessLevel())) {
                    context.addProblem(Errors.CODE_REFERENCES_INACCESSIBLE_CLASS, className, resolvedClass.getAccess().accessLevel().getLowerName());
                }
            } else if (label instanceof String enumConstant) {
                if (resolvedEnum.getFields().stream().noneMatch(field -> field.access().isEnum() && field.name().equals(enumConstant))) {
                    context.addProblem(Errors.ENUM_SWITCH_REMOVED_ENUM_CONSTANT, enumClass, enumConstant);
                }
            }
        }
    }
}
