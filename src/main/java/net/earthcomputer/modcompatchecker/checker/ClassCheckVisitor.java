package net.earthcomputer.modcompatchecker.checker;

import net.earthcomputer.modcompatchecker.indexer.IResolvedClass;
import net.earthcomputer.modcompatchecker.indexer.Index;
import net.earthcomputer.modcompatchecker.util.AccessFlags;
import net.earthcomputer.modcompatchecker.util.AsmUtil;
import net.earthcomputer.modcompatchecker.util.ClassMember;
import net.earthcomputer.modcompatchecker.util.NameAndDesc;
import net.earthcomputer.modcompatchecker.util.OwnedClassMember;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ClassCheckVisitor extends ClassVisitor {
    private final Index index;
    private final ICheckerConfig config;
    private final ProblemCollector problems;
    private String superName;
    @Nullable
    private IResolvedClass superClass;
    private String className;

    public ClassCheckVisitor(Index index, ICheckerConfig config, ProblemCollector problems) {
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
            checkPossiblyUnimplementedMethods(interfaces);
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

    // Possibly unimplemented methods are all abstract methods from all superclasses and superinterfaces,
    // and all other visible methods from superinterfaces.
    // The latter case is because calls to interface methods can lookup to abstract, static or inaccessible methods in
    // superclasses, even if the interface has a default implementation. See https://bugs.openjdk.org/browse/JDK-8021581
    private void checkPossiblyUnimplementedMethods(String @Nullable [] interfaces) {
        Map<NameAndDesc, PossiblyUnimplementedMethod> possiblyUnimplementedMethods = new LinkedHashMap<>();

        searchParentsForPossiblyUnimplementedMethods(interfaces, possiblyUnimplementedMethods);

        // remove the possibly unimplemented methods which are actually implemented and visible
        possiblyUnimplementedMethods.entrySet().removeIf(abstractMethod -> {
            OwnedClassMember concreteMethod = AsmUtil.lookupMethod(index, className, abstractMethod.getKey().name(), abstractMethod.getKey().desc());
            assert concreteMethod != null; // it shouldn't be null because we just found it in a parent class/interface
            if (concreteMethod.member().access().isAbstract() || concreteMethod.member().access().isStatic()) {
                return false;
            }
            if (abstractMethod.getValue().access.accessLevel().isHigherVisibility(concreteMethod.member().access().accessLevel())) {
                return false;
            }
            return AsmUtil.isMemberAccessible(index, className, concreteMethod.owner(), concreteMethod.member().access().accessLevel())
                && AsmUtil.isMemberAccessible(index, abstractMethod.getValue().owner(), concreteMethod.owner(), concreteMethod.member().access().accessLevel());
        });

        possiblyUnimplementedMethods.forEach((nameAndDesc, possiblyUnimplementedMethod) -> {
            if (possiblyUnimplementedMethod.access.isAbstract()) {
                problems.addProblem(className, Errors.ABSTRACT_METHOD_UNIMPLEMENTED, possiblyUnimplementedMethod.owner, nameAndDesc.name(), nameAndDesc.desc());
            } else {
                OwnedClassMember concreteMethod = AsmUtil.lookupMethod(index, className, nameAndDesc.name(), nameAndDesc.desc());
                assert concreteMethod != null;
                String problematicAccessModifier;
                if (concreteMethod.member().access().isStatic()) {
                    problematicAccessModifier = "static";
                } else if (concreteMethod.member().access().isAbstract()) {
                    problematicAccessModifier = "abstract";
                } else {
                    problematicAccessModifier = concreteMethod.member().access().accessLevel().getLowerName();
                }
                problems.addProblem(className, Errors.INCORRECT_INTERFACE_METHOD_LOOKUP, possiblyUnimplementedMethod.owner, nameAndDesc.name(), nameAndDesc.desc(), problematicAccessModifier, concreteMethod.owner());
            }
        });
    }

    private void searchParentsForPossiblyUnimplementedMethods(String @Nullable [] interfaces, Map<NameAndDesc, PossiblyUnimplementedMethod> possiblyUnimplementedMethods) {
        if (superClass != null) {
            searchClassAndParentsForPossiblyUnimplementedMethods(superName, superClass, possiblyUnimplementedMethods);
        }
        if (interfaces != null) {
            for (String itf : interfaces) {
                IResolvedClass resolvedClass = index.findClass(itf);
                if (resolvedClass != null) {
                    searchClassAndParentsForPossiblyUnimplementedMethods(itf, resolvedClass, possiblyUnimplementedMethods);
                }
            }
        }
    }

    private void searchClassAndParentsForPossiblyUnimplementedMethods(String className, IResolvedClass clazz, Map<NameAndDesc, PossiblyUnimplementedMethod> possiblyUnimplementedMethods) {
        for (ClassMember method : clazz.getMethods()) {
            NameAndDesc nameAndDesc = new NameAndDesc(method.name(), method.descriptor());
            if (method.access().isAbstract()
                || (clazz.getAccess().isInterface() && AsmUtil.isMemberAccessible(index, this.className, className, clazz, method.access().accessLevel()))) {
                possiblyUnimplementedMethods.merge(nameAndDesc, new PossiblyUnimplementedMethod(className, method.access()), PossiblyUnimplementedMethod::maxVisibility);
            }
        }

        IResolvedClass resolvedSuperClass = index.findClass(clazz.getSuperclass());
        if (resolvedSuperClass != null) {
            searchClassAndParentsForPossiblyUnimplementedMethods(clazz.getSuperclass(), resolvedSuperClass, possiblyUnimplementedMethods);
        }
        for (String itf : clazz.getInterfaces()) {
            IResolvedClass resolvedInterface = index.findClass(itf);
            if (resolvedInterface != null) {
                searchClassAndParentsForPossiblyUnimplementedMethods(itf, resolvedInterface, possiblyUnimplementedMethods);
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
                if (AsmUtil.canOverride(className, superName, method.access())) {
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

    private record PossiblyUnimplementedMethod(String owner, AccessFlags access) {
        static PossiblyUnimplementedMethod maxVisibility(PossiblyUnimplementedMethod a, PossiblyUnimplementedMethod b) {
            return b.access.accessLevel().isHigherVisibility(a.access().accessLevel()) ? b : a;
        }
    }
}
