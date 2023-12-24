package net.earthcomputer.modcompatchecker.config;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class PropertiesSectionType extends ConfigSectionType<Properties> {
    public PropertiesSectionType(String name) {
        super(name);
    }

    @Override
    public Properties load(List<String> lines) throws IOException {
        Properties properties = new Properties();
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }

            String[] kv = line.strip().split("\\s*=\\s*");
            if (kv.length != 2) {
                throw new IOException("Properties line does not contain '='");
            }
            if (properties.setProperty(kv[0], kv[1]) != null) {
                throw new IOException("Duplicate property \"" + kv[0] + "\"");
            }
        }
        return properties;
    }
}
