package net.earthcomputer.modcompatchecker.util;

public record ClassMember(AccessFlags access, String name, String descriptor) implements Comparable<ClassMember> {
    public ClassMember withAccess(AccessFlags access) {
        return new ClassMember(access, name, descriptor);
    }

    @Override
    public int compareTo(ClassMember o) {
        int nameCmp = this.name.compareTo(o.name);
        if (nameCmp != 0) {
            return nameCmp;
        } else {
            return this.descriptor.compareTo(o.descriptor);
        }
    }
}
