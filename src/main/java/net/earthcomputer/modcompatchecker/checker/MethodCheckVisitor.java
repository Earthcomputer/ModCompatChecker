package net.earthcomputer.modcompatchecker.checker;

import net.earthcomputer.modcompatchecker.checker.condy.CondyChecker;
import net.earthcomputer.modcompatchecker.checker.indy.IndyChecker;
import net.earthcomputer.modcompatchecker.checker.indy.IndyContext;
import net.earthcomputer.modcompatchecker.indexer.IResolvedClass;
import net.earthcomputer.modcompatchecker.indexer.Index;
import net.earthcomputer.modcompatchecker.util.AccessLevel;
import net.earthcomputer.modcompatchecker.util.AsmUtil;
import net.earthcomputer.modcompatchecker.util.InheritanceUtil;
import net.earthcomputer.modcompatchecker.util.OwnedClassMember;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.List;

public final class MethodCheckVisitor extends MethodVisitor {
    private final Index index;
    private final ProblemCollector problems;
    private final String className;
    private final String methodName;
    private final String methodDesc;
    private int lineNumber = 1;

    public MethodCheckVisitor(Index index, ProblemCollector problems, String className, String methodName, String methodDesc) {
        super(AsmUtil.API);
        this.index = index;
        this.problems = problems;
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        lineNumber = line;
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
                    problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.INSTANTIATING_INTERFACE, type);
                } else if (resolvedClass.getAccess().isAbstract()) {
                    problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.INSTANTIATING_ABSTRACT_CLASS, type);
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

        checkFieldAccess(FieldAccessType.fromOpcode(opcode), owner, name, descriptor);
    }

    private void checkFieldAccess(FieldAccessType accessType, String owner, String name, String descriptor) {
        OwnedClassMember field = InheritanceUtil.lookupField(index, owner, name, descriptor);
        if (field == null) {
            problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.ACCESS_REMOVED_FIELD, owner, name, descriptor);
        } else {
            if (!AsmUtil.isMemberAccessible(index, className, field.owner(), field.member().access().accessLevel())) {
                problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.ACCESS_INACCESSIBLE_FIELD, owner, name, descriptor, field.member().access().accessLevel().getLowerName());
            }
            if (field.member().access().isStatic()) {
                if (!accessType.isStatic()) {
                    problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.NONSTATIC_ACCESS_TO_STATIC_FIELD, owner, name, descriptor);
                }
            } else {
                if (accessType.isStatic()) {
                    problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.STATIC_ACCESS_TO_NONSTATIC_FIELD, owner, name, descriptor);
                }
            }
            if (field.member().access().isFinal()) {
                if (accessType.isWrite()) {
                    String expectedMethod = accessType.isStatic() ? AsmUtil.CLASS_INITIALIZER_NAME : AsmUtil.CONSTRUCTOR_NAME;
                    if (!field.owner().equals(className) || !expectedMethod.equals(methodName)) {
                        problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.WRITE_FINAL_FIELD, owner, name, descriptor);
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

        checkMethodCall(MethodInvocationType.fromOpcode(opcode), owner, name, descriptor, isInterface);
    }

    private void checkMethodCall(MethodInvocationType invocationType, String owner, String name, String descriptor, boolean isInterface) {
        IResolvedClass resolvedClass = index.findClass(owner);
        if (resolvedClass != null && resolvedClass.getAccess().isInterface() != isInterface) {
            problems.addProblem(className, methodName, methodDesc, lineNumber, isInterface ? Errors.INTERFACE_CALL_TO_NON_INTERFACE_METHOD : Errors.NON_INTERFACE_CALL_TO_INTERFACE_METHOD, owner, name, descriptor);
        }

        switch (invocationType) {
            case VIRTUAL -> {
                OwnedClassMember method = InheritanceUtil.lookupMethod(index, owner, name, descriptor);
                if (method == null) {
                    problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.ACCESS_REMOVED_METHOD, owner, name, descriptor);
                } else {
                    if (!AsmUtil.isMemberAccessible(index, className, method.owner(), method.member().access().accessLevel())) {
                        problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.ACCESS_INACCESSIBLE_METHOD, owner, name, descriptor, method.member().access().accessLevel().getLowerName());
                    }
                    if (method.member().access().isStatic()) {
                        problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.NONSTATIC_CALL_TO_STATIC_METHOD, owner, name, descriptor);
                    }
                }
            }
            case INTERFACE -> {
                OwnedClassMember method = InheritanceUtil.lookupMethod(index, owner, name, descriptor);
                if (method == null) {
                    problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.ACCESS_REMOVED_METHOD, owner, name, descriptor);
                } else {
                    AccessLevel accessLevel = method.member().access().accessLevel();
                    if (accessLevel == AccessLevel.PROTECTED || accessLevel == AccessLevel.PACKAGE) {
                        problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.INTERFACE_CALL_TO_PACKAGE_OR_PROTECTED, owner, name, descriptor, accessLevel.getLowerName());
                    } else if (!AsmUtil.isMemberAccessible(index, className, method.owner(), accessLevel)) {
                        problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.ACCESS_INACCESSIBLE_METHOD, owner, name, descriptor, accessLevel.getLowerName());
                    }
                    if (method.member().access().isStatic()) {
                        problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.NONSTATIC_CALL_TO_STATIC_METHOD, owner, name, descriptor);
                    }
                }
            }
            case SPECIAL -> {
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

                List<OwnedClassMember> lookupResult = InheritanceUtil.multiLookupMethod(index, effectiveOwner, name, descriptor);
                List<OwnedClassMember> nonAbstractMethods = lookupResult.stream().filter(method -> !method.member().access().isAbstract()).toList();
                switch (nonAbstractMethods.size()) {
                    case 0 -> {
                        if (lookupResult.isEmpty()) {
                            problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.ACCESS_REMOVED_METHOD, owner, name, descriptor);
                        } else {
                            problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.INVOKESPECIAL_ABSTRACT_METHOD, owner, name, descriptor);
                        }
                    }
                    case 1 -> {
                        OwnedClassMember method = nonAbstractMethods.get(0);
                        if (AsmUtil.CONSTRUCTOR_NAME.equals(name) && !method.owner().equals(owner)) {
                            if (resolvedClass == null || !resolvedClass.getAccess().isInterface()) {
                                // if the resolved class is an interface, then we already gave an error about that
                                // don't report a related error about the constructor being removed because it's an interface
                                problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.ACCESS_REMOVED_METHOD, owner, name, descriptor);
                            }
                        } else {
                            if (!AsmUtil.isMemberAccessible(index, className, method.owner(), method.member().access().accessLevel())) {
                                problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.ACCESS_INACCESSIBLE_METHOD, owner, name, descriptor, method.member().access().accessLevel().getLowerName());
                            }
                            if (method.member().access().isStatic()) {
                                problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.NONSTATIC_CALL_TO_STATIC_METHOD, owner, name, descriptor);
                            }
                        }
                    }
                    default -> problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.INVOKESPECIAL_DIAMOND_PROBLEM, owner, name, descriptor);
                }
            }
            case STATIC -> {
                OwnedClassMember method = InheritanceUtil.lookupMethod(index, owner, name, descriptor);
                if (method == null) {
                    problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.ACCESS_REMOVED_METHOD, owner, name, descriptor);
                } else {
                    if (!AsmUtil.isMemberAccessible(index, className, method.owner(), method.member().access().accessLevel())) {
                        problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.ACCESS_INACCESSIBLE_METHOD, owner, name, descriptor, method.member().access().accessLevel().getLowerName());
                    }
                    if (!method.member().access().isStatic()) {
                        problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.STATIC_CALL_TO_NONSTATIC_METHOD, owner, name, descriptor);
                    }
                }
            }
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bsm, Object... args) {
        checkConstant(bsm);

        IndyChecker checker = IndyChecker.CHECKERS.get(bsm);
        if (checker != null) {
            checker.check(new IndyContext(index, problems, className, methodName, methodDesc, lineNumber, name, descriptor, bsm, args));
        } else {
            checkType(Type.getMethodType(descriptor));
            for (Object arg : args) {
                checkConstant(arg);
            }
        }
    }

    @Override
    public void visitLdcInsn(Object value) {
        checkConstant(value);
    }

    private void checkConstant(Object value) {
        if (value instanceof Type classConstant) {
            checkType(classConstant);
        } else if (value instanceof Handle handle) {
            if (handle.getTag() <= Opcodes.H_PUTSTATIC) {
                checkFieldAccess(FieldAccessType.fromHandleReferenceKind(handle.getTag()), handle.getOwner(), handle.getName(), handle.getDesc());
            } else {
                checkMethodCall(MethodInvocationType.fromHandleReferenceKind(handle.getTag()), handle.getOwner(), handle.getName(), handle.getDesc(), handle.isInterface());
            }
        } else if (value instanceof ConstantDynamic constantDynamic) {
            checkConstantDynamic(constantDynamic);
        }
    }

    private void checkConstantDynamic(ConstantDynamic constantDynamic) {
        checkConstant(constantDynamic.getBootstrapMethod());

        CondyChecker checker = CondyChecker.CHECKERS.get(constantDynamic.getBootstrapMethod());
        if (checker != null) {
            Object[] condyArgs = new Object[constantDynamic.getBootstrapMethodArgumentCount()];
            Arrays.setAll(condyArgs, constantDynamic::getBootstrapMethodArgument);
            checker.check(new IndyContext(index, problems, className, methodName, methodDesc, lineNumber, constantDynamic.getName(), constantDynamic.getDescriptor(), constantDynamic.getBootstrapMethod(), condyArgs));
        } else {
            checkType(Type.getType(constantDynamic.getDescriptor()));
            for (int i = 0; i < constantDynamic.getBootstrapMethodArgumentCount(); i++) {
                checkConstant(constantDynamic.getBootstrapMethodArgument(i));
            }
        }
    }

    private boolean checkType(Type type) {
        if (type.getSort() == Type.METHOD) {
            boolean result = true;
            for (Type argumentType : type.getArgumentTypes()) {
                if (!checkType(argumentType)) {
                    result = false;
                }
            }
            return checkType(type.getReturnType()) && result;
        }

        return checkClassReference(AsmUtil.getReferredClass(type));
    }

    private boolean checkClassReference(@Nullable String referredClass) {
        if (referredClass != null) {
            IResolvedClass resolvedClass = index.findClass(referredClass);
            if (resolvedClass == null) {
                problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.CODE_REFERENCES_REMOVED_CLASS, referredClass);
                return false;
            } else if (!AsmUtil.isClassAccessible(className, referredClass, resolvedClass.getAccess().accessLevel())) {
                problems.addProblem(className, methodName, methodDesc, lineNumber, Errors.CODE_REFERENCES_INACCESSIBLE_CLASS, referredClass, resolvedClass.getAccess().accessLevel().getLowerName());
                return false;
            }
        }

        return true;
    }

    private enum FieldAccessType {
        GETFIELD, GETSTATIC, PUTFIELD, PUTSTATIC;

        boolean isStatic() {
            return this == GETSTATIC || this == PUTSTATIC;
        }

        boolean isWrite() {
            return this == PUTFIELD || this == PUTSTATIC;
        }

        static FieldAccessType fromOpcode(int opcode) {
            return switch (opcode) {
                case Opcodes.GETFIELD -> GETFIELD;
                case Opcodes.GETSTATIC -> GETSTATIC;
                case Opcodes.PUTFIELD -> PUTFIELD;
                case Opcodes.PUTSTATIC -> PUTSTATIC;
                default -> throw new IllegalArgumentException("Invalid field access opcode " + opcode);
            };
        }

        static FieldAccessType fromHandleReferenceKind(int tag) {
            return switch (tag) {
                case Opcodes.H_GETFIELD -> GETFIELD;
                case Opcodes.H_GETSTATIC -> GETSTATIC;
                case Opcodes.H_PUTFIELD -> PUTFIELD;
                case Opcodes.H_PUTSTATIC -> PUTSTATIC;
                default -> throw new IllegalArgumentException("Invalid field access handle reference kind " + tag);
            };
        }
    }

    private enum MethodInvocationType {
        VIRTUAL, STATIC, SPECIAL, INTERFACE;

        static MethodInvocationType fromOpcode(int opcode) {
            return switch (opcode) {
                case Opcodes.INVOKEVIRTUAL -> VIRTUAL;
                case Opcodes.INVOKESTATIC -> STATIC;
                case Opcodes.INVOKESPECIAL -> SPECIAL;
                case Opcodes.INVOKEINTERFACE -> INTERFACE;
                default -> throw new IllegalArgumentException("Invalid method invocation opcode " + opcode);
            };
        }

        static MethodInvocationType fromHandleReferenceKind(int tag) {
            return switch (tag) {
                case Opcodes.H_INVOKEVIRTUAL -> VIRTUAL;
                case Opcodes.H_INVOKESTATIC -> STATIC;
                case Opcodes.H_INVOKESPECIAL, Opcodes.H_NEWINVOKESPECIAL -> SPECIAL;
                case Opcodes.H_INVOKEINTERFACE -> INTERFACE;
                default -> throw new IllegalArgumentException("Invalid method invocation handle reference kind " + tag);
            };
        }
    }
}
