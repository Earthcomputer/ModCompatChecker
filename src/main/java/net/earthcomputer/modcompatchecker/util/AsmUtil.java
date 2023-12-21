package net.earthcomputer.modcompatchecker.util;

import net.earthcomputer.modcompatchecker.indexer.IResolvedClass;
import net.earthcomputer.modcompatchecker.indexer.Index;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.util.Objects;

public final class AsmUtil {
    public static final int API = getAsmApi();

    public static final String OBJECT = "java/lang/Object";
    public static final String CONSTRUCTOR_NAME = "<init>";
    public static final String CLASS_INITIALIZER_NAME = "<clinit>";

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

    public static boolean areSamePackage(String a, String b) {
        int slashA = a.lastIndexOf('/');
        int slashB = b.lastIndexOf('/');
        return slashA == slashB && (slashA == -1 || a.startsWith(b.substring(0, slashB)));
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
