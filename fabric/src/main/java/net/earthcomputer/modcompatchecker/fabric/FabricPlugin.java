package net.earthcomputer.modcompatchecker.fabric;

import net.earthcomputer.modcompatchecker.config.Config;
import net.earthcomputer.modcompatchecker.config.Plugin;
import net.earthcomputer.modcompatchecker.indexer.Index;
import net.earthcomputer.modcompatchecker.util.ThreeState;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

public class FabricPlugin implements Plugin {
    private final Set<String> entrypointClasses = new HashSet<>();

    @Override
    public String id() {
        return "fabric";
    }

    @Override
    public void initialize() {
        Config.registerSectionType(FabricUtil.FABRIC_SECTION);
    }

    @Override
    public void preIndexMod(Config config, Index index, Path modPath) throws IOException {
        try (JarFile modJar = new JarFile(modPath.toFile())) {
            FabricModJson modJson = FabricModJson.load(modJar);
            if (modJson == null) {
                return;
            }
            for (List<String> entrypointCategory : modJson.entrypoints.values()) {
                for (String entrypoint : entrypointCategory) {
                    entrypointClasses.add(entrypoint.replace('.', '/'));
                }
            }
        }
    }

    @Override
    public ThreeState isClassAccessedViaReflection(String className) {
        return entrypointClasses.contains(className) ? ThreeState.TRUE : ThreeState.UNKNOWN;
    }
}
