package net.earthcomputer.modcompatchecker.fabric;

import net.earthcomputer.modcompatchecker.config.Config;
import net.earthcomputer.modcompatchecker.config.Plugin;
import net.earthcomputer.modcompatchecker.indexer.ClassIndex;
import net.earthcomputer.modcompatchecker.indexer.Index;
import net.earthcomputer.modcompatchecker.util.AccessFlags;
import net.earthcomputer.modcompatchecker.util.ClassMember;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AccessWidenerPlugin implements Plugin {
    private final Map<String, List<AccessWidenerOp>> widenedClasses = new HashMap<>();
    private final Map<String, List<AccessWidenerOp>> widenedFields = new HashMap<>();
    private final Map<String, List<AccessWidenerOp>> widenedMethods = new HashMap<>();

    @Override
    public String id() {
        return "access_widener";
    }

    @Override
    public void preIndexMod(Config config, Index index, Path modPath) throws IOException {
        readAccessWidener(config, modPath);
    }

    @Override
    public void preIndexLibrary(Config config, Index index, Path libraryPath) throws IOException {
        readAccessWidener(config, libraryPath);
    }

    private void readAccessWidener(Config config, Path modPath) throws IOException {
        try (JarFile modJar = new JarFile(modPath.toFile())) {
            FabricModJson modJson = FabricModJson.load(modJar);
            if (modJson == null) {
                return;
            }

            if (modJson.accessWidener != null) {
                JarEntry accessWidenerEntry = modJar.getJarEntry(modJson.accessWidener);
                if (accessWidenerEntry == null) {
                    throw new IOException("Could not find specified access widener \"" + modJson.accessWidener + "\"");
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(modJar.getInputStream(accessWidenerEntry), StandardCharsets.UTF_8))) {
                    doReadAccessWidener(config, reader);
                }
            }
        }
    }

    private void doReadAccessWidener(Config config, BufferedReader reader) throws IOException {
        boolean readHeader = false;
        String line;
        while ((line = reader.readLine()) != null) {
            int hashIndex = line.indexOf('#');
            if (hashIndex != -1) {
                line = line.substring(0, hashIndex);
            }
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.strip().split("\\s+");
            if (!readHeader) {
                readHeader = true;
                if (parts.length != 3 || !"accessWidener".equals(parts[0]) || !parts[1].startsWith("v")) {
                    throw new IOException("Access widener file did not start with \"accessWidener v<N> <namespace>\"");
                }
                int formatVersion;
                try {
                    formatVersion = Integer.parseInt(parts[1].substring(1));
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid number in access widener format version: " + parts[1]);
                }
                if (formatVersion > 2) {
                    return;
                }
                if (!getRuntimeNamespace(config).equals(parts[2])) {
                    return;
                }
            } else {
                if (parts.length < 3) {
                    throw new IOException("Invalid access widener format");
                }
                AccessWidenerOp widenerOps = switch (parts[0]) {
                    case "accessible", "transitive-accessible" -> AccessWidenerOp.ACCESSIBLE;
                    case "extendable", "transitive-extendable" -> AccessWidenerOp.EXTENDABLE;
                    case "mutable", "transitive-mutable" -> AccessWidenerOp.MUTABLE;
                    default -> throw new IOException("Invalid access widener directive " + parts[0]);
                };
                switch (parts[1]) {
                    case "class" -> {
                        if (parts.length != 3) {
                            throw new IOException("Invalid access widener format");
                        }
                        widenedClasses.computeIfAbsent(parts[2], k -> new ArrayList<>(1)).add(widenerOps);
                    }
                    case "field" -> {
                        if (parts.length != 5) {
                            throw new IOException("Invalid access widener format");
                        }
                        widenedFields.computeIfAbsent(parts[2] + " " + parts[3] + " " + parts[4], k -> new ArrayList<>(1)).add(widenerOps);
                    }
                    case "method" -> {
                        if (parts.length != 5) {
                            throw new IOException("Invalid access widener format");
                        }
                        widenedMethods.computeIfAbsent(parts[2] + " " + parts[3] + " " + parts[4], k -> new ArrayList<>(1)).add(widenerOps);
                    }
                    default -> throw new IOException("Invalid access widener format");
                }
            }
        }
    }

    private static String getRuntimeNamespace(Config config) {
        Properties props = config.getSection(FabricUtil.FABRIC_SECTION);
        return props == null ? "intermediary" : props.getProperty("runtimeNamespace", "intermediary");
    }

    @Override
    @Nullable
    public ClassIndex onIndexClass(Index index, String className, ClassIndex clazz) {
        List<AccessWidenerOp> widenOps = widenedClasses.get(className);
        if (widenOps != null) {
            for (AccessWidenerOp widenOp : widenOps) {
                clazz.setAccess(new AccessFlags(widenOp.apply(clazz.getAccess().toAsm(), false)));
            }
        }
        return clazz;
    }

    @Override
    @Nullable
    public ClassMember onIndexField(String className, ClassIndex clazz, ClassMember field) {
        List<AccessWidenerOp> widenOps = widenedFields.get(className + " " + field.name() + " " + field.descriptor());
        if (widenOps != null) {
            for (AccessWidenerOp widenOp : widenOps) {
                field = field.withAccess(new AccessFlags(widenOp.apply(field.access().toAsm(), true)));
            }
        }
        return field;
    }

    @Override
    @Nullable
    public ClassMember onIndexMethod(String className, ClassIndex clazz, ClassMember method) {
        List<AccessWidenerOp> widenOps = widenedMethods.get(className + " " + method.name() + " " + method.descriptor());
        if (widenOps != null) {
            for (AccessWidenerOp widenOp : widenOps) {
                method = method.withAccess(new AccessFlags(widenOp.apply(method.access().toAsm(), true)));
            }
        }
        return method;
    }

    @Override
    @Nullable
    public String onIndexPermittedSubclass(String className, ClassIndex clazz, String permittedSubclass) {
        List<AccessWidenerOp> widenOps = widenedClasses.get(className);
        if (widenOps != null && widenOps.contains(AccessWidenerOp.EXTENDABLE)) {
            return null;
        }
        return permittedSubclass;
    }

    private enum AccessWidenerOp {
        ACCESSIBLE, EXTENDABLE, MUTABLE;

        int apply(int access, boolean isMember) {
            return switch (this) {
                case ACCESSIBLE -> (access & ~(Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE)) | Opcodes.ACC_PUBLIC;
                case EXTENDABLE -> {
                    access &= ~(Opcodes.ACC_FINAL | Opcodes.ACC_PRIVATE);
                    if (isMember) {
                        if ((access & (Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC)) == 0) {
                            access |= Opcodes.ACC_PROTECTED;
                        }
                        yield access;
                    } else {
                        yield (access & ~Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
                    }
                }
                case MUTABLE -> access & ~Opcodes.ACC_FINAL;
            };
        }
    }
}
