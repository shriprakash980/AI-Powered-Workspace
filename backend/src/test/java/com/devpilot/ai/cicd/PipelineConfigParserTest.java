package com.devpilot.ai.cicd;

import com.devpilot.ai.cicd.parser.PipelineConfig;
import com.devpilot.ai.cicd.parser.PipelineConfigParser;
import com.devpilot.ai.cicd.parser.PipelineConfigValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PipelineConfigParserTest {

    private PipelineConfigParser parser;

    @BeforeEach
    void setUp() {
        PipelineConfigValidator validator = new PipelineConfigValidator();
        parser = new PipelineConfigParser(validator);
    }

    @Test
    @DisplayName("Should successfully parse valid devpilot-ci.yml configuration")
    void testParseValidYaml() {
        String yaml = """
                name: Demo App
                triggers:
                  push: true
                  pull_request: true
                steps:
                  - name: Build Code
                    type: BUILD
                  - name: Run Tests
                    type: TEST
                artifact:
                  enabled: true
                """;

        PipelineConfig config = parser.parseAndValidate(yaml);
        assertNotNull(config);
        assertEquals("Demo App", config.getName());
        assertEquals(2, config.getSteps().size());
        assertEquals("BUILD", config.getSteps().get(0).getType());
    }

    @Test
    @DisplayName("Should reject configuration with unsupported step type")
    void testParseUnsupportedStepType() {
        String yaml = """
                name: Malicious Pipeline
                steps:
                  - name: Run Shell Command
                    type: EXECUTE_SHELL
                """;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parser.parseAndValidate(yaml));
        assertTrue(ex.getMessage().contains("EXECUTE_SHELL"));
    }

    @Test
    @DisplayName("Should reject dangerous shell injection patterns")
    void testParseDangerousShellInjection() {
        String yaml = """
                name: Injection Pipeline
                steps:
                  - name: Attack
                    type: BUILD
                # system('/bin/sh -c rm -rf /')
                """;

        assertThrows(IllegalArgumentException.class, () -> parser.parseAndValidate(yaml));
    }
}
