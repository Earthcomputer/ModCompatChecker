package net.earthcomputer.modcompatchecker.util;

import org.objectweb.asm.Opcodes;

public enum AccessLevel {
    PUBLIC("public", Opcodes.ACC_PUBLIC),
    PROTECTED("protected", Opcodes.ACC_PROTECTED),
    PACKAGE("package", 0),
    PRIVATE("private", Opcodes.ACC_PRIVATE);

    private final String lowerName;
    private final int asm;

    AccessLevel(String lowerName, int asm) {
        this.lowerName = lowerName;
        this.asm = asm;
    }

    public String getLowerName() {
        return lowerName;
    }

    public int toAsm() {
        return asm;
    }

    public static AccessLevel fromAsm(int access) {
        if ((access & Opcodes.ACC_PUBLIC) != 0) {
            return PUBLIC;
        }
        if ((access & Opcodes.ACC_PROTECTED) != 0) {
            return PROTECTED;
        }
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            return PRIVATE;
        }
        return PACKAGE;
    }

    public boolean isHigherVisibility(AccessLevel other) {
        return ordinal() < other.ordinal();
    }
}
