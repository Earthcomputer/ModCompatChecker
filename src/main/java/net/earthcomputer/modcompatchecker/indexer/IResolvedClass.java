package net.earthcomputer.modcompatchecker.indexer;

import net.earthcomputer.modcompatchecker.util.AccessFlags;
import net.earthcomputer.modcompatchecker.util.ClassMember;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public sealed interface IResolvedClass permits ClassIndex, ClasspathClass {
    AccessFlags getAccess();
    @Nullable
    String getSuperclass();
    List<String> getInterfaces();
    Collection<ClassMember> getFields();
    Collection<ClassMember> getMethods();
    Collection<String> getPermittedSubclasses();
    @Nullable
    String getNestHost();
    Collection<String> getNestMembers();
}
