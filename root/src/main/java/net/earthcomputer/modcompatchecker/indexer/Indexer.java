package net.earthcomputer.modcompatchecker.indexer;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class Indexer {
    private Indexer() {
    }

    public static void indexJar(Path jarFile, Index outIndex) throws IOException {
        try (JarFile jar = new JarFile(jarFile.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    ClassReader reader = new ClassReader(jar.getInputStream(entry));
                    reader.accept(new IndexerClassVisitor(outIndex), ClassReader.SKIP_CODE);
                }
            }
        }
    }
}
