package net.earthcomputer.modcompatchecker.config;

import java.util.List;
import java.util.regex.Pattern;

public final class IncludeList {
    private final List<Directive> directives;

    IncludeList(List<Directive> directives) {
        this.directives = directives;
    }

    public boolean isIncluded(String file) {
        boolean included = true;
        for (Directive directive : directives) {
            if (directive.pattern.matcher(file).matches()) {
                included = directive.type == DirectiveType.INCLUDE;
            }
        }
        return included;
    }

    record Directive(DirectiveType type, Pattern pattern) {
    }

    enum DirectiveType {
        INCLUDE, EXCLUDE
    }
}
