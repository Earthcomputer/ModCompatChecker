package net.earthcomputer.modcompatchecker.indexer;

import net.earthcomputer.modcompatchecker.util.AccessFlags;
import net.earthcomputer.modcompatchecker.util.AsmUtil;
import net.earthcomputer.modcompatchecker.util.ClassMember;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public final class ClasspathClass implements IResolvedClass {
    public static final ClasspathClass OBJECT = new ClasspathClass(Object.class);

    private final Class<?> clazz;

    @Nullable
    private AccessFlags access;
    private boolean queriedSuperclass = false;
    @Nullable
    private String superclass;
    @Nullable
    private List<String> interfaces;
    @Nullable
    private List<ClassMember> fields;
    @Nullable
    private List<ClassMember> methods;
    @Nullable
    private List<String> permittedSubclasses;
    private boolean queriedNestHost = false;
    @Nullable
    private String nestHost;
    @Nullable
    private List<String> nestMembers;

    public ClasspathClass(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public AccessFlags getAccess() {
        if (access != null) {
            return access;
        }
        int flags = AccessFlags.fromReflectionModifiers(clazz.getModifiers()).toAsm();
        if (Enum.class.isAssignableFrom(clazz)) { // this differs from Class.isEnum for classes for specialized enum constants
            flags |= Opcodes.ACC_ENUM;
        }
        return access = new AccessFlags(flags);
    }

    @Override
    @Nullable
    public String getSuperclass() {
        if (queriedSuperclass) {
            return superclass;
        }
        if (clazz == Object.class) {
            queriedSuperclass = true;
            return null;
        }
        Class<?> superCls = clazz.getSuperclass();
        if (superCls == null) {
            superclass = AsmUtil.OBJECT;
            queriedSuperclass = true;
            return AsmUtil.OBJECT;
        }
        superclass = Type.getInternalName(superCls);
        queriedSuperclass = true;
        return superclass;
    }

    @Override
    public List<String> getInterfaces() {
        if (interfaces != null) {
            return interfaces;
        }
        return interfaces = Arrays.stream(clazz.getInterfaces()).map(Type::getInternalName).toList();
    }

    @Override
    public Collection<ClassMember> getFields() {
        if (fields != null) {
            return fields;
        }
        return fields = Arrays.stream(clazz.getDeclaredFields()).map(ClasspathClass::fieldToMember).toList();
    }

    private static ClassMember fieldToMember(Field field) {
        int flags = AccessFlags.fromReflectionModifiers(field.getModifiers()).toAsm();
        if (field.isEnumConstant()) {
            flags |= Opcodes.ACC_ENUM;
        }
        return new ClassMember(new AccessFlags(flags), field.getName(), Type.getDescriptor(field.getType()));
    }

    @Override
    public Collection<ClassMember> getMethods() {
        if (methods != null) {
            return methods;
        }
        return methods = Stream.concat(
            Arrays.stream(clazz.getDeclaredConstructors()).map(ClasspathClass::constructorToMember),
            Arrays.stream(clazz.getDeclaredMethods()).map(ClasspathClass::methodToMember)
        ).toList();
    }

    private static ClassMember constructorToMember(Constructor<?> constructor) {
        int flags = AccessFlags.fromReflectionModifiers(constructor.getModifiers()).toAsm();
        if (constructor.isVarArgs()) {
            flags |= Opcodes.ACC_VARARGS;
        }
        return new ClassMember(new AccessFlags(flags), AsmUtil.CONSTRUCTOR_NAME, Type.getConstructorDescriptor(constructor));
    }

    private static ClassMember methodToMember(Method method) {
        int flags = AccessFlags.fromReflectionModifiers(method.getModifiers()).toAsm();
        if (method.isVarArgs()) {
            flags |= Opcodes.ACC_VARARGS;
        }
        return new ClassMember(new AccessFlags(flags), method.getName(), Type.getMethodDescriptor(method));
    }

    @Override
    public Collection<String> getPermittedSubclasses() {
        if (permittedSubclasses != null) {
            return permittedSubclasses;
        }
        Class<?>[] permits = clazz.getPermittedSubclasses();
        if (permits == null) {
            return permittedSubclasses = List.of();
        } else {
            return permittedSubclasses = Arrays.stream(permits).map(Type::getInternalName).toList();
        }
    }

    @Nullable
    @Override
    public String getNestHost() {
        if (queriedNestHost) {
            return nestHost;
        }
        Class<?> host = clazz.getNestHost();
        nestHost = host == clazz ? null : Type.getInternalName(host);
        queriedNestHost = true;
        return nestHost;
    }

    @Override
    public Collection<String> getNestMembers() {
        if (nestMembers != null) {
            return nestMembers;
        }
        return nestMembers = Arrays.stream(clazz.getNestMembers())
            .filter(c -> c != clazz)
            .map(Type::getInternalName)
            .toList();
    }
}
