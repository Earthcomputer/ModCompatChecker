package net.earthcomputer.modcompatchecker.util;

import net.earthcomputer.modcompatchecker.indexer.IResolvedClass;
import net.earthcomputer.modcompatchecker.indexer.Index;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Possibly unimplemented methods are all abstract methods from all superclasses and superinterfaces,
// and all other visible methods from superinterfaces.
// The latter case is because calls to interface methods can lookup to abstract, static or inaccessible methods in
// superclasses, even if the interface has a default implementation. See https://bugs.openjdk.org/browse/JDK-8021581
public abstract class UnimplementedMethodChecker {
    protected final Index index;
    protected final String superName;
    protected final String @Nullable[] interfaces;

    protected UnimplementedMethodChecker(Index index, String superName, String @Nullable [] interfaces) {
        this.index = index;
        this.superName = superName;
        this.interfaces = interfaces;
    }

    public void run() {
        Map<NameAndDesc, PossiblyUnimplementedMethod> possiblyUnimplementedMethods = new LinkedHashMap<>();

        searchParentsForPossiblyUnimplementedMethods(possiblyUnimplementedMethods);

        // remove the possibly unimplemented methods which are actually implemented and visible
        possiblyUnimplementedMethods.entrySet().removeIf(abstractMethod -> {
            List<OwnedClassMember> lookupResult = multiLookupMethod(abstractMethod.getKey().name(), abstractMethod.getKey().desc());
            List<OwnedClassMember> nonAbstractMethods = lookupResult.stream().filter(method -> !method.member().access().isAbstract()).toList();
            if (nonAbstractMethods.size() != 1) {
                return false;
            }
            OwnedClassMember concreteMethod = nonAbstractMethods.get(0);
            if (concreteMethod.member().access().isStatic()) {
                return false;
            }
            if (abstractMethod.getValue().access.accessLevel().isHigherVisibility(concreteMethod.member().access().accessLevel())) {
                return false;
            }
            return isMethodAccessible(concreteMethod.owner(), concreteMethod.member().access().accessLevel())
                && AsmUtil.isMemberAccessible(index, abstractMethod.getValue().owner(), concreteMethod.owner(), concreteMethod.member().access().accessLevel());
        });

        possiblyUnimplementedMethods.forEach((nameAndDesc, possiblyUnimplementedMethod) -> {
            List<OwnedClassMember> lookupResult = multiLookupMethod(nameAndDesc.name(), nameAndDesc.desc());
            List<OwnedClassMember> nonAbstractMethods = lookupResult.stream().filter(method -> !method.member().access().isAbstract()).toList();
            if (nonAbstractMethods.size() > 1) {
                onDiamondProblem(nameAndDesc.name(), nameAndDesc.desc());
            } else if (possiblyUnimplementedMethod.access.isAbstract()) {
                onAbstractMethodUnimplemented(possiblyUnimplementedMethod.owner, nameAndDesc.name(), nameAndDesc.desc());
            } else {
                String problematicAccessModifier;
                String owner;
                if (nonAbstractMethods.isEmpty()) {
                    problematicAccessModifier = "abstract";
                    owner = lookupResult.get(0).owner();
                } else {
                    OwnedClassMember concreteMethod = nonAbstractMethods.get(0);
                    if (concreteMethod.member().access().isStatic()) {
                        problematicAccessModifier = "static";
                    } else {
                        problematicAccessModifier = concreteMethod.member().access().accessLevel().getLowerName();
                    }
                    owner = concreteMethod.owner();
                }
                onIncorrectInterfaceMethodLookup(possiblyUnimplementedMethod.owner, nameAndDesc.name(), nameAndDesc.desc(), owner, problematicAccessModifier);
            }
        });
    }

    private void searchParentsForPossiblyUnimplementedMethods(Map<NameAndDesc, PossiblyUnimplementedMethod> possiblyUnimplementedMethods) {
        IResolvedClass superClass = index.findClass(superName);
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
                || (clazz.getAccess().isInterface() && !method.access().isStatic() && isMethodAccessible(className, clazz, method.access().accessLevel()))) {
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

    protected boolean isMethodAccessible(String containingClassName, AccessLevel accessLevel) {
        IResolvedClass containingClass = index.findClass(containingClassName);
        return containingClass != null && isMethodAccessible(containingClassName, containingClass, accessLevel);
    }

    protected abstract boolean isMethodAccessible(String containingClassName, IResolvedClass containingClass, AccessLevel accessLevel);

    protected abstract List<OwnedClassMember> multiLookupMethod(String name, String desc);

    protected abstract void onDiamondProblem(String methodName, String methodDesc);
    protected abstract void onAbstractMethodUnimplemented(String methodOwner, String methodName, String methodDesc);
    protected abstract void onIncorrectInterfaceMethodLookup(String interfaceName, String methodName, String methodDesc, String resolvedClassName, String problematicAccessModifier);

    private record PossiblyUnimplementedMethod(String owner, AccessFlags access) {
        static PossiblyUnimplementedMethod maxVisibility(PossiblyUnimplementedMethod a, PossiblyUnimplementedMethod b) {
            return b.access.accessLevel().isHigherVisibility(a.access().accessLevel()) ? b : a;
        }
    }

    public static abstract class Simple extends UnimplementedMethodChecker {
        protected final String className;

        protected Simple(Index index, String className, String superName, String @Nullable [] interfaces) {
            super(index, superName, interfaces);
            this.className = className;
        }

        @Override
        protected boolean isMethodAccessible(String containingClassName, IResolvedClass containingClass, AccessLevel accessLevel) {
            return AsmUtil.isMemberAccessible(index, className, containingClassName, containingClass, accessLevel);
        }

        @Override
        protected List<OwnedClassMember> multiLookupMethod(String name, String desc) {
            return InheritanceUtil.multiLookupMethod(index, className, name, desc);
        }
    }
}
