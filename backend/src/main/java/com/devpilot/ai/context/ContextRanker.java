package com.devpilot.ai.context;

import com.devpilot.ai.entity.ProjectFile;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ContextRanker {

    private final DependencyAnalyzer dependencyAnalyzer;

    public ContextRanker(DependencyAnalyzer dependencyAnalyzer) {
        this.dependencyAnalyzer = dependencyAnalyzer;
    }

    /**
     * Ranks all project files according to their relevance to the target file and query.
     */
    public List<RankedFile> rankFiles(List<ProjectFile> allFiles,
                                      ProjectFile activeFile,
                                      String selectedCode,
                                      String query) {
        if (allFiles == null || allFiles.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> activeDeps = Collections.emptySet();
        String activeDir = "";
        String activeBaseName = "";
        String activeExt = "";

        if (activeFile != null) {
            activeDeps = dependencyAnalyzer.extractDependencies(activeFile.getPath(), activeFile.getContent());
            activeDir = getDirectory(activeFile.getPath());
            activeBaseName = getBaseName(activeFile.getName());
            activeExt = getExtension(activeFile.getPath());
        }

        Set<String> queryKeywords = extractKeywords(query);

        List<RankedFile> ranked = new ArrayList<>();
        for (ProjectFile file : allFiles) {
            if (file.isDirectory()) continue;

            int score = 0;
            List<String> reasons = new ArrayList<>();

            boolean isCurrent = (activeFile != null && file.getId().equals(activeFile.getId()));
            if (isCurrent) {
                score += 100;
                reasons.add("Current file");
                if (selectedCode != null && !selectedCode.isBlank()) {
                    score += 100;
                    reasons.add("Selected code");
                }
            } else {
                String fileName = file.getName();
                String fileBase = getBaseName(fileName);
                String fileExt = getExtension(file.getPath());
                String fileDir = getDirectory(file.getPath());

                // Check direct dependency
                if (isDependencyMatch(file, activeDeps)) {
                    score += 80;
                    reasons.add("Direct dependency");
                }

                // Check matching class/component naming pattern (e.g. UserService and UserRepository / UserController)
                if (!activeBaseName.isEmpty() && isNameAffinity(activeBaseName, fileBase)) {
                    score += 50;
                    reasons.add("Matching component name");
                }

                // Check query keywords
                if (matchesQuery(file, queryKeywords)) {
                    score += 40;
                    reasons.add("Matches query keywords");
                }

                // Check same directory
                if (!activeDir.isEmpty() && activeDir.equals(fileDir)) {
                    score += 30;
                    reasons.add("Same directory");
                }

                // Check same extension
                if (!activeExt.isEmpty() && activeExt.equals(fileExt)) {
                    score += 10;
                    reasons.add("Same file type");
                }
            }

            if (score > 0 || isCurrent) {
                String primaryReason = String.join(", ", reasons);
                ranked.add(new RankedFile(file, score, primaryReason));
            }
        }

        // Sort descending by score
        ranked.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        return ranked;
    }

    private boolean isDependencyMatch(ProjectFile file, Set<String> activeDeps) {
        if (activeDeps.isEmpty()) return false;
        String name = file.getName();
        String base = getBaseName(name);
        return activeDeps.contains(name) || activeDeps.contains(base) || activeDeps.contains(file.getPath());
    }

    private boolean isNameAffinity(String base1, String base2) {
        String lower1 = base1.toLowerCase();
        String lower2 = base2.toLowerCase();
        return (lower1.length() >= 4 && lower2.contains(lower1)) ||
               (lower2.length() >= 4 && lower1.contains(lower2)) ||
               (commonPrefixLength(lower1, lower2) >= 4);
    }

    private int commonPrefixLength(String s1, String s2) {
        int len = 0;
        int max = Math.min(s1.length(), s2.length());
        while (len < max && s1.charAt(len) == s2.charAt(len)) {
            len++;
        }
        return len;
    }

    private boolean matchesQuery(ProjectFile file, Set<String> keywords) {
        if (keywords.isEmpty()) return false;
        String lowerName = file.getName().toLowerCase();
        for (String kw : keywords) {
            if (lowerName.contains(kw)) return true;
        }
        return false;
    }

    private Set<String> extractKeywords(String query) {
        if (query == null || query.isBlank()) return Collections.emptySet();
        String[] tokens = query.toLowerCase().replaceAll("[^a-zA-Z0-9_]", " ").split("\\s+");
        Set<String> set = new HashSet<>();
        for (String t : tokens) {
            if (t.length() >= 3 && !isStopWord(t)) {
                set.add(t);
            }
        }
        return set;
    }

    private boolean isStopWord(String word) {
        return Set.of("the", "and", "for", "with", "this", "that", "from", "file", "code", "what", "how", "make", "change").contains(word);
    }

    private String getDirectory(String path) {
        if (path == null) return "";
        int slash = path.lastIndexOf('/');
        return (slash != -1) ? path.substring(0, slash) : "";
    }

    private String getBaseName(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return (dot != -1) ? name.substring(0, dot) : name;
    }

    private String getExtension(String path) {
        if (path == null || !path.contains(".")) return "";
        return path.substring(path.lastIndexOf('.') + 1).toLowerCase();
    }

    public static class RankedFile {
        private final ProjectFile file;
        private final int score;
        private final String reason;

        public RankedFile(ProjectFile file, int score, String reason) {
            this.file = file;
            this.score = score;
            this.reason = reason;
        }

        public ProjectFile getFile() {
            return file;
        }

        public int getScore() {
            return score;
        }

        public String getReason() {
            return reason;
        }
    }
}
