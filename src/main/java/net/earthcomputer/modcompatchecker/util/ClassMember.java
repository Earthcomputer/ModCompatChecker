package net.earthcomputer.modcompatchecker.util;

public record ClassMember(AccessFlags access, String name, String descriptor) implements Comparable<ClassMember> {
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
