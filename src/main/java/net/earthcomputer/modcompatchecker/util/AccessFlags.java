package net.earthcomputer.modcompatchecker.util;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;

import java.util.Locale;

public final class AccessFlags {
    private final int flags;

    public AccessFlags(int flags) {
        this.flags = flags;
    }

    public AccessLevel accessLevel() {
        return AccessLevel.fromAsm(flags);
    }

    public boolean isStatic() {
        return (flags & Opcodes.ACC_STATIC) != 0;
    }

    public boolean isFinal() {
        return (flags & Opcodes.ACC_FINAL) != 0;
    }

    public boolean isAbstract() {
        return (flags & Opcodes.ACC_ABSTRACT) != 0;
    }

    public boolean isNative() {
        return (flags & Opcodes.ACC_NATIVE) != 0;
    }

    public boolean isVarArgs() {
        return (flags & Opcodes.ACC_VARARGS) != 0;
    }

    public boolean isInterface() {
        return (flags & Opcodes.ACC_INTERFACE) != 0;
    }

    public int toAsm() {
        return flags;
    }

    public static AccessFlags fromReflectionModifiers(int modifiers) {
        return new AccessFlags(modifiers & (
            Opcodes.ACC_PUBLIC
            | Opcodes.ACC_PROTECTED
            | Opcodes.ACC_PRIVATE
            | Opcodes.ACC_STATIC
            | Opcodes.ACC_FINAL
            | Opcodes.ACC_ABSTRACT
            | Opcodes.ACC_NATIVE
            | Opcodes.ACC_INTERFACE
        ));
    }

    @Nullable
    public static AccessFlags parse(String str) {
        String[] parts = str.split("-");
        AccessLevel accessLevel;
        try {
            accessLevel = AccessLevel.valueOf(parts[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
        int flags = accessLevel.toAsm();
        for (int i = 1; i < parts.length; i++) {
            switch (parts[i]) {
                case "static" -> flags |= Opcodes.ACC_STATIC;
                case "final" -> flags |= Opcodes.ACC_FINAL;
                case "abstract" -> flags |= Opcodes.ACC_ABSTRACT;
                case "native" -> flags |= Opcodes.ACC_NATIVE;
                case "varargs" -> flags |= Opcodes.ACC_VARARGS;
                case "interface" -> flags |= Opcodes.ACC_INTERFACE;
                default -> {
                    return null;
                }
            }
        }
        return new AccessFlags(flags);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(accessLevel().getLowerName());
        if (isStatic()) {
            sb.append("-static");
        }
        if (isFinal()) {
            sb.append("-final");
        }
        if (isAbstract()) {
            sb.append("-abstract");
        }
        if (isNative()) {
            sb.append("-native");
        }
        if (isVarArgs()) {
            sb.append("-varargs");
        }
        if (isInterface()) {
            sb.append("-interface");
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return flags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AccessFlags that = (AccessFlags) o;
        return flags == that.flags;
    }
}
