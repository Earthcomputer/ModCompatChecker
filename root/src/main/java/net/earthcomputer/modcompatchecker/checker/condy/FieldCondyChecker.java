package net.earthcomputer.modcompatchecker.checker.condy;

import net.earthcomputer.modcompatchecker.checker.Errors;
import net.earthcomputer.modcompatchecker.checker.indy.IndyContext;
import net.earthcomputer.modcompatchecker.indexer.IResolvedClass;
import net.earthcomputer.modcompatchecker.util.AsmUtil;
import net.earthcomputer.modcompatchecker.util.ClassMember;
import org.objectweb.asm.Type;

public enum FieldCondyChecker implements CondyChecker {
    INSTANCE;

    @Override
    public void check(IndyContext context) {
        boolean wantsStatic = !"fieldVarHandle".equals(context.bsm().getName());
        Type fieldType;
        Type declaringType;
        if ("getStaticFinal".equals(context.bsm().getName())) {
            fieldType = Type.getType(context.descriptor());
            if (context.args().length == 0) {
                declaringType = fieldType;
            } else {
                declaringType = AsmUtil.toClassConstant(context.args()[0]);
            }
        } else {
            if (context.args().length < 2) {
                return;
            }
            fieldType = AsmUtil.toClassConstant(context.args()[0]);
            declaringType = AsmUtil.toClassConstant(context.args()[1]);
        }

        if (fieldType == null || declaringType == null || declaringType.getSort() != Type.OBJECT) {
            return;
        }
        String declaringClass = declaringType.getInternalName();
        IResolvedClass resolvedType = context.index().findClass(declaringClass);
        if (resolvedType == null) {
            context.addProblem(Errors.CODE_REFERENCES_REMOVED_CLASS, declaringClass);
            return;
        }
        if (!AsmUtil.isClassAccessible(context.className(), declaringClass, resolvedType.getAccess().accessLevel())) {
            context.addProblem(Errors.CODE_REFERENCES_INACCESSIBLE_CLASS, declaringClass, resolvedType.getAccess().accessLevel().getLowerName());
        }

        ClassMember resolvedField = null;
        for (ClassMember field : resolvedType.getFields()) {
            if (field.name().equals(context.name()) && field.descriptor().equals(fieldType.getDescriptor())) {
                resolvedField = field;
                break;
            }
        }

        if (resolvedField == null) {
            context.addProblem(Errors.ACCESS_REMOVED_FIELD, declaringClass, context.name(), fieldType.getDescriptor());
            return;
        }
        if (!AsmUtil.isMemberAccessible(context.index(), context.className(), declaringClass, resolvedType, resolvedField.access().accessLevel())) {
            context.addProblem(Errors.ACCESS_INACCESSIBLE_FIELD, declaringClass, context.name(), fieldType.getDescriptor(), resolvedField.access().accessLevel().getLowerName());
        }
        if (resolvedField.access().isStatic() != wantsStatic) {
            if (wantsStatic) {
                context.addProblem(Errors.STATIC_ACCESS_TO_NONSTATIC_FIELD, declaringClass, context.name(), fieldType.getDescriptor());
            } else {
                context.addProblem(Errors.NONSTATIC_ACCESS_TO_STATIC_FIELD, declaringClass, context.name(), fieldType.getDescriptor());
            }
        }
        if ("getStaticFinal".equals(context.bsm().getName()) && !resolvedField.access().isFinal()) {
            context.addProblem(Errors.STATIC_FINAL_FIELD_NOT_FINAL, declaringClass, context.name(), fieldType.getDescriptor());
        }
    }
}
