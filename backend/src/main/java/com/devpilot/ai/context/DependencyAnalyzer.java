package com.devpilot.ai.context;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DependencyAnalyzer {

    private static final Pattern JAVA_IMPORT = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([a-zA-Z0-9_.]+);", Pattern.MULTILINE);
    private static final Pattern JS_IMPORT = Pattern.compile("(?:import\\s+.*?from\\s+['\"]([^'\"]+)['\"]|require\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\))");
    private static final Pattern HTML_SRC = Pattern.compile("<(?:script|link)[^>]+(?:src|href)=['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_IMPORT = Pattern.compile("@import\\s+(?:url\\(['\"]?|['\"])([^'\"\\)]+)(?:['\"]?\\)|['\"])");

    /**
     * Extracts referenced file paths, module names, or class names from the given source file.
     */
    public Set<String> extractDependencies(String path, String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptySet();
        }

        Set<String> deps = new HashSet<>();
        String ext = getExtension(path);

        switch (ext) {
            case "java" -> {
                Matcher m = JAVA_IMPORT.matcher(content);
                while (m.find()) {
                    String fqcn = m.group(1);
                    deps.add(fqcn);
                    int lastDot = fqcn.lastIndexOf('.');
                    if (lastDot != -1) {
                        deps.add(fqcn.substring(lastDot + 1)); // simple class name e.g. "UserService"
                    }
                }
            }
            case "js", "ts", "mjs", "jsx", "tsx" -> {
                Matcher m = JS_IMPORT.matcher(content);
                while (m.find()) {
                    String ref = m.group(1) != null ? m.group(1) : m.group(2);
                    addSanitizedRefs(deps, ref);
                }
            }
            case "html", "htm" -> {
                Matcher m = HTML_SRC.matcher(content);
                while (m.find()) {
                    addSanitizedRefs(deps, m.group(1));
                }
            }
            case "css" -> {
                Matcher m = CSS_IMPORT.matcher(content);
                while (m.find()) {
                    addSanitizedRefs(deps, m.group(1));
                }
            }
        }

        return deps;
    }

    private void addSanitizedRefs(Set<String> deps, String ref) {
        if (ref == null || ref.isBlank()) return;
        String clean = ref.trim();
        if (clean.startsWith("./")) clean = clean.substring(2);
        deps.add(clean);
        int lastSlash = clean.lastIndexOf('/');
        if (lastSlash != -1) {
            deps.add(clean.substring(lastSlash + 1));
        }
    }

    private String getExtension(String path) {
        if (path == null || !path.contains(".")) return "";
        return path.substring(path.lastIndexOf('.') + 1).toLowerCase();
    }
}
