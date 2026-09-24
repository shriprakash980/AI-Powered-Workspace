package com.devpilot.ai.cicd.parser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PipelineConfig {

    private String name;
    private Triggers triggers = new Triggers();
    private Environment environment = new Environment();
    private List<StepConfig> steps = new ArrayList<>();
    private ArtifactConfig artifact = new ArtifactConfig();
    private DeploymentConfig deployment = new DeploymentConfig();

    public PipelineConfig() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Triggers getTriggers() { return triggers; }
    public void setTriggers(Triggers triggers) { this.triggers = triggers; }

    public Environment getEnvironment() { return environment; }
    public void setEnvironment(Environment environment) { this.environment = environment; }

    public List<StepConfig> getSteps() { return steps; }
    public void setSteps(List<StepConfig> steps) { this.steps = steps; }

    public ArtifactConfig getArtifact() { return artifact; }
    public void setArtifact(ArtifactConfig artifact) { this.artifact = artifact; }

    public DeploymentConfig getDeployment() { return deployment; }
    public void setDeployment(DeploymentConfig deployment) { this.deployment = deployment; }

    public static class Triggers {
        private boolean push = true;
        private boolean pull_request = true;
        private boolean manual = true;

        public boolean isPush() { return push; }
        public void setPush(boolean push) { this.push = push; }

        public boolean isPull_request() { return pull_request; }
        public void setPull_request(boolean pull_request) { this.pull_request = pull_request; }

        public boolean isManual() { return manual; }
        public void setManual(boolean manual) { this.manual = manual; }
    }

    public static class Environment {
        private String java;
        private String node;
        private String python;

        public String getJava() { return java; }
        public void setJava(String java) { this.java = java; }

        public String getNode() { return node; }
        public void setNode(String node) { this.node = node; }

        public String getPython() { return python; }
        public void setPython(String python) { this.python = python; }
    }

    public static class StepConfig {
        private String name;
        private String type;

        public StepConfig() {}

        public StepConfig(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class ArtifactConfig {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class DeploymentConfig {
        private boolean enabled = false;
        private String environment = "development";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
    }
}
