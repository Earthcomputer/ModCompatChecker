package net.earthcomputer.modcompatchecker.checker;

public interface ICheckerConfig {
    boolean shouldCheckClass(String className);
    boolean isClassAccessedViaReflection(String className);
}
