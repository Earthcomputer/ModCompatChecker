package net.earthcomputer.modcompatchecker.util;

import net.earthcomputer.modcompatchecker.indexer.IResolvedClass;
import net.earthcomputer.modcompatchecker.indexer.Index;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InheritanceUtil {
    private InheritanceUtil() {
    }

    public static boolean canOverride(String subclass, String superclass, AccessFlags superMethodAccess) {
        // JVMS 21 §5.4.5, assumes no intermediate subclasses between `subclass` and `superclass` which contain methods
        // that can override the super method.
        if (superMethodAccess.isStatic()) {
            return false;
        }
        return switch (superMethodAccess.accessLevel()) {
            case PUBLIC, PROTECTED -> true;
            case PACKAGE -> AsmUtil.areSamePackage(subclass, superclass);
            case PRIVATE -> false;
        };
    }

    @Nullable
    public static OwnedClassMember lookupField(Index index, String owner, String name, String desc) {
        // JVMS 21 §5.4.3.2 field resolution, 1-4 (field lookup)
        for (IResolvedClass resolvedClass = index.findClass(owner); resolvedClass != null; resolvedClass = index.findClass(owner = resolvedClass.getSuperclass())) {
            for (ClassMember field : resolvedClass.getFields()) {
                if (field.name().equals(name) && field.descriptor().equals(desc)) {
                    return new OwnedClassMember(owner, field);
                }
            }
        }
        return null;
    }

    @Nullable
    public static OwnedClassMember lookupMethod(Index index, String owner, String name, String desc) {
        List<OwnedClassMember> multiLookup = multiLookupMethod(index, owner, name, desc);
        return multiLookup.isEmpty() ? null : multiLookup.get(0);
    }

    public static List<OwnedClassMember> multiLookupMethod(Index index, String owner, String name, String desc) {
        return multiLookupMethod(index, owner, List.of(), name, desc);
    }

    public static List<OwnedClassMember> multiLookupMethod(Index index, String owner, List<String> additionalInterfaces, String name, String desc) {
        // JVMS 21 §5.4.3.3 method resolution, 1-3 (method lookup)
        // JVMS 21 §5.4.3.4 interface method resolution, 1-6 (interface method lookup)
        // the below algorithm implements §5.4.3.3 if `owner` is a class and §5.4.3.4 if `owner` is an interface.

        // search superclasses first
        String className = owner;
        for (IResolvedClass resolvedClass = index.findClass(className); resolvedClass != null; resolvedClass = index.findClass(className = resolvedClass.getSuperclass())) {
            for (ClassMember method : resolvedClass.getMethods()) {
                if (method.name().equals(name)) {
                    if (isSignaturePolymorphic(className, method.descriptor(), method.access())) {
                        return List.of(new OwnedClassMember(className, method));
                    }
                    if (method.descriptor().equals(desc)) {
                        return List.of(new OwnedClassMember(className, method));
                    }
                }
            }
        }

        // search for maximally specific methods
        Map<String, ClassMember> maximallySpecificMethods = new LinkedHashMap<>();
        Set<String> superinterfacesOfMaximallySpecificMethods = new HashSet<>();
        for (String additionalInterface : additionalInterfaces) {
            IResolvedClass resolvedItf = index.findClass(additionalInterface);
            if (resolvedItf != null) {
                searchForMaximallySpecificMethods(index, additionalInterface, resolvedItf, name, desc, maximallySpecificMethods, superinterfacesOfMaximallySpecificMethods);
            }
        }
        for (IResolvedClass resolvedClass = index.findClass(owner); resolvedClass != null; resolvedClass = index.findClass(resolvedClass.getSuperclass())) {
            for (String itf : resolvedClass.getInterfaces()) {
                IResolvedClass resolvedItf = index.findClass(itf);
                if (resolvedItf != null) {
                    searchForMaximallySpecificMethods(index, itf, resolvedItf, name, desc, maximallySpecificMethods, superinterfacesOfMaximallySpecificMethods);
                }
            }
        }

        if (maximallySpecificMethods.isEmpty()) {
            return List.of();
        }

        // check if there is exactly one non-abstract maximally specific method
        OwnedClassMember nonAbstractMethod = null;
        for (var entry : maximallySpecificMethods.entrySet()) {
            if (!entry.getValue().access().isAbstract()) {
                if (nonAbstractMethod != null) {
                    nonAbstractMethod = null;
                    break;
                } else {
                    nonAbstractMethod = new OwnedClassMember(entry.getKey(), entry.getValue());
                }
            }
        }
        if (nonAbstractMethod != null) {
            return List.of(nonAbstractMethod);
        }

        // the spec says we can return an arbitrary method here, we return all of them
        return maximallySpecificMethods.entrySet().stream().map(entry -> new OwnedClassMember(entry.getKey(), entry.getValue())).toList();
    }

    private static void searchForMaximallySpecificMethods(Index index, String itf, IResolvedClass resolvedInterface, String name, String desc, Map<String, ClassMember> maximallySpecificMethods, Set<String> superinterfacesOfMaximallySpecificMethods) {
        if (maximallySpecificMethods.containsKey(itf) || superinterfacesOfMaximallySpecificMethods.contains(itf)) {
            return;
        }

        for (ClassMember method : resolvedInterface.getMethods()) {
            if (!method.name().equals(name) || !method.descriptor().equals(desc)) {
                continue;
            }
            if (method.access().isStatic() || method.access().accessLevel() == AccessLevel.PRIVATE) {
                continue;
            }

            maximallySpecificMethods.put(itf, method);
            findSuperinterfacesOfMaximallySpecificMethod(index, resolvedInterface, maximallySpecificMethods, superinterfacesOfMaximallySpecificMethods);
            return;
        }

        for (String superinterface : resolvedInterface.getInterfaces()) {
            IResolvedClass resolvedSuperinterface = index.findClass(superinterface);
            if (resolvedSuperinterface != null) {
                searchForMaximallySpecificMethods(index, superinterface, resolvedSuperinterface, name, desc, maximallySpecificMethods, superinterfacesOfMaximallySpecificMethods);
            }
        }
    }

    private static void findSuperinterfacesOfMaximallySpecificMethod(Index index, IResolvedClass resolvedInterface, Map<String, ClassMember> maximallySpecificMethods, Set<String> superinterfacesOfMaximallySpecificMethods) {
        for (String superinterface : resolvedInterface.getInterfaces()) {
            if (superinterfacesOfMaximallySpecificMethods.add(superinterface)) {
                maximallySpecificMethods.remove(superinterface);
                IResolvedClass resolvedSuperinterface = index.findClass(superinterface);
                if (resolvedSuperinterface != null) {
                    findSuperinterfacesOfMaximallySpecificMethod(index, resolvedSuperinterface, maximallySpecificMethods, superinterfacesOfMaximallySpecificMethods);
                }
            }
        }
    }

    private static boolean isSignaturePolymorphic(String owner, String desc, AccessFlags flags) {
        // JVMS 21 §2.9.3 - signature polymorphic methods
        if (!"java/lang/invoke/MethodHandle".equals(owner) && !"java/lang/invoke/VarHandle".equals(owner)) {
            return false;
        }
        if (!desc.startsWith("([Ljava/lang/Object;)")) {
            return false;
        }
        return flags.isVarArgs() && flags.isNative();
    }
}
