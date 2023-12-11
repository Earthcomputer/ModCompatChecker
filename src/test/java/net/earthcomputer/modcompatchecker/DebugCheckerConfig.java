package net.earthcomputer.modcompatchecker;

import net.earthcomputer.modcompatchecker.checker.ICheckerConfig;

public class DebugCheckerConfig implements ICheckerConfig {
    @Override
    public boolean shouldCheckClass(String className) {
        return true;
    }

    @Override
    public boolean isClassAccessedViaReflection(String className) {
        return true;
    }
}
