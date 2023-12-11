package net.earthcomputer.modcompatchecker.checker;

public final class CheckerConfig implements ICheckerConfig {
    @Override
    public boolean shouldCheckClass(String className) {
        // TODO: load config from file
        return true;
    }

    @Override
    public boolean isClassAccessedViaReflection(String className) {
        // TODO: load config from file
        return false;
    }
}
