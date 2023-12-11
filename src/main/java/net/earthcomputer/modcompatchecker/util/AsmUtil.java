package net.earthcomputer.modcompatchecker.util;

import net.earthcomputer.modcompatchecker.indexer.ClassIndex;
import net.earthcomputer.modcompatchecker.indexer.IResolvedClass;
import net.earthcomputer.modcompatchecker.indexer.Index;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AsmUtil {
    public static final int API = getAsmApi();

    public static final String OBJECT = "java/lang/Object";
    public static final String CONSTRUCTOR_NAME = "<init>";

    private AsmUtil() {
    }

    private static int getAsmApi() {
        try {
            int apiVersion = Opcodes.ASM4;
            for (Field field : Opcodes.class.getFields()) {
                if (field.getName().matches("ASM\\d+")) {
                    apiVersion = Math.max(apiVersion, field.getInt(null));
                }
            }
            return apiVersion;
        } catch (Exception e) {
            throw new RuntimeException("Failed to determine ASM API", e);
        }
    }

    public static boolean isClassAccessible(String fromClass, String targetClass, int accessFlags) {
        return isClassAccessible(fromClass, targetClass, AccessLevel.fromAsm(accessFlags));
    }

    public static boolean isClassAccessible(String fromClass, String targetClass, AccessLevel accessLevel) {
        // JVMS 21 §5.4.4
        return accessLevel == AccessLevel.PUBLIC || areSamePackage(fromClass, targetClass);
    }

    public static boolean isMemberAccessible(Index index, String fromClass, String containingClassName, AccessLevel accessLevel) {
        IResolvedClass resolvedClass = index.findClass(containingClassName);
        return resolvedClass != null && isMemberAccessible(index, fromClass, containingClassName, resolvedClass, accessLevel);
    }

    public static boolean isMemberAccessible(Index index, String fromClass, String containingClassName, IResolvedClass containingClass, AccessLevel accessLevel) {
        // JVMS 21 §5.4.4

        switch (accessLevel) {
            case PUBLIC -> {
                return true;
            }
            case PROTECTED -> {
                if (isSubclass(index, fromClass, containingClassName) || areSamePackage(fromClass, containingClassName)) {
                    return true;
                }
            }
            case PACKAGE -> {
                if (areSamePackage(fromClass, containingClassName)) {
                    return true;
                }
            }
        }

        String nestHost = Objects.requireNonNullElse(containingClass.getNestHost(), containingClassName);
        if (nestHost.equals(fromClass)) {
            return true;
        }
        IResolvedClass resolvedNestHost = index.findClass(nestHost);
        return resolvedNestHost != null && resolvedNestHost.getNestMembers().contains(fromClass);
    }

    private static boolean areSamePackage(String a, String b) {
        int slashA = a.lastIndexOf('/');
        int slashB = b.lastIndexOf('/');
        return slashA == slashB && (slashA == -1 || a.startsWith(b.substring(0, slashB)));
    }

    private static boolean isSubclass(Index index, String className, String superclass) {
        IResolvedClass clazz;
        while ((clazz = index.findClass(className)) != null) {
            if (superclass.equals(className)) {
                return true;
            }
            className = clazz.getSuperclass();
        }

        return false;
    }

    public static boolean canOverride(String subclass, String superclass, AccessFlags superMethodAccess) {
        // JVMS 21 §5.4.5, assumes no intermediate subclasses between `subclass` and `superclass` which contain methods
        // that can override the super method.
        if (superMethodAccess.isStatic()) {
            return false;
        }
        return switch (superMethodAccess.accessLevel()) {
            case PUBLIC, PROTECTED -> true;
            case PACKAGE -> areSamePackage(subclass, superclass);
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
        // JVMS 21 §5.4.3.3 method resolution, 1-3 (method lookup)

        // search superclasses first
        String className = owner;
        for (IResolvedClass resolvedClass = index.findClass(className); resolvedClass != null; resolvedClass = index.findClass(className = resolvedClass.getSuperclass())) {
            for (ClassMember method : resolvedClass.getMethods()) {
                if (method.name().equals(name)) {
                    if (isSignaturePolymorphic(className, method.descriptor(), method.access())) {
                        return new OwnedClassMember(owner, method);
                    }
                    if (method.descriptor().equals(desc)) {
                        return new OwnedClassMember(owner, method);
                    }
                }
            }
        }

        // search for maximally specific methods
        Map<String, ClassMember> maximallySpecificMethods = new LinkedHashMap<>();
        Set<String> superinterfacesOfMaximallySpecificMethods = new HashSet<>();
        for (IResolvedClass resolvedClass = index.findClass(owner); resolvedClass != null; resolvedClass = index.findClass(resolvedClass.getSuperclass())) {
            for (String itf : resolvedClass.getInterfaces()) {
                IResolvedClass resolvedItf = index.findClass(itf);
                if (resolvedItf != null) {
                    searchForMaximallySpecificMethods(index, itf, resolvedItf, name, desc, maximallySpecificMethods, superinterfacesOfMaximallySpecificMethods);
                }
            }
        }

        if (maximallySpecificMethods.isEmpty()) {
            return null;
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
            return nonAbstractMethod;
        }

        // the spec says we can return an arbitrary method here, but we force an abstract method if possible so we trigger an abstract method error
        for (var entry : maximallySpecificMethods.entrySet()) {
            if (entry.getValue().access().isAbstract()) {
                return new OwnedClassMember(entry.getKey(), entry.getValue());
            }
        }

        // no abstract methods, so we return the first one we found
        var entry = maximallySpecificMethods.entrySet().iterator().next();
        return new OwnedClassMember(entry.getKey(), entry.getValue());
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

    @Nullable
    public static String getReferredClass(String typeDescriptor) {
        return getReferredClass(Type.getType(typeDescriptor));
    }

    @Nullable
    public static String getReferredClass(Type type) {
        if (type.getSort() == Type.ARRAY) {
            type = type.getElementType();
        }
        return type.getSort() == Type.OBJECT ? type.getInternalName() : null;
    }
}
