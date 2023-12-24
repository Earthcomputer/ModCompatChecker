package net.earthcomputer.modcompatchecker.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class IncludeListSectionType extends ConfigSectionType<IncludeList> {
    public IncludeListSectionType(String name) {
        super(name);
    }

    @Override
    public IncludeList load(List<String> lines) throws IOException {
        List<IncludeList.Directive> directives = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            String[] kv = line.split("\\s+");
            if (kv.length != 2) {
                throw new IOException("Include list directive did not match <include|exclude> <pattern>");
            }
            IncludeList.DirectiveType type = switch (kv[0]) {
                case "include" -> IncludeList.DirectiveType.INCLUDE;
                case "exclude" -> IncludeList.DirectiveType.EXCLUDE;
                default -> throw new IOException("Include list directive did not match <include|exclude> <pattern>");
            };
            directives.add(new IncludeList.Directive(type, patternToRegex(kv[1])));
        }
        return new IncludeList(directives);
    }

    private static Pattern patternToRegex(String pattern) {
        StringBuilder regex = new StringBuilder();
        int lastIndex = 0;
        for (int index = pattern.indexOf('*'); index != -1; index = pattern.indexOf('*', lastIndex)) {
            regex.append(Pattern.quote(pattern.substring(lastIndex, index)));
            lastIndex = index + 1;
            if (lastIndex < pattern.length() && pattern.charAt(lastIndex) == '*') {
                lastIndex++;
                regex.append(".*");
            } else {
                regex.append("[^/]*");
            }
        }
        regex.append(Pattern.quote(pattern.substring(lastIndex)));
        return Pattern.compile(regex.toString());
    }
}
