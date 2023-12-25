package net.earthcomputer.modcompatchecker.config;

import net.earthcomputer.modcompatchecker.checker.CheckerConfig;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Config {
    static final Map<String, ConfigSectionType<?>> SECTION_TYPES = new HashMap<>();
    private final Map<String, Object> sections;

    Config(Map<String, Object> sections) {
        this.sections = sections;
    }

    public static Config empty() {
        return new Config(Collections.emptyMap());
    }

    public static void registerSectionType(ConfigSectionType<?> type) {
        if (SECTION_TYPES.putIfAbsent(type.getName(), type) != null) {
            throw new IllegalStateException("Tried to register config section type \"" + type.getName() + "\" but a config section type with that name is already registered");
        }
    }

    @VisibleForTesting
    public static void unregisterAll() {
        SECTION_TYPES.clear();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getSection(ConfigSectionType<T> type) {
        if (!SECTION_TYPES.containsKey(type.getName())) {
            throw new IllegalArgumentException("Unregistered section type \"" + type.getName() + "\"");
        }
        return (T) sections.get(type.getName());
    }

    static {
        registerSectionType(CheckerConfig.CHECK_SECTION);
        registerSectionType(CheckerConfig.REFLECTION_SECTION);
    }
}
