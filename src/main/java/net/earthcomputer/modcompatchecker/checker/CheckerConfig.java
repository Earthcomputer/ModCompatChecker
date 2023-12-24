package net.earthcomputer.modcompatchecker.checker;

import net.earthcomputer.modcompatchecker.config.Config;
import net.earthcomputer.modcompatchecker.config.IncludeList;
import net.earthcomputer.modcompatchecker.config.IncludeListSectionType;

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
        return checkList == null || checkList.isIncluded(className);
    }

    @Override
    public boolean isClassAccessedViaReflection(String className) {
        IncludeList reflectList = config.getSection(REFLECTION_SECTION);
        return reflectList != null && reflectList.isIncluded(className);
    }
}
