package com.devpilot.ai.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DependencyAnalyzerTest {

    private DependencyAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new DependencyAnalyzer();
    }

    @Test
    @DisplayName("Should detect Java import statements")
    void testJavaImports() {
        String javaCode = """
                package com.devpilot.ai.service;
                
                import com.devpilot.ai.entity.Project;
                import static org.junit.jupiter.api.Assertions.assertEquals;
                import java.util.List;
                
                public class ProjectService {}
                """;

        Set<String> deps = analyzer.extractDependencies("ProjectService.java", javaCode);
        assertTrue(deps.contains("Project") || deps.contains("com.devpilot.ai.entity.Project"));
    }

    @Test
    @DisplayName("Should detect JS/TS import and require statements")
    void testJavaScriptImports() {
        String jsCode = """
                import { api } from './api.js';
                import Header from '../components/Header.js';
                const utils = require('./utils');
                """;

        Set<String> deps = analyzer.extractDependencies("main.js", jsCode);
        assertTrue(deps.contains("api.js") || deps.contains("./api.js"));
        assertTrue(deps.contains("Header.js") || deps.contains("../components/Header.js"));
    }

    @Test
    @DisplayName("Should detect HTML script and link tags")
    void testHtmlDependencies() {
        String htmlCode = """
                <!DOCTYPE html>
                <html>
                <head>
                    <link rel="stylesheet" href="css/style.css">
                    <script src="js/app.js"></script>
                </head>
                <body></body>
                </html>
                """;

        Set<String> deps = analyzer.extractDependencies("index.html", htmlCode);
        assertTrue(deps.contains("css/style.css"));
        assertTrue(deps.contains("js/app.js"));
    }
}
