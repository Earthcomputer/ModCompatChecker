package net.earthcomputer.modcompatchecker.fabric;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class FabricModJson {
    private static final Gson GSON = new Gson();

    public int schemaVersion;
    @Nullable
    public String accessWidener;

    @Nullable
    public static FabricModJson load(JarFile modJar) throws IOException {
        JarEntry modJsonEntry = modJar.getJarEntry("fabric.mod.json");
        if (modJsonEntry == null) {
            return null;
        }
        FabricModJson fabricModJson;
        try (Reader reader = new InputStreamReader(modJar.getInputStream(modJsonEntry), StandardCharsets.UTF_8)) {
            fabricModJson = GSON.fromJson(reader, FabricModJson.class);
        } catch (JsonParseException e) {
            throw new IOException("Failed to parse fabric.mod.json", e);
        }
        if (fabricModJson == null || fabricModJson.schemaVersion > 1) {
            return null;
        }
        return fabricModJson;
    }
}
