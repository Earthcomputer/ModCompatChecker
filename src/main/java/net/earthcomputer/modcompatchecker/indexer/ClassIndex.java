package net.earthcomputer.modcompatchecker.indexer;

import net.earthcomputer.modcompatchecker.util.AccessFlags;
import net.earthcomputer.modcompatchecker.util.ClassMember;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class ClassIndex implements IResolvedClass {
    private final AccessFlags access;
    private final String superclass;
    private final List<String> interfaces;
    private final Set<ClassMember> fields = new TreeSet<>();
    private final Set<ClassMember> methods = new TreeSet<>();
    private final Set<String> permittedSubclasses = new TreeSet<>();
    @Nullable
    private String nestHost;
    private final Set<String> nestMembers = new TreeSet<>();

    public ClassIndex(AccessFlags access, String superclass, List<String> interfaces) {
        this.access = access;
        this.superclass = superclass;
        this.interfaces = interfaces;
    }

    @Override
    public AccessFlags getAccess() {
        return access;
    }

    @Override
    public String getSuperclass() {
        return superclass;
    }

    @Override
    public List<String> getInterfaces() {
        return interfaces;
    }

    public void addField(AccessFlags access, String name, String descriptor) {
        fields.add(new ClassMember(access, name, descriptor));
    }

    public void addMethod(AccessFlags access, String name, String descriptor) {
        methods.add(new ClassMember(access, name, descriptor));
    }

    public void addPermittedSubclass(String permittedSubclass) {
        permittedSubclasses.add(permittedSubclass);
    }

    public void setNestHost(@Nullable String nestHost) {
        this.nestHost = nestHost;
    }

    public void addNestMember(String nestMember) {
        nestMembers.add(nestMember);
    }

    @Override
    public Collection<ClassMember> getFields() {
        return fields;
    }

    @Override
    public Collection<ClassMember> getMethods() {
        return methods;
    }

    @Override
    public Collection<String> getPermittedSubclasses() {
        return permittedSubclasses;
    }

    @Nullable
    @Override
    public String getNestHost() {
        return nestHost;
    }

    @Override
    public Set<String> getNestMembers() {
        return nestMembers;
    }

    public void serialize(PrintWriter writer) {
        if (nestHost != null) {
            writer.printf("  nestHost %s\n", nestHost);
        }
        for (String nestMember : nestMembers) {
            writer.printf("  nestMember %s\n", nestMember);
        }
        for (String permittedSubclass : permittedSubclasses) {
            writer.printf("  permits %s\n", permittedSubclass);
        }
        for (ClassMember field : fields) {
            writer.printf("  field %s %s %s\n", field.access(), field.name(), field.descriptor());
        }
        for (ClassMember method : methods) {
            writer.printf("  method %s %s %s\n", method.access(), method.name(), method.descriptor());
        }
    }

    public void deserializeFrom(BufferedReader reader) throws IOException {
        while (true) {
            reader.mark(2);
            if (reader.read() != ' ') {
                reader.reset();
                return;
            }
            if (reader.read() != ' ') {
                reader.reset();
                return;
            }
            reader.reset();

            String line = reader.readLine();

            String[] parts = line.trim().split(" ");
            switch(parts[0]) {
                case "field", "method" -> {
                    if (parts.length < 4) {
                        throw new IOException("Invalid input line: Expected format - '<field|method> <access> <name> <descriptor>'");
                    }

                    AccessFlags access = AccessFlags.parse(parts[1]);
                    if (access == null) {
                        throw new IOException("Invalid access flags: " + parts[1]);
                    }

                    String name = parts[2];
                    String descriptor = parts[3];

                    if ("field".equals(parts[0])) {
                        addField(access, name, descriptor);
                    } else {
                        addMethod(access, name, descriptor);
                    }
                }
                case "permits" -> {
                    if (parts.length < 2) {
                        throw new IOException("Invalid input line: Expected format - 'permits <subclass>'");
                    }
                    addPermittedSubclass(parts[1]);
                }
                case "nestHost" -> {
                    if (parts.length < 2) {
                        throw new IOException("Invalid input line: Expected format - 'nestHost <host>'");
                    }
                    setNestHost(parts[1]);
                }
                case "nestMember" -> {
                    if (parts.length < 2) {
                        throw new IOException("Invalid input line: Expected format - 'nestMember <member>'");
                    }
                    addNestMember(parts[1]);
                }
                default -> throw new IOException("Invalid type: " + parts[0] + ". Expected field, method, permits, nestHost or nestMembers");
            }
        }
    }
}
