package net.earthcomputer.modcompatchecker.checker;

import net.earthcomputer.modcompatchecker.indexer.IResolvedClass;
import net.earthcomputer.modcompatchecker.indexer.Index;
import net.earthcomputer.modcompatchecker.util.AsmUtil;
import net.earthcomputer.modcompatchecker.util.ClassMember;
import net.earthcomputer.modcompatchecker.util.InheritanceUtil;
import net.earthcomputer.modcompatchecker.util.UnimplementedMethodChecker;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class ClassCheckVisitor extends ClassVisitor {
    private final Index index;
    private final CheckerConfig config;
    private final ProblemCollector problems;
    private String superName;
    @Nullable
    private IResolvedClass superClass;
    private String className;

    public ClassCheckVisitor(Index index, CheckerConfig config, ProblemCollector problems) {
        super(AsmUtil.API);
        this.index = index;
        this.config = config;
        this.problems = problems;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String @Nullable [] interfaces) {
        className = name;

        this.superName = superName;
        superClass = index.findClass(superName);
        checkSuperClass();
        checkInterfaces(superName, interfaces);

        if ((access & Opcodes.ACC_ABSTRACT) == 0 && (access & Opcodes.ACC_INTERFACE) == 0) {
            UnimplementedMethodChecker checker = new UnimplementedMethodChecker.Simple(index, name, superName, interfaces) {
                @Override
                protected void onDiamondProblem(String methodName, String methodDesc) {
                    problems.addProblem(className, Errors.DIAMOND_PROBLEM, methodName, methodDesc);
                }

                @Override
                protected void onAbstractMethodUnimplemented(String methodOwner, String methodName, String methodDesc) {
                    problems.addProblem(className, Errors.ABSTRACT_METHOD_UNIMPLEMENTED, methodOwner, methodName, methodDesc);
                }

                @Override
                protected void onIncorrectInterfaceMethodLookup(String interfaceName, String methodName, String methodDesc, String resolvedClassName, String problematicAccessModifier) {
                    problems.addProblem(className, Errors.INCORRECT_INTERFACE_METHOD_LOOKUP, interfaceName, methodName, methodDesc, problematicAccessModifier, resolvedClassName);
                }
            };
            checker.run();
        }
    }

    private void checkSuperClass() {
        // JVMS 21 §5.3.5.3 - superclass resolution

        if (AsmUtil.OBJECT.equals(className)) {
            return;
        }

        if (superClass == null) {
            problems.addProblem(className, Errors.CLASS_EXTENDS_REMOVED, superName);
        } else {
            if (!AsmUtil.isClassAccessible(className, superName, superClass.getAccess().accessLevel())) {
                problems.addProblem(className, Errors.CLASS_EXTENDS_INACCESSIBLE, superName, superClass.getAccess().accessLevel().getLowerName());
            }

            if (superClass.getAccess().isInterface()) {
                problems.addProblem(className, Errors.CLASS_EXTENDS_INTERFACE, superName);
            }

            if (superClass.getAccess().isFinal()) {
                problems.addProblem(className, Errors.CLASS_EXTENDS_FINAL, superName);
            }

            if (!superClass.getPermittedSubclasses().isEmpty()) {
                if (!superClass.getPermittedSubclasses().contains(className)) {
                    problems.addProblem(className, Errors.CLASS_EXTENDS_SEALED, superName);
                }
            }
        }
    }

    private void checkInterfaces(String superName, String @Nullable [] interfaces) {
        // JVMS 21 §5.3.5.4 - superinterface resolution
        if (interfaces != null) {
            for (String itf : interfaces) {
                IResolvedClass resolvedInterface = index.findClass(itf);
                if (resolvedInterface == null) {
                    problems.addProblem(className, Errors.CLASS_IMPLEMENTS_REMOVED, itf);
                } else {
                    if (!AsmUtil.isClassAccessible(className, itf, resolvedInterface.getAccess().accessLevel())) {
                        problems.addProblem(className, Errors.CLASS_IMPLEMENTS_INACCESSIBLE, itf, resolvedInterface.getAccess().accessLevel().getLowerName());
                    }
                    if (!resolvedInterface.getAccess().isInterface()) {
                        problems.addProblem(className, Errors.CLASS_IMPLEMENTS_CLASS, itf);
                    }
                    if (!resolvedInterface.getPermittedSubclasses().isEmpty()) {
                        if (!resolvedInterface.getPermittedSubclasses().contains(className)) {
                            problems.addProblem(className, Errors.CLASS_IMPLEMENTS_SEALED, superName);
                        }
                    }
                }
            }
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        resolveFieldType(name, descriptor);
        return null;
    }

    private void resolveFieldType(String name, String descriptor) {
        String classType = AsmUtil.getReferredClass(descriptor);
        if (classType != null) {
            IResolvedClass resolvedClass = index.findClass(classType);
            if (resolvedClass == null) {
                problems.addProblem(className, name, descriptor, Errors.FIELD_TYPE_REMOVED, classType);
            } else if (!AsmUtil.isClassAccessible(className, classType, resolvedClass.getAccess().accessLevel())) {
                problems.addProblem(className, name, descriptor, Errors.FIELD_TYPE_INACCESSIBLE, classType, resolvedClass.getAccess().accessLevel().getLowerName());
            }
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String @Nullable [] exceptions) {
        resolveMethodReturnAndParamTypes(name, descriptor);
        if (config.isClassAccessedViaReflection(className)) {
            resolveExceptions(name, descriptor, exceptions);
        }

        checkOverridesFinalMethod(access, name, descriptor);

        return new MethodCheckVisitor(index, problems, className, name, descriptor);
    }

    private void resolveMethodReturnAndParamTypes(String name, String descriptor) {
        Type methodType = Type.getType(descriptor);

        String returnClass = AsmUtil.getReferredClass(methodType.getReturnType());
        if (returnClass != null) {
            IResolvedClass resolvedClass = index.findClass(returnClass);
            if (resolvedClass == null) {
                problems.addProblem(className, name, descriptor, Errors.METHOD_RETURN_TYPE_REMOVED, returnClass);
            } else if (!AsmUtil.isClassAccessible(className, returnClass, resolvedClass.getAccess().accessLevel())) {
                problems.addProblem(className, name, descriptor, Errors.METHOD_RETURN_TYPE_INACCESSIBLE, returnClass, resolvedClass.getAccess().accessLevel().getLowerName());
            }
        }

        for (Type argumentType : methodType.getArgumentTypes()) {
            String argumentClass = AsmUtil.getReferredClass(argumentType);
            if (argumentClass != null) {
                IResolvedClass resolvedClass = index.findClass(argumentClass);
                if (resolvedClass == null) {
                    problems.addProblem(className, name, descriptor, Errors.METHOD_PARAM_TYPE_REMOVED, argumentClass);
                } else if (!AsmUtil.isClassAccessible(className, argumentClass, resolvedClass.getAccess().accessLevel())) {
                    problems.addProblem(className, name, descriptor, Errors.METHOD_PARAM_TYPE_INACCESSIBLE, argumentClass, resolvedClass.getAccess().accessLevel().getLowerName());
                }
            }
        }
    }

    private void resolveExceptions(String name, String descriptor, String @Nullable [] exceptions) {
        if (exceptions != null) {
            for (String exception : exceptions) {
                IResolvedClass resolvedClass = index.findClass(exception);
                if (resolvedClass == null) {
                    problems.addProblem(className, name, descriptor, Errors.METHOD_THROWS_TYPE_REMOVED, exception);
                } else if (!AsmUtil.isClassAccessible(className, exception, resolvedClass.getAccess().accessLevel())) {
                    problems.addProblem(className, name, descriptor, Errors.METHOD_THROWS_TYPE_INACCESSIBLE, exception, resolvedClass.getAccess().accessLevel().getLowerName());
                }
            }
        }
    }

    private void checkOverridesFinalMethod(int access, String name, String descriptor) {
        // JVMS 21 §5.3.5.3

        if (AsmUtil.CONSTRUCTOR_NAME.equals(name) || (access & Opcodes.ACC_PRIVATE) != 0 || (access & Opcodes.ACC_STATIC) != 0) {
            return;
        }

        String superName = this.superName;
        for (IResolvedClass superClass = this.superClass; superClass != null; superClass = index.findClass(superName = superClass.getSuperclass())) {
            for (ClassMember method : superClass.getMethods()) {
                if (!method.name().equals(name) || !method.descriptor().equals(descriptor)) {
                    continue;
                }
                if (InheritanceUtil.canOverride(className, superName, method.access())) {
                    if (method.access().isFinal()) {
                        problems.addProblem(className, name, descriptor, Errors.METHOD_OVERRIDES_FINAL);
                    } else {
                        // we've found the method we are overriding
                    }
                    return;
                }
            }
        }
    }
}
