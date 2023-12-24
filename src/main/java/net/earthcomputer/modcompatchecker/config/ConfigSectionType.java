package net.earthcomputer.modcompatchecker.config;

import java.io.IOException;
import java.util.List;

public abstract class ConfigSectionType<T> {
    private final String name;

    protected ConfigSectionType(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    public abstract T load(List<String> lines) throws IOException;
}
