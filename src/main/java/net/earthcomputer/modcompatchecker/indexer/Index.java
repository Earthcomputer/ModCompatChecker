package net.earthcomputer.modcompatchecker.indexer;

import net.earthcomputer.modcompatchecker.util.AccessFlags;
import net.earthcomputer.modcompatchecker.util.AsmUtil;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Index {
    private final Map<String, ClassIndex> classes = new HashMap<>();
    private final Map<String, ClasspathClass> classpathClassCache = new ConcurrentHashMap<>();

    public ClassIndex addClass(String name, AccessFlags access, String superclass, List<String> interfaces) {
        ClassIndex classIndex = new ClassIndex(access, superclass, interfaces);
        classes.put(name, classIndex);
        return classIndex;
    }

    @Nullable
    public IResolvedClass findClass(@Nullable String name) {
        if (name == null) {
            return null;
        }

        if (AsmUtil.OBJECT.equals(name)) {
            return ClasspathClass.OBJECT;
        }

        ClassIndex classIndex = classes.get(name);
        if (classIndex != null) {
            return classIndex;
        }

        ClasspathClass classpathClass = classpathClassCache.get(name);
        if (classpathClass != null) {
            return classpathClass;
        }

        try {
            Class<?> classpathCls = Class.forName(Type.getObjectType(name).getClassName());
            if (classpathClassCache.size() >= 200) {
                // periodically clear the cache to stop it getting out of hand
                classpathClassCache.clear();
            }
            classpathClass = new ClasspathClass(classpathCls);
            classpathClassCache.put(name, classpathClass);
            return classpathClass;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public void serialize(PrintWriter writer) {
        var entries = new ArrayList<>(classes.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        for (var entry : entries) {
            ClassIndex clazz = entry.getValue();
            writer.printf("class %s %s %s%s%s\n", clazz.getAccess(), entry.getKey(), clazz.getSuperclass(), clazz.getInterfaces().isEmpty() ? "" : " ", String.join(" ", clazz.getInterfaces()));
            clazz.serialize(writer);
        }
    }

    public void deserializeFrom(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(" ");
            if (parts.length < 4) {
                throw new IOException("Incomplete line: " + line);
            }

            String classType = parts[0];
            if (!"class".equals(classType)) {
                throw new IOException("Invalid line start: " + line);
            }

            String key = parts[2];
            String superclass = parts[3];
            List<String> interfaces = new ArrayList<>(Arrays.asList(parts).subList(4, parts.length));

            AccessFlags access = AccessFlags.parse(parts[1]);
            if (access == null) {
                throw new IOException("Invalid access flags: " + parts[1]);
            }

            ClassIndex classIndex = new ClassIndex(access, superclass, interfaces);

            classIndex.deserializeFrom(reader);
            classes.put(key, classIndex);
        }
    }
}