package net.earthcomputer.modcompatchecker.checker;

import net.earthcomputer.modcompatchecker.config.Config;
import net.earthcomputer.modcompatchecker.config.IncludeList;
import net.earthcomputer.modcompatchecker.config.IncludeListSectionType;
import net.earthcomputer.modcompatchecker.config.Plugin;
import net.earthcomputer.modcompatchecker.config.PluginLoader;
import net.earthcomputer.modcompatchecker.util.ThreeState;

public final class CheckerConfig implements ICheckerConfig {
    public static final IncludeListSectionType CHECK_SECTION = new IncludeListSectionType("check");
    public static final IncludeListSectionType REFLECTION_SECTION = new IncludeListSectionType("reflection");

    private final Config config;

    public CheckerConfig(Config config) {
        this.config = config;
    }

    @Override
    public boolean shouldCheckClass(String className) {
        IncludeList checkList = config.getSection(CHECK_SECTION);
        if (checkList != null && checkList.hasMatchingDirective(className)) {
            return checkList.isIncluded(className);
        }
        for (Plugin plugin : PluginLoader.plugins()) {
            ThreeState shouldCheckClass = plugin.shouldCheckClass(className);
            if (shouldCheckClass == ThreeState.TRUE) {
                return true;
            } else if (shouldCheckClass == ThreeState.FALSE) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isClassAccessedViaReflection(String className) {
        IncludeList reflectList = config.getSection(REFLECTION_SECTION);
        if (reflectList != null && reflectList.hasMatchingDirective(className)) {
            return reflectList.isIncluded(className);
        }
        for (Plugin plugin : PluginLoader.plugins()) {
            ThreeState classAccessedViaReflection = plugin.isClassAccessedViaReflection(className);
            if (classAccessedViaReflection == ThreeState.TRUE) {
                return true;
            } else if (classAccessedViaReflection == ThreeState.FALSE) {
                return false;
            }
        }
        return false;
    }
}
