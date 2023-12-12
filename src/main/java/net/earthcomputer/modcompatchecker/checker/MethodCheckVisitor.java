package net.earthcomputer.modcompatchecker.checker;

import net.earthcomputer.modcompatchecker.indexer.IResolvedClass;
import net.earthcomputer.modcompatchecker.indexer.Index;
import net.earthcomputer.modcompatchecker.util.AccessLevel;
import net.earthcomputer.modcompatchecker.util.AsmUtil;
import net.earthcomputer.modcompatchecker.util.OwnedClassMember;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

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

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (!checkClassReference(AsmUtil.getReferredClass(Type.getObjectType(type)))) {
            return;
        }

        if (opcode == Opcodes.NEW && Type.getObjectType(type).getSort() == Type.OBJECT) {
            IResolvedClass resolvedClass = index.findClass(type);
            if (resolvedClass != null) {
                if (resolvedClass.getAccess().isInterface()) {
                    problems.addProblem(className, methodName, methodDesc, Errors.INSTANTIATING_INTERFACE, type);
                } else if (resolvedClass.getAccess().isAbstract()) {
                    problems.addProblem(className, methodName, methodDesc, Errors.INSTANTIATING_ABSTRACT_CLASS, type);
                }
            }
        }
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        checkClassReference(AsmUtil.getReferredClass(descriptor));
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        if (!checkClassReference(owner)) {
            return;
        }

        OwnedClassMember field = AsmUtil.lookupField(index, owner, name, descriptor);
        if (field == null) {
            problems.addProblem(className, methodName, methodDesc, Errors.ACCESS_REMOVED_FIELD, owner, name, descriptor);
        } else {
            if (!AsmUtil.isMemberAccessible(index, className, field.owner(), field.member().access().accessLevel())) {
                problems.addProblem(className, methodName, methodDesc, Errors.ACCESS_INACCESSIBLE_FIELD, owner, name, descriptor, field.member().access().accessLevel().getLowerName());
            }
            if (field.member().access().isStatic()) {
                if (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD) {
                    problems.addProblem(className, methodName, methodDesc, Errors.NONSTATIC_ACCESS_TO_STATIC_FIELD, owner, name, descriptor);
                }
            } else {
                if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {
                    problems.addProblem(className, methodName, methodDesc, Errors.STATIC_ACCESS_TO_NONSTATIC_FIELD, owner, name, descriptor);
                }
            }
            if (field.member().access().isFinal()) {
                if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) {
                    String expectedMethod = opcode == Opcodes.PUTFIELD ? AsmUtil.CONSTRUCTOR_NAME : AsmUtil.CLASS_INITIALIZER_NAME;
                    if (!field.owner().equals(className) || !expectedMethod.equals(methodName)) {
                        problems.addProblem(className, methodName, methodDesc, Errors.WRITE_FINAL_FIELD, owner, name, descriptor);
                    }
                }
            }
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if (!checkClassReference(owner)) {
            return;
        }

        IResolvedClass resolvedClass = index.findClass(owner);
        if (resolvedClass != null && resolvedClass.getAccess().isInterface() != isInterface) {
            problems.addProblem(className, methodName, methodDesc, isInterface ? Errors.INTERFACE_CALL_TO_NON_INTERFACE_METHOD : Errors.NON_INTERFACE_CALL_TO_INTERFACE_METHOD, owner, name, descriptor);
        }

        switch (opcode) {
            case Opcodes.INVOKEVIRTUAL -> {
                OwnedClassMember method = AsmUtil.lookupMethod(index, owner, name, descriptor);
                if (method == null) {
                    problems.addProblem(className, methodName, methodDesc, Errors.ACCESS_REMOVED_METHOD, owner, name, descriptor);
                } else {
                    if (!AsmUtil.isMemberAccessible(index, className, method.owner(), method.member().access().accessLevel())) {
                        problems.addProblem(className, methodName, methodDesc, Errors.ACCESS_INACCESSIBLE_METHOD, owner, name, descriptor, method.member().access().accessLevel().getLowerName());
                    }
                    if (method.member().access().isStatic()) {
                        problems.addProblem(className, methodName, methodDesc, Errors.NONSTATIC_CALL_TO_STATIC_METHOD, owner, name, descriptor);
                    }
                }
            }
            case Opcodes.INVOKEINTERFACE -> {
                OwnedClassMember method = AsmUtil.lookupMethod(index, owner, name, descriptor);
                if (method == null) {
                    problems.addProblem(className, methodName, methodDesc, Errors.ACCESS_REMOVED_METHOD, owner, name, descriptor);
                } else {
                    AccessLevel accessLevel = method.member().access().accessLevel();
                    if (accessLevel == AccessLevel.PROTECTED || accessLevel == AccessLevel.PACKAGE) {
                        problems.addProblem(className, methodName, methodDesc, Errors.INTERFACE_CALL_TO_PACKAGE_OR_PROTECTED, owner, name, descriptor, accessLevel.getLowerName());
                    } else if (!AsmUtil.isMemberAccessible(index, className, method.owner(), accessLevel)) {
                        problems.addProblem(className, methodName, methodDesc, Errors.ACCESS_INACCESSIBLE_METHOD, owner, name, descriptor, accessLevel.getLowerName());
                    }
                    if (method.member().access().isStatic()) {
                        problems.addProblem(className, methodName, methodDesc, Errors.NONSTATIC_CALL_TO_STATIC_METHOD, owner, name, descriptor);
                    }
                }
            }
            case Opcodes.INVOKESPECIAL -> {
                // JVMS 21 §6.5.invokespecial
                String effectiveOwner = owner;
                if (!AsmUtil.CONSTRUCTOR_NAME.equals(name)) {
                    IResolvedClass currentClass = index.findClass(className);
                    for (IResolvedClass clazz = currentClass; clazz != null; clazz = index.findClass(clazz.getSuperclass())) {
                        if (owner.equals(clazz.getSuperclass())) {
                            effectiveOwner = currentClass.getSuperclass();
                            break;
                        }
                    }
                }

                List<OwnedClassMember> lookupResult = AsmUtil.multiLookupMethod(index, effectiveOwner, name, descriptor);
                List<OwnedClassMember> nonAbstractMethods = lookupResult.stream().filter(method -> !method.member().access().isAbstract()).toList();
                switch (nonAbstractMethods.size()) {
                    case 0 -> {
                        if (lookupResult.isEmpty()) {
                            problems.addProblem(className, methodName, methodDesc, Errors.ACCESS_REMOVED_METHOD, owner, name, descriptor);
                        } else {
                            problems.addProblem(className, methodName, methodDesc, Errors.INVOKESPECIAL_ABSTRACT_METHOD, owner, name, descriptor);
                        }
                    }
                    case 1 -> {
                        OwnedClassMember method = nonAbstractMethods.get(0);
                        if (AsmUtil.CONSTRUCTOR_NAME.equals(name) && !method.owner().equals(owner)) {
                            problems.addProblem(className, methodName, methodDesc, Errors.ACCESS_REMOVED_FIELD, owner, name, descriptor);
                        } else {
                            if (!AsmUtil.isMemberAccessible(index, className, method.owner(), method.member().access().accessLevel())) {
                                problems.addProblem(className, methodName, methodDesc, Errors.ACCESS_INACCESSIBLE_METHOD, owner, name, descriptor, method.member().access().accessLevel().getLowerName());
                            }
                            if (method.member().access().isStatic()) {
                                problems.addProblem(className, methodName, methodDesc, Errors.NONSTATIC_CALL_TO_STATIC_METHOD, owner, name, descriptor);
                            }
                        }
                    }
                    default -> problems.addProblem(className, methodName, methodDesc, Errors.INVOKESPECIAL_DIAMOND_PROBLEM, owner, name, descriptor);
                }
            }
            case Opcodes.INVOKESTATIC -> {
                OwnedClassMember method = AsmUtil.lookupMethod(index, owner, name, descriptor);
                if (method == null) {
                    problems.addProblem(className, methodName, methodDesc, Errors.ACCESS_REMOVED_METHOD, owner, name, descriptor);
                } else {
                    if (!AsmUtil.isMemberAccessible(index, className, method.owner(), method.member().access().accessLevel())) {
                        problems.addProblem(className, methodName, methodDesc, Errors.ACCESS_INACCESSIBLE_METHOD, owner, name, descriptor, method.member().access().accessLevel().getLowerName());
                    }
                    if (!method.member().access().isStatic()) {
                        problems.addProblem(className, methodName, methodDesc, Errors.STATIC_CALL_TO_NONSTATIC_METHOD, owner, name, descriptor);
                    }
                }
            }
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        // TODO
    }

    @Override
    public void visitLdcInsn(Object value) {
        if (value instanceof Type classConstant) {
            if (classConstant.getSort() == Type.METHOD) {
                // TODO: MethodType
            } else {
                checkClassReference(AsmUtil.getReferredClass(classConstant));
            }
        } else if (value instanceof Handle Handle) {
            // TODO
        } else if (value instanceof ConstantDynamic constantDynamic) {
            // TODO
        }
    }

    private boolean checkClassReference(@Nullable String referredClass) {
        if (referredClass != null) {
            IResolvedClass resolvedClass = index.findClass(referredClass);
            if (resolvedClass == null) {
                problems.addProblem(className, methodName, methodDesc, Errors.CODE_REFERENCES_REMOVED_CLASS, referredClass);
                return false;
            } else if (!AsmUtil.isClassAccessible(className, referredClass, resolvedClass.getAccess().accessLevel())) {
                problems.addProblem(className, methodName, methodDesc, Errors.CODE_REFERENCES_INACCESSIBLE_CLASS, referredClass, resolvedClass.getAccess().accessLevel().getLowerName());
                return false;
            }
        }

        return true;
    }
}
