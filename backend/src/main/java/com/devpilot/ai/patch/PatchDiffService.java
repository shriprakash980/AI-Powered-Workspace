package com.devpilot.ai.patch;

import com.devpilot.ai.ai.util.DiffGenerator;
import org.springframework.stereotype.Component;

@Component
public class PatchDiffService {

    public DiffSummary computeDiff(String original, String modified, String filePath) {
        String orig = (original != null) ? original : "";
        String mod = (modified != null) ? modified : "";

        String unified = DiffGenerator.generateUnifiedDiff(orig, mod, filePath);
        int additions = 0;
        int deletions = 0;

        String[] lines = unified.split("\n");
        for (String line : lines) {
            if (line.startsWith("+") && !line.startsWith("+++")) {
                additions++;
            } else if (line.startsWith("-") && !line.startsWith("---")) {
                deletions++;
            }
        }

        return new DiffSummary(additions, deletions, unified);
    }

    public static class DiffSummary {
        private final int additions;
        private final int deletions;
        private final String unifiedDiff;

        public DiffSummary(int additions, int deletions, String unifiedDiff) {
            this.additions = additions;
            this.deletions = deletions;
            this.unifiedDiff = unifiedDiff;
        }

        public int getAdditions() {
            return additions;
        }

        public int getDeletions() {
            return deletions;
        }

        public String getUnifiedDiff() {
            return unifiedDiff;
        }
    }
}
