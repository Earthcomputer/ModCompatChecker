package net.earthcomputer.modcompatchecker.fabric;

import net.earthcomputer.modcompatchecker.config.Config;
import net.earthcomputer.modcompatchecker.config.Plugin;

public class FabricPlugin implements Plugin {
    @Override
    public String id() {
        return "fabric";
    }

    @Override
    public void initialize() {
        Config.registerSectionType(FabricUtil.FABRIC_SECTION);
    }
}
