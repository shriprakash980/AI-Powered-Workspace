package com.devpilot.ai.cicd.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Component;

@Component
public class PipelineConfigParser {

    private final ObjectMapper yamlMapper;
    private final PipelineConfigValidator validator;

    public PipelineConfigParser(PipelineConfigValidator validator) {
        this.validator = validator;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.yamlMapper.findAndRegisterModules();
    }

    public PipelineConfig parseAndValidate(String rawYaml) {
        validator.validateRawYaml(rawYaml);
        try {
            PipelineConfig config = yamlMapper.readValue(rawYaml, PipelineConfig.class);
            validator.validateConfig(config);
            return config;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse pipeline configuration YAML: " + e.getMessage(), e);
        }
    }
}
