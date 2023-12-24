package net.earthcomputer.modcompatchecker.checker.condy;

import net.earthcomputer.modcompatchecker.checker.Errors;
import net.earthcomputer.modcompatchecker.checker.indy.IndyContext;
import net.earthcomputer.modcompatchecker.indexer.IResolvedClass;
import net.earthcomputer.modcompatchecker.util.AsmUtil;
import net.earthcomputer.modcompatchecker.util.ClassMember;
import org.objectweb.asm.Type;

public enum EnumConstantChecker implements CondyChecker {
    INSTANCE;

    @Override
    public void check(IndyContext context) {
        Type enumType = Type.getType(context.descriptor());
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
            context.addProblem(Errors.ENUM_CONSTANT_NOT_AN_ENUM_CONSTANT, enumClass, context.name());
        } else {
            ClassMember resolvedField = null;
            for (ClassMember field : resolvedEnum.getFields()) {
                if (field.name().equals(context.name())) {
                    resolvedField = field;
                    break;
                }
            }
            if (resolvedField == null) {
                context.addProblem(Errors.ACCESS_REMOVED_FIELD, enumClass, context.name(), context.descriptor());
            } else if (!resolvedField.access().isEnum()) {
                context.addProblem(Errors.ENUM_CONSTANT_NOT_AN_ENUM_CONSTANT, enumClass, context.name());
            }
        }
    }
}
