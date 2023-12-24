package net.earthcomputer.modcompatchecker.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConfigLoader {
    private static final Pattern HEADER_PATTERN = Pattern.compile("\\s*\\[(\\w+)]\\s*");

    private ConfigLoader() {
    }

    public static Config load(BufferedReader reader) throws IOException {
        Map<String, Object> sections = new HashMap<>();

        ConfigSectionType<?> currentSectionType = null;
        List<String> sectionLines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            Matcher headerMatcher = HEADER_PATTERN.matcher(line);
            if (headerMatcher.matches()) {
                if (currentSectionType != null) {
                    Object section = currentSectionType.load(sectionLines);
                    sectionLines.clear();
                    sections.put(currentSectionType.getName(), section);
                }

                String header = headerMatcher.group(1);
                if (sections.containsKey(header)) {
                    throw new IOException("Duplicate section \"" + header + "\"");
                }
                currentSectionType = Config.SECTION_TYPES.get(header);
                if (currentSectionType == null) {
                    throw new IOException("Unregistered section type + \"" + header + "\"");
                }
            } else {
                StringBuilder sectionLine = new StringBuilder();
                int lastIndex = 0;
                for (int index = line.indexOf('\\'); index != -1; index = line.indexOf('\\', lastIndex)) {
                    int commentIndex = line.indexOf('#', lastIndex);
                    if (commentIndex != -1 && commentIndex < index) {
                        sectionLine.append(line, lastIndex, commentIndex);
                        lastIndex = line.length();
                        break;
                    }
                    sectionLine.append(line, lastIndex, index);
                    if (index == line.length() - 1) {
                        line = reader.readLine();
                        if (line == null) {
                            throw new IOException("File ends with '\\'");
                        }
                        sectionLine.append('\n');
                        lastIndex = 0;
                    } else {
                        char c = line.charAt(index + 1);
                        switch (c) {
                            case '\\' -> sectionLine.append('\\');
                            case '[' -> sectionLine.append('[');
                            case '#' -> sectionLine.append('#');
                            case 'n' -> sectionLine.append('\n');
                            case 't' -> sectionLine.append('\t');
                            case 'r' -> sectionLine.append('\r');
                            case 'f' -> sectionLine.append('\f');
                            default -> throw new IOException("Invalid escape '\\" + c + "'");
                        }
                        lastIndex = index + 2;
                    }
                }
                sectionLine.append(line.substring(lastIndex));
                sectionLines.add(sectionLine.toString());
            }
        }

        if (currentSectionType != null) {
            Object section = currentSectionType.load(sectionLines);
            sections.put(currentSectionType.getName(), section);
        }

        return new Config(sections);
    }
}
